package sketch.compiler.ast.core.exprs;

import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStructRef;

/**
 * The expression that constructs an object of type T (new T()). Now only support
 * constructing <code>TypeStruct</code> typed object.
 */
public class ExprNew extends Expression {

	Type typeToConstruct;

    List<ExprNamedParam> params;

    public ExprNew(FENode context, Type typeToConstruct, List<ExprNamedParam> params) {
		super(context);
		this.typeToConstruct = typeToConstruct;
        assert typeToConstruct instanceof TypeStructRef;
		// TODO Auto-generated constructor stub
        this.params = params;
	}

    public List<ExprNamedParam> getParams() {
        return this.params;
    }

	/**
	 *
	 * @param context
	 * @param typeToConstruct
	 * @deprecated
	 */
    public ExprNew(FEContext context, Type typeToConstruct, List<ExprNamedParam> params) {
		super(context);
		this.typeToConstruct = typeToConstruct;
        this.params = params;
	}

	public Type getTypeToConstruct () {
		return typeToConstruct;
	}

	@Override
	public Object accept(FEVisitor v) {
		return v.visitExprNew(this);
	}

	public String toString(){
        String rv = "new " + typeToConstruct + "(";
        boolean isFirst = true;
        for (ExprNamedParam enp : getParams()) {
            if (isFirst) {
                isFirst = false;
            } else {
                rv += (", ");
            }
            rv += (enp.getName() + "=" + enp.getExpr().toString());
        }
        rv += ")";
        return rv;
	}

}

