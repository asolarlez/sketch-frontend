package sketch.util.exceptions;

import sketch.compiler.ast.core.FENode;

public class ExceptionAtNode extends SketchException {
    private static final long serialVersionUID = 1L;
    protected final FENode node;

    public ExceptionAtNode(String message, FENode node) {
        super(message + " (at " + node.getCx() + ")");
        this.node = node;
    }

    @Override
    protected String messageClass() {
        return "Error at node";
    }
}
