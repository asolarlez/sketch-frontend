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
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import streamit.frontend.nodes.MakeBodiesBlocks;
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.passes.AssembleInitializers;
import streamit.frontend.passes.AssignLoopTypes;
import streamit.frontend.passes.BitTypeRemover;
import streamit.frontend.passes.BitVectorPreprocessor;
import streamit.frontend.passes.ConstantReplacer;
import streamit.frontend.passes.DisambiguateUnaries;
import streamit.frontend.passes.EliminateArrayRange;
import streamit.frontend.passes.ExprArrayToArrayRange;
import streamit.frontend.passes.ExtractRightShifts;
import streamit.frontend.passes.ExtractVectorsInCasts;
import streamit.frontend.passes.FindFreeVariables;
import streamit.frontend.passes.FunctionParamExtension;
import streamit.frontend.passes.GenerateCopies;
import streamit.frontend.passes.NoRefTypes;
import streamit.frontend.passes.NoticePhasedFilters;
import streamit.frontend.passes.SeparateInitializers;
import streamit.frontend.passes.TrimDumbDeadCode;
import streamit.frontend.passes.VariableDisambiguator;
import streamit.frontend.stencilSK.*;
import streamit.frontend.tojava.ComplexToStruct;
import streamit.frontend.tojava.DoComplexProp;
import streamit.frontend.tojava.EnqueueToFunction;
import streamit.frontend.tojava.InsertIODecls;
import streamit.frontend.tojava.MoveStreamParameters;
import streamit.frontend.tojava.NameAnonymousFunctions;
import streamit.frontend.tosbit.EliminateStar;
import streamit.frontend.tosbit.NodesToC;
import streamit.frontend.tosbit.NodesToCTest;
import streamit.frontend.tosbit.NodesToH;
import streamit.frontend.tosbit.ProduceBooleanFunctions;
import streamit.frontend.tosbit.SimplifyExpressions;
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
        

        partialEvalAndSolve();
        
    }
    
    
    public static void main(String[] args)
    {
        new ToStencilSK(new CommandLineParams(args)).run();
        System.exit(0);
    }
}

