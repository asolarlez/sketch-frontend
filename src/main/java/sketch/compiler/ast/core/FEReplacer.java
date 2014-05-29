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

import java.util.*;
import java.util.Map.Entry;

import sketch.compiler.ast.core.exprs.*;
import sketch.compiler.ast.core.exprs.ExprArrayRange.RangeLen;
import sketch.compiler.ast.core.exprs.regens.ExprAlt;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceBinary;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceUnary;
import sketch.compiler.ast.core.exprs.regens.ExprParen;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.ast.cuda.exprs.CudaBlockDim;
import sketch.compiler.ast.cuda.exprs.CudaInstrumentCall;
import sketch.compiler.ast.cuda.exprs.CudaThreadIdx;
import sketch.compiler.ast.cuda.exprs.ExprRange;
import sketch.compiler.ast.cuda.stmts.CudaSyncthreads;
import sketch.compiler.ast.cuda.stmts.StmtParfor;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.ast.promela.stmts.StmtJoin;
import sketch.compiler.ast.spmd.exprs.SpmdNProc;
import sketch.compiler.ast.spmd.exprs.SpmdPid;
import sketch.compiler.ast.spmd.stmts.SpmdBarrier;
import sketch.compiler.ast.spmd.stmts.StmtSpmdfork;
import sketch.util.datastructures.TypedHashMap;

import static sketch.util.DebugOut.assertFalse;

