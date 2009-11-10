package sketch.compiler.smt.passes;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;

public class ReplaceStructTypeWithInt extends FEReplacer {

	
	@Override
	public Object visitTypeStruct(TypeStruct ts) {
		return TypePrimitive.inttype;
	}
	
	@Override
	public Object visitTypeStructRef(TypeStructRef tsr) {
		return TypePrimitive.inttype;
	}
	
	@Override
	public Object visitExprNullPtr(ExprNullPtr nptr) {
		return new ExprConstInt(nptr, -1);
	}
}
