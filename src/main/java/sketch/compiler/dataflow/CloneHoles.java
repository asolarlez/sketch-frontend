package sketch.compiler.dataflow;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprHole;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;

public class CloneHoles extends FEReplacer {

    // TODO xzl: what's this?
    public Object visitExprStar(ExprHole es) {
        ExprHole newStar = new ExprHole(es);
        if (es.special())
            newStar.makeSpecial(es.parentHoles());
        es.renewName();
        return newStar;
    }

    public Statement process(Statement s) {
        return (Statement) s.accept(this);
    }

    public Expression process(Expression e) {
        return (Expression) e.accept(this);
    }

    public Object visitExprFunCall(ExprFunCall exp) {
        List<Expression> newParams = new ArrayList<Expression>();
        for (Expression param : exp.getParams()) {
            Expression newParam = doExpression(param);
            newParams.add(newParam);
        }
        ExprFunCall rv = new ExprFunCall(exp, exp.getName(), newParams, exp.getTypeParams());
        rv.resetCallid();
        return rv;
    }

    public Object visitExprNew(ExprNew exp) {
        ExprNew nexp = (ExprNew) super.visitExprNew(exp);
        if (nexp.isHole() && nexp.getStar() != null) {
            ExprHole newStar = (ExprHole) nexp.getStar().accept(this);
        }
        return nexp;
    }

    public Object visitExprField(ExprField exp) {
        if (exp.isHole()) {
            return new ExprField(exp, exp.getLeft(), exp.getName(), true);
        }
        return exp;
    }

}
