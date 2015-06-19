package sketch.compiler.ast.core.exprs;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

/**
 * An expression that represents the desire to use local variables in the context. The
 * symbol is <code>$$$</code>.
 * 
 * @author Miguel Velez
 * @version 0.1
 */
public class ExprLocalVariables extends Expression {
    // This is the class that extends an expression. Probably, I will not need to
    // change it further, but look at other classes that extend expression to know what to
    // do.

    /**
     * Creates a new local variable expression by passing a front end node.
     * 
     * @param context
     */
    public ExprLocalVariables(FENode context) {
        super(context);
        System.out.println("***************************Constructor node in expression");
    }

    /**
     * Creates a new local variable expression by passing a front end context.
     * 
     * @param context
     */
    public ExprLocalVariables(FEContext context) {
        super(context);
        System.out.println("***************************Constructor context in expression");
        System.out.println("We found a symbol at line: " + context.getLineNumber() +
                " that represents that the user wants to use local variables");
    }

    /**
     * Calls an appropriate method in a visitor object with this as a parameter.
     *
     * @param v  visitor object
     * @return   the value returned by the method corresponding to
     *           this type in the visitor object
     */
    @Override
    public Object accept(FEVisitor visitor) {
		System.out.println("***************************Visitor in expression");

        // Visit a local variable expression
        return visitor.visitExprLocalVariables(this);
    }

}
