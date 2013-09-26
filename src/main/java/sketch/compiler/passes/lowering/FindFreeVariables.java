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
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.typs.Type;
import sketch.util.exceptions.UnrecognizedVariableException;

/**
 * Front-end visitor pass to find free variables in anonymous streams.
 * The StreamIt language spec allows anonymous streams to use
 * compile-time constants from the enclosing code, with this typically
 * being induction variables and stream parameters.  In the Java code,
 * though, this is only allowed for variables declared final, or
 * fields of final objects.  Implementation-wise, this means that the
 * output Java code must contain a final wrapper variable when a free
 * variable in an anonymous stream (that is, one without a declaration
 * inside the anonymous stream) corresponds to a local.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class FindFreeVariables extends SymbolTableVisitor
{
    List freeVars;

    public FindFreeVariables()
    {
        super(null);
        freeVars = null;
    }

    public Object visitExprVar(ExprVar expr)
    {
        Object result = super.visitExprVar(expr);
        if (!(symtab.hasVar(expr.getName()))){
        	System.err.println("Can't find variable " + expr);
            freeVars.add(expr.getName());
        }
        return result;
    }

    public Object visitPackage(Package spec)
    {
        // Skip all of this if the spec is named.
        if (spec.getName() != null)
            return super.visitPackage(spec);

        List oldFreeVars = freeVars;
        freeVars = new java.util.ArrayList();
        // Wrap this in an empty symbol table.
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(null);
        Object result = super.visitPackage(spec);
        symtab = oldSymTab;
        for (Iterator iter = freeVars.iterator(); iter.hasNext(); )
        {
            String name = (String)iter.next();
            // Is the variable free here, too?
            if (!symtab.hasVar(name))
                oldFreeVars.add(name);
            // Look up the variable in the symbol table; only print
            // if it's a local.
            else if (symtab.lookupKind(name, spec) == SymbolTable.KIND_LOCAL)
            {
                final String was = name;
                final String wrapped = "_final_" + name;
                Type type = symtab.lookupVar(name, spec);
                result = ((FENode)result).accept
                    (new SymbolTableVisitor(new SymbolTable(null)) {
                        public Object visitExprVar(ExprVar expr)
                        {
                            Object result = super.visitExprVar(expr);
                            try
                            {
                                    symtab.lookupVar(expr.getName(), expr);
                            }
                            catch (UnrecognizedVariableException e)
                            {
                                if (expr.getName().equals(was))
                                    return new ExprVar(expr,
                                                       wrapped);
                                // else fall through
                            }
                            return result;
                        }
                    });
                // Also insert a statement for that variable and add
                // it to the local symbol table.  addStatement will
                // add the statement *before* the one that includes this
                // StreamSpec, so we're set.  But only do this if
                // we haven't already; specifically, if symtab doesn't
                // contain the wrapped variable.
                if (!(symtab.hasVar(wrapped)))
                {
                    FENode context = ((FENode)result);
                    addVarDecl(context, type, wrapped);
                    addStatement(new StmtAssign(new ExprVar(context, wrapped),
                                                new ExprVar(context, name)));
                }
            }
        }
        freeVars = oldFreeVars;
        return result;
    }
}
