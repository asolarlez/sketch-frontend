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
import sketch.compiler.ast.core.exprs.*;
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

/**
 * Visitor interface for <code>FENode</code> nodes. This class implements part of the
 * "visitor" design pattern for <code>FENode</code>s. The pattern basically exchanges type
 * structures for function calls, so a different function in the visitor is called
 * depending on the run-time type of the object being visited. Calling visitor methods
 * returns some value, the type of which depends on the semantics of the visitor in
 * question. In general, you will create a visitor object, and then pass it to the
 * <code>FENode.accept()</code> method of the object in question.
 * 
 * @author David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public interface FEVisitor
{
	public Object visitExprAlt(ExprAlt exp);

    public Object visitExprTuple(ExprTuple exp);
    public Object visitExprArrayInit(ExprArrayInit exp);

    public Object visitExprADTHole(ExprADTHole exp);
    public Object visitExprTupleAccess(ExprTupleAccess exp);
    public Object visitExprArrayRange(ExprArrayRange exp);
    public Object visitExprBinary(ExprBinary exp);
    public Object visitExprChoiceBinary(ExprChoiceBinary exp);
    public Object visitExprChoiceSelect(ExprChoiceSelect exp);
    public Object visitExprChoiceUnary(ExprChoiceUnary exp);
    public Object visitExprConstChar(ExprConstChar exp);
    public Object visitExprConstFloat(ExprConstFloat exp);
    public Object visitExprConstInt(ExprConstInt exp);
    public Object visitExprLiteral(ExprLiteral exp);
    public Object visitExprField(ExprField exp);
    public Object visitExprFunCall(ExprFunCall exp);
    public Object visitExprParen(ExprParen exp);
    public Object visitExprRegen(ExprRegen exp);
	public Object visitExprStar(ExprStar star);
    public Object visitExprTernary(ExprTernary exp);
    public Object visitExprTypeCast(ExprTypeCast exp);
    public Object visitExprUnary(ExprUnary exp);
    public Object visitExprVar(ExprVar exp);
    public Object visitFieldDecl(FieldDecl field);
    public Object visitFunction(Function func);
    
    public Object visitProgram(Program prog);    
    public Object visitStmtAssign(StmtAssign stmt);
    public Object visitStmtAtomicBlock(StmtAtomicBlock stmt);
    public Object visitStmtBlock(StmtBlock stmt);
    public Object visitStmtBreak(StmtBreak stmt);
    public Object visitStmtContinue(StmtContinue stmt);
    public Object visitStmtDoWhile(StmtDoWhile stmt);
    public Object visitStmtEmpty(StmtEmpty stmt);
    public Object visitStmtExpr(StmtExpr stmt);
    public Object visitStmtFor(StmtFor stmt);
    public Object visitStmtIfThen(StmtIfThen stmt);
    public Object visitStmtInsertBlock (StmtInsertBlock stmt);
    public Object visitStmtJoin(StmtJoin stmt);
    public Object visitStmtLoop(StmtLoop stmt);
    public Object visitStmtReturn(StmtReturn stmt);
    public Object visitStmtAssert(StmtAssert stmt);
    public Object visitStmtVarDecl(StmtVarDecl stmt);
    public Object visitStmtWhile(StmtWhile stmt);
    public Object visitStmtFunDecl(StmtFunDecl stmt);
    public Object visitPackage(Package spec);    
    public Object visitOther(FENode node);
    public Object visitType(Type t);
    public Object visitTypePrimitive(TypePrimitive t);
    public Object visitTypeArray(TypeArray t);
    public Object visitStructDef(StructDef ts);
    public Object visitTypeStructRef(TypeStructRef ts);
    public Object visitParameter(Parameter par);
    public Object visitExprNew(ExprNew expNew);
    public Object visitStmtFork(StmtFork loop);
    public Object visitStmtReorderBlock(StmtReorderBlock block);
    public Object visitStmtSwitch(StmtSwitch sw);
    public Object visitExprNullPtr(ExprNullPtr nptr);
    public Object visitStmtMinimize(StmtMinimize stmtMinimize);
    public Object visitStmtMinLoop(StmtMinLoop stmtMinLoop);

    public Object visitExprFieldsListMacro(ExprFieldsListMacro exp);

    public Object visitExprSpecialStar(ExprSpecialStar exprSpecialStar);

    public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx);
    public Object visitCudaBlockDim(CudaBlockDim cudaBlockDim);
    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads);
    public Object visitCudaInstrumentCall(CudaInstrumentCall instrumentCall);
    public Object visitExprRange(ExprRange exprRange);
    public Object visitStmtParfor(StmtParfor stmtParfor);
    public Object visitStmtImplicitVarDecl(StmtImplicitVarDecl decl);
    public Object visitExprNamedParam(ExprNamedParam exprNamedParam);
    public Object visitExprType(ExprType exprtyp);
    public Object visitStmtSpmdfork(StmtSpmdfork stmtSpmdfork);
    public Object visitSpmdBarrier(SpmdBarrier spmdBarrier);

    public Object visitSpmdPid(SpmdPid spmdpid);

    public Object visitSpmdNProc(SpmdNProc spmdnproc);

    public Object visitStmtAssume(StmtAssume stmtAssume);
}
