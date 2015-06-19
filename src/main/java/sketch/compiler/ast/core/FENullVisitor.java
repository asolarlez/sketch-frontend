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
 * Implementation of FEVisitor that always returns <code>null</code>.
 * This is intended to be a base class for other visitors that only
 * visit a subset of the node tree, and don't want to return objects
 * of the same type as the parameter.  {@link
 * sketch.compiler.nodes.FEReplacer} is a better default for
 * transformations on the tree.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class FENullVisitor implements FEVisitor
{
	public Object visitExprAlt(ExprAlt exp) { return null; }

    public Object visitExprTuple(ExprTuple exp) {
        return null;
    }

    public Object visitExprTupleAccess(ExprTupleAccess exp) {
        return null;
    }
    public Object visitExprArrayInit(ExprArrayInit exp) { return null; }
    public Object visitExprBinary(ExprBinary exp) { return null; }
    public Object visitExprChoiceBinary(ExprChoiceBinary exp) { return null; }
    public Object visitExprChoiceSelect(ExprChoiceSelect exp) { return null; }
    public Object visitExprChoiceUnary(ExprChoiceUnary exp) { return null; }
    public Object visitExprConstChar(ExprConstChar exp) { return null; }
    public Object visitExprConstFloat(ExprConstFloat exp) { return null; }
    public Object visitExprConstInt(ExprConstInt exp) { return null; }
    public Object visitExprLiteral(ExprLiteral exp) { return null; }
    public Object visitExprField(ExprField exp) { return null; }
    public Object visitExprFunCall(ExprFunCall exp) { return null; }
    public Object visitExprParen(ExprParen exp) { return null; }
    public Object visitExprRegen(ExprRegen exp) { return null; }
    public Object visitExprTernary(ExprTernary exp) { return null; }
    public Object visitExprTypeCast(ExprTypeCast exp) { return null; }
    public Object visitExprUnary(ExprUnary exp) { return null; }
    public Object visitExprVar(ExprVar exp) { return null; }

    public Object visitExprStar(ExprStar star) {
        return null;
    }

    public Object visitExprArrayRange(ExprArrayRange exp) {
        return null;
    }

    public Object visitExprNew(ExprNew expNew) {
        return null;
    }

    public Object visitExprNullPtr(ExprNullPtr nptr) {
        return null;
    }

    public Object visitExprRange(ExprRange exprRange) {
        return null;
    }

    public Object visitFieldDecl(FieldDecl field) { return null; }
    public Object visitFunction(Function func) { return null; }    
    public Object visitProgram(Program prog) { return null; }    


    public Object visitStmtAssign(StmtAssign stmt) {
        return null;
    }

    public Object visitStmtAtomicBlock(StmtAtomicBlock stmt) {
        return null;
    }
    public Object visitStmtBlock(StmtBlock stmt) { return null; }
    public Object visitStmtBreak(StmtBreak stmt) { return null; }
    public Object visitStmtContinue(StmtContinue stmt) { return null; }
    public Object visitStmtDoWhile(StmtDoWhile stmt) { return null; }
    public Object visitStmtEmpty(StmtEmpty stmt) { return null; }
    public Object visitStmtExpr(StmtExpr stmt) { return null; }
    public Object visitStmtFor(StmtFor stmt) { return null; }
    public Object visitStmtIfThen(StmtIfThen stmt) { return null; }
    public Object visitStmtInsertBlock (StmtInsertBlock stmt) { return null; }
    public Object visitStmtJoin(StmtJoin stmt) { return null; }
    public Object visitStmtLoop(StmtLoop stmt) { return null; }
    public Object visitStmtReturn(StmtReturn stmt) { return null; }
    public Object visitStmtAssert(StmtAssert stmt) { return null; }
    public Object visitStmtVarDecl(StmtVarDecl stmt) { return null; }
    public Object visitStmtWhile(StmtWhile stmt) { return null; }

    public Object visitStmtFunDecl(StmtFunDecl stmt) {
        return null;
    }

    public Object visitPackage(Package spec) {
        return null;
    }

    public Object visitOther(FENode node) { return null; }
	public Object visitType(Type t) { return null; }
    public Object visitTypePrimitive(TypePrimitive t) { return null; }
    public Object visitTypeArray(TypeArray t) { return null; }
    public Object visitStructDef(StructDef t) { return null; }
    public Object visitTypeStructRef(TypeStructRef t) { return null; }
    public Object visitParameter(Parameter par){ return null; }
    public Object visitStmtFork(StmtFork loop){ return null; }
    public Object visitStmtSpmdfork(StmtSpmdfork loop){ return null; }
    public Object visitSpmdPid(SpmdPid x){ return null; }

    public Object visitSpmdNProc(SpmdNProc spmdnproc) {
        return null;
    }
    public Object visitSpmdBarrier(SpmdBarrier stmt){ return null; }
    public Object visitStmtReorderBlock(StmtReorderBlock block){return null;}

	public Object visitStmtSwitch(StmtSwitch sw) { return null; }
	public Object visitStmtMinimize(StmtMinimize stmtMinimize) { return null; }
    public Object visitStmtMinLoop(StmtMinLoop stmtMinLoop) { return null; }
    public Object visitExprSpecialStar(ExprSpecialStar var) { return null; }
    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) { return null; }
    public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) { return null; }
    public Object visitCudaBlockDim(CudaBlockDim cudaBlockDim) { return null; }
    public Object visitCudaInstrumentCall(CudaInstrumentCall cudaInstrumentCall) { return null; }
    public Object visitStmtParfor(StmtParfor stmtParfor) { return null; }
    public Object visitStmtImplicitVarDecl(StmtImplicitVarDecl decl) { return null; }
    public Object visitExprNamedParam(ExprNamedParam exprNamedParam) { return null; }
    public Object visitExprType(ExprType exprtyp) { return null; }

    public Object visitStmtAssume(StmtAssume stmtAssume) {
        return null;
    }

    public Object visitExprFieldMacro(ExprFieldMacro exp) {

        return null;
    }

	/**
	 * A local variable expression visitor that always returns null.
	 */
    public Object visitExprLocalVariables(ExprLocalVariables exprLocalVariables) {
		// TODO MIGUEL here just return null
        return null;
    }
}
