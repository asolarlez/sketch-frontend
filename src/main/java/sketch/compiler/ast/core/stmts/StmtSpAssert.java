package sketch.compiler.ast.core.stmts;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprFunCall;

public class StmtSpAssert extends Statement {

    ExprFunCall f1;
    ExprFunCall f2;
    int stateCount;

    public StmtSpAssert(FEContext context, ExprFunCall f12, ExprFunCall f22) {
        super(context);
        this.f1 = f12;
        this.f2 = f22;
        stateCount = 0;
    }

    @Override
    public Object accept(FEVisitor v) {
        // TODO Auto-generated method stub
        return null;
    }

    public ExprFunCall getFirstFun() {
        return f1;
    }

    public ExprFunCall getSecondFun() {
        return f2;
    }

    public void setStateCount(int count) {
        this.stateCount = count;
    }

    public int getStateCount() {
        return stateCount;
    }
}
