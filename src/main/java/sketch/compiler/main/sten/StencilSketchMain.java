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
import java.io.FileWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.CommandLineParamManager;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.codegenerators.SNodesToC;
import sketch.compiler.codegenerators.SNodesToFortran;
import sketch.compiler.dataflow.DataflowWithFixpoint;
import sketch.compiler.dataflow.deadCodeElimination.EliminateDeadCode;
import sketch.compiler.dataflow.eliminateTransAssign.EliminateTransAssns;
import sketch.compiler.dataflow.nodesToSB.IntVtype;
import sketch.compiler.dataflow.preprocessor.FlattenStmtBlocks;
import sketch.compiler.dataflow.preprocessor.PreprocessSketch;
import sketch.compiler.dataflow.preprocessor.PropagateFinals;
import sketch.compiler.dataflow.preprocessor.SimplifyVarNames;
import sketch.compiler.dataflow.preprocessor.TypeInferenceForStars;
import sketch.compiler.dataflow.recursionCtrl.AdvancedRControl;
import sketch.compiler.dataflow.recursionCtrl.DelayedInlineRControl;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.dataflow.simplifier.ScalarizeVectorAssignments;
import sketch.compiler.main.seq.SequentialSketchMain;
import sketch.compiler.passes.lowering.*;
import sketch.compiler.passes.printers.SimpleCodePrinter;
import sketch.compiler.solvers.constructs.StaticHoleTracker;
import sketch.compiler.solvers.constructs.ValueOracle;
import sketch.compiler.stencilSK.EliminateCompoundAssignments;
import sketch.compiler.stencilSK.EliminateStarStatic;
import sketch.compiler.stencilSK.FunctionalizeStencils;
import sketch.compiler.stencilSK.MatchParamNames;
import sketch.compiler.stencilSK.StencilSemanticChecker;
import sketch.compiler.stencilSK.preprocessor.ReplaceFloatsWithBits;

/**
 * This class manages all the work involed in compiling a stencil
 * sketch into C (or Fortran) code.
 *
 * @author asolar
 */
public class StencilSketchMain extends SequentialSketchMain
{

	Program originalProg;
	public StencilSketchMain(String[] args){
		super(args);
	}

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
		lprog = (Program) lprog.accept( new PreprocessSketch( varGen, params.flagValue("unrollamnt"), visibleRControl() ) );
		if(params.flagEquals("showphase", "preproc")) dump (prog, "After Preprocessing");
		prog = lprog;
        originalProg = prog;
//    	System.out.println("=============================================================");
    	prog = (Program)prog.accept(new FlattenStmtBlocks());
    	prog= (Program)prog.accept(new EliminateTransAssns());
    	prog= (Program)prog.accept(new PropagateFinals());
    	//System.out.println("=========  After ElimTransAssign  =========");
    	prog = (Program)prog.accept(new EliminateDeadCode(true));
//    	System.out.println("=============================================================");
//    	prog.accept( new SimpleCodePrinter() );

