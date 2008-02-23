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

package streamit.frontend.tojava;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;

/**
 * Inserts statements in init functions to call member object constructors.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class InsertInitConstructors extends InitMunger
{
    private TempVarGen varGen;

    /**
     * Create a new pass to insert constructors.
     *
     * @param varGen  global object to generate variable names
     */
    public InsertInitConstructors(TempVarGen varGen)
    {
        this.varGen = varGen;
    }

    /**
     * Returns true if this type needs a constructor generated.
     * This happens if the type is complex, or if it is not a
     * primitive type.  (Complex primitive types use the Java
     * 'Complex' class.)
     */
    private static boolean needsConstructor(Type type)
    {
        return type.isComplex() || !(type instanceof TypePrimitive);
    }

    /**
     * Return an ordered list of all of the constructors that need to
     * be generated to initialize a particular variable.
     *
     * @param ctx   file name and line number for created statements
     * @param name  base name expression
     * @param type  type of object to create constructors for
     * @param arrayConstructor if false, only recurse, don't actually
     *              generate a constructor for name if it is an array
     *              type; useful for members of multidimensional
     *              arrays
     */
    private List stmtsForConstructor(FENode ctx, Expression name,
                                     Type type, boolean arrayConstructor)
    {
        List result = new java.util.ArrayList();

        // If the type doesn't involve a constructor, there are no
        // generated statements.
        if (!needsConstructor(type))
            return result;

        // No; generate the constructor.
        if (arrayConstructor || !(type instanceof TypeArray))
            result.add(new StmtAssign(name,
                                      new ExprJavaConstructor(ctx, type)));

        // Now, if this is a structure type, we might need to
        // recursively generate constructors for the structure
        // members.
        if (type instanceof TypeStruct)
        {
            TypeStruct ts = (TypeStruct)type;
            for (int i = 0; i < ts.getNumFields(); i++)
            {
                String fname = ts.getField(i);
                Type ftype = ts.getType(fname);
                if (needsConstructor(ftype))
                {
                    // Construct the new left-hand side:
                    Expression lhs = new ExprField(ctx, name, fname);
                    // Get child constructors and add them:
                    result.addAll(stmtsForConstructor(ctx, lhs, ftype, true));
                }
            }
        }
        // Or, if this is an array of structures, we might need to
        // recursively generate constructors.
        if (type instanceof TypeArray)
        {
            TypeArray ta = (TypeArray)type;
            Type base = ta.getBase();
            if (needsConstructor(base))
            {
                // The length might be non-constant.  This means that
                // we need to do this by looping through the array.
                String tempVar = varGen.nextVar();
                Expression varExp = new ExprVar(ctx, tempVar);
                Statement decl =
                    new StmtVarDecl(ctx,
                    				TypePrimitive.inttype,
                                    tempVar,
                                    new ExprConstInt(ctx, 0));
                Expression cond =
                    new ExprBinary(varExp,
                                   ExprBinary.BINOP_LT,
                                   varExp,
                                   ta.getLength());
                Statement incr =
                    new StmtExpr(ctx,
                                 new ExprUnary(ctx,
                                               ExprUnary.UNOP_POSTINC,
                                               varExp));
                Expression lhs = new ExprArrayRange(varExp, name, varExp);
                Statement body =
                    new StmtBlock(ctx,
                                  stmtsForConstructor(ctx, lhs, base, false));
                Statement loop =
                    new StmtFor(ctx, decl, cond, incr, body);
                result.add(loop);
            }
        }

        return result;
    }

    /**
     * Return an ordered list of all of the constructors that need to
     * be generated to initialize a particular variable.
     *
     * @param ctx   file name and line number for created statements
     * @param name  base name expression
     * @param type  type of object to create constructors for
     * @param arrayConstructor if false, only recurse, don't actually
     *              generate a constructor for name if it is an array
     *              type; useful for members of multidimensional
     *              arrays
     * @deprecated
     */
