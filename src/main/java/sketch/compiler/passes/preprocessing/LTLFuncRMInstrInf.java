package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FunctionCreator;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.monitor.Graph;
import sketch.compiler.passes.lowering.MakeBodiesBlocks;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.Pair;

/**
 * Front-end visitor pass that generates and incorporates the instrumentation of
 * each RM associated with each LTL formula.
 * 
 * @author Fernando A. Galicia-Mendoza &lt;fmendoza@mit.edu&gt;
 * @version $Id$
 *
 */
//public class LTLRegression extends FEReplacer {
public class LTLFuncRMInstrInf extends SymbolTableVisitor {

	private List<Statement> ltlAsserts;
	// Change: wed sept 3
	private List<Pair<Statement, Graph>> mapLTLAut;
	// Change: wed sept 3
	private int idST;
	private List<Graph> automata;

	Set<Statement> assumes;
	Set<Statement> asserts;

	public LTLFuncRMInstrInf(List<Statement> ltlAsserts, int idST) {
		super(null);
		this.ltlAsserts = ltlAsserts;
		this.idST = idST;
		this.assumes = new HashSet<Statement>();
		this.asserts = new HashSet<Statement>();
		this.automata = new ArrayList<Graph>();
	}

	// Change: wed sept 3
	public LTLFuncRMInstrInf(List<Statement> ltlAsserts, int idST, List<Pair<Statement, Graph>> mapLTLAut) {
		super(null);
		this.ltlAsserts = ltlAsserts;
		this.idST = idST;
		this.assumes = new HashSet<Statement>();
		this.asserts = new HashSet<Statement>();
		this.automata = new ArrayList<Graph>();
		this.mapLTLAut = mapLTLAut;
	}
	// Change: wed sept 3

