package sketch.compiler.ast.core.exprs;

import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

public class ExprADTHole extends Expression {
    private String name = null;
    private final List<Expression> params;
	boolean simple = false;

    /**
     * Represents an arbitrary ADT.
     * 
     * @param context
     * @param params
     *            Parameters to use while constructing ADT
     */
    public ExprADTHole(FENode context, List<Expression> params) {
        super(context);
        this.params = params;
    }

    public ExprADTHole(FEContext context, List<Expression> params) {
        super(context);
        this.params = params;
    }

	public ExprADTHole(FENode context, List<Expression> params, boolean simple) {
		super(context);
		this.params = params;
		this.simple = simple;
	}

	public ExprADTHole(FEContext context, List<Expression> params,
			boolean simple) {
		super(context);
		this.params = params;
		this.simple = simple;
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
        return v.visitExprADTHole(this);
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

	public boolean isSimple() {
		return simple;
	}
}
