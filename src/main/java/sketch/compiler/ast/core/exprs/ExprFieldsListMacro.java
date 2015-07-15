package sketch.compiler.ast.core.exprs;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.typs.Type;

public class ExprFieldsListMacro extends Expression {
    private Expression left;
    private Type type;

    public ExprFieldsListMacro(FENode node, Expression left, Type type) {
        super(node);
        this.left = left;
        this.type = type;
    }

    /** Returns the expression we're taking a field from. */
    public Expression getLeft() {
        return left;
    }

    /** Returns the name of the field. */
    public Type getType() {
        return type;
    }

    /** Accept a front-end visitor. */
    public Object accept(FEVisitor v) {
        return v.visitExprFieldMacro(this);
    }

    public String toString() {
        return left + ".{" + type + "}";
    }


}

