package sketch.util.exceptions;

import sketch.compiler.ast.core.FENode;

import static sketch.util.DebugOut.printError;

/**
 * An exception that's related to an AST node
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ExceptionAtNode extends SketchException {
    private static final long serialVersionUID = 1L;
    protected final FENode node;

    public ExceptionAtNode(String message, FENode node) {
        super(message + " (at " + node.getCx() + ")");
        this.node = node;
    }

    public ExceptionAtNode(FENode node, String format, Object... args) {
        this(String.format(format, args), node);
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
