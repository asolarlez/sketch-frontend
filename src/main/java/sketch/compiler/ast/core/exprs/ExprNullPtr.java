package sketch.compiler.ast.core.exprs;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

public class ExprNullPtr extends Expression {

	public static ExprNullPtr nullPtr = new ExprNullPtr();
	public ExprNullPtr(){
        super((FENode)null);
    }

	@Override
	public Object accept(FEVisitor v) {
		return v.visitExprNullPtr(this);
	}

	public String toString(){
		return "null";
	}

}
