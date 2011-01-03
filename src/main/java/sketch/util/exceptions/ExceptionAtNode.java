package sketch.util.exceptions;

import static sketch.util.DebugOut.printError;
import sketch.compiler.ast.core.FENode;

public class ExceptionAtNode extends SketchException {
    private static final long serialVersionUID = 1L;
    protected final FENode node;

    public ExceptionAtNode(String message, FENode node) {
        super(message + " (at " + node.getCx() + ")");
        this.node = node;
    }
    
    @Override
    public void print() {
        super.print();
        printError("[SKETCH]", "Class of node related to failure:", this.node.getClass());
        printError("[SKETCH]", "Node related to failure:", this.node);
    }

    @Override
    protected String messageClass() {
        return "Error at node";
    }
}
