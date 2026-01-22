package sketch.compiler.main.seq;

import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.util.Pair;

public class LTLSortRankFuncs extends FEReplacer {

	private List<Pair<String, List<Parameter>>> fNamesParams;

	public LTLSortRankFuncs(List<Pair<String, List<Parameter>>> fNamesParams) {
		this.fNamesParams = fNamesParams;
	}

	public Object visitFunction(Function func) {
		for (Pair<String, List<Parameter>> p : fNamesParams) {
			if (p.getFirst().equals(func.getName())) {
				func = func.creator().params(p.getSecond()).create();
				return super.visitFunction(func);
			}
		}
		return super.visitFunction(func);
	}
}
