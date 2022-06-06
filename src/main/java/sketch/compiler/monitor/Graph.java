package sketch.compiler.monitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import rwth.i2.ltl2ba4j.internal.jnibridge.BAJni;
import rwth.i2.ltl2ba4j.model.IGraphProposition;
import rwth.i2.ltl2ba4j.model.IState;
import rwth.i2.ltl2ba4j.model.ITransition;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.util.Pair;

public class Graph {

	private int v;
	private int initv;
	private LinkedList<Pair<Expression, Integer>> adj[];
	private LinkedList<Pair<Expression, Integer>> adjE[];
	private LinkedList<Integer> finalS;
	private int time;
	private Expression language[];
	private LinkedList<IState> stG;
	private Collection<ITransition> transitions;
	private int idA;

	@SuppressWarnings("unchecked")
	public Graph(int v, Expression language[], int initv, LinkedList<Integer> finalS) {
		this.v = v;
		this.initv = initv;
		this.language = language;
		this.finalS = finalS;
		adj = new LinkedList[v];
		for (int i = 0; i < v; i++)
			adj[i] = new LinkedList<Pair<Expression, Integer>>();
		time = 0;
	}

	@SuppressWarnings("unchecked")
	public Graph(String form, int idA) {
		this.idA = idA;
		BAJni bajni = new BAJni(form);
		transitions = bajni.getTransitions();

		Set<Expression> langBA = new HashSet<Expression>();
		Set<IState> stBA = new HashSet<IState>();
		for (ITransition t : transitions) {
			IState u = t.getSourceState();
			IState v = t.getTargetState();
			langBA.add(makeProp(t.getLabels()));
			stBA.add(u);
			stBA.add(v);
		}
		stG = new LinkedList<IState>();
		for (IState i : stBA) {
			stG.add(i);
		}
		int vG = stG.size();
		int initSG = -1;
		LinkedList<Integer> finalG = new LinkedList<Integer>();
		for (IState s : stG) {
			if (s.isInitial()) {
				initSG = stG.indexOf(s);
				break;
			}
		}
		for (IState s : stG) {
			if (s.isFinal())
				finalG.add(stG.indexOf(s));
		}
		Expression langBAA[] = langBA.toArray(new Expression[0]);
		// new Graph(vG, langBAA, initSG, finalG);
		this.v = vG;
		this.initv = initSG;
		this.language = langBAA;
		this.finalS = finalG;
		this.adj = new LinkedList[vG];
		this.adjE = new LinkedList[vG];
		for (int i = 0; i < v; i++)
			adj[i] = new LinkedList<Pair<Expression, Integer>>();
		for (ITransition t : transitions) {
			IState u = t.getSourceState();
			IState v = t.getTargetState();
			addEdge(stG.indexOf(u), makeProp(t.getLabels()), stG.indexOf(v));
		}
	}

	private Expression makeProp(Set<IGraphProposition> labels) {
		Expression ret = null;

		IGraphProposition[] labs = labels.toArray(new IGraphProposition[0]);
		String lab = "";
		FEContext nctx = null;

		if (labs.length == 1) {
			lab = labs[0].getLabel();
			if (labs[0].isNegated()) {
				return new ExprUnary(nctx, ExprUnary.UNOP_NOT, new ExprVar(nctx, lab));
			} else {
				return new ExprVar(nctx, lab);
			}
		} else {
			for (int i = 0; i < labs.length; i++) {
				IGraphProposition p = labs[i];
				lab = p.getLabel();
				if (i == 0) {
					if (p.isNegated()) {
						ret = new ExprUnary(nctx, ExprUnary.UNOP_NOT, new ExprVar(nctx, lab));
					} else {
						ret = new ExprVar(nctx, lab);
					}
				} else {
					if (p.isNegated()) {
						Expression prev = new ExprUnary(nctx, ExprUnary.UNOP_NOT, new ExprVar(nctx, lab));
						ret = new ExprBinary(ExprBinary.BINOP_AND, prev, ret);
					} else {
						Expression prev = new ExprVar(nctx, lab);
						ret = new ExprBinary(ExprBinary.BINOP_AND, prev, ret);
					}
				}
			}
		}

		return ret;
	}

