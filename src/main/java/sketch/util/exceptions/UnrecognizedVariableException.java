package sketch.util.exceptions;

import sketch.compiler.ast.core.FENode;

/**
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class UnrecognizedVariableException extends ExceptionAtNode {

    private static final long serialVersionUID = -7947547883325124420L;

    public UnrecognizedVariableException(String name, FENode node) {
        super("unrecognized variable '" + name + "'", node);
    }

    @Override
    protected String messageClass() {
        return "Unrecognized Variable Error";
    }
}
