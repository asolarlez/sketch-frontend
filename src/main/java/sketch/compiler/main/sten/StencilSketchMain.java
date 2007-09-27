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

package streamit.frontend;
import java.io.FileWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import streamit.frontend.codegenerators.SNodesToC;
import streamit.frontend.codegenerators.SNodesToFortran;
import streamit.frontend.experimental.DataflowWithFixpoint;
import streamit.frontend.experimental.deadCodeElimination.EliminateDeadCode;
import streamit.frontend.experimental.eliminateTransAssign.EliminateTransitiveAssignments;
import streamit.frontend.experimental.nodesToSB.IntVtype;
import streamit.frontend.experimental.preprocessor.FlattenStmtBlocks;
import streamit.frontend.experimental.preprocessor.PreprocessSketch;
import streamit.frontend.experimental.preprocessor.PropagateFinals;
import streamit.frontend.experimental.preprocessor.SimplifyVarNames;
import streamit.frontend.experimental.simplifier.ScalarizeVectorAssignments;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.passes.BackendCleanup;
import streamit.frontend.passes.SeparateInitializers;
import streamit.frontend.passes.VariableDeclarationMover;
import streamit.frontend.passes.VariableDisambiguator;
import streamit.frontend.stencilSK.EliminateCompoundAssignments;
import streamit.frontend.stencilSK.EliminateStarStatic;
import streamit.frontend.stencilSK.FunctionalizeStencils;
import streamit.frontend.stencilSK.MatchParamNames;
import streamit.frontend.stencilSK.SimpleCodePrinter;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.stencilSK.StencilSemanticChecker;
import streamit.frontend.stencilSK.preprocessor.ReplaceFloatsWithBits;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tosbit.recursionCtrl.AdvancedRControl;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

/**
 * This class manages all the work involed in compiling a stencil 
 * sketch into C (or Fortran) code.
 * 
 * @author asolar
 */
public class ToStencilSK extends ToSBit
{

	Program originalProg;
	ToStencilSK(String[] args){
		super(args);
	}
	
	
	
    protected Program preprocessProgram(Program prog) {    	
        prog = super.preprocessProgram(prog);
        originalProg = prog;    	
    	System.out.println("=============================================================");    	
    	prog = (Program)prog.accept(new FlattenStmtBlocks());    	
    	prog= (Program)prog.accept(new EliminateTransitiveAssignments());
    	prog= (Program)prog.accept(new PropagateFinals());    	
    	//System.out.println("=========  After ElimTransAssign  =========");
    	prog = (Program)prog.accept(new EliminateDeadCode());
    	System.out.println("=============================================================");
    	prog.accept( new SimpleCodePrinter() );
    	
        prog = (Program) prog.accept(new ReplaceFloatsWithBits());
        //prog = (Program)prog.accept(new VariableDisambiguator());
        System.out.println(" After preprocessing level 1. ");
        prog = (Program) prog.accept(new MatchParamNames());
        System.out.println(" After mpn ");             
        return prog;
    }
	
	
	
    public RecursionControl newRControl(){
    	// return new DelayedInlineRControl(params.inlineAmt, params.branchingFactor);
    	return new AdvancedRControl(params.flagValue("branchamnt"), params.flagValue("inlineamnt"), prog); 
    }
    
    public void run()
    {    	
        
        parseProgram();       // parse
        
        //run semantic checker
        if (!StencilSemanticChecker.check(prog))
            throw new IllegalStateException("Semantic check failed");
                
        
        prog=preprocessProgram(prog); // perform prereq transformations        

        
        if (prog == null)
            throw new IllegalStateException();
        
        TempVarGen varGen = new TempVarGen();
        prog = (Program)prog.accept(new SeparateInitializers());
        prog = (Program) prog.accept( new ScalarizeVectorAssignments(varGen, true) );  

        System.out.println("After SVA.");
        
       //System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
       //prog.accept(new SimpleCodePrinter());
       //System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        System.out.println("Before preprocessing.");
        
        prog = (Program)prog.accept(new EliminateCompoundAssignments());
        
//        prog.accept(new SimpleCodePrinter());
        
        FunctionalizeStencils fs = new FunctionalizeStencils();
        
        prog = (Program)prog.accept(fs); //convert Function's to ArrFunction's
        prog = fs.processFuns(prog); //process the ArrFunction's and create new Function's
        //fs.printFuns();
        System.out.println("After running transformation.");
        
       
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
    			new DataflowWithFixpoint(new IntVtype(), varGen, true,  params.flagValue("unrollamnt"), newRControl() ){
    				protected List<Function> functionsToAnalyze(StreamSpec spec){
    				    return new LinkedList<Function>(spec.getFuncs());
    			    }
    				public String transName(String name){
    					return state.transName(name);
    				}
    			});    	
        //Program tmp = (Program) prog.accept( new PreprocessSketch(varGen, params.unrollAmt, newRControl()));
        tmp = (Program)tmp.accept(new FlattenStmtBlocks());
    	tmp = (Program)tmp.accept(new EliminateTransitiveAssignments());
    	//System.out.println("=========  After ElimTransAssign  =========");
    	tmp = (Program)tmp.accept(new EliminateDeadCode());
    	//System.out.println("=========  After ElimDeadCode  =========");
    	tmp = (Program)tmp.accept(new SimplifyVarNames());    	
        tmp.accept(new SimpleCodePrinter());
        
        prog = tmp;
        
        
        oracle = new ValueOracle( new StaticHoleTracker(varGen) );
        partialEvalAndSolve();
        eliminateStar();
//        finalCode.accept(new SimpleCodePrinter());
        generateCode();
        System.out.print("DONE");
    }

	public void eliminateStar(){
		finalCode=(Program)originalProg.accept(new EliminateStarStatic(oracle));
		
		finalCode=(Program)finalCode.accept(new PreprocessSketch( varGen,  params.flagValue("unrollamnt"), newRControl() ));
    	//finalCode.accept( new SimpleCodePrinter() );
    	finalCode = (Program)finalCode.accept(new FlattenStmtBlocks());
    	finalCode = (Program)finalCode.accept(new EliminateTransitiveAssignments());
    	//System.out.println("=========  After ElimTransAssign  =========");
    	//finalCode.accept( new SimpleCodePrinter() );
    	finalCode = (Program)finalCode.accept(new EliminateDeadCode());
    	//System.out.println("=========  After ElimDeadCode  =========");
    	//finalCode.accept( new SimpleCodePrinter() );
    	finalCode = (Program)finalCode.accept(new SimplifyVarNames());
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
	
	public static void main(String[] args)
	{
		new ToStencilSK(args).run();
		System.exit(0);
	}
}

