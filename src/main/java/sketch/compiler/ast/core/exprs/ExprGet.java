package sketch.compiler.ast.core.exprs;

import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

public class ExprGet extends Expression {
    private String name = null;
    private final List<Expression> params;

    /**
     * Creates a new get expression that creates an ADT of type and depth given.
     * 
     * @param context
     * @param name
     *            Name of the ADT to construct
     * @param params
     *            Parameters to use while constructing ADT
     * @param depth
     *            Maximum depth of the ADT to be created.
     */
    public ExprGet(FENode context, List<Expression> params) {
        super(context);
        this.params = params;
    }

    public ExprGet(FEContext context, List<Expression> params) {
        super(context);
        this.params = params;
    }

    /** Returns the name of the type of returned ADT */
    public String getName() {
        return name;
    }

    /** Returns the list of parameters */
    public List<Expression> getParams() {
        return params;
    }

    /** Accepts a front-end visitor */
    @Override
    public Object accept(FEVisitor v) {
        return v.visitExprGet(this);
    }

    public String toString() {
        return "??(" + printParams() + ")";
    }

    public String printParams() {
        String s = "";
        boolean notf = false;
        for (Expression p : params) {
            if (notf) {
                s += ", ";
            }
            s += p.toString();
            notf = true;
        }
        return s;
    }

    public void setName(String name) {
        this.name = name;
    }
}