	public Object visitFunction(Function func) {
		SymbolTable oldSymTab = symtab;
		symtab = new SymbolTable(symtab);
		if (func.getName().startsWith("forever")) {
			Statement body = func.getBody();
			Iterator<Statement> it2 = ltlAsserts.iterator();
			Iterator<Statement> it3 = ltlAsserts.iterator();

			List<String> declare = new LinkedList<>();
			List<String> assign = new LinkedList<>();
			List<String> used = new LinkedList<>();

			while (it2.hasNext()) {
				Statement ltlCurrentLine = it2.next();
				// This pass creates a list of pre_ variables
				LTLPreVars pv = new LTLPreVars(ltlCurrentLine, declare, assign, used, true);
				ltlCurrentLine.accept(pv);
				declare = pv.getDeclare();
				assign = pv.getAssign();
				used = pv.getUsed();

				// This pass adds the pre_ variables assigns
				body = (Statement) body.accept(new LTLPreAssigns(ltlCurrentLine, declare, assign, used, false, symtab));
				Statement block = (Statement) body.accept(new MakeBodiesBlocks());
				body = (Statement) block.accept(new LTLPreAssigns(ltlCurrentLine, declare, assign, used, true, symtab));

				declare = new LinkedList<String>();
				assign = new LinkedList<String>();
			}

			declare = new LinkedList<String>();
			used = new LinkedList<String>();
			ltlAsserts = new LinkedList<>();

			while (it3.hasNext()) {
				Statement ltlCurrentLine = it3.next();
				// This pass creates a list of pre_ variables
				LTLNowVars pv = new LTLNowVars(ltlCurrentLine, declare, used);
				body.accept(pv);
				declare = pv.getDeclare();
				used = pv.getUsed();

				// This pass adds the pre_ variables assigns
				int tmpIDST = idST;
				body = (Statement) body.accept(new LTLNewInits(ltlCurrentLine, declare, idST));
				ltlCurrentLine = (Statement) ltlCurrentLine.accept(new LTLNewDecls(declare, idST));
				idST = tmpIDST == idST ? idST : idST + 1;
				ltlAsserts.add(ltlCurrentLine);

				declare = new LinkedList<String>();
			}

			Iterator<Statement> it = ltlAsserts.iterator();

			while (it.hasNext()) {
				Statement ltlCurrentLine = it.next();
				Filter filtering = new Filter();
				ltlCurrentLine = (Statement) ltlCurrentLine.accept(filtering);

				if (ltlCurrentLine instanceof StmtFor) {
					StmtFor LTLLoop = (StmtFor) ltlCurrentLine;
					Statement bodyLoop = LTLLoop.getBody();
					List<Statement> auxLTLs = new ArrayList<>();
					bodyLoop.accept(new InnerAsserts(auxLTLs));
					Iterator<Statement> itAux = auxLTLs.iterator();
					List<Graph> automataAux = new ArrayList<>();
					while (itAux.hasNext()) {
						Statement ltlAux = itAux.next();
						// This pass transforms an LTL formula into a string
						// format that LTL2BA recognizes.
						LTL2BAFormat stringFormat = new LTL2BAFormat(ltlAux, true);
						ltlAux.accept(stringFormat);
						// If the assert has an LTL formula as condition, the
						// following passes incorporates the RM instrumentation
						// into the current function's body.
						String ltlString = stringFormat.getLtlString();
						if (!ltlString.equals("")) {
							// Creation of the equivalent BA.
							Graph LTLFA = new Graph(ltlString, idST);
							automata.add(LTLFA);
							LTLFA.castAdj(body, stringFormat.getPropNames());
							// Change: wed sept 3
							mapLTLAut.add(new Pair<Statement, Graph>(ltlAux, LTLFA));
							// Change: wed sept 3
							automataAux.add(LTLFA);
						}
						idST++;
					}
					boolean hasLTL = true;

					List<String> namesInit = ((StmtVarDecl) LTLLoop.getInit()).getNames();
					Expression cond = LTLLoop.getCond();
					IndexBound bound = new IndexBound(namesInit);
					cond.accept(bound);
					// This pass parses the function body and incorporates
					// the RM instrumentation.
					LTLRMInstrumentations regressions = new LTLRMInstrumentations(hasLTL, automataAux, LTLLoop,
							declare, bound.getIndex(), true);
					body = (Statement) body.accept(regressions);
					hasLTL = regressions.getHasLTL();

					List<Parameter> newPs = new LinkedList<Parameter>();
					for (Parameter p : func.getParams()) {
						newPs.add(p);
					}
					FENode cx = func;
					for (Graph LTLFA : automataAux) {
						int numSts = LTLFA.getV();
						newPs.add(new Parameter(cx,
								new TypeArray(new TypeArray(TypePrimitive.bittype, new ExprConstInt(cx, numSts)),
										bound.getBound()),
								"st" + LTLFA.getIdA()));
						newPs.add(new Parameter(cx,
								new TypeArray(new TypeArray(TypePrimitive.bittype, new ExprConstInt(cx, numSts)),
										bound.getBound()),
								"stc" + LTLFA.getIdA()));
					}

					FunctionCreator creation = func.creator();
					creation.params(newPs);
					creation.body(body);

					func = creation.create();
				} else {

					// This pass transforms an LTL formula into a string format
					// that
					// LTL2BA recognizes.
					LTL2BAFormat stringFormat = new LTL2BAFormat(ltlCurrentLine, true);
					ltlCurrentLine.accept(stringFormat);
					// If the assert has an LTL formula as condition, the
					// following
					// passes incorporates the RM instrumentation into the
					// current
					// function's body.
					String ltlString = stringFormat.getLtlString();
					if (!ltlString.equals("")) {
						// Creation of the equivalent BA.
						Graph LTLFA = new Graph(ltlString, idST, filtering.getHelper());
						automata.add(LTLFA);
						// Transformation of a string format to sketch code over
						// the
						// transition labels.
						LTLFA.castAdj(body, stringFormat.getPropNames());
						// Change: wed sept 3
						mapLTLAut.add(new Pair<Statement, Graph>(ltlCurrentLine, LTLFA));
						// Change: wed sept 3
						boolean hasLTL = true;

						// This pass parses the function body and incorporates
						// the
						// RM
						// instrumentation.
						LTLRMInstrumentations regressions = new LTLRMInstrumentations(hasLTL, LTLFA, ltlCurrentLine,
								declare, true);
						body = (Statement) body.accept(regressions);
						hasLTL = regressions.getHasLTL();

						List<Parameter> newPs = new LinkedList<Parameter>();
						for (Parameter p : func.getParams()) {
							newPs.add(p);
						}
						FENode cx = func;
						int numSts = LTLFA.getV();
						newPs.add(new Parameter(cx,
								new TypeArray(TypePrimitive.bittype, new ExprConstInt(cx, numSts)),
								"st" + idST));
						newPs.add(new Parameter(cx,
								new TypeArray(TypePrimitive.bittype, new ExprConstInt(cx, numSts)),
								"stc" + idST));

						FunctionCreator creation = func.creator();
						creation.params(newPs);
						creation.body(body);

						func = creation.create();
						idST++;
					}
				}
			}
			symtab = oldSymTab;
			return func;
		}
		symtab = oldSymTab;
		return super.visitFunction(func);
	}

	public List<Pair<Statement, Graph>> getMapLTLAut() {
		return this.mapLTLAut;
	}

	public Object visitStmtAssign(StmtAssign assign) {
		return super.visitStmtAssign(assign);
	}

	public List<Graph> getAutomata() {
		return automata;
	}

	class Filter extends FEReplacer {

		String helper;

		Filter() {
			helper = "";
		}

		public Object visitExprFunCall(ExprFunCall func) {
			if (func.getName().equals("with_help")) {
				helper = func.getParams().get(1).toString();
				return func.getParams().get(0);
			}
			return super.visitExprFunCall(func);
		}

		public String getHelper() {
			return helper;
		}

	}

	class IndexBound extends FEReplacer {

		List<String> names;
		Expression bound;

		IndexBound(List<String> names) {
			this.names = names;
			bound = null;
		}

		public Object visitExprVar(ExprVar var) {
			if (!names.contains(var.getName()))
				bound = var;
			return super.visitExprVar(var);
		}

		public Expression getBound() {
			return bound;
		}

		public String getIndex() {
			return names.get(0);
		}

	}

	class InnerAsserts extends FEReplacer {

		List<Statement> inner;

		InnerAsserts(List<Statement> inner) {
			this.inner = inner;
		}

		public Object visitStmtAssert(StmtAssert stmt) {
			inner.add(stmt);
			return super.visitStmtAssert(stmt);
		}

		public List<Statement> getInner() {
			return inner;
		}

	}

}
