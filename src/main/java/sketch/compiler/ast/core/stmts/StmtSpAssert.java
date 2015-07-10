package sketch.compiler.ast.core.stmts;

import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;

public class StmtSpAssert extends Statement {

    ExprFunCall f1;
    ExprFunCall f2;
    int stateCount;
    Map<String, Expression> varBindings;
    Expression preCond;
    List<Expression> asserts;
    List<String> bindingsInOrder;

    public StmtSpAssert(FEContext context, ExprFunCall f12, ExprFunCall f22) {
        super(context);
        this.f1 = f12;
        this.f2 = f22;
        stateCount = 0;
    }

    public StmtSpAssert(FEContext context, Map<String, Expression> set,
            Expression preCond, List<Expression> asserts, List<String> bindingsInOrder)
    {
        super(context);
        this.varBindings = set;
        this.preCond = preCond;
        this.asserts = asserts;
        this.bindingsInOrder = bindingsInOrder;
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

    public Expression getExprForVar(String var) {
        return varBindings.get(var);
    }

    public Expression getPreCond() {
        return preCond;
    }

    public List<Expression> getAssertExprs() {
        return asserts;
    }

    public Map<String, Expression> getVarBindings() {
        return varBindings;
    }

    public List<String> bindingsInOrder() {
        return bindingsInOrder;
    }
}
