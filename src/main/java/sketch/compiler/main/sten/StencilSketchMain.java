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
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.passes.AssignLoopTypes;
import streamit.frontend.passes.BackendCleanup;
import streamit.frontend.passes.ExprArrayToArrayRange;
import streamit.frontend.passes.GenerateCopies;
import streamit.frontend.passes.VariableDisambiguator;
import streamit.frontend.stencilSK.EliminateCompoundAssignments;
import streamit.frontend.stencilSK.EliminateStarStatic;
import streamit.frontend.stencilSK.FunctionalizeStencils;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.stencilSK.StencilSemanticChecker;
import streamit.frontend.tosbit.ValueOracle;

/**
 * This class manages all the work involed in compiling a stencil 
 * sketch into C (or Fortran) code.
 * 
 * @author asolar
 */
public class ToStencilSK extends ToSBit
{

	Program originalProg;
	ToStencilSK(CommandLineParams params){
		super(params);
	}
	
    protected Program preprocessProgram(Program prog) {
        prog = super.preprocessProgram(prog);
        prog = (Program)prog.accept(new VariableDisambiguator());
        return prog;
    }
	
    public void run()
    {
    	if (params.printHelp)
        {
            printUsage();
            return;
        }
        
        parseProgram();       // parse
        originalProg = prog;  // save
        prog=preprocessProgram(prog); // perform prereq transformations

        //run semantic checker
        if (!StencilSemanticChecker.check(prog))
            throw new IllegalStateException("Semantic check failed");

        //is this necessary?
        prog = (Program)prog.accept(new AssignLoopTypes());
        if (prog == null)
            throw new IllegalStateException();
        
        TempVarGen varGen = new TempVarGen();
        prog = (Program) prog.accept( new GenerateCopies(varGen) );  
        prog = (Program) prog.accept( new ExprArrayToArrayRange());
//        prog.accept(new SimpleCodePrinter());
        System.out.println("Before preprocessing.");
        
        prog = (Program)prog.accept(new EliminateCompoundAssignments());
        FunctionalizeStencils fs = new FunctionalizeStencils();
        
        prog = (Program)prog.accept(fs); //convert Function's to ArrFunction's
        prog = fs.processFuns(prog); //process the ArrFunction's and create new Function's
        //fs.printFuns();
//        prog.accept(new SimpleCodePrinter());
        
        oracle = new ValueOracle( new StaticHoleTracker(varGen) );
        partialEvalAndSolve();
        eliminateStar();
//        finalCode.accept(new SimpleCodePrinter());
        generateCode();
        System.out.print("DONE");
    }

	public void eliminateStar(){
		finalCode=(Program)originalProg.accept(new EliminateStarStatic(oracle));
	}

	
	protected Program doBackendPasses(Program prog) {
    	prog=super.doBackendPasses(prog);
    	prog=(Program) prog.accept(new BackendCleanup());
    	return prog;
	}

	public void generateCode(){
		finalCode=doBackendPasses(finalCode);
		if(!params.outputFortran) {
			outputCCode();
		} else {
			//TODO
		}
	}
	
	public static void main(String[] args)
	{
		new ToStencilSK(new CommandLineParams(args)).run();
		System.exit(0);
	}
}

