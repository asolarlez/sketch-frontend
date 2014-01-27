package sketch.compiler.ast.core.stmts;
import java.util.HashMap;
import java.util.LinkedList;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprVar;


public class StmtSwitch extends Statement {


    ExprVar expr;

	
    LinkedList<String> cases;
    HashMap<String, Statement> bodies;
    boolean mustBeCloned = false;
	
    public StmtSwitch(FENode node, ExprVar expr) {
		super(node);
		
        this.expr = expr;
		
        cases = new LinkedList<String>();
        bodies = new HashMap<String, Statement>();
	}
	
    public StmtSwitch(FEContext context, ExprVar expr) {
        super(context);
        this.expr = expr;
        cases = new LinkedList<String>();
        bodies = new HashMap<String, Statement>();
    }



    public void addCaseBlock(String caseName, Statement caseBody) {
        assert caseName != null;
		assert caseBody != null;
		
        cases.addLast(caseName);
        bodies.put(caseName, caseBody);
	}

    public void updateCaseBody(String caseName, Statement caseBody) {
        bodies.put(caseName, caseBody);
    }

    public void setMustBeCloned() {
        mustBeCloned = true;
    }

    public boolean mustBeCloned() {
        return mustBeCloned;
    }
	@Override
	public Object accept(FEVisitor v) {
		return v.visitStmtSwitch(this);
	}

    public ExprVar getExpr() {
        return expr;
	}

    public void updateExpr(ExprVar exp) {
        this.expr = exp;
    }
	
    public LinkedList<String> getCaseConditions() {
		return cases;
	}
	
    public Statement getBody(String caseExpr) {
		return bodies.get(caseExpr);
	}

    public String toString() {
        String result = "switch(";
        result = result + expr.getName() + "):\n";
        for (String c : cases) {
            result = result + "case " + c + "{";
            result = result + getBody(c) + "}";
        }

        return result;
    }

}
