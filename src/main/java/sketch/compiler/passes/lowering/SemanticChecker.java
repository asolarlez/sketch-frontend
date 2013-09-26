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

package sketch.compiler.passes.lowering;
import java.util.Stack;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtReorderBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.util.exceptions.ExceptionAtNode;
import sketch.util.exceptions.UnrecognizedVariableException;

import static sketch.util.DebugOut.printError;

/**
 * Perform checks on the semantic correctness of a StreamIt program.
 * The main entry point to this class is the static
 * <code>sketch.compiler.passes.SemanticChecker.check</code> method,
 * which returns <code>true</code> if a program has no detected
 * semantic errors.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class SemanticChecker
{
	/**
	 * Check a SKETCH/PSKETCH program for semantic correctness.  This
	 * returns <code>false</code> and prints diagnostics to standard
	 * error if errors are detected.
	 *
	 * @param prog  parsed program object to check
	 * @returns     <code>true</code> if no errors are detected
	 */
    public static boolean check(Program prog, boolean isParseCheck) {
        return check(prog, ParallelCheckOption.SERIAL, isParseCheck);
	}

    public enum ParallelCheckOption { PARALLEL, SERIAL, DONTCARE; }

	/**
	 * Check a SKETCH/PSKETCH program for semantic correctness.  This
	 * returns <code>false</code> and prints diagnostics to standard
	 * error if errors are detected.
	 *
	 * @param prog  parsed program object to check
	 * @param parallel  are parallel constructs allowed?
	 * @returns     <code>true</code> if no errors are detected
	 */
    public static boolean check(Program prog, ParallelCheckOption parallel,
            boolean isParseCheck)
	{
        SemanticChecker checker = new SemanticChecker(isParseCheck);


        try {
            // checker.checkStatementPlacement(prog);
            switch (parallel) {
                case PARALLEL:
                    checker.checkParallelConstructs(prog);
                case SERIAL:
                    checker.banParallelConstructs(prog);
            }
		} catch (UnrecognizedVariableException uve) {
            if (checker.good) {
                throw uve;
            }
			// Don't care about this exception during type checking
            // assert !checker.good;
		}

		return checker.good;
	}

	// true if we haven't found any errors
	protected boolean good;
    protected final boolean isParseCheck;

	protected void report(FENode node, String message)
	{
        if (!isParseCheck) {
            message = "INTERNAL ERROR " + message;
        }
        (new ExceptionAtNode(message, node)).printNoStacktrace();
        good = false;
	}

	protected void report(FEContext ctx, String message)
	{
		good = false;
        printError(ctx + ":", message);
	}

	/** Report incompatible alternative field selections. */
	protected void report (ExprChoiceSelect exp, Type t1, Type t2,
			String f1, String f2) {
		report (exp, "incompatible types '"+ t1 +"', '"+ t2 +"'"
				+" in alternative selections '"+ f1 +"', '"+ f2 +"'");
	}

    public SemanticChecker(boolean isParseCheck)
	{
        this.isParseCheck = isParseCheck;
        good = true;
	}





	/**
	 * Checks that statements exist in valid contexts for the type of
	 * statement.  This checks that add, split, join, loop, and body
	 * statements are only in appropriate initialization code, and
	 * that push, pop, peek, and keep statements are only in
	 * appropriate work function code.
	 *
	 * @param prog  parsed program object to check
	 */
	public void checkStatementPlacement(Program prog)
	{
		//System.out.println("checkStatementPlacement");

		// This is easiest to implement as a visitor, since otherwise
		// we'll end up trying to recursively call ourselves if there's
		// an anonymous stream declaration.  Ick.
		prog.accept(new FEReplacer() {
			// Track the current streamspec and function:
			private Package spec = null;
			private Function func = null;


			public Object visitStmtFor(StmtFor stmt)
			{
				// check the condition

				if( stmt.getInit() == null){
					report(stmt, "For loops without initializer not supported." );
				}
				return super.visitStmtFor(stmt);
			}

			public Object visitPackage(Package ss)
			{
				//System.out.println("checkStatementPlacement::visitStreamSpec");

				//9assert false;

				Package oldSpec = spec;
				spec = ss;
				Object result = super.visitPackage(ss);
				spec = oldSpec;
				return result;
			}

			public Object visitFunction(Function func2)
			{
				Function oldFunc = func;
				func = func2;
				Object result = super.visitFunction(func2);
				func = oldFunc;
				return result;
			}
		});
	}



	

	/** Trigger a semantic error if parallel constructs are used. */
	public void banParallelConstructs (Program prog) {
		prog.accept(new SymbolTableVisitor(null) {
			public Object visitExprFunCall (ExprFunCall fc) {
				String name = fc.getName ();
				if ("lock".equals (name) || "unlock".equals (name))
					report (fc, "sorry, locking not allowed in sequential code");
				return fc;
			}

			public Object visitStmtAtomicBlock (StmtAtomicBlock sab) {
				report (sab, "sorry, atomics not allowed in sequential code");
				return sab;
			}

			public Object visitStmtFork (StmtFork sf) {
				report (sf, "sorry, forking not allowed in sequential code");
				return sf;
			}
		});
	}

	public void checkParallelConstructs (Program prog) {
		prog.accept (new SymbolTableVisitor(null) {
			private Stack<StmtAtomicBlock> atomics = new Stack<StmtAtomicBlock>();

			@Override
			public Object visitStmtAtomicBlock(StmtAtomicBlock stmt) {
				if(stmt.isCond() && atomics.size() > 0)
					report(stmt, "Conditional atomics not allowed inside other atomics");
				atomics.push(stmt);

				if (stmt.isCond ()) {
					Expression cond = stmt.getCond ();
					if (!ExprTools.isSideEffectFree (cond))
						report (cond, "conditions of conditional atomics must be side-effect free");
					else if (1 < ExprTools.numGlobalReads (cond, symtab))
						report (cond, "conditions of conditional atomics can read at most one global variable");
				}

				Object o = super.visitStmtAtomicBlock(stmt);
				StmtAtomicBlock sab = atomics.pop();
				assert sab == stmt : "This is strange";
				return o;

			}

			private int nForks = 0;
			public Object visitStmtFork (StmtFork stmt) {
				if (nForks > 0)
					report (stmt, "sorry, nested 'fork' blocks are not currently supported");
				++nForks;
				Object rv = super.visitStmtFork (stmt);
				--nForks;
				return rv;
			}

			public Object visitStmtReorderBlock (StmtReorderBlock stmt) {
				for (Statement s : stmt.getStmts ())
					if (s instanceof StmtVarDecl)
						report (s, "variable declarations make no sense in a 'reorder' block");
				return super.visitStmtReorderBlock (stmt);
			}
		});
	}

}

