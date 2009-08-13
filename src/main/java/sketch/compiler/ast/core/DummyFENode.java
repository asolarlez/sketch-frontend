package sketch.compiler.ast.core;

public class DummyFENode extends FENode {

	public DummyFENode(FENode node) {
		super(node);
	}

	/**
	 *
	 * @param context
	 * @deprecated
	 */
	public DummyFENode(FEContext context) {
		super(context);
	}

	@Override
	public Object accept(FEVisitor v) {
		assert false : "This is a dummy class; you shouldn't be calling it's visitor.";
		return null;
	}

}
