package sketch.compiler.ast.core.stmts;
import java.util.HashMap;
import java.util.LinkedList;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;


public class StmtSwitch extends Statement {

	Expression cond;
    Statement body;
	
	LinkedList<Expression> cases;
	HashMap<Expression, Statement> bodies; 
	
    public StmtSwitch(FENode node, Expression cond, Statement body) {
		super(node);
		
		this.cond = cond;
        this.body = body;
		
		cases = new LinkedList<Expression>();
		bodies = new HashMap<Expression, Statement>();
	}
	
    public StmtSwitch(FEContext context, Expression x, Statement b) {
        super(context);
        this.cond = cond;
        this.body = body;
    }

    public void addCaseBlock(Expression caseExpr, LinkedList<Statement> caseBody) {
		assert caseExpr != null;
		assert caseBody != null;
		
		cases.addLast(caseExpr);
		FENode dummy = null;
		bodies.put(caseExpr, new StmtBlock(dummy, caseBody));
	}
	public void addCaseBlock(Expression caseExpr, Statement caseBody) {
		assert caseExpr != null;
		assert caseBody != null;
		
		cases.addLast(caseExpr);
		bodies.put(caseExpr, caseBody);
	}
	
	@Override
	public Object accept(FEVisitor v) {
		return v.visitStmtSwitch(this);
	}

	public Expression getCond() {
		return cond;
	}
	
	public Iterable<Expression> getCaseConditions() {
		return cases;
	}
	
	public Statement getBody(Expression caseExpr) {
		return bodies.get(caseExpr);
	}

}
