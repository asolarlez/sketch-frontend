/**
 *
 */
package sketch.compiler.spin;

import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.dataflow.deadCodeElimination.EliminateDeadCode;

/**
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 *
 */
public class EliminateDeadParallelCode extends EliminateDeadCode {
	public EliminateDeadParallelCode () {
		super (true);
	}

	protected List<Function> functionsToAnalyze(StreamSpec spec){
		List<Function> fns = new LinkedList<Function> ();

		for (Function f : spec.getFuncs ())
			if (-1 < f.getName ().indexOf (Preprocessor.PROC_PFX))
				fns.add (f);

		System.out.println (fns);

		return new LinkedList<Function>(spec.getFuncs());
	}
}
