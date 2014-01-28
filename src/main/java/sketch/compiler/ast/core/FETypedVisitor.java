/*
 * Copyright 2003 by the Massachusetts Institute of Technology. Permission to use, copy,
 * modify, and distribute this software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright notice appear in all copies
 * and that both that copyright notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in advertising or publicity
 * pertaining to distribution of the software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of this software for any purpose.
 * It is provided "as is" without express or implied warranty.
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
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.StructDef;
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
 * typed version of <code>FENullVisitor</code>
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class FETypedVisitor<T> implements FEVisitor {
    public T visitExprAlt(ExprAlt exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprArrayInit(ExprArrayInit exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprArrayRange(ExprArrayRange exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprBinary(ExprBinary exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprChoiceBinary(ExprChoiceBinary exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprChoiceSelect(ExprChoiceSelect exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprChoiceUnary(ExprChoiceUnary exp) {
        throw new FEVisitorException(this, exp);
    }



    public T visitExprConstChar(ExprConstChar exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprConstFloat(ExprConstFloat exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprConstInt(ExprConstInt exp) {
        throw new FEVisitorException(this, exp);
    }


    public T visitExprField(ExprField exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprFunCall(ExprFunCall exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprLiteral(ExprLiteral exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprNew(ExprNew expNew) {
        throw new FEVisitorException(this, expNew);
    }

    public T visitExprNullPtr(ExprNullPtr nptr) {
        throw new FEVisitorException(this, nptr);
    }

    public T visitExprParen(ExprParen exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprRegen(ExprRegen exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprStar(ExprStar star) {
        throw new FEVisitorException(this, star);
    }

    public T visitExprTernary(ExprTernary exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprTypeCast(ExprTypeCast exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprUnary(ExprUnary exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitExprVar(ExprVar exp) {
        throw new FEVisitorException(this, exp);
    }

    public T visitFieldDecl(FieldDecl field) {
        throw new FEVisitorException(this, field);
    }

   
    public T visitFunction(Function func) {
        throw new FEVisitorException(this, func);
    }

    public T visitStmtMinimize(StmtMinimize stmtMinimize) {
        throw new FEVisitorException(this, stmtMinimize);
    }

    public T visitOther(FENode node) {
        throw new FEVisitorException(this, node);
    }

    public T visitParameter(Parameter par) {
        throw new FEVisitorException(this, par);
    }

    public T visitProgram(Program prog) {
        throw new FEVisitorException(this, prog);
    }




    public T visitStmtAssert(StmtAssert stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtAssume(StmtAssume stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtAssign(StmtAssign stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtAtomicBlock(StmtAtomicBlock stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtBlock(StmtBlock stmt) {
        throw new FEVisitorException(this, stmt);
    }


    public T visitStmtBreak(StmtBreak stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtContinue(StmtContinue stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtDoWhile(StmtDoWhile stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtFunDecl(StmtFunDecl stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtEmpty(StmtEmpty stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtExpr(StmtExpr stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtFor(StmtFor stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtFork(StmtFork loop) {
        throw new FEVisitorException(this, loop);
    }

    public T visitStmtSpmdfork(StmtSpmdfork loop) {
        throw new FEVisitorException(this, loop);
    }

    public T visitSpmdBarrier(SpmdBarrier stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitSpmdPid(SpmdPid stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public Object visitSpmdNProc(SpmdNProc spmdnproc) {
        throw new FEVisitorException(this, spmdnproc);
    }

    public T visitStmtIfThen(StmtIfThen stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtInsertBlock(StmtInsertBlock stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtJoin(StmtJoin stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtLoop(StmtLoop stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtMinLoop(StmtMinLoop stmtMinLoop) {
        throw new FEVisitorException(this, stmtMinLoop);
    }

    public T visitStmtReorderBlock(StmtReorderBlock block) {
        throw new FEVisitorException(this, block);
    }

    public T visitStmtReturn(StmtReturn stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtSwitch(StmtSwitch sw) {
        throw new FEVisitorException(this, sw);
    }

    public T visitStmtVarDecl(StmtVarDecl stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitStmtWhile(StmtWhile stmt) {
        throw new FEVisitorException(this, stmt);
    }

    public T visitPackage(Package spec) {
        throw new FEVisitorException(this, spec);
    }


    public T visitType(Type t) {
        throw new FEVisitorException(this, t);
    }

    public T visitTypeArray(TypeArray t) {
        throw new FEVisitorException(this, t);
    }

    public T visitTypePrimitive(TypePrimitive t) {
        throw new FEVisitorException(this, t);
    }

    public T visitStructDef(StructDef ts) {
        throw new FEVisitorException(this, ts);
    }

    public T visitTypeStructRef(TypeStructRef ts) {
        throw new FEVisitorException(this, ts);
    }

    public Object visitExprSpecialStar(ExprSpecialStar var) {
        throw new FEVisitorException(this, var);
    }


    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
        throw new FEVisitorException(this, cudaSyncthreads);
    }

    public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) {
        throw new FEVisitorException(this, cudaThreadIdx);
    }

    public Object visitCudaBlockDim(CudaBlockDim cudaBlockDim) {
        throw new FEVisitorException(this, cudaBlockDim);
    }

    public Object visitCudaInstrumentCall(CudaInstrumentCall instrumentCall) {
        throw new FEVisitorException(this, instrumentCall);
    }

    public Object visitExprRange(ExprRange exprRange) {
        throw new FEVisitorException(this, exprRange);
    }

    public Object visitStmtParfor(StmtParfor stmtParfor) {
        throw new FEVisitorException(this, stmtParfor);
    }

    public Object visitStmtImplicitVarDecl(StmtImplicitVarDecl decl) {
        throw new FEVisitorException(this, decl);
    }

    public Object visitExprNamedParam(ExprNamedParam exprNamedParam) {
        throw new FEVisitorException(this, exprNamedParam);
    }

    public Object visitExprType(ExprType exprtyp) {
        throw new FEVisitorException(this, exprtyp);
    }

    public Object visitExprFieldMacro(ExprFieldMacro exp) {
        // TODO Auto-generated method stub
        throw new FEVisitorException(this, exp);
    }
}
