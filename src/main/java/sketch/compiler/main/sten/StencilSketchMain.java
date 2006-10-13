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
import streamit.frontend.passes.ExprArrayToArrayRange;
import streamit.frontend.passes.GenerateCopies;
import streamit.frontend.passes.VariableDisambiguator;
import streamit.frontend.stencilSK.EliminateCompoundAssignments;
import streamit.frontend.stencilSK.EliminateStarStatic;
import streamit.frontend.stencilSK.FunctionalizeStencils;
import streamit.frontend.stencilSK.SimpleCodePrinter;
import streamit.frontend.stencilSK.StaticHoleTracker;
import streamit.frontend.stencilSK.StencilSemanticChecker;
import streamit.frontend.tosbit.ValueOracle;

/**
 * Convert StreamIt programs to legal Java code.  This is the main
 * entry point for the StreamIt syntax converter.  Running it as
 * a standalone program reads the list of files provided on the
 * command line and produces equivalent Java code on standard
 * output or the file named in the <tt>--output</tt> command-line
 * parameter.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class ToStencilSK extends ToSBit
{

	Program originalProg;
	ToStencilSK(CommandLineParams params){
		super(params);
	}
	
    public void run()
    {
    	if (params.printHelp)
        {
            printUsage();
            return;
        }
        
        parseProgram();
        originalProg = prog;
        // RenameBitVars is buggy!! prog = (Program)prog.accept(new RenameBitVars());
        prog = (Program)prog.accept(new VariableDisambiguator());
        if (!StencilSemanticChecker.check(prog))
            throw new IllegalStateException("Semantic check failed");
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
        prog.accept(new SimpleCodePrinter());
        System.out.println("DONE!");
        
        oracle = new ValueOracle( new StaticHoleTracker(varGen) );
        partialEvalAndSolve();
        eliminateStar();
        finalCode.accept(new SimpleCodePrinter());
        System.out.print("DONE");
    }
    
    
    public void eliminateStar(){
   	 finalCode =
            (Program) originalProg.accept (
                new EliminateStarStatic(oracle));      
   }
    
    public static void main(String[] args)
    {
        new ToStencilSK(new CommandLineParams(args)).run();
        System.exit(0);
    }
}

