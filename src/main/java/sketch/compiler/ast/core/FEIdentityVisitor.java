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

public class FEIdentityVisitor implements FEVisitor {

    public Object visitExprAlt(ExprAlt exp) {
        return exp;
    }

    public Object visitExprTuple(ExprTuple exp) {
        return exp;
    }

    public Object visitExprArrayInit(ExprArrayInit exp) {
        return exp;
    }

    public Object visitExprADTHole(ExprADTHole exp) {
        return exp;
    }

    public Object visitExprTupleAccess(ExprTupleAccess exp) {
        return exp;
    }

    public Object visitExprArrayRange(ExprArrayRange exp) {

        return exp;
    }

    public Object visitExprBinary(ExprBinary exp) {

        return exp;
    }

    public Object visitExprChoiceBinary(ExprChoiceBinary exp) {

        return exp;
    }

    public Object visitExprChoiceSelect(ExprChoiceSelect exp) {

        return exp;
    }

    public Object visitExprChoiceUnary(ExprChoiceUnary exp) {

        return exp;
    }

    public Object visitExprConstChar(ExprConstChar exp) {

        return exp;
    }

    public Object visitExprConstFloat(ExprConstFloat exp) {

        return exp;
    }

    public Object visitExprConstInt(ExprConstInt exp) {

        return exp;
    }

    public Object visitExprLiteral(ExprLiteral exp) {

        return exp;
    }

    public Object visitExprField(ExprField exp) {

        return exp;
    }

    public Object visitExprFunCall(ExprFunCall exp) {

        return exp;
    }

    public Object visitExprParen(ExprParen exp) {

        return exp;
    }

    public Object visitExprRegen(ExprRegen exp) {

        return exp;
    }

    public Object visitExprStar(ExprStar star) {

        return star;
    }

    public Object visitExprTernary(ExprTernary exp) {

        return exp;
    }

    public Object visitExprTypeCast(ExprTypeCast exp) {

        return exp;
    }

    public Object visitExprUnary(ExprUnary exp) {

        return exp;
    }

    public Object visitExprVar(ExprVar exp) {
        return exp;
    }

    public Object visitFieldDecl(FieldDecl field) {
        return field;
    }

    public Object visitFunction(Function func) {
        return func;
    }

    public Object visitProgram(Program prog) {
        return prog;
    }

    public Object visitStmtAssign(StmtAssign stmt) {

        return stmt;
    }

    public Object visitStmtAtomicBlock(StmtAtomicBlock stmt) {

        return stmt;
    }

    public Object visitStmtBlock(StmtBlock stmt) {

        return stmt;
    }

    public Object visitStmtBreak(StmtBreak stmt) {

        return stmt;
    }

    public Object visitStmtContinue(StmtContinue stmt) {

        return stmt;
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt) {

        return stmt;
    }

    public Object visitStmtEmpty(StmtEmpty stmt) {

        return stmt;
    }

    public Object visitStmtExpr(StmtExpr stmt) {

        return stmt;
    }

    public Object visitStmtFor(StmtFor stmt) {

        return stmt;
    }

    public Object visitStmtIfThen(StmtIfThen stmt) {

        return stmt;
    }

    public Object visitStmtInsertBlock(StmtInsertBlock stmt) {

        return stmt;
    }

    public Object visitStmtJoin(StmtJoin stmt) {

        return stmt;
    }

    public Object visitStmtLoop(StmtLoop stmt) {

        return stmt;
    }

    public Object visitStmtReturn(StmtReturn stmt) {

        return stmt;
    }

    public Object visitStmtAssert(StmtAssert stmt) {

        return stmt;
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt) {

        return stmt;
    }

    public Object visitStmtWhile(StmtWhile stmt) {

        return stmt;
    }

    public Object visitStmtFunDecl(StmtFunDecl stmt) {

        return stmt;
    }

    public Object visitPackage(Package pkg) {

        return pkg;
    }

    public Object visitOther(FENode node) {

        return node;
    }

    public Object visitType(Type t) {

        return t;
    }

    public Object visitTypePrimitive(TypePrimitive t) {

        return t;
    }

    public Object visitTypeArray(TypeArray t) {

        return t;
    }

    public Object visitStructDef(StructDef ts) {

        return ts;
    }

    public Object visitTypeStructRef(TypeStructRef ts) {

        return ts;
    }

    public Object visitParameter(Parameter par) {

        return par;
    }

    public Object visitExprNew(ExprNew expNew) {

        return expNew;
    }

    public Object visitStmtFork(StmtFork loop) {

        return loop;
    }

    public Object visitStmtReorderBlock(StmtReorderBlock block) {

        return block;
    }

    public Object visitStmtSwitch(StmtSwitch sw) {

        return sw;
    }

    public Object visitExprNullPtr(ExprNullPtr nptr) {

        return nptr;
    }

    public Object visitStmtMinimize(StmtMinimize stmtMinimize) {

        return stmtMinimize;
    }

    public Object visitStmtMinLoop(StmtMinLoop stmtMinLoop) {

        return stmtMinLoop;
    }

    public Object visitExprFieldsListMacro(ExprFieldsListMacro exp) {

        return exp;
    }

    public Object visitExprSpecialStar(ExprSpecialStar exprSpecialStar) {

        return exprSpecialStar;
    }

    public Object visitCudaThreadIdx(CudaThreadIdx cudaThreadIdx) {

        return cudaThreadIdx;
    }

    public Object visitCudaBlockDim(CudaBlockDim cudaBlockDim) {

        return cudaBlockDim;
    }

    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {

        return cudaSyncthreads;
    }

    public Object visitCudaInstrumentCall(CudaInstrumentCall instrumentCall) {

        return instrumentCall;
    }

    public Object visitExprRange(ExprRange exprRange) {

        return exprRange;
    }

    public Object visitStmtParfor(StmtParfor stmtParfor) {

        return stmtParfor;
    }

    public Object visitStmtImplicitVarDecl(StmtImplicitVarDecl decl) {

        return decl;
    }

    public Object visitExprNamedParam(ExprNamedParam exprNamedParam) {

        return exprNamedParam;
    }

    public Object visitExprType(ExprType exprtyp) {

        return exprtyp;
    }

    public Object visitStmtSpmdfork(StmtSpmdfork stmtSpmdfork) {

        return stmtSpmdfork;
    }

    public Object visitSpmdBarrier(SpmdBarrier spmdBarrier) {

        return spmdBarrier;
    }

    public Object visitSpmdPid(SpmdPid spmdpid) {

        return spmdpid;
    }

    public Object visitSpmdNProc(SpmdNProc spmdnproc) {

        return spmdnproc;
    }

    public Object visitStmtAssume(StmtAssume stmtAssume) {

        return stmtAssume;
    }

    public Object visitExprLocalVariables(ExprLocalVariables exprLocalVariables) {

        return exprLocalVariables;
    }

    public Object visitExprLambda(ExprLambda exprLambda) {

        return exprLambda;
    }

}