	/*
	 * Adds an edge to the graph by giving the initial node, tag and target
	 * node.
	 */
	public void addEdge(int v, Expression exp, int w) {
		adj[v].add(new Pair<Expression, Integer>(exp, w));
	}

	public void addEdgeE(int v, Expression exp, int w) {
		adjE[v].add(new Pair<Expression, Integer>(exp, w));
	}

	/*
	 * Returns the transpose of this graph.
	 */
	public Graph transpose() {
		Graph g = new Graph(v, language, initv, finalS);
		for (int u = 0; u < v; u++) {
			Iterator<Pair<Expression, Integer>> i = adj[u].iterator();
			while (i.hasNext()) {
				Pair<Expression, Integer> v = i.next();
				g.addEdge(v.getSecond(), v.getFirst(), u);
			}
		}
		g.transitions = this.transitions;
		g.stG = this.stG;
		return g;
	}

	/*
	 * DFS modified as helper method for Tarjan's algorithm.
	 */
	private void SCCUtil(int u, int low[], int disc[], boolean stackMember[], Stack<Integer> st,
			LinkedList<LinkedList<Integer>> sccs) {
		disc[u] = time;
		low[u] = time;
		time += 1;
		stackMember[u] = true;
		st.push(u);

		Pair<Expression, Integer> n;

		Iterator<Pair<Expression, Integer>> i = adj[u].iterator();

		while (i.hasNext()) {
			n = i.next();

			if (disc[n.getSecond()] == -1) {
				SCCUtil(n.getSecond(), low, disc, stackMember, st, sccs);
				low[u] = Math.min(low[u], low[n.getSecond()]);
			} else if (stackMember[n.getSecond()]) {
				low[u] = Math.min(low[u], disc[n.getSecond()]);
			}
		}

		LinkedList<Integer> tmp;

		int w = -1;
		if (low[u] == disc[u]) {
			tmp = new LinkedList<Integer>();
			while (w != u) {
				w = (int) st.pop();
				// System.out.print(w + " ");
				tmp.add(w);
				stackMember[w] = false;
			}
			// System.out.println();
			sccs.add(tmp);
		}
	}

	/*
	 * Returns the Strongly Connected Components (SCC) of the graph.
	 */
	private LinkedList<LinkedList<Integer>> SCC() {
		int disc[] = new int[v];
		int low[] = new int[v];
		LinkedList<LinkedList<Integer>> sccs = new LinkedList<LinkedList<Integer>>();
		for (int i = 0; i < v; i++) {
			disc[i] = -1;
			low[i] = -1;
		}

		boolean stackMember[] = new boolean[v];
		Stack<Integer> st = new Stack<Integer>();

		for (int i = 0; i < v; i++) {
				if (disc[i] == -1)
					SCCUtil(i, low, disc, stackMember, st, sccs);
		}

		return sccs;
	}
	
	/*
	 * Returns the subgraphs according to each symbol of the language.
	 */
	private LinkedList<Pair<Expression, Graph>> subgraphs() {
		LinkedList<Pair<Expression, Graph>> subs = new LinkedList<Pair<Expression, Graph>>();

		for (Expression a : language) {
			Expression lt[] = { a };
			Graph sg = new Graph(v, lt, initv, finalS);
			for (int u = 0; u < v; u++) {
				Iterator<Pair<Expression, Integer>> i = adj[u].iterator();
				while(i.hasNext()) {
					Pair<Expression, Integer> v = i.next();
					if (v.getFirst().equals(a)) {
						sg.addEdge(u, a, v.getSecond());
					}
				}
			}
			subs.add(new Pair<Expression, Graph>(a, sg));
		}

		return subs;
	}

	/*
	 * Returns the list of the SCC's for each symbol of the language.
	 */
	LinkedList<Pair<LinkedList<Integer>, Expression>> ascc() {
		LinkedList<Pair<LinkedList<Integer>, Expression>> asccs = new LinkedList<Pair<LinkedList<Integer>, Expression>>();
		for (Pair<Expression, Graph> gi : subgraphs()) {
			LinkedList<LinkedList<Integer>> anSCC = gi.getSecond().SCC();
			for (LinkedList<Integer> scc : anSCC) {
				if (acceptedSCC(scc, gi.getSecond())) {
					asccs.add(new Pair<LinkedList<Integer>, Expression>(scc, gi.getFirst()));
				}
			}
		}
		return asccs;

	}

