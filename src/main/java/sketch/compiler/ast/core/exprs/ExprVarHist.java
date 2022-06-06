/**
 * 
 */
package sketch.compiler.ast.core.exprs;

import sketch.compiler.ast.core.FENode;

/**
 * @author ferna
 *
 */
public class ExprVarHist extends ExprVar {

	private int history;

	/**
	 * @param context
	 * @param name
	 */
	public ExprVarHist(FENode context, String name) {
		super(context, name);
		this.setHistory(0);
		// TODO Auto-generated constructor stub
	}

	public ExprVarHist(FENode context, String name, int history) {
		super(context, name);
		this.setHistory(history);
	}

	public int getHistory() {
		return history;
	}

	public void setHistory(int history) {
		this.history = history;
	}

}
