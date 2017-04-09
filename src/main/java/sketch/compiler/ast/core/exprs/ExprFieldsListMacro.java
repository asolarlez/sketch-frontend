package sketch.compiler.ast.core.exprs;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

public class ExprFieldsListMacro extends Expression {
    private Expression left;

	public ExprFieldsListMacro(FENode node, Expression left) {
        super(node);
        this.left = left;
    }

    /** Returns the expression we're taking a field from. */
    public Expression getLeft() {
        return left;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v) {
        return v.visitExprFieldsListMacro(this);
    }

    public String toString() {
		return left + ".fields?";
    }


}