	/*
	 * Returns true if the SCC satisfies the conditions of being an ASCC.
	 */
	boolean acceptedSCC(LinkedList<Integer> scc,Graph sub) {
		boolean accept = scc.size() > 1;
		if (scc.size() == 1) {
			int u = scc.getFirst();
			Iterator<Pair<Expression, Integer>> i = sub.adj[u].iterator();
			while (i.hasNext()) {
				int up = i.next().getSecond();
				accept |= u == up;
			}
		}
		for (int u : scc) {
			if (finalS.indexOf(u) != -1)
				return accept;
		}

		return false;
	}

	/*
	 * Returns true if the ASCC is not empty.
	 */
	boolean hasASCC(LinkedList<Pair<LinkedList<Integer>, String>> ascc) {
		boolean ret = true;
		for (Pair<LinkedList<Integer>, String> scc : ascc) {
			ret &= !scc.getFirst().isEmpty();
		}
		return ret;
	}

	/*
	 * Returns the set of the nodes that are halted states.
	 */
	LinkedList<Integer> halted() {
		LinkedList<Integer> halt = new LinkedList<Integer>();
		Graph gt = transpose();

		for (Pair<LinkedList<Integer>, Expression> a : ascc()) {
			for (Integer s : a.getFirst()) {
				if (halt.indexOf(s) == -1)
					halt.add(s);

				boolean visited[] = new boolean[v];
				LinkedList<Integer> queue = new LinkedList<Integer>();
				queue.add(s);
				visited[s] = true;

				while (queue.size() != 0) {
					s = queue.poll();

					Iterator<Pair<Expression, Integer>> i = gt.adj[s].iterator();
					while (i.hasNext()) {
						Pair<Expression, Integer> v = i.next();
						if (!visited[v.getSecond()] && v.getFirst().equals(a.getSecond())
								&& gt.adj[v.getSecond()].size() > 0) {
							queue.add(v.getSecond());
							visited[v.getSecond()] = true;
							/*
							 * if (halt.indexOf(s) == -1) halt.add(s);
							 */
							if (halt.indexOf(v.getSecond()) == -1)
								halt.add(v.getSecond());
						}
					}
				}
			}
		}

		return halt;
	}

	/*
	 * Returns the finite automaton version of the graph.
	 */
	public Graph finiteExc() {
		int vN = v;
		LinkedList<Integer> fS = new LinkedList<Integer>();
		fS.add(vN);
		Graph finite = new Graph(vN + 1, this.language, this.initv, fS);
		for (int u = 0; u < v; u++) {
			Iterator<Pair<Expression, Integer>> i = adj[u].iterator();
			while (i.hasNext()) {
				Pair<Expression, Integer> v = i.next();
				finite.addEdge(u, v.getFirst(), v.getSecond());
			}
		}
		for (int hi : halted()) {
			FEContext nctx = null;
			finite.addEdge(hi, new ExprVar(nctx,"h"), vN);
		}
		finite.transitions = this.transitions;
		finite.stG = this.stG;
		finite.idA = this.idA;
		return finite;
	}

	/*
	 * Returns true if the
	 */
	boolean verifyVHalted(int s, Expression a, LinkedList<Integer> scc) {
		boolean visited[] = new boolean[v];
		Stack<Integer> st = new Stack<Integer>();
		st.push(s);

		while (!st.empty()) {
			int u = st.pop();

			Iterator<Pair<Expression, Integer>> i = adj[u].iterator();
			while (i.hasNext()) {
				Pair<Expression, Integer> av = i.next();
				if (av.getFirst().equals(a)) {
					for (int cascc : scc) {
						if (av.getSecond() == cascc && transpose().adj[s].size() > 0)
							return true;
					}

					if (!visited[av.getSecond()]) {
						st.push(av.getSecond());
						visited[av.getSecond()] = true;
					}
				}
			}
		}

		return false;
	}

