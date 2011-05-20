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

package sketch.compiler.ast.core;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.typs.Type;
import sketch.util.exceptions.UnrecognizedVariableException;

/**
 * A symbol table for StreamIt programs.  This keeps a mapping from a
 * string name to a front-end type and an origin object, and has a
 * parent symbol table (possibly null).  A name can be registered in
 * the current symbol table.  When resolving a name's type, the name
 * is searched for first in the current symbol table, and if not
 * present than in the parent symbol table.
 *
 * <p>Each symbol may be associated with an <i>origin</i>.  This is
 * the object that initially defines the symbol, typically a
 * <code>StmtVarDecl</code> for local variables or filter fields or a
 * <code>Parameter</code> for stream parameters.  Each symbol also
 * has an integer kind to distinguish which of these it is.
 *
 * @see     sketch.compiler.passes.SymbolTableVisitor
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class SymbolTable
{
    /** Kind of a local variable. */
    public static final int KIND_LOCAL = 1;
    /** Kind of a filter field. */
    public static final int KIND_FIELD = 2;
    /** Kind of a stream parameter. */
    public static final int KIND_STREAM_PARAM = 3;
    /** Kind of a function parameter. */
    public static final int KIND_FUNC_PARAM = 4;

    private Map<String, VarInfo> vars;
    private Map<String, Function> fns;
    private SymbolTable parent;
    private List includedFns;

    private boolean makeShared = false;

    private static class VarInfo
    {
        public VarInfo(Type type, Object origin, int kind)
        {
            this.type = type;
            this.origin = origin;
            this.kind = kind;
        }
        public Type type;
        public Object origin;
        public int kind;
    }

    /** Creates a new symbol table with the specified parent (possibly
     * null). */
    public SymbolTable(SymbolTable parent)
    {
        vars = new HashMap<String, VarInfo>();
        fns = new HashMap<String, Function>();
        this.parent = parent;
        this.includedFns = null;
    }


    /** Creates a new symbol table with the specified parent (possibly
     * null). */
    public SymbolTable(SymbolTable parent, boolean makeShared)
    {
        vars = new HashMap<String, VarInfo>();
        fns = new HashMap<String, Function>();
        this.parent = parent;
        this.includedFns = null;
        this.makeShared = makeShared;
    }

    /**
     * This function will upgrade the type of a variable to a new
     * 
     * @param name
     * @param newType
     * @param errSource
     */
    public void upgradeVar(String name, Type newType, FENode errSource) {
        Type oldType = lookupVar(name, errSource);
    	Type lcpType = oldType.leastCommonPromotion(newType);
    	registerVar(name, lcpType);
    }

    /** Registers a new symbol in the symbol table, using default
     * origin and kind. */
    public void registerVar(String name, Type type)
    {
        registerVar(name, type, null, 0);
    }

    /**
     * Registers a new symbol in the symbol table.
     *
     * @param  name    name of the variable
     * @param  type    front-end type of the variable
     * @param  origin  statement or other object defining the variable
     * @param  kind    KIND_* constant describing the variable
     */
    public void registerVar(String name, Type type, Object origin, int kind)
    {
        vars.put(name, new VarInfo(type, origin, kind));
    }

    /** Registers a new function in the symbol table. */
    public void registerFn(Function fn)
    {
        // Ignore null-named functions.
        if (fn.getName() != null)
            fns.put(fn.getName(), fn);
    }

    /** Helper method to get the VarInfo for a name.  If the symbol is
     * not in the current symbol table, search in the parent.  If the
     * parent is null, return null. */
    private VarInfo lookupVarInfo(String name)
    {
        VarInfo info = (VarInfo)vars.get(name);
        if (info != null)
            return info;
        if (parent != null)
            return parent.lookupVarInfo(name);
        return null;
    }

    /**
     * Check to see if a symbol exists.  Searches parent symbol tables.
     *
     * @param   name  name of the variable to search for
     * @return  true if defined, false otherwise
     */
    public boolean hasVar(String name)
    {
        VarInfo info = lookupVarInfo(name);
        return (info != null);
    }

    /**
     * Looks up the type for a symbol. If that symbol is not in the current symbol table,
     * search in the parent.
     * 
     * @param name
     *            name of the variable to search for
     * @param errSource
     *            A source node if the name is not found
     * @return the front-end type of the variable, if defined
     * @throws UnrecognizedVariableException
     *             if the variable is defined in neither this nor any of its ancestor
     *             symbol tables
     */
    public Type lookupVar(String name, FENode errSource)
    {
        VarInfo info = lookupVarInfo(name);
        if (info != null)
            return info.type;
        throw new UnrecognizedVariableException(name, errSource);
    }


    public boolean isVarShared(String name, FENode errSource) {

    	return isVarShared(name, false, errSource);


    }

    public boolean isVarShared(String name, boolean isShared, FENode errSource) {
    	   VarInfo info = (VarInfo)vars.get(name);
           if (info != null)
               return isShared;
           if (parent != null)
            return parent.isVarShared(name, makeShared || isShared, errSource);
        throw new UnrecognizedVariableException(name, errSource);
    }


    /**
     * Looks up the type for a variable expression.  If the named
     * symbol is not in the current symbol table, search in the
     * parent.
     *
     * @param   var  variable expression to search for
     * @return  the front-end type of the variable, if defined
     * @throws  UnrecognizedVariableException if the variable is
     *          defined in neither this nor any of its ancestor
     *          symbol tables
     */
    public Type lookupVar(ExprVar var)
    {
        VarInfo info = lookupVarInfo(var.getName());
        if (info != null)
            return info.type;
        throw new UnrecognizedVariableException(var.getName(), var);
    }

    /**
     * Finds the object that declared a particular symbol. If that symbol is not in the
     * current symbol table, search in the parent.
     * 
     * @param name
     *            name of the variable to search for
     * @param errSource
     * @return the object that declares the variable, if defined
     * @throws UnrecognizedVariableException
     *             if the variable is defined in neither this nor any of its ancestor
     *             symbol tables
     */
    public Object lookupOrigin(String name, FENode errSource)
    {
        VarInfo info = lookupVarInfo(name);
        if (info != null)
            return info.origin;
        throw new UnrecognizedVariableException(name, errSource);
    }

    /**
     * Gets the kind (local, field, etc.) of a particular symbol. If that symbol is not in
     * the current symbol table, search in the parent.
     * 
     * @param name
     *            name of the variable to search for
     * @param errSource
     * @return KIND_* constant describing the variable
     * @throws UnrecognizedVariableException
     *             if the variable is defined in neither this nor any of its ancestor
     *             symbol tables
     */
    public int lookupKind(String name, FENode errSource)
    {
        VarInfo info = lookupVarInfo(name);
        if (info != null)
            return info.kind;
        throw new UnrecognizedVariableException(name, errSource);
    }

    /**
     * Looks up the function corresponding to a particular name. If that name is not in
     * the symbol table, searches the parent, and then each of the symbol tables in
     * includedFns, depth-first, in order. Throws an UnrecognizedVariableException if the
     * function doesn't exist.
     * 
     * @param errSource
     */
    public Function lookupFn(String name, FENode errSource)
    {
        Function fn = doLookupFn(name);
        if (fn != null) return fn;
        throw new UnrecognizedVariableException(name, errSource);
    }

    private Function doLookupFn(String name)
    {
        Function fn = (Function)fns.get(name);
        if (fn != null)
            return fn;
        if (parent != null)
        {
            fn = parent.doLookupFn(name);
            if (fn != null)
                return fn;
        }
        if (includedFns != null)
        {
            for (Iterator iter = includedFns.iterator(); iter.hasNext(); )
            {
                SymbolTable other = (SymbolTable)iter.next();
                fn = other.doLookupFn(name);
                if (fn != null)
                    return fn;
            }
        }
        return null;
    }

    /** Returns the parent of this, or null if this has no parent. */
    public SymbolTable getParent()
    {
        return parent;
    }
}