        prog = (Program) prog.accept(new ReplaceFloatsWithBits());
        //prog = (Program)prog.accept(new VariableDisambiguator());
//        System.out.println(" After preprocessing level 1. ");
        prog = (Program) prog.accept(new MatchParamNames());
//        System.out.println(" After mpn ");
        return prog;
    }

    @Override
    protected void backendParameters(List<String> commandLineOptions){
		super.backendParameters(commandLineOptions);
		commandLineOptions.add("-ufunSymmetry");
    }


    public RecursionControl visibleRControl(){
    	// return new DelayedInlineRControl(params.flagValue("inlineamnt"), params.flagValue("branchamnt"));
    	return new AdvancedRControl(params.flagValue("branchamnt"), params.flagValue("inlineamnt"), prog);
    }
    
    
    public RecursionControl internalRControl(){
    	return new DelayedInlineRControl(params.flagValue("inlineamnt"), params.flagValue("branchamnt"));		
	}
	
	

    public void run()
    {
    	parseProgram();       // parse
        preprocAndSemanticCheck();

        oracle = new ValueOracle( new StaticHoleTracker(varGen) );
        partialEvalAndSolve();
        eliminateStar();
//        finalCode.accept(new SimpleCodePrinter());
        generateCode();
        System.out.print("[STENCIL_SKETCH] DONE");
    }

	public void eliminateStar(){
		finalCode=(Program)originalProg.accept(new EliminateStarStatic(oracle));		
		finalCode=(Program)finalCode.accept(new PreprocessSketch( varGen,  params.flagValue("unrollamnt"), visibleRControl() ));
    	//finalCode.accept( new SimpleCodePrinter() );
    	finalCode = (Program)finalCode.accept(new FlattenStmtBlocks());
    	finalCode = (Program)finalCode.accept(new EliminateTransAssns());
    	//System.out.println("=========  After ElimTransAssign  =========");
    	//finalCode.accept( new SimpleCodePrinter() );
    	finalCode = (Program)finalCode.accept(new EliminateDeadCode(params.hasFlag("keepasserts")));
    	//System.out.println("=========  After ElimDeadCode  =========");
    	//finalCode.accept( new SimpleCodePrinter() );
    	finalCode = (Program)finalCode.accept(new SimplifyVarNames());
	}
	
	public void preprocAndSemanticCheck() {
        //run semantic checker
        if (!StencilSemanticChecker.check(prog))
            throw new IllegalStateException("Semantic check failed");


        prog=preprocessProgram(prog); // perform prereq transformations


        if (prog == null)
            throw new IllegalStateException();

        TempVarGen varGen = new TempVarGen();
        prog = (Program)prog.accept(new SeparateInitializers());
        prog = (Program) prog.accept( new ScalarizeVectorAssignments(varGen, true) );

//        System.out.println("After SVA.");

       //System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
       //prog.accept(new SimpleCodePrinter());
       //System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
//        System.out.println("Before preprocessing.");

        prog = (Program)prog.accept(new EliminateCompoundAssignments());

//        prog.accept(new SimpleCodePrinter());

        FunctionalizeStencils fs = new FunctionalizeStencils();

        prog = (Program)prog.accept(fs); //convert Function's to ArrFunction's

        prog = fs.processFuns(prog, varGen); //process the ArrFunction's and create new Function's
        //fs.printFuns();
//        System.out.println("After running transformation.");


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
    			new DataflowWithFixpoint(new IntVtype(), varGen, true,  params.flagValue("unrollamnt"), visibleRControl() ){
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
        
    	prog = tmp;
    	if(params.flagEquals("showphase", "preproc")){
    		dump(tmp, "After transformations");
    	}
	}


	protected Program doBackendPasses(Program prog) {
    	prog=(Program) prog.accept(new BackendCleanup());
    	return prog;
	}

    protected void outputCCode() {
        String resultFile = getOutputFileName();
        String ccode = (String)finalCode.accept(new SNodesToC(varGen,resultFile));
        if(!params.hasFlag("outputcode")){
        	System.out.println(ccode);
        }else{
        	try{
				Writer outWriter = new FileWriter(params.sValue("outputdir")+resultFile+".cpp");
				outWriter.write(ccode);
				outWriter.flush();
				outWriter.close();
            }
            catch (java.io.IOException e){
                throw new RuntimeException(e);
            }
        }
    }

    protected void outputFortranCode() {
        String resultFile = getOutputFileName();
		finalCode=(Program) finalCode.accept(new VariableDisambiguator());
		finalCode=(Program) finalCode.accept(new VariableDeclarationMover());
        String fcode = (String)finalCode.accept(new SNodesToFortran(resultFile));
        if(!params.hasFlag("outputcode")){
        	System.out.println(fcode);
        }else{
        	try{
				Writer outWriter = new FileWriter(params.sValue("outputdir")+resultFile+".f");
				outWriter.write(fcode);
				outWriter.flush();
				outWriter.close();
            }
            catch (java.io.IOException e){
                throw new RuntimeException(e);
            }
        }
    }

	public void generateCode(){
		finalCode.accept(new SimpleCodePrinter());
		finalCode=doBackendPasses(finalCode);
		if(false && params.hasFlag("outputfortran")) {
			outputFortranCode();
		} else {
			outputCCode();
		}
	}

    public static void main(String[] args) {
        CommandLineParamManager.reset_singleton();
        try {
            new StencilSketchMain(args).run();
        } catch (RuntimeException e) {
            System.err.println("[ERROR] [STENCIL_SKETCH] Failed with exception "
                    + e.getMessage());
            throw e;
        }
    }
}