/**
 * Replaces nodes in a front-end tree.  This is a skeleton for writing
 * replacing passes, which implements <code>FEVisitor</code>.  On its
 * own it does nothing, but it is a convenice class for deriving your
 * own replacers from.  All of the member functions of
 * <code>FEReplacer</code> return objects of appropriate types
 * (<code>Expression</code> subclasses return
 * <code>Expression</code>s; <code>Statement</code> subclasses return
 * <code>Statement</code>s; other objects return their own types); an
 * attempt is made to not create new objects if they would be
 * identical to the original objects.
 *
 * <p> For <code>Statement</code>s, this class also keeps a list of
 * statements in the current block.  Calling the
 * <code>addStatement</code> method will add a statement to the end of
 * the list; a statement visitor can return a statement, or can call
 * <code>addStatement</code> itself and return <code>null</code>.
 * Derived classes should take care to only call
 * <code>addStatement</code> for statements inside a block;
 * practically, this means that any pass that adds or removes
 * statements should be called after the <code>MakeBodiesBlocks</code>
 * pass.
 *
 * <p> <code>Statement</code>s that visit <code>Expression</code>s
 * call <code>doExpression</code> to do the actual visitation; by
 * default, this accepts <code>this</code> on the provided expression,
 * but derived classes can override <code>doExpression</code> to
 * perform some custom action.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class FEReplacer implements FEVisitor
{

    /**
     * Mutable list of statements to be added to the current block.
     * This is only usefully defined within a call to
     * <code>visitStmtBlock</code>; it will generally be defined
     * within any statement or expression visitor.  Passes probably
     * don't want to modify this directly; use
     * <code>addStatement</code>, <code>addStatements</code>, or
     * return a <code>Statement</code> object from a statement visitor
     * function.
     */
    protected List<Statement> newStatements;

    /**
     * Adds a statement to the list of statements that replace the
     * current one.  This should be called inside a statement
     * visitor (and possibly inside a recursively called
     * expression visitor, but take care).  Statements added go
     * before the statement returned by the visitor, if any.  For
     * example, it is legitimate to use <code>addStatement</code> to
     * add a declaration for a variable that is used inside a rewritten
     * statement, and return the statement from the visitor function.
     *
     * @param stmt The statement to add
     */
    protected void addStatement(Statement stmt)
    {
        assert(newStatements != null);
        newStatements.add(stmt);
    }

    protected NameResolver nres;

    protected Function getFuncNamed(String name) {
        return nres.getFun(name);
    }

    public void setNres(NameResolver nr) {
        nres = nr;
    }

    protected void addExprStatement(Expression expr) {
        addStatement(new StmtExpr(expr));
    }

    /**
     * Adds a sequence of statements to the list of statements that
     * replace the current one.  This should be called inside a
     * statement visitor (and possibly inside a recursively called
     * expression visitor, but take care).  Statements added go before
     * the statement returned by the visitor, if any.  For example, it
     * is legitimate to use <code>addStatement</code> to add a
     * declaration for a variable that is used inside a rewritten
     * statement, and return the statement from the visitor function.
     *
     * @param stmt The statement to add
     */
    protected void addStatements(Collection<Statement> stmts)
    {
        newStatements.addAll(stmts);
    }

    /**
     * Accept an arbitrary <code>Statement</code>.  This by default
     * just asks <code>stmt</code> to accept <code>this</code>, and
     * adds the returned statement (if any) to the statement list.  If
     * a derived class needs to do extra processing on every
     * statement, it can override this method.
     *
     * @param stmt  Statement to visit
     */
    protected void doStatement(Statement stmt)
    {
        Statement result = (Statement)stmt.accept(this);
        if (result != null)
            addStatement(result);
    }

    /**
     * Accept an arbitrary <code>Expression</code>.  This by default
     * just asks <code>expr</code> to accept <code>this</code>, but if
     * a derived class needs to do extra processing on every
     * expression, it can override this method.  This function is
     * always called in a statement context; <code>addStatement</code>
     * will add a statement before the current statement.
     *
     * @param expr  Expression to visit
     * @return      Expression to replace <code>expr</code>
     */
    protected Expression doExpression(Expression expr)
    {
        if(expr != null)
            return (Expression)expr.accept(this);
        else
            return null;
    }

    public List<Statement> visitStatementsAsBlock(Vector<Statement> s) {
        return ((StmtBlock) (new StmtBlock(s)).accept(this)).getStmts();
    }

    public Statement[] visitStatementsAsBlock(Statement... s) {
        return ((StmtBlock) (new StmtBlock(Arrays.asList(s))).accept(this)).getStmts().toArray(
                new Statement[0]);
    }

    public Object visitExprNew(ExprNew expNew){
        Type nt = null;
        if (expNew.getTypeToConstruct() != null) {
            nt = (Type) expNew.getTypeToConstruct().accept(this);
        }
        boolean changed = false;
        List<ExprNamedParam> enl =
                new ArrayList<ExprNamedParam>(expNew.getParams().size());
        for (ExprNamedParam en : expNew.getParams()) {
            Expression old = en.getExpr();
            Expression rhs = doExpression(old);
            if (rhs != old) {
                enl.add(new ExprNamedParam(en, en.getName(), rhs));
                changed = true;
            } else {
                enl.add(en);
            }
        }

        if (nt != expNew.getTypeToConstruct() || changed) {
            if (!changed) {
                enl = expNew.getParams();
            }
            return new ExprNew(expNew, nt, enl, expNew.isHole(), expNew.getStar());
        } else {
            return expNew;
        }
    }

    public Object visitExprAlt (ExprAlt exp) {
        Expression ths = doExpression(exp.getThis());
        Expression that = doExpression(exp.getThat());
        return (ths == exp.getThis() && that == exp.getThat()) ? exp : new ExprAlt(exp,
                ths, that);
    }

    public Object visitExprTuple(ExprTuple exp) {
        boolean hasChanged = false;
        List<Expression> newElements = new ArrayList<Expression>();
        for (Iterator iter = exp.getElements().iterator(); iter.hasNext();) {
            Expression element = (Expression) iter.next();
            Expression newElement = doExpression(element);
            newElements.add(newElement);
            if (element != newElement)
                hasChanged = true;
        }
        if (!hasChanged)
            return exp;
        return new ExprTuple(exp, newElements, exp.getName());
    }
    public Object visitExprArrayInit(ExprArrayInit exp)
    {
        boolean hasChanged = false;
        List<Expression> newElements = new ArrayList<Expression>();
        for (Iterator iter = exp.getElements().iterator(); iter.hasNext(); )
        {
            Expression element = (Expression)iter.next();
            Expression newElement = doExpression(element);
            newElements.add(newElement);
            if (element != newElement) hasChanged = true;
        }
        if (!hasChanged) return exp;
        return new ExprArrayInit(exp, newElements);
    }

    public Object visitExprBinary(ExprBinary exp)
 {
        Expression left = doExpression(exp.getLeft());
        Expression right = doExpression(exp.getRight());
        if (left == exp.getLeft() && right == exp.getRight())
            return exp;
        else
            return new ExprBinary(exp, exp.getOp(), left, right, exp.getAlias());
    }

    public Object visitExprChoiceBinary (ExprChoiceBinary exp) {
        Expression left = doExpression(exp.getLeft());
        Expression right = doExpression(exp.getRight());
        if (left == exp.getLeft() && right == exp.getRight())
            return exp;
        else
            return new ExprChoiceBinary(exp, left, exp.getOps(), right);
    }

    public Object visitExprChoiceSelect (ExprChoiceSelect exp) {
        Expression obj = doExpression(exp.getObj());
        if (obj == exp.getObj())
            return exp;
        else
            return new ExprChoiceSelect(exp, obj, exp.getField());
    }

    public Object visitExprChoiceUnary (ExprChoiceUnary exp) {
        Expression expr = doExpression(exp.getExpr());
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprChoiceUnary(exp, exp.getOps(), expr);
    }

    public Object visitExprConstChar(ExprConstChar exp) { return exp; }
    public Object visitExprConstFloat(ExprConstFloat exp) { return exp; }
    public Object visitExprConstInt(ExprConstInt exp) { return exp; }
    public Object visitExprLiteral(ExprLiteral exp) { return exp; }
    public Object visitExprNullPtr(ExprNullPtr nptr){ return nptr; }

    public Object visitExprFieldMacro(ExprFieldMacro exp) {
        Expression left = doExpression(exp.getLeft());
        if (left == exp.getLeft()) {
            return exp;
        } else {
            return new ExprFieldMacro(exp, left, exp.getType());
        }

    }

    public Object visitExprField(ExprField exp)
    {
        Expression left = doExpression(exp.getLeft());
        if (left == exp.getLeft())
            return exp;
        else
            return new ExprField(exp, left, exp.getName());
    }

    public Object visitExprFunCall(ExprFunCall exp)
    {
        boolean hasChanged = false;
        List<Expression> newParams = new ArrayList<Expression>();
        for (Expression param : exp.getParams()) {
            Expression newParam = doExpression(param);
            newParams.add(newParam);
            if (param != newParam) hasChanged = true;
        }
        if (!hasChanged) return exp;
        return new ExprFunCall(exp, exp.getName(), newParams);
    }

    public Object visitExprParen (ExprParen exp) {
        Expression expr = doExpression(exp.getExpr());
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprParen(exp, expr);
    }

    public Object visitExprRegen (ExprRegen exp) {
        Expression expr = doExpression(exp.getExpr());
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprRegen(exp, expr);
    }

    public Object visitExprTernary(ExprTernary exp)
    {
        Expression a = doExpression(exp.getA());
        Expression b = doExpression(exp.getB());
        Expression c = doExpression(exp.getC());
        if (a == exp.getA() && b == exp.getB() && c == exp.getC())
            return exp;
        else
            return new ExprTernary(exp, exp.getOp(), a, b, c);
    }

    public Object visitExprTypeCast(ExprTypeCast exp)
    {
        Expression expr = doExpression(exp.getExpr());
        Type t = (Type) exp.getType().accept(this);
        if (expr == exp.getExpr() && t == exp.getType())
            return exp;
        else
            return new ExprTypeCast(exp,t, expr);
    }

    public Object visitExprUnary(ExprUnary exp)
    {
        Expression expr = doExpression(exp.getExpr());
        if (expr == exp.getExpr())
            return exp;
        else
            return new ExprUnary(exp, exp.getOp(), expr);
    }

    public Object visitExprVar(ExprVar exp) { return exp; }

    public Object visitFieldDecl(FieldDecl field)
    {
        List<Expression> newInits = new ArrayList<Expression>();
        List<Type> newTypes = new ArrayList<Type>();
        for (int i = 0; i < field.getNumFields(); i++)
        {
            Expression init = field.getInit(i);
            if (init != null)
                init = (Expression)init.accept(this);
            newInits.add(init);
            newTypes.add((Type) field.getType(i).accept(this));
        }
        return new FieldDecl(field, newTypes,
 field.getNames(), newInits);
    }

    public Object visitFunction(Function func)
    {


        List<Parameter> newParam = new ArrayList<Parameter>();
        Iterator<Parameter> it = func.getParams().iterator();
        boolean samePars = true;
        while (it.hasNext()) {
            Parameter par = it.next();
            Parameter newPar = (Parameter) par.accept(this);
            if (par != newPar)
                samePars = false;
            newParam.add(newPar);
        }

        Type rtype = (Type) func.getReturnType().accept(this);

        if (func.getBody() == null) {
            assert func.isUninterp() : "Only uninterpreted functions are allowed to have null bodies.";
            if (samePars && rtype == func.getReturnType())
                return func;
            return func.creator().returnType(rtype).params(newParam).create();
        }
        Statement newBody = (Statement)func.getBody().accept(this);        
        if(newBody == null) newBody = new StmtEmpty(func);
        if (newBody == func.getBody() && samePars && rtype == func.getReturnType()) return func;
        return func.creator().returnType(rtype).params(newParam).body(newBody).create();
    }



    public Object visitProgram(Program prog) {
        assert prog != null : "FEReplacer.visitProgram: argument null!";
        nres = new NameResolver(prog);
        List<Package> newStreams = new ArrayList<Package>();
        for (Package ssOrig : prog.getPackages()) {
            newStreams.add((Package) ssOrig.accept(this));
        }


        return prog.creator().streams(newStreams).create();
    }





    public Object visitStmtAssign(StmtAssign stmt)
    {
        Expression newLHS = doExpression(stmt.getLHS());
        Expression newRHS = doExpression(stmt.getRHS());
        if (newLHS == stmt.getLHS() && newRHS == stmt.getRHS())
            return stmt;
        return new StmtAssign(stmt, newLHS, newRHS,
 stmt.getOp());
    }

    public Object visitStmtFunDecl(StmtFunDecl stmt) {
        Function f = (Function) stmt.getDecl().accept(this);
        if (f != stmt.getDecl()) {
            return new StmtFunDecl(stmt, f);
        } else {
            return stmt;
        }
    }

    public Object visitStmtBlock(StmtBlock stmt)
    {
        List<Statement> oldStatements = newStatements;
        newStatements = new ArrayList<Statement>();
        boolean changed = false;
        int i=0;
        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext();++i )
        {
            Statement s = (Statement)iter.next();
            // completely ignore null statements, causing them to
            // be dropped in the output
            if (s == null)
                continue;
            try{
                doStatement(s);
                if (!(newStatements.size() == i + 1 && newStatements.get(i) == s)) {
                    changed = true;
                }
                if (i < newStatements.size() &&
                        newStatements.get(i) instanceof StmtReturn)
                {
                    if (iter.hasNext()) {
                        changed = true;
                        break;
                    }
                }
                /*
                 * Statement tmpres = (Statement)s.accept(this); if (tmpres != null)
                 * addStatement(tmpres);
                 */
            }catch(RuntimeException e){
                newStatements = oldStatements;
                throw e;
            }
        }
        if(!changed){
            newStatements = oldStatements;
            return stmt;
        }
        Statement result = new StmtBlock(stmt, newStatements);
        newStatements = oldStatements;
        return result;
    }


    public Object visitStmtReorderBlock(StmtReorderBlock stmt) {
        Object o = stmt.getBlock().accept(this);
        if (o == stmt.getBlock())
            return stmt;
        else if (o == null)
            return null;
        else if (o instanceof StmtBlock)
            return new StmtReorderBlock(stmt, (StmtBlock) o);
        else
            return new StmtReorderBlock(stmt, Collections.singletonList((Statement) o));

        /*
         * List<Statement> oldStatements = newStatements; newStatements = new
         * ArrayList<Statement>(); for (Iterator iter = stmt.getStmts().iterator();
         * iter.hasNext(); ) { Statement s = (Statement)iter.next(); // completely ignore
         * null statements, causing them to // be dropped in the output if (s == null)
         * continue; try{ doStatement(s); }catch(RuntimeException e){ newStatements =
         * oldStatements; throw e; } } Statement result = new StmtReorderBlock(stmt,
         * newStatements); newStatements = oldStatements; return result;
         */
    }

    public Object visitStmtInsertBlock(StmtInsertBlock stmt)
    {
        Statement newIns = (Statement) stmt.getInsertStmt().accept(this);
        Statement newInto = (Statement) stmt.getIntoBlock().accept(this);

        if (newIns == stmt.getInsertStmt() && newInto == stmt.getIntoBlock())
            return stmt;
        else if (null == newIns || null == newInto)
            return null;
        else {
            if (!(newInto.isBlock()))
                newInto = new StmtBlock(newInto);
            return new StmtInsertBlock(stmt, newIns, (StmtBlock) newInto);
        }
    }

    public Object visitStmtAtomicBlock (StmtAtomicBlock ab) {
        Statement tmp = ab.getBlock().doStatement(this);
        Expression exp = ab.getCond();
        if (ab.isCond()) {
            exp = exp.doExpr(this);
        }
        if (tmp == null) {
            return null;
        } else if (tmp != ab.getBlock() || exp != ab.getCond()) {
            if (tmp instanceof StmtBlock) {
                StmtBlock sb = (StmtBlock) tmp;
                return new StmtAtomicBlock(ab, sb, exp);
            } else {
                return new StmtAtomicBlock(ab, Collections.singletonList(tmp), exp);
            }
        } else {
            return ab;
        }
    }


    public Object visitStmtBreak(StmtBreak stmt) { return stmt; }
    public Object visitStmtContinue(StmtContinue stmt) { return stmt; }

    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        Statement newBody = (Statement)stmt.getBody().accept(this);
        Expression newCond = doExpression(stmt.getCond());
        if (newBody == stmt.getBody() && newCond == stmt.getCond())
            return stmt;
        return new StmtDoWhile(stmt, newBody, newCond);
    }

    public Object visitStmtEmpty(StmtEmpty stmt) { return stmt; }

    public Object visitStmtExpr(StmtExpr stmt) {
        Object nextInner = stmt.getExpression().accept(this);
        if (nextInner == null) {
            return null;
        } else if (nextInner instanceof Expression) {
            Expression newExpr = (Expression) nextInner;
            if (newExpr == stmt.getExpression())
                return stmt;
            return new StmtExpr(stmt, newExpr);
        } else if (nextInner instanceof Statement) {
            return (Statement) nextInner;
        } else {
            throw new RuntimeException("unknown return value from stmt expr: " +
                    nextInner);
        }
    }

    public Object visitStmtFor(StmtFor stmt)
    {

        Statement newInit = null;
        if(stmt.getInit() != null){
            newInit = (Statement) stmt.getInit().accept(this);
        }
        Expression newCond = doExpression(stmt.getCond());
        Statement newIncr = null;
        if(stmt.getIncr() != null){
            newIncr = (Statement) stmt.getIncr().accept(this);
        }
        Statement tmp = stmt.getBody();
        Statement newBody = StmtEmpty.EMPTY; 
        if(tmp != null){ 
            newBody = (Statement) tmp.accept(this);
        }

        if (newInit == stmt.getInit() && newCond == stmt.getCond() &&
                newIncr == stmt.getIncr() && newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt, newInit, newCond, newIncr,
 newBody, stmt.isCanonical());
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        Expression newCond = doExpression(stmt.getCond());
        Statement newCons = stmt.getCons() == null ? null :
            (Statement)stmt.getCons().accept(this);
        Statement newAlt = stmt.getAlt() == null ? null :
            (Statement)stmt.getAlt().accept(this);
        if (newCond == stmt.getCond() && newCons == stmt.getCons() &&
                newAlt == stmt.getAlt())
            return stmt;
        if(newCons == null && newAlt == null){
            return new StmtExpr(stmt, newCond);
        }
        StmtIfThen newStmt = new StmtIfThen(stmt, newCond, newCons, newAlt);
        if (stmt.isAtomic())
            newStmt.setAtomic();
        return newStmt;
    }

    public Object visitStmtJoin(StmtJoin stmt)
    {
        return stmt;
    }

    public Object visitStmtLoop(StmtLoop stmt)
    {
        Expression newIter = doExpression(stmt.getIter());
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if (newIter == stmt.getIter() && newBody == stmt.getBody())
            return stmt;
        return new StmtLoop(stmt, newIter, newBody);
    }

    public Object visitStmtReturn(StmtReturn stmt)
    {
        Expression newValue = stmt.getValue() == null ? null :
            doExpression(stmt.getValue());
        if (newValue == stmt.getValue()) return stmt;
        return new StmtReturn(stmt, newValue);
    }

    public Object visitStmtAssert(StmtAssert stmt)
    {
        Expression newValue = stmt.getCond() == null ? null :
            doExpression(stmt.getCond());
        Integer ev = newValue.getIValue();
        if (ev != null && ev == 1) {
            return null;
        }
        if (newValue == stmt.getCond()) {
            return stmt;
        }
        return new StmtAssert(stmt, newValue, stmt.getMsg(), stmt.isSuper());
    }

    public Object visitStmtAssume(StmtAssume stmt) {
        Expression newValue =
                stmt.getCond() == null ? null : doExpression(stmt.getCond());
        if (newValue == stmt.getCond())
            return stmt;
        return new StmtAssume(stmt, newValue, stmt.getMsg());
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        List<Expression> newInits = new ArrayList<Expression>();
        List<Type> newTypes = new ArrayList<Type>();
        boolean changed = false;
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            Expression oinit = stmt.getInit(i);
            Expression init = null;
            if (oinit != null)
                init = doExpression(oinit);
            Type ot = stmt.getType(i);
            Type t = (Type) ot.accept(this);
            if(ot != t || oinit != init){
                changed = true;
            }
            newInits.add(init);
            newTypes.add(t);
        }
        if(!changed){ return stmt; }
        return new StmtVarDecl(stmt, newTypes,
 stmt.getNames(), newInits);
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
        Expression newCond = doExpression(stmt.getCond());
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if (newCond == stmt.getCond() && newBody == stmt.getBody())
            return stmt;
        return new StmtWhile(stmt, newCond, newBody);
    }


    protected List<Function> newFuncs;
    protected int nstructsInPkg = -1;

    /**
     * StreamSpec represents a namespace. spec.getVars() will get you all the global
     * variable declarations. spec.getStructs() gets you the structure declarations.
     * spec.getFuncs() gets you all the function declarations.
     */
    public Object visitPackage(Package spec)
    {

        if (nres != null)
            nres.setPackage(spec);

        List<FieldDecl> newVars = new ArrayList<FieldDecl>();
        List<Function> oldNewFuncs = newFuncs;
        newFuncs = new ArrayList<Function>();

        boolean changed = false;

        for (Iterator iter = spec.getVars().iterator(); iter.hasNext();) {
            FieldDecl oldVar = (FieldDecl) iter.next();
            FieldDecl newVar = (FieldDecl) oldVar.accept(this);
            if (oldVar != newVar)
                changed = true;
            if (newVar != null)
                newVars.add(newVar);
        }

        List<StructDef> newStructs = new ArrayList<StructDef>();
        nstructsInPkg = spec.getStructs().size();
        for (StructDef tsOrig : spec.getStructs()) {
            StructDef ts = (StructDef) tsOrig.accept(this);
            if (ts != tsOrig) {
                changed = true;
            }
            newStructs.add(ts);
        }
        nstructsInPkg = -1;

        int nonNull = 0;
        for (Iterator<Function> iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
            Function oldFunc = (Function)iter.next();
            Function newFunc = (Function)oldFunc.accept(this);
            if (oldFunc != newFunc) changed = true;
            // if(oldFunc != null)++nonNull;
            if(newFunc!=null) newFuncs.add(newFunc);
        }

        if(newFuncs.size() != nonNull){
            changed = true;
        }



        List<Function> nf = newFuncs;
        // newFuncs = oldNewFuncs;
        if (!changed)
            return spec;
        return new Package(spec, spec.getName(), newStructs, newVars, nf);

    }


    public Object visitOther(FENode node) { return node; }

    public Object visitExprStar(ExprStar star) {
        if (star.getType() != null) {
            Type t = (Type) star.getType().accept(this);
            if (t != star.getType()) {
                ExprStar s = new ExprStar(star);
                s.setType(t);
            }
        }
        return star;
    }

    public Object visitExprTupleAccess(ExprTupleAccess exp) {
        final Expression newBase = doExpression(exp.getBase());

        if (newBase != exp.getBase()) {
            return new ExprTupleAccess(exp, newBase, exp.getIndex());
        }
        return exp;
    }
    public Object visitExprArrayRange(ExprArrayRange exp) {

        final Expression newBase = doExpression(exp.getBase());

        RangeLen range = exp.getSelection();
        Expression newStart = doExpression(range.start());
        Expression newLen = null;
        if (range.hasLen()) {
            newLen = doExpression(range.getLenExpression());
        }
        if (newBase != exp.getBase() || newStart != range.start() ||
                (newLen != null && newLen != range.getLenExpression()))
        {
            return new ExprArrayRange(exp, newBase, new RangeLen(newStart, newLen));
        }
        return exp;
    }

    public Object visitType(Type t) {
        return t;
    }
    public Object visitTypePrimitive(TypePrimitive t) {
        return t;
    }
    public Object visitTypeArray(TypeArray t) {
        Type nbase = (Type) t.getBase().accept(this);
        Expression nlen = null;
        if (t.getLength() != null) {
            nlen = (Expression) t.getLength().accept(this);
        }
        if (nbase == t.getBase() && t.getLength() == nlen)
            return t;
        TypeArray newtype =
                new TypeArray(t.getCudaMemType(), nbase, nlen, t.getMaxlength());
        return newtype;
    }

    public Set<String> fields = null;
    public Object visitStructDef (StructDef ts) {
        boolean changed = false;
        TypedHashMap<String, Type> map = new TypedHashMap<String, Type>();
        fields = new HashSet<String>();

        StructDef sdf = ts;
        while (sdf != null) {
            for (Entry<String, Type> entry : sdf) {
                fields.add(entry.getKey());
            }
            String pn = sdf.getParentName();
            if (pn != null) {
                sdf = nres.getStruct(pn);
            } else {
                sdf = null;
            }
        }
        for (Entry<String, Type> entry : ts) {
            Type type = (Type) entry.getValue().accept (this);
            changed |= (type != entry.getValue());
            map.put(entry.getKey(), type);
        }
        if (changed) {
            return ts.creator().fields(map).create();
        } else {
            return ts;
        }
    }

    public Object visitTypeStructRef (TypeStructRef tsr) {
        return tsr;
    }

    public Object visitParameter(Parameter par){
        Type t = (Type) par.getType().accept(this);
        if (t == par.getType()) {
            return par;
        } else {
            return new Parameter(par, t, par.getName(), par.getPtype());
        }
    }

    public Object visitStmtFork(StmtFork loop){
        StmtVarDecl decl = (StmtVarDecl) loop.getLoopVarDecl().accept(this);
        Expression niter = (Expression) loop.getIter().accept(this);
        Statement body = (Statement) loop.getBody().accept(this);
        if (decl == loop.getLoopVarDecl() && niter == loop.getIter() &&
                body == loop.getBody())
        {
            return loop;
        }
        return new StmtFork(loop, decl, niter, body);
    }

    public Object visitStmtSpmdfork(StmtSpmdfork loop){
        //System.out.println(loop.toString());
        // StmtVarDecl oldDecl = loop.getLoopVarDecl();
        // StmtVarDecl decl = oldDecl==null ? null : (StmtVarDecl)oldDecl.accept(this);
        Expression nproc = (Expression) loop.getNProc().accept(this);
        Statement body = (Statement) loop.getBody().accept(this);
        if (nproc == loop.getNProc() && body == loop.getBody()) {
            return loop;
        }
        //if (decl == null) { System.out.println("null!"); throw new RuntimeException("change decl to null! " + nproc + " " + body); }
        return new StmtSpmdfork(loop.getCx(), null, nproc, body);
    }

    public Object visitSpmdBarrier(SpmdBarrier stmt) {
        return stmt;
    }  

    public Object visitSpmdPid(SpmdPid stmt) {
        return stmt;
    }  

    public Object visitSpmdNProc(SpmdNProc spmdnproc) {
        return spmdnproc;
    }

    // ADT

    public Object visitStmtSwitch(StmtSwitch stmt) {
        ExprVar var = (ExprVar) stmt.getExpr().accept(this);
        StmtSwitch newStmt = new StmtSwitch(stmt.getContext(), var);

        for (String caseExpr : stmt.getCaseConditions()) {
            Statement body = (Statement) stmt.getBody(caseExpr).accept(this);
            newStmt.addCaseBlock(caseExpr, body);
        }
        return newStmt;
    }

    /** generic tree replacement code */
    public Object visitStmtMinimize(StmtMinimize stmtMinimize) {
        final Expression previous = stmtMinimize.getMinimizeExpr();
        final Object newVariable = previous.accept(this);
        if (newVariable == previous || !(newVariable instanceof Expression)) {
            return stmtMinimize;
        } else {
            return new StmtMinimize((Expression) newVariable, stmtMinimize.userGenerated);
        }
    }

    public Object visitStmtMinLoop(StmtMinLoop stmtMinLoop) {
        final Statement newBody = (Statement) stmtMinLoop.getBody().accept(this);
        if (newBody == stmtMinLoop.getBody()) {
            return stmtMinLoop;
        } else {
            return new StmtMinLoop(stmtMinLoop, newBody);
        }
    }

    public Object visitExprSpecialStar(ExprSpecialStar star) {
        return visitExprStar(star);
    }


    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
        return cudaSyncthreads;
    }

    public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) {
        return cudaThreadIdx;
    }

    public Object visitCudaBlockDim(CudaBlockDim cudaBlockDim) {
        return cudaBlockDim;
    }

    public Object visitCudaInstrumentCall(CudaInstrumentCall instrumentCall) {
        ExprVar expr2 = instrumentCall.getToImplement().acceptAndCast(this);
        ExprVar expr3 = instrumentCall.getImplVariable().acceptAndCast(this);
        if (expr2 != instrumentCall.getToImplement() ||
                expr3 != instrumentCall.getImplVariable())
        {
            return new CudaInstrumentCall(instrumentCall, expr2, expr3,
                    instrumentCall.getImplName());
        }
        return instrumentCall;
    }

    public Object visitExprRange(ExprRange exprRange) {
        Expression nextFrom = (Expression) exprRange.getFrom().accept(this);
        Expression nextTo = (Expression) exprRange.getUntil().accept(this);
        Expression nextBy = exprRange.getBy().acceptAndCast(this);
        if (nextFrom != exprRange.getFrom() || nextTo != exprRange.getUntil() ||
                nextBy != exprRange.getBy())
        {
            return new ExprRange(exprRange, nextFrom, nextTo, nextBy);
        }
        return exprRange;
    }

    public Object visitStmtParfor(StmtParfor stmtParfor) {
        final StmtVarDecl iterVarDecl = stmtParfor.getIterVarDecl();
        StmtVarDecl nextVarDecl = null;
        if (iterVarDecl != null) {
            nextVarDecl = iterVarDecl.acceptAndCast(this);
        }
        ExprRange nextRange = stmtParfor.getRange().acceptAndCast(this);
        Statement nextBody = stmtParfor.getBody().acceptAndCast(this);

        if (nextVarDecl != iterVarDecl || nextRange != stmtParfor.getRange() ||
                nextBody != stmtParfor.getBody())
        {
            return stmtParfor.next(nextVarDecl, nextRange, nextBody);
        }
        return stmtParfor;
    }

    public Object visitStmtImplicitVarDecl(StmtImplicitVarDecl decl) {
        return decl;
    }

    public Object visitExprNamedParam(ExprNamedParam exprNamedParam) {
        Expression sub = exprNamedParam.getExpr().acceptAndCast(this);
        if (sub != exprNamedParam.getExpr()) {
            return exprNamedParam.next(sub);
        }
        return exprNamedParam;
    }

    public Object visitExprType(ExprType exprtyp) {
        assertFalse("Not implemented: visitExprType()");
        return null;
    }

}
