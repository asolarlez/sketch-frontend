package sketch.compiler.ast.core;

import sketch.compiler.ast.core.typs.Type;

/**
 * exception if visitors don't exist.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class FEVisitorException extends RuntimeException {
    private static final long serialVersionUID = -8674430098027635134L;
    protected final FENode node;
    protected final FEVisitor visitor;
    protected final Type typ;

    public FEVisitorException(FEVisitor visitor, FENode node) {
        this.visitor = visitor;
        this.node = node;
        this.typ = null;
    }

    public FEVisitorException(FEVisitor visitor, Type typ) {
        this.visitor = visitor;
        this.node = null;
        this.typ = typ;
    }

    @Override
    public String getMessage() {
        if (node == null) {
            return "Visitor " + visitor.getClass().getSimpleName() +
                    " -- unexpected type node of AST type " +
                    node.getClass().getSimpleName() + "encountered";
        } else {
            return "Visitor " + visitor.getClass().getSimpleName() +
                    " -- unexpected node of type " + node.getClass().getSimpleName() +
                    "encountered";
        }
    }
}
