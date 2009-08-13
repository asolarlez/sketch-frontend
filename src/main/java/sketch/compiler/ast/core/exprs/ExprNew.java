package sketch.compiler.ast.core.exprs;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.typs.Type;

public class ExprNew extends Expression {

	Type typeToConstruct;


	public ExprNew(FENode context, Type typeToConstruct) {
		super(context);
		this.typeToConstruct = typeToConstruct;
		// TODO Auto-generated constructor stub
	}

	/**
	 *
	 * @param context
	 * @param typeToConstruct
	 * @deprecated
	 */
	public ExprNew(FEContext context, Type typeToConstruct) {
		super(context);
		this.typeToConstruct = typeToConstruct;
	}

	public Type getTypeToConstruct () {
		return typeToConstruct;
	}

	@Override
	public Object accept(FEVisitor v) {
		return v.visitExprNew(this);
	}

	public String toString(){
		return "new " + typeToConstruct + "()";
	}

}

