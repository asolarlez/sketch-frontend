/**
 *
 */
package sketch.compiler.dataflow.deadCodeElimination;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.dataflow.recursionCtrl.BaseRControl;
import sketch.util.ControlFlowException;
import sketch.util.UndirectedColoredGraph;
import sketch.util.UndirectedGraph;
import sketch.util.UndirectedColoredGraph.ColoredVertex;
import sketch.util.UndirectedGraph.Vertex;

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
 * WARNING: this pass assumes that all variables have been named uniquely,
 * although the PartialEvaluator does not assume this.
 *
 *
 * XXX/cgjones: this pass suffers from a major design flaw that is very painful
 * to fix.  Consider this extremely simple program:
 * <code>
 *   int x = 1;
 *   assert x;
 * </code>
 * The visitation of the second statement will proceed as follows:
 *
 * + enter assertion visitor
 *   ++ enter exprvar 'x' visitor
 *   -- exit exprvar visitor
 * - exit assertion visitor
 *
 * The easiest solution to allocating registers is to override the exprvar
 * visitor.  However, this doesn't work in our dataflow framework because
 * the abstract value of variable 'x' is updated at the assertion visitor
 * for this example.  This means that for liveness analysis, 'x' will not
 * be marked live until we return from the exprvar visitor to the assertion
 * visitor.
 *
 * At this time, working around this limitation is not worth the time it would
 * take.
 *
 * @author <a href="mailto:cgjones@cs.berkeley.edu">Chris Jones</a>
 * @deprecated
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
		private Set<String> localInts = new HashSet<String> ();

		public BuildInterferenceGraph () {
			super(LiveVariableVType.vtype, null, false, -1, new BaseRControl (10));
		}

		public Object visitExprVar (ExprVar ev) {
			String var = ev.getName ();
			LiveVariableAV val = (LiveVariableAV) super.visitExprVar (ev);

			if (isLocal (var) && val.isLive ())
				for (String liveVar : liveVars ())
					interferes (var, liveVar);

			return val;
		}

		public Object visitStmtFork (StmtFork sf) {
			Set<String> oldLocals = localInts;
			localInts = new HashSet<String> ();

			localInts.add (sf.getLoopVarDecl ().getName (0));
			try {
				sf.getBody ().accept (new FEReplacer () {
					public Object visitStmtVarDecl (StmtVarDecl svd) {
						for (int i = 0; i < svd.getNumVars (); ++i)
							if (TypePrimitive.inttype.equals (svd.getType (i))) {
								localInts.add (svd.getName (i));
								interferers.find (svd.getName (i));
							}
						return super.visitStmtVarDecl (svd);
					}
					public Object visitStmtFork (StmtFork sf) {
						throw new ControlFlowException ("done");
					}
				});
			} catch (ControlFlowException cfe) { }

			Object res = super.visitStmtFork (sf);
			localInts = oldLocals;
			return res;
		}

		private Set<String> liveVars () {
			Set<String> liveVars = new HashSet<String> ();
			for (String varTrans : state.getVarsInScope ()) {
				String var = state.untransName (varTrans);
				if (isLocal (var)
					&& ((LiveVariableAV)state.varValue (var)).isLive ())
					liveVars.add (var);
			}
			return liveVars;
		}

		private boolean isLocal (String var) {
			return localInts.contains (var);
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
