package sketch.compiler.smt.passes;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstBoolean;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;

/**
 * The parent class is not distinguishing the value
 * for a bool type and a bit type. It always assign 0
 * to both. That doesn't work for SMT translation, where
 * precise type is important
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 *
 */
public class FunctionParamExtension extends
		sketch.compiler.passes.lowering.FunctionParamExtension {

    public FunctionParamExtension(boolean b, TempVarGen vg) {
        super(b, vg);
	}
	
	@Override
	protected Expression getDefaultValue(Type t) {
		if(t.isStruct()){
			return ExprNullPtr.nullPtr;
		} else if (t.equals(TypePrimitive.booltype)) {
			return ExprConstBoolean.FALSE;
		} else {
			return ExprConstInt.zero;
		}
	}
	
	// @Override
	// protected Expression getFalseLiteral() {
    //     return ExprConstBoolean.FALSE;
	// }
}
