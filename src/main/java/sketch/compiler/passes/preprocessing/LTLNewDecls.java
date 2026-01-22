package sketch.compiler.passes.preprocessing;

import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprVar;

public class LTLNewDecls extends FEReplacer {

	private List<String> declare;
	private int idST;

	public LTLNewDecls(List<String> declare, int idST) {
		this.declare = declare;
		this.idST = idST;
	}

	public Object visitExprVar(ExprVar var) {
		Iterator<String> it = declare.iterator();
		while (it.hasNext()) {
			String name = it.next();
			if (var.getName().equals(name)) {
				return new ExprVar(var.getCx(), name + idST);
			}
		}
		return super.visitExprVar(var);
	}

}
