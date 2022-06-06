package sketch.compiler.monitor;

import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;

public class CastExpression extends FEReplacer {

	private Map<Integer, Expression> propNames;
	private int idA;

	public CastExpression(Map<Integer, Expression> propNames, int idA) {
		this.propNames = propNames;
		this.idA = idA;
	}

	public Object visitExprVar(ExprVar var) {
		String name = var.getName();
		if (name.equals("<SIGMA>"))
			return new ExprConstInt(var, 1);
		if(name.substring(0,1).equals("h"))
			return new ExprVar(var, "h" + idA);
		int intLabel = Integer.valueOf(name.substring(1));
		Expression label = propNames.get(intLabel);
		return label;
	}

}
