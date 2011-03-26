/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package sketch.compiler.main.sten;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.cmdline.SMTOptions.IntModel;
import sketch.compiler.dataflow.DataflowWithFixpoint;
import sketch.compiler.dataflow.deadCodeElimination.EliminateDeadCode;
import sketch.compiler.dataflow.eliminateTransAssign.EliminateTransAssns;
import sketch.compiler.dataflow.nodesToSB.IntVtype;
import sketch.compiler.dataflow.preprocessor.FlattenStmtBlocks;
import sketch.compiler.dataflow.preprocessor.PreprocessSketch;
import sketch.compiler.dataflow.preprocessor.PropagateFinals;
import sketch.compiler.dataflow.preprocessor.SimplifyVarNames;
import sketch.compiler.dataflow.preprocessor.TypeInferenceForStars;
import sketch.compiler.dataflow.simplifier.ScalarizeVectorAssignments;
import sketch.compiler.main.seq.SequentialSMTSketchMain;
import sketch.compiler.passes.lowering.AssembleInitializers;
import sketch.compiler.passes.lowering.BlockifyRewriteableStmts;
import sketch.compiler.passes.lowering.DisambiguateUnaries;
import sketch.compiler.passes.lowering.EliminateReorderBlocks;
import sketch.compiler.passes.lowering.SeparateInitializers;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.smt.ProduceSMTCode;
import sketch.compiler.smt.ProduceSMTStencilCode;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.passes.AddWrapper;
import sketch.compiler.smt.passes.CollectInputArrayStat;
import sketch.compiler.smt.passes.EliminateRegens;
import sketch.compiler.smt.passes.FunctionParamExtension;
import sketch.compiler.smt.passes.RegularizeTypesByTypeCheck;
import sketch.compiler.stencilSK.EliminateCompoundAssignments;
import sketch.compiler.stencilSK.FunctionalizeStencils;
import sketch.compiler.stencilSK.MatchParamNames;
import sketch.compiler.stencilSK.preprocessor.ReplaceFloatsWithBits;

/**
 * Convert StreamIt programs to legal Java code. This is the main entry point
 * for the StreamIt syntax converter. Running it as a standalone program reads
 * the list of files provided on the command line and produces equivalent Java
 * code on standard output or the file named in the <tt>--output</tt>
 * command-line parameter.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id: StencilSmtSketchMain.java,v 1.16 2009/10/23 18:42:54 lshan Exp $
 */
public class StencilSmtSketchMain extends SequentialSMTSketchMain {
	
	Program originalProg;
	
	private static Logger log = Logger.getLogger(SequentialSMTSketchMain.class.getCanonicalName());
	protected Map<String, Integer> numGridAccesses;
	
	/*
	 * Getters & Setters
	 */


	/*
	 * Constructors
	 */
	public StencilSmtSketchMain(String[] args) {
		super(args);
	}

	@Override
	protected Program preprocessProgram(Program prog) {
    	Program lprog = prog;
    	lprog = (Program)lprog.accept(new SeparateInitializers ());
    	lprog = (Program)lprog.accept(new BlockifyRewriteableStmts ());
		lprog = (Program)lprog.accept(new EliminateRegens(varGen));
    	lprog = (Program)lprog.accept(new EliminateReorderBlocks(varGen));
    	lprog = (Program)lprog.accept(new AssembleInitializers());
		lprog = (Program)lprog.accept(new FunctionParamExtension(true));
		//dump (lprog, "fpe:");
		lprog = (Program)lprog.accept(new DisambiguateUnaries(varGen));
		lprog = (Program)lprog.accept(new TypeInferenceForStars());

		//dump (prog, "After first elimination of multi-dim arrays:");
        lprog = (Program) lprog.accept(new PreprocessSketch(varGen,
                        options.bndOpts.unrollAmnt, visibleRControl()));
        if (showPhaseOpt("preproc"))
            dump(prog, "After Preprocessing");
		prog = lprog;
        originalProg = prog;
    	
    	prog = (Program)prog.accept(new FlattenStmtBlocks());
    	prog= (Program)prog.accept(new EliminateTransAssns());
    	prog= (Program)prog.accept(new PropagateFinals());
    	prog = (Program)prog.accept(new EliminateDeadCode(true));

        prog = (Program) prog.accept(new ReplaceFloatsWithBits(varGen));
        //prog = (Program)prog.accept(new VariableDisambiguator());
        
        prog = (Program) prog.accept(new MatchParamNames());
        
        return prog;
    }
	

