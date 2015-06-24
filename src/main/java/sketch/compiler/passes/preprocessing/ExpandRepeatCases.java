package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtSwitch;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

class CloneHoles extends FEReplacer {


    public Object visitExprStar(ExprStar es) {
        ExprStar newStar = new ExprStar(es);
        es.renewName();
        return newStar;
    }

    public Statement process(Statement s) {
        return (Statement) s.accept(this);
    }

    public Object visitExprFunCall(ExprFunCall exp) {
        List<Expression> newParams = new ArrayList<Expression>();
        for (Iterator iter = exp.getParams().iterator(); iter.hasNext();) {
            Expression param = (Expression) iter.next();
            Expression newParam = doExpression(param);
            newParams.add(newParam);
        }
        ExprFunCall rv = new ExprFunCall(exp, exp.getName(), newParams);
        rv.resetCallid();
        return rv;
    }

}

public class ExpandRepeatCases extends SymbolTableVisitor {

    public ExpandRepeatCases() {
        super(null);

    }

    @Override
    public Object visitStmtSwitch(StmtSwitch stmt){
        StmtSwitch newStmt = new StmtSwitch(stmt.getContext(), stmt.getExpr());
        TypeStructRef tres = (TypeStructRef) getType(stmt.getExpr());
        LinkedList<String> queue = new LinkedList<String>();
        String name = nres.getStruct(tres.getName()).getFullName();
        queue.add(name);
        // List<String> children = nres.getStructChildren(tres.getName());
        if (stmt.getCaseConditions().size() == 0) {
            return stmt;
        }
        String c = stmt.getCaseConditions().get(0);
        if (c == "repeat") {

            while (!queue.isEmpty()) {

                String parent = queue.removeFirst();
                List<String> children = nres.getStructChildren(parent);
                if (children.isEmpty()) {
                    Statement body = (Statement) stmt.getBody(c).accept(this);
                    body = (Statement) (new CloneHoles()).process(body).accept(this);
                    newStmt.addCaseBlock(parent.split("@")[0], body);
                } else {
                    queue.addAll(children);

                }
            }

            return newStmt;
        } else {
            return stmt;
        }

    }

}
