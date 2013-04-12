package sketch.util.exceptions;

import sketch.compiler.ast.core.FENode;

public class TypeErrorException extends ExceptionAtNode {

    @Override
    protected String messageClass() {
        return "Type Error";
    }

    public TypeErrorException(String msg, FENode fn) {
        super(msg, fn);
        // TODO Auto-generated constructor stub
    }



}