/* TODO:    private List stmtsForConstructor(FEContext ctx, Expression name,
                                     Type type, boolean arrayConstructor)
    {
        List result = new java.util.ArrayList();

        // If the type doesn't involve a constructor, there are no
        // generated statements.
        if (!needsConstructor(type))
            return result;

        // No; generate the constructor.
        if (arrayConstructor || !(type instanceof TypeArray))
            result.add(new StmtAssign(ctx, name,
                                      new ExprJavaConstructor(ctx, type)));

        // Now, if this is a structure type, we might need to
        // recursively generate constructors for the structure
        // members.
        if (type instanceof TypeStruct)
        {
            TypeStruct ts = (TypeStruct)type;
            for (int i = 0; i < ts.getNumFields(); i++)
            {
                String fname = ts.getField(i);
                Type ftype = ts.getType(fname);
                if (needsConstructor(ftype))
                {
                    // Construct the new left-hand side:
                    Expression lhs = new ExprField(ctx, name, fname);
                    // Get child constructors and add them:
                    result.addAll(stmtsForConstructor(ctx, lhs, ftype, true));
                }
            }
        }
        // Or, if this is an array of structures, we might need to
        // recursively generate constructors.
        if (type instanceof TypeArray)
        {
            TypeArray ta = (TypeArray)type;
            Type base = ta.getBase();
            if (needsConstructor(base))
            {
                // The length might be non-constant.  This means that
                // we need to do this by looping through the array.
                String tempVar = varGen.nextVar();
                Expression varExp = new ExprVar(ctx, tempVar);
                Statement decl =
                    new StmtVarDecl(ctx,
                    				TypePrimitive.inttype,
                                    tempVar,
                                    new ExprConstInt(ctx, 0));
                Expression cond =
                    new ExprBinary(varExp,
                                   ExprBinary.BINOP_LT,
                                   varExp,
                                   ta.getLength());
                Statement incr =
                    new StmtExpr(ctx,
                                 new ExprUnary(ctx,
                                               ExprUnary.UNOP_POSTINC,
                                               varExp));
                Expression lhs = new ExprArrayRange(varExp, name, varExp);
                Statement body =
                    new StmtBlock(ctx,
                                  stmtsForConstructor(ctx, lhs, base, false));
                Statement loop =
                    new StmtFor(ctx, decl, cond, incr, body);
                result.add(loop);
            }
        }

        return result;
    }*/

    public Object visitStreamSpec(StreamSpec spec)
    {
        spec = (StreamSpec)super.visitStreamSpec(spec);

        // Stop if there are no fields.
        if (spec.getVars().isEmpty())
            return spec;

        List newStmts = new ArrayList();

        // Walk through the variables.  If any of them are for
        // complex or non-primitive types, generate a constructor.
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            FieldDecl field = (FieldDecl)iter.next();
            for (int i = 0; i < field.getNumFields(); i++)
            {
                Type type = field.getType(i);
                Object ob = field.getInit(i);
                if (needsConstructor(type) && ob == null)
                {
                    Expression lhs = new ExprVar(field, field.getName(i));
                    newStmts.addAll(stmtsForConstructor(field, lhs, type, true));
                }
            }
        }

        // Stop if there are no constructors to generate.
        if (newStmts.isEmpty())
            return spec;

        // Okay.  Prepend the new statements to the init function.
        List newFuncs = new ArrayList(spec.getFuncs());
        newFuncs = replaceInitWithPrepended(spec, newFuncs,
                                            newStmts);

        return new StreamSpec(spec, spec.getType(),
                              spec.getStreamType(), spec.getName(),
                              spec.getParams(), spec.getVars(),
                              newFuncs);
    }

    public Object visitStmtVarDecl(StmtVarDecl decl)
    {
        // Prepass: check all of the types in the declaration.
        // If none of them need constructors, don't actually
        // go through with this.  (So what?  This hack lets
        // the code work correctly even with a variable declaration
        // in the initializer part of a for loop, otherwise the
        // declaration gets pulled out of the loop and null replaces
        // the initializer.)
        boolean needed = false;
        for (int i = 0; i < decl.getNumVars(); i++)
            if (needsConstructor(decl.getType(i)))
                needed = true;
        if (!needed)
            return decl;

        // We're not actually going to modify this declaration, but
        // we may generate some additional statements (for constructors)
        // that go after it.
        addStatement(decl);

        // So, now go through the list of all the variables and add
        // constructors as needed:
        for (int i = 0; i < decl.getNumVars(); i++)
        {
            Type type = decl.getType(i);
            if (needsConstructor(type))
            {
                FENode ctx = decl;
                Expression lhs = new ExprVar(ctx, decl.getName(i));
                if( decl.getInit(i)== null)
                	addStatements(stmtsForConstructor(ctx, lhs, type, true));
            }
        }

        // We already added the statement, return null so there aren't
        // duplicate declarations.
        return null;
    }
}
