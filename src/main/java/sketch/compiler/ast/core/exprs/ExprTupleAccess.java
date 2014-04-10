package sketch.compiler.ast.core.exprs;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

public class ExprTupleAccess extends Expression {

    private Expression base;
    private int index;

    public ExprTupleAccess(FENode node, Expression base, int index) {
        super(node);
        this.base = base;
        this.index = index;
    }

    public ExprTupleAccess(FEContext context, Expression base, int index) {
        super(context);
        this.base = base;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    /*
     * (non-Javadoc)
     * @see sketch.compiler.nodes.FENode#accept(sketch.compiler.nodes.FEVisitor)
     */
    public Object accept(FEVisitor v) {
        return v.visitExprTupleAccess(this);
    }

    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append(base);
        ret.append('[');
        ret.append('[');
        ret.append(index);
        ret.append(']');
        ret.append(']');
        return ret.toString();
    }

    public Expression getBase() {
        return base;
    }


    @Override
    public boolean isLValue() {
        return false;
    }

    public boolean equals(Object other) {
        if (!(other instanceof ExprTupleAccess))
            return false;
        return this.toString().equals(other.toString());
    }


}

