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
import java.util.Map.Entry;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtImplicitVarDecl;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.NotYetComputedType;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.util.datastructures.TypedHashMap;

/**
 * Front-end visitor pass that maintains a symbol table.  Other
 * passes that need symbol table information can extend this.
 * The protected <code>symtab</code> member has the prevailing
 * symbol table as each node is visited.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class SymbolTableVisitor extends FEReplacer
{
    /**
     * The current symbol table.  Functions in this class keep the
     * symbol table up to date; calling
     * <code>super.visitSomething</code> from a derived class will
     * update the symbol table if necessary and recursively visit
     * children.
     */
    protected SymbolTable symtab;





    /**
     * Create a new symbol table visitor.
     *
     * @param symtab  Symbol table to use if no other is available,
     *                can be null
     * @param st      Prevailing stream type, can be null
     */
    public SymbolTableVisitor(SymbolTable symtab)
    {
        this.symtab = (symtab == null ? new SymbolTable(null) : symtab);

    }

    public Type getTypeOrNotYetComputed(Expression expr) {
        Type t = getType(expr);
        return (t == null) ? new NotYetComputedType(CudaMemoryType.UNDEFINED) : t;
    }

    /**
     * Get the type of an <code>Expression</code>.
     *
     * @param expr  Expression to get the type of
     * @returns     Type of the expression
     * @see         sketch.compiler.nodes.GetExprType
     */
    public Type getType(Expression expr) {
    	return getType (expr, TypePrimitive.nulltype);
    }
    
    
    public Type getTypeReal(Expression expr) {
    	return getTypeReal (expr, TypePrimitive.nulltype);
    }

    /**
     * Get the type of an <code>Expression</code>.
     *
     * @param expr  Expression to get the type of
     * @param nullType The type ExprNullPointer has.  (hack)
     * @returns     Type of the expression
     * @see         sketch.compiler.nodes.GetExprType
     */
    public Type getType(Expression expr, Type nullType)
    {
        return actualType(getTypeReal(expr, nullType));
    }
    
    public Type getTypeReal(Expression expr, Type nullType)
    {
    	if(expr == null){ return TypePrimitive.voidtype; }

        // To think about: should we cache GetExprType objects?
        GetExprType get = new GetExprType(symtab, this.nres, nullType);
        Type type = (Type)expr.accept(get);
        return type;
    }

    public boolean isGlobal(ExprVar ev){
        return symtab.isVarShared(ev.getName(), ev);
    }

    public boolean isGlobal(String name, FENode errSource) {
        return symtab.isVarShared(name, errSource);
    }


    public boolean isGlobal(Expression exp){
    	class checker extends FEReplacer{
    		boolean isglobal = false;
    		public Object visitExprVar(ExprVar ev){
    			isglobal = isglobal || isGlobal(ev);
    			return ev;
    		}
    	};
    	checker c = new checker();
    	exp.accept(c);
    	return c.isglobal;
    }



    /**
     * Add a variable declaration and register the variable in the
     * symbol table.  This creates a {@link
     * sketch.compiler.nodes.StmtVarDecl} for the specified type and
     * name, and adds that statement using {@link addStatement}.  It
     * also registers the new variable in the current symbol table.
     *
     * @param context  file and line number the statement belongs to
     * @param type     type of the variable
     * @param name     name of the variable
     */
    protected void addVarDecl(FENode context, Type type, String name)
    {
        Statement stmt = new StmtVarDecl(context, type, name, null);
        addStatement(stmt);
        symtab.registerVar(name, type, stmt, SymbolTable.KIND_LOCAL);
    }

    /**
     * Get the actual type for a type.  In particular, if we have a
     * structure-reference type and the name of the reference is
     * registered, then the actual type is the corresponding
     * structure type.
     *
     * @param type  type to resolve to actual type
     * @return      actual resolved type
     */
    protected Type actualType(Type type)
    {
        if (type instanceof TypeStructRef)
        {
            String name = ((TypeStructRef)type).getName();
            Type tmp = nres.getStruct(name);
            if (tmp != null)
                type = tmp;
        }
        return type;
    }

    public Object visitFieldDecl(FieldDecl field)
    {
        for (int i = 0; i < field.getNumFields(); i++)
            symtab.registerVar(field.getName(i),
                               actualType(field.getType(i)),
                               field,
                    SymbolTable.KIND_GLOBAL);
        return super.visitFieldDecl(field);
    }


	@Override
    public Object visitParameter(Parameter par) {
        symtab.registerVar(par.getName(), actualType(par.getType()), par,
                SymbolTable.KIND_FUNC_PARAM);

        Type t = (Type) par.getType().accept(this);
        if (t == par.getType()) {
            return par;
        } else {
            return new Parameter(t, par.getName(), par.getPtype());
        }
    }


    public Object visitFunction(Function func)
    {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        Object result = super.visitFunction(func);
        symtab = oldSymTab;
        return result;
    }

   

    public Object visitProgram(Program prog)
 {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        Object o = super.visitProgram(prog); 
        symtab = oldSymTab;
        return o;
    }

    public Object visitStmtBlock(StmtBlock block)
    {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        Object result = super.visitStmtBlock(block);
        symtab = oldSymTab;
        return result;
    }

    @Override
    public Object visitStmtFork(StmtFork ploop){
    	SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab, true);
        Object result = super.visitStmtFork(ploop);
        symtab = oldSymTab;
        return result;
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        for (int i = 0; i < stmt.getNumVars(); i++)
            symtab.registerVar(stmt.getName(i),
                               actualType(stmt.getType(i)),
                               stmt,
                               SymbolTable.KIND_LOCAL);
        return super.visitStmtVarDecl(stmt);
    }

    public Object visitTypeStruct(TypeStruct ts) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        boolean changed = false;
        TypedHashMap<String, Type> map = new TypedHashMap<String, Type>();
        for (Entry<String, Type> entry : ts) {
            symtab.registerVar(entry.getKey(), actualType(entry.getValue()), ts,
                    SymbolTable.KIND_FIELD);
        }

        for (Entry<String, Type> entry : ts) {
            Type type = (Type) entry.getValue().accept(this);
            changed |= (type != entry.getValue());
            map.put(entry.getKey(), type);
        }

        symtab = oldSymTab;

        if (changed) {
            return ts.creator().fields(map).create();
        } else {
            return ts;
        }
    }

    public Object visitStmtImplicitVarDecl(StmtImplicitVarDecl decl) {
        Type t = getTypeOrNotYetComputed(decl.getInitExpr());
        symtab.registerVar(decl.getName(), t, decl, SymbolTable.KIND_LOCAL);
        // printDebug("[SymbolTableVisitor] implicit type for ", decl.getName(), t);
        return super.visitStmtImplicitVarDecl(decl);
    }

    public Object visitStreamSpec(StreamSpec spec)
    {

        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        if (nres != null)
            nres.setPackage(spec);

	// register functions
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
	    Function func = (Function)iter.next();
	    symtab.registerFn(func);
	}
        Object result = super.visitStreamSpec(spec);
        symtab = oldSymTab;
        return result;
    }

}
