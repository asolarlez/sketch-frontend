package sketch.compiler.passes.cuda;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.cuda.exprs.CudaThreadIdx;
import sketch.compiler.ast.cuda.stmts.CudaSyncthreads;
import sketch.compiler.ast.cuda.stmts.StmtParfor;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.structure.ASTQuery;
import sketch.util.cuda.CudaThreadBlockDim;

import static sketch.util.DebugOut.assertFalse;

/**
 * lower parfor() style loops
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class ReplaceParforLoops extends FEReplacer {
    private final CudaThreadBlockDim cudaBlockDim;
    private boolean enclosingIsParallel;

    public ReplaceParforLoops(CudaThreadBlockDim cudaBlockDim) {
        this.cudaBlockDim = cudaBlockDim;
    }

    @Override
    public Object visitFunction(Function func) {
        this.enclosingIsParallel = func.isParallel();
        return super.visitFunction(func);
    }

    @Override
    public Object visitStmtParfor(StmtParfor stmtParfor) {
        if (!enclosingIsParallel) {
            assertFalse("parfor must be in device / global function");
        }
        final StmtVarDecl iterVarDecl = stmtParfor.getAssociatedIterVarDecl();
        final String iterVarName = iterVarDecl.getName(0);
        if (new VariableAssigned(iterVarName).run(stmtParfor)) {
            assertFalse("Cannot assign to iteration variable", iterVarName);
        }

        final ExprVar iterVarRef = new ExprVar(iterVarDecl, iterVarName);

        final Expression rangeFrom = stmtParfor.getRange().getFrom();
        final Expression rangeTo = stmtParfor.getRange().getTo();
        final ExprBinary start =
                new ExprBinary(rangeFrom, "+", new CudaThreadIdx(null, "x"));
        final ExprBinary cond = new ExprBinary(iterVarRef, "<", rangeTo);
        final StmtAssign update =
                new StmtAssign(iterVarRef, cudaBlockDim.blockDimConstInt("x"),
                        ExprBinary.BINOP_ADD);
        final StmtVarDecl lowLevelInit =
                new StmtVarDecl(iterVarDecl, iterVarDecl.getType(0), iterVarName, start);
        Statement loop =
                new StmtFor(stmtParfor, lowLevelInit, cond, update, stmtParfor.getBody());
        addStatement(new CudaSyncthreads(stmtParfor.getCx()));
        addStatement(loop);
        return new CudaSyncthreads(stmtParfor.getCx());
    }

    public static class VariableAssigned extends ASTQuery {
        protected final String varname;

        public VariableAssigned(String varname) {
            this.varname = varname;
        }

        @Override
        public Object visitStmtAssign(StmtAssign stmt) {
            result |= stmt.getLhsBase().getName().equals(varname);
            return super.visitStmtAssign(stmt);
        }
    }
}
