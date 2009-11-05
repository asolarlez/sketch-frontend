package sketch.compiler.smt.passes;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.smt.partialeval.BitVectUtil;
import sketch.compiler.solvers.constructs.AbstractValueOracle;

public class EliminateStarStatic extends
		FEReplacer {

	AbstractValueOracle oracle;

	public EliminateStarStatic(AbstractValueOracle oracle){
		
		this.oracle = oracle;
		oracle.initCurrentVals();
	}

	public Object visitExprStar(ExprStar star) {
		Type t = star.getType();
		int ssz = 1;
		
		if (BitVectUtil.isPrimitive(t)) {
			return oracle.popValueForNode(star);
		} else {
			Integer iv = ((TypeArray)t).getLength().getIValue();
			assert iv != null;
			ssz = iv;
			List<Expression> lst = new ArrayList<Expression>(ssz);
			for(int i=0; i<ssz; ++i){
				lst.add(oracle.popValueForNode(star.getDepObject(i)));
			}

			ExprArrayInit ainit = new ExprArrayInit(star, lst);
			return ainit;
		}
	}
}
