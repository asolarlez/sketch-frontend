/**
 *
 */
package streamit.frontend.experimental.deadCodeElimination;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Program;
import streamit.frontend.tosbit.recursionCtrl.BaseRControl;
import streamit.misc.UndirectedColoredGraph;
import streamit.misc.UndirectedGraph;
import streamit.misc.UndirectedColoredGraph.ColoredVertex;
import streamit.misc.UndirectedGraph.Vertex;

/**
 * A pass to minimize (well, reduce) the number of local variables used in the
 * bodies of functions.
 *
 * The idea is to use the classic register allocation algorithm on the local
 * variables, but instead of allocating the variables into a fixed k number of
 * registers, find a (near) optimal value of k registers into which to allocate
 * the variables.  This is done very efficiently using the Welsh-Powell
 * algorithm
 * (<a>http://en.wikipedia.org/wiki/Graph_coloring#Algorithm_of_Welsh_and_Powell</a>).
 *
 * This is sort of a dead-code-elimination pass in that unnecessary local
 * variables are killed off.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 */
public class MinimizeLocalVariables {
	static public Program go (Program p) {
		BuildInterferenceGraph big = new BuildInterferenceGraph ();
		p.accept (big);
		UndirectedColoredGraph<String> rig = big.interferers.color ();

		System.out.println (big.interferers);
		System.out.println (rig);

		Map<String, String> transMap = new HashMap<String, String> ();
		for (Vertex<String> _v : rig.vertices ()) {
			ColoredVertex<String> v = (ColoredVertex<String>) _v;
			transMap.put (v.item (), "__reg_"+ v.color ());
		}

		return (Program) p.accept (new RenameVariables (transMap));
	}

	static class BuildInterferenceGraph extends BackwardDataflow {
		private UndirectedGraph<String> interferers = new UndirectedGraph<String> ();

		public BuildInterferenceGraph () {
			super(LiveVariableVType.vtype, null, true, -1, new BaseRControl (10));
		}

		public Object visitExprVar (ExprVar ev) {
			LiveVariableAV val = (LiveVariableAV) super.visitExprVar (ev);
			ExprVar var = (ExprVar) exprRV;

			if (!val.isVolatile () && val.isLive ()) {
				String varName = state.untransName (var.getName ());
				for (String liveVarTrans : liveVars ())
					interferes (varName, state.untransName (liveVarTrans));
			}

			return val;
		}

		private Set<String> liveVars () {
			return state.getVarsInScope ();
		}

		private void interferes (String var1, String var2) {
			interferers.addEdge (var1, var2);
		}
	}

	static class RenameVariables extends FEReplacer {
		private Map<String, String> transMap;

		public RenameVariables (Map<String, String> transMap) {
			this.transMap = transMap;
		}

		public Object visitExprVar (ExprVar ev) {
			return !transMap.containsKey (ev.getName ()) ? ev
					: new ExprVar (ev, transMap.get (ev.getName ()));
		}
	}
}
