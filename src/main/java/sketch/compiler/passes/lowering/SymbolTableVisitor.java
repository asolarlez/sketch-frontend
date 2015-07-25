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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtImplicitVarDecl;
import sketch.compiler.ast.core.stmts.StmtSwitch;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.NotYetComputedType;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.util.datastructures.TypedHashMap;
import sketch.util.exceptions.ExceptionAtNode;

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


    public static class TypeRenamer extends FEReplacer {
        public Map<String, Type> tmap = new HashMap<String, Type>();

        String postfix() {
            String rv = "";
            for (Entry<String, Type> e : tmap.entrySet()) {
                rv += "_" + e.getValue().toString().replace('@', '_');
            }
            return rv;
        }

        public TypeRenamer() {}

        @Override
        public Object visitTypeStructRef(TypeStructRef tr) {
            if (tmap.containsKey(tr.getName())) {
                return tmap.get(tr.getName());
            }
            return tr;
        }

        public Type rename(Type t) {
            if (tmap.isEmpty()) {
                return t;
            }
            return (Type) t.accept(this);
        }

        public String toString() {
            return tmap.toString();
        }
    }

    private static final TypeRenamer emptyrenamer = new TypeRenamer();

    public static TypeRenamer getRenaming(Function f, List<Type> efcTypes) {
        List<String> tps = f.getTypeParams();
        if (tps.isEmpty()) {
            return emptyrenamer;
        }
        TypeRenamer rv = new TypeRenamer();
        Set<String> st = new HashSet<String>(tps);
        List<Parameter> formals = f.getParams();
        Iterator<Parameter> paramIt = f.getParams().iterator();
        int dif = f.getParams().size() - efcTypes.size();
        if (dif != 0) {
            if (dif < 0) {
                throw new ExceptionAtNode("Wrong number of parameters", null);
            }
            boolean alloutputs = true;
            for (int i = 0; i < dif; ++i) {
                Parameter p = formals.get(formals.size() - 1 - i);
                if (!p.isParameterOutput()) {
                    alloutputs = false;
                }
            }
            if (!alloutputs) {
                for (int i = 0; i < dif; ++i) {
                    Parameter p = paramIt.next();
                    if (!p.isImplicit()) {
                        throw new ExceptionAtNode("Wrong number of parameters", null);
                    }
                }
            }
        }
        for (Type actual : efcTypes) {
            Parameter p = paramIt.next();
            Type ptype = p.getType();
            // The idea is that if I expect A[] and the user gives int[]
            // then A should be int, but if I expect A[][] and the user gives int[]
            // then A should also be int. On the other hand, if I expect A[]
            // and the user gives int[][][], then A should be int[][]
            Type tact = actual;
            while (ptype instanceof TypeArray) {
                TypeArray ta = (TypeArray) ptype;
                ptype = ta.getBase();
                if (tact instanceof TypeArray) {
                    tact = ((TypeArray) actual).getBase();
                }
            }
            String tn = ptype.toString();
            if (st.contains(tn)) {
                if (tact instanceof TypeArray) {
                    throw new ExceptionAtNode(
                            "Generics not allowed to resolve to an array type " + tn +
                                    "->" + tact, null);
                }
                rv.tmap.put(tn, tact);
            }
        }
        return rv;
    }

    public void setSymtab(SymbolTable symtab) {
        this.symtab = symtab;
    }


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
    
    public SymbolTable getSymbolTable() {
    	return this.symtab;
    }

    /**
     * Get the type of an <code>Expression</code>.
     *
     * @param expr  Expression to get the type of
     * @returns     Type of the expression
     * @see         sketch.compiler.nodes.GetExprType
     */
    public Type getType(Expression expr) {
        return getType(expr, TypePrimitive.nulltype);
    }

    public Type getTypeReal(Expression expr) {
        return getTypeReal(expr, TypePrimitive.nulltype);
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
        return getTypeReal(expr, nullType);
    }

    public Type getTypeReal(Expression expr, Type nullType)
    {
        if (expr == null) {
            return TypePrimitive.voidtype;
        }

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
        class checker extends FEReplacer {
            boolean isglobal = false;

            public Object visitExprVar(ExprVar ev) {
                isglobal = isglobal || isGlobal(ev);
                return ev;
            }
        }
        ;
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
    protected StructDef getStructDef(Type type)
    {
        if (type instanceof TypeStructRef)
        {
            String name = ((TypeStructRef)type).getName();
            StructDef tmp = nres.getStruct(name);
            return tmp;
        } else {
            throw new ExceptionAtNode(type + " is not a struct type", null);
        }
    }

    public Object visitFieldDecl(FieldDecl field)
    {
        for (int i = 0; i < field.getNumFields(); i++)
            symtab.registerVar(field.getName(i), field.getType(i), field,
                    SymbolTable.KIND_GLOBAL);
        return super.visitFieldDecl(field);
    }



    @Override
    public Object visitParameter(Parameter par) {
        symtab.registerVar(par.getName(), par.getType(), par,
                SymbolTable.KIND_FUNC_PARAM);

        Type t = (Type) par.getType().accept(this);
        if (t == par.getType()) {
            return par;
        } else {
            return new Parameter(par, par.getSrcTupleDepth(), t, par.getName(),
                    par.getPtype());
        }
    }


    public Object visitFunction(Function func)
    {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        boolean hadTPs = !func.getTypeParams().isEmpty();
        if (hadTPs) {
            nres.pushTempTypes(func.getTypeParams());
        }
        Object result = super.visitFunction(func);
        if (hadTPs) {
            nres.popTempTypes();
        }
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

    public Object visitStmtSwitch(StmtSwitch stmt) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        ExprVar var = (ExprVar) stmt.getExpr().accept(this);

        // visit each case body
        StmtSwitch newStmt = new StmtSwitch(stmt.getContext(), var);
        String name = ((TypeStructRef) getType(var)).getName();
        StructDef ts = nres.getStruct(name);
        String pkg;
        if (ts == null)
            pkg = nres.curPkg().getName();
        else
            pkg = ts.getPkg();
        for (String caseExpr : stmt.getCaseConditions()) {
            if (!("default".equals(caseExpr) || "repeat".equals(caseExpr))) {
                SymbolTable oldSymTab1 = symtab;
                symtab = new SymbolTable(symtab);
                symtab.registerVar(var.getName(),
                        (new TypeStructRef(caseExpr, false)).addDefaultPkg(pkg, nres));

                Statement body = (Statement) stmt.getBody(caseExpr).accept(this);
                newStmt.addCaseBlock(caseExpr, body);
                symtab = oldSymTab1;
            } else {
                SymbolTable oldSymTab1 = symtab;
                symtab = new SymbolTable(symtab);

                Statement body = (Statement) stmt.getBody(caseExpr).accept(this);
                newStmt.addCaseBlock(caseExpr, body);
                symtab = oldSymTab1;

            }
        }
        symtab = oldSymTab;

        return newStmt;

    }

    public Object visitStmtFor(StmtFor stmt) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        Object result = super.visitStmtFor(stmt);
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
            symtab.registerVar(stmt.getName(i), (stmt.getType(i)),
 stmt,
                    SymbolTable.KIND_LOCAL);
        return super.visitStmtVarDecl(stmt);
    }

    public Object visitStructDef(StructDef ts) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);

        StructDef sdl = ts;
        int maxcnt = nstructsInPkg;
        Set<String> s = null;
        while (sdl != null) {
            for (Entry<String, Type> entry : sdl) {
                symtab.registerVar(entry.getKey(), (entry.getValue()), sdl,
                        SymbolTable.KIND_FIELD);
            }
            String pn = sdl.getParentName();
            if (pn == null) {
                break;
            } else {
                sdl = nres.getStruct(pn);
            }
            --maxcnt;
            if (maxcnt < 0) {
                if (s == null) {
                    s = new HashSet<String>();
                }
                if (s.contains(pn)) {
                    throw new ExceptionAtNode(
                            "There is a loop in the extends relation involving struct " +
                                    pn, ts);
                }
                s.add(pn);
            }
        }

        boolean changed = false;
        TypedHashMap<String, Type> map = new TypedHashMap<String, Type>();

        for (Entry<String, Type> entry : ts) {
            Type type = (Type) entry.getValue().accept(this);
            changed |= (type != entry.getValue());
            map.put(entry.getKey(), type);
        }

        symtab = oldSymTab;

        if (changed) {
            StructDef new_struct = ts.creator().fields(map).create();
            if (ts.immutable())
                new_struct.setImmutable();
            return new_struct;
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

    public Object visitPackage(Package spec)
    {

        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        if (nres != null)
            nres.setPackage(spec);

        // register functions
        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
            Function func = (Function) iter.next();
            symtab.registerFn(func);
        }
        Object result = super.visitPackage(spec);
        symtab = oldSymTab;
        return result;
    }

}