	boolean verifyHalted() {
		LinkedList<Integer> candidates = new LinkedList<Integer>();
		for (Pair<LinkedList<Integer>, Expression> scca : ascc()) {
			for (int u = 0; u < v; u++) {
				if (verifyVHalted(u, scca.getSecond(), scca.getFirst()))
					candidates.add(u);
			}
		}
		for (int c : candidates) {
			if (halted().indexOf(c) == -1)
				return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	public void castAdj(FENode context, Map<Integer, Expression> propNames) {
		adjE = new LinkedList[v];
		for (int i = 0; i < v; i++)
			adjE[i] = new LinkedList<Pair<Expression, Integer>>();
		for (int i = 0; i < v; i++) {
			Iterator<Pair<Expression, Integer>> v = adj[i].iterator();
			while (v.hasNext()) {
				Pair<Expression, Integer> c = v.next();
				Expression label = c.getFirst();
				label = (Expression) label.accept(new CastExpression(propNames, idA));
				this.addEdgeE(i, label, c.getSecond());
			}
		}
	}

	public String toString() {
		String ret = "";
		ret += "ID:\n" + idA + "\n";
		ret += "Initial state:\n" + initv + "\n";
		ret += "Final states:\n" + finalS + "\n";
		ret += "Delta String:\n";
		for (int i = 0; i < v; i++) {
			Iterator<Pair<Expression, Integer>> v = adj[i].iterator();
			while (v.hasNext()) {
				Pair<Expression, Integer> c = v.next();
				ret += i + " -> " + c.getSecond() + " [label= \"" + c.getFirst() + "\"]\n";
			}
		}
		ret += "Delta Expression:\n";
		for (int i = 0; i < v; i++) {
			Iterator<Pair<Expression, Integer>> v1 = adjE[i].iterator();
			while (v1.hasNext()) {
				Pair<Expression, Integer> c = v1.next();
				ret += i + " -> " + c.getSecond() + " [label= \"" + c.getFirst() + "\"]\n";
			}
		}
		return ret;
	}

	public StmtAssign makeRegression(FENode context, int vT) {
		FEContext curr = context.getCx();
		FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
				curr.getComment());
		ncontext.setLTL(true);
		ncontext.setAut(true);
		Expression ret = (Expression) new ExprConstInt(ncontext, 0);
		for (int u = 0; u < v; u++) {
			Iterator<Pair<Expression, Integer>> v1 = adjE[u].iterator();
			while (v1.hasNext()) {
				Pair<Expression, Integer> c = v1.next();
				if (c.getSecond() == vT) {
					ExprArrayRange preSt = new ExprArrayRange(new ExprVar(ncontext, "stc" + idA),
							new ExprConstInt(context, u));
					Expression labelSt = c.getFirst();
					ExprBinary regr = new ExprBinary(ExprBinary.BINOP_AND, (Expression) preSt, labelSt);
					ret = (Expression) new ExprBinary(ExprBinary.BINOP_OR, ret, regr);
				}
			}
		}
		ExprArrayRange lhs = new ExprArrayRange(new ExprVar(context, "st" + idA), new ExprConstInt(ncontext, vT));
		return new StmtAssign(ncontext, lhs, ret);
	}

	public int getV() {
		return this.v;
	}

	public void setV(int v) {
		this.v = v;
	}

	public int getInitv() {
		return this.initv;
	}

	public void setInitv(int initv) {
		this.initv = initv;
	}

	public LinkedList<Pair<Expression, Integer>>[] getAdj() {
		return this.adj;
	}

	public void setAdj(LinkedList<Pair<Expression, Integer>>[] adj) {
		this.adj = adj;
	}

	public LinkedList<Pair<Expression, Integer>>[] getAdjE() {
		return this.adjE;
	}

	public void setAdjE(LinkedList<Pair<Expression, Integer>>[] adjE) {
		this.adjE = adjE;
	}

	public LinkedList<Integer> getFinalS() {
		return this.finalS;
	}

	public void setFinalS(LinkedList<Integer> finalS) {
		this.finalS = finalS;
	}

	public int getTime() {
		return this.time;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public Expression[] getLanguage() {
		return this.language;
	}

	public void setLanguage(Expression[] language) {
		this.language = language;
	}

	public LinkedList<IState> getStG() {
		return this.stG;
	}

	public void setStG(LinkedList<IState> stG) {
		this.stG = stG;
	}

	public Collection<ITransition> getTransitions() {
		return this.transitions;
	}

	public void setTransitions(Collection<ITransition> transitions) {
		this.transitions = transitions;
	}

	public int getIdA() {
		return this.idA;
	}

	public void setIdA(int idA) {
		this.idA = idA;
	}

	/*
	 * public static void main(String args[]) { // Create a graph given in the
	 * above diagram String lan1[] = { "a", "b", "c" }; Graph g4 = new Graph(10,
	 * lan1); g4.addEdge(0, "a", 1); g4.addEdge(0, "a", 3); g4.addEdge(1, "a",
	 * 2); g4.addEdge(1, "a", 4); g4.addEdge(2, "a", 0); g4.addEdge(2, "a", 6);
	 * g4.addEdge(3, "a", 2); g4.addEdge(4, "a", 5); g4.addEdge(4, "a", 6);
	 * g4.addEdge(5, "a", 6); g4.addEdge(5, "a", 7); g4.addEdge(5, "a", 8);
	 * g4.addEdge(5, "a", 9); g4.addEdge(6, "a", 4); g4.addEdge(7, "a", 9);
	 * g4.addEdge(8, "b", 9); g4.addEdge(9, "b", 8); g4.addEdge(7, "c", 8);
	 * g4.addEdge(8, "c", 9); g4.addEdge(9, "c", 9); //
	 * System.out.println("\nSSC in fourth graph "); // g4.SCC();
	 * 
	 * // System.out.println("Halted: " + g4.halted());
	 * 
	 * LinkedList<Integer> finalg5 = new LinkedList<Integer>(); finalg5.add(2);
	 * finalg5.add(3);
	 * 
	 * Graph g5 = new Graph(7, lan1, 0, finalg5); g5.addEdge(1, "a", 0);
	 * g5.addEdge(0, "a", 2); g5.addEdge(2, "a", 1); g5.addEdge(3, "a", 0);
	 * g5.addEdge(3, "b", 3); g5.addEdge(4, "b", 3); g5.addEdge(5, "b", 4);
	 * g5.addEdge(6, "c", 5);
	 * 
	 * // System.out.println("Original:\n" + g5);
	 * 
	 * // System.out.println("Halted: " + g5.halted());
	 * 
	 * // System.out.println("Finite execution:\n" + g5.finiteExc());
	 * 
	 * String form1;
	 * 
	 * BAJni bajni;
	 * 
	 * Graph f1g;
	 * 
	 * form1 = "[]((q && <>r) -> !p U r)";
	 * 
	 * bajni = new BAJni(form1);
	 * 
	 * // f1g = g5.BAtoFA(bajni.getTransitions());
	 * 
	 * 
	 * /* try { File tests = new File(
	 * "C:\\Users\\ferna\\Dropbox\\FGM_Phd_Thesis\\Implementation\\Patterns_Experiments.txt"
	 * ); FileWriter tests_res = new FileWriter(
	 * "C:\\Users\\ferna\\Dropbox\\FGM_Phd_Thesis\\Implementation\\Results.txt"
	 * ); Scanner sc = new Scanner(tests); long startTests = System.nanoTime();
	 * while (sc.hasNextLine()) { String form = sc.nextLine(); bajni = new
	 * BAJni(form); tests_res.write("Formula:\n" + form + "\n"); long startTrans
	 * = System.nanoTime(); Graph fg = g5.BAtoFA(bajni.getTransitions(),
	 * tests_res); long finalTrans = System.nanoTime() - startTrans;
	 * tests_res.write("Time: " + finalTrans + " nanosec. \n\n");
	 * 
	 * } long finalTests = System.nanoTime() - startTests;
	 * tests_res.write("Total time: " + finalTests + " nanosec."); sc.close();
	 * tests_res.close(); } catch (Exception e) { System.out.println(e); }
	 */

	// No partition, it is "cover"
}