	@Override
	public Program lowering(Program prog) {
		TempVarGen varGen = new TempVarGen();
		super.beforeUnvectorizing = prog;
        prog = (Program)prog.accept(new SeparateInitializers());
        prog = (Program) prog.accept( new ScalarizeVectorAssignments(varGen, true) );

//        System.out.println("After SVA.");

       //System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
       //prog.accept(new SimpleCodePrinter());
       //System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        

        prog = (Program)prog.accept(new EliminateCompoundAssignments());
//      dump(prog, "After EliminateCompoundAssignments");

        
        FunctionalizeStencils fs = new FunctionalizeStencils(varGen);
        
        prog = (Program)prog.accept(fs); //convert Function's to ArrFunction's
        prog = fs.processFuns(prog, varGen); //process the ArrFunction's and create new Function's
//        dump(prog, "After functionalize");
        
        CollectInputArrayStat pe = new CollectInputArrayStat(fs.getGlobalInVars(),
				varGen, false, options.bndOpts.unrollAmnt, visibleRControl(prog));
        prog.accept(pe);
        
        numGridAccesses = pe.getNumGridAccesses();
//        prog = (Program) prog.accept(new ModelGridWithUFun(numGridAccesses, varGen));
//        dump(prog, "After Modeling");
        
        
        //fs.printFuns();

        /*
        Program tmp = (Program)prog.accept(new FlattenStmtBlocks());
    	tmp = (Program)tmp.accept(new EliminateTransitiveAssignments());
    	//System.out.println("=========  After ElimTransAssign  =========");
    	tmp = (Program)tmp.accept(new EliminateDeadCode());
    	//System.out.println("=========  After ElimDeadCode  =========");
    	tmp = (Program)tmp.accept(new SimplifyVarNames());
        tmp.accept(new SimpleCodePrinter());
        */


    	Program tmp = (Program) prog.accept(
    			new DataflowWithFixpoint(new IntVtype(), varGen, true,  options.bndOpts.unrollAmnt, visibleRControl(prog) ){
    				protected List<Function> functionsToAnalyze(StreamSpec spec){
    				    return new LinkedList<Function>(spec.getFuncs());
    			    }
    				public String transName(String name){
    					return state.transName(name);
    				}
    			});
    	
    	
        //Program tmp = (Program) prog.accept( new PreprocessSketch(varGen, params.unrollAmt, newRControl()));
        tmp = (Program)tmp.accept(new FlattenStmtBlocks());
    	tmp = (Program)tmp.accept(new EliminateTransAssns());
    	//System.out.println("=========  After ElimTransAssign  =========");
    	tmp = (Program)tmp.accept(new EliminateDeadCode(true));
    	//System.out.println("=========  After ElimDeadCode  =========");
    	tmp = (Program)tmp.accept(new SimplifyVarNames());
    	  	
    	tmp = (Program) tmp.accept(new SeparateInitializers());
    	tmp = (Program) tmp.accept( new ScalarizeVectorAssignments(varGen, true) );
    	
    	if( showPhaseOpt("lowering")){
    		dump(tmp, "After transformations");
    	}
    	
//    	dump(tmp, "Before RegularizeTypes");
    	tmp = (Program) tmp.accept(new AddWrapper());
    	
//    	dump(tmp, "Before RegularizeTypes");
    	tmp = (Program) tmp.accept(new RegularizeTypesByTypeCheck());
//    	tmp = (Program) tmp.accept(new RegularizeTypes());
//    	dump(tmp, "After RegularizeTypes");
    	
        prog = tmp;
       
        return prog;
	}
	
	@Override
	protected ProduceSMTCode getPartialEvaluator(NodeToSmtVtype vtype) {
		ProduceSMTCode partialEval = new ProduceSMTStencilCode(vtype, numGridAccesses, varGen,
		        options.smtOpts.theoryOfArray,
		        options.smtOpts.intmodel == IntModel.bv,
				options.bndOpts.unrollAmnt,
				internalRControl(), options.debugOpts.trace);
		return partialEval;
	}
	
	/*
	 * Helper functions
	 */
	public static void dump(Program prog) {
		dump(prog, "");
	}

	public static void dump(Program prog, String message) {
		System.out
				.println("=============================================================");
		System.out.println("  ----- " + message + " -----");
		prog.accept(new SimpleCodePrinter());
		System.out
				.println("=============================================================");
	}

	public static void main(String[] args) {
		new StencilSmtSketchMain(args).run();
		System.exit(0);
	}

}
