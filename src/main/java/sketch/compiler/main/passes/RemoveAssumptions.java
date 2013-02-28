package sketch.compiler.main.passes;

import java.util.List;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.passes.annotations.CompilerPassDeps;

@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class RemoveAssumptions extends FEReplacer {
    private Set<String> topFuncs;
    private List<Expression> assumptions;
    
    public final static String nopName = InsertAssumptions.nopName;

    @Override
    public Object visitStmtIfThen(StmtIfThen s) {
        if (s.getAlt() instanceof StmtBlock &&
                ((StmtBlock) s.getAlt()).getStmts().size() == 1)
        {
            Statement t = ((StmtBlock) s.getAlt()).getStmts().get(0);
            if (t instanceof StmtExpr &&
                    ((StmtExpr) t).getExpression() instanceof ExprFunCall)
            {
                ExprFunCall c = (ExprFunCall) ((StmtExpr) t).getExpression();
                if (c.getName() == nopName) {
                    Statement cons = s.getCons();
                    if (cons != null) {
                        return cons.accept(this);
                    } else {
                        return null;
                    }
                }
            }
        }
        return super.visitStmtIfThen(s);
    }
}
