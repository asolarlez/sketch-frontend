package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.Function.FunctionCreator;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssume;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtReturn;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.monitor.Graph;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.Pair;

public class LTLInvAut extends SymbolTableVisitor {

	List<Statement> ltlAsserts;
	List<Function> nf;
	// Change: wed sept 3
	List<Pair<Statement, Graph>> mapLTLAut;
	// Change: wed sept 3
	// Change: dec 25
	List<Pair<String, List<Parameter>>> fNamesParams;
	// Change: dec 25

	public LTLInvAut(List<Statement> ltlAsserts) {
		super(null);
		this.ltlAsserts = ltlAsserts;
		this.nf = new ArrayList<Function>();
	}

	// Change: wed sept 3
	public LTLInvAut(List<Statement> ltlAsserts, List<Pair<Statement, Graph>> mapLTLAut) {
		super(null);
		this.ltlAsserts = ltlAsserts;
		this.mapLTLAut = mapLTLAut;
		this.nf = new ArrayList<Function>();
	}
	// Change: wed sept 3

	// Change: dec 25
	public LTLInvAut(List<Statement> ltlAsserts, List<Pair<Statement, Graph>> mapLTLAut,
			List<Pair<String, List<Parameter>>> fNamesParams) {
		super(null);
		this.ltlAsserts = ltlAsserts;
		this.mapLTLAut = mapLTLAut;
		this.nf = new ArrayList<Function>();
		this.fNamesParams = fNamesParams;
	}
	// Change: wed sept 3

	public Object visitPackage(Package pkg) {
		List<Function> funcs = pkg.getFuncs();
		for (Function func : funcs) {
			nf.add(func);
			if (func.getName().startsWith("forever")) {
				nf.remove(func);
				CreateInvAut invAut = new CreateInvAut(symtab, ltlAsserts, fNamesParams);
				func = (Function) func.accept(invAut);
				nf.add(func);
				nf.addAll(invAut.getNewFuncs());
			}
		}
		return pkg.newFromFcns(nf);
	}

	class CreateInvAut extends SymbolTableVisitor {

		List<Statement> ltlAsserts;
		List<Function> newFuncs;
		List<Statement> assumes;
		List<Statement> asserts;
		List<Statement> converges;
		// Change: dec 25
		List<Pair<String, List<Parameter>>> fNamesParams;
		// Change: dec 25

		CreateInvAut(SymbolTable symtab, List<Statement> ltlAsserts, List<Pair<String, List<Parameter>>> fNamesParams) {
			super(symtab);
			this.ltlAsserts = ltlAsserts;
			this.newFuncs = new ArrayList<Function>();
			this.fNamesParams = fNamesParams;
		}

		public Object visitFunction(Function func) {
			SymbolTable oldSymTab = symtab;
			symtab = new SymbolTable(symtab);
			assumes = new ArrayList<>();
			asserts = new ArrayList<>();
			converges = new ArrayList<>();
			Statement body = func.getBody();
			FENode cx = func;
			int idA = 0;
			for (Statement st : ltlAsserts) {
				if (st instanceof StmtFor) {
					StmtFor stmtFor = (StmtFor) st;
					List<String> namesInit = ((StmtVarDecl) stmtFor.getInit()).getNames();
					IndexBound inB = new IndexBound(namesInit);
					stmtFor.getCond().accept(inB);
					String index = inB.getIndex();
					Statement bodyAux = stmtFor.getBody();
					List<Statement> assertsAux = new ArrayList<>();
					bodyAux.accept(new InnerAsserts(assertsAux));
					int accessAux = 0;
					List<Statement> assuAux = new ArrayList<>();
					List<Statement> asseAux = new ArrayList<>();
					List<Statement> convergesAux = new ArrayList<>();
					for (Statement stAux : assertsAux) {
						StmtAssert assAux = (StmtAssert) stAux;
						ExprFunCall funCall = (ExprFunCall) assAux.getCond();
						stAux = new StmtAssert(stAux, funCall.getParams().get(0), false);
						LTL2BAFormat stringFormat = new LTL2BAFormat(stAux, true);
						stAux.accept(stringFormat);
						String ltlString = stringFormat.getLtlString();
						Graph automaton = null;
						if (!ltlString.equals("")) {
							Graph LTLFA = new Graph(ltlString, idA, funCall.getParams().get(1).toString());
							LTLFA.castAdj(body, stringFormat.getPropNames());
							automaton = LTLFA;
						}
						Iterator<Pair<Statement, Graph>> itstg = mapLTLAut.iterator();
						while (itstg.hasNext()) {
							Pair<Statement, Graph> stg = itstg.next();
							StmtAssert auxStg = (StmtAssert) stg.getFirst();
							if (auxStg.getCond().equals(((StmtAssert) assAux).getCond())) {
								automaton = stg.getSecond();
							}
						}
						List<Parameter> newParams = new ArrayList<>();
						for(Parameter p : func.getParams()) {
							String nameP = p.getName();
							if (!(nameP.startsWith("st") || nameP.startsWith("stc")))
								newParams.add(p);
						}

						newParams.add(new Parameter(cx, TypePrimitive.inttype, "st"));
						newParams.add(new Parameter(cx, TypePrimitive.inttype, index));
						LinkedList<Pair<Expression, Integer>> adjE[] = automaton.transposeE().getAdjE();
						Statement nbody = accessCond(adjE, func);
						FENode func1 = func;
						func1.getContext().setLTL(true);
						FunctionCreator newFCreator = Function.creator(func1, "access_" + idA + "_" + accessAux,
								FcnType.Static);
						newFCreator.body(nbody);
						newFCreator.params(newParams);
						newFCreator.pkg(func.getPkg());
						newFCreator.returnType(TypePrimitive.bittype);
						Function funcAccess = newFCreator.create();
						newFuncs.add(funcAccess);
						String name = "stc" + idA;
						cx.getCx().setLTL(true);
						assuAux.add((Statement) declareReachOneFor(
										new ExprArrayRange(new ExprVar(cx, name), new ExprVar(cx, index)),
										automaton.getV(), true));
						assuAux.add((Statement) accessConditionFor(cx, new ExprVar(cx, name), automaton.getV(),
								idA + "_" + accessAux, newParams, index, true));
						name = "st" + idA;
						asseAux.add((Statement) declareReachOneFor(
										new ExprArrayRange(new ExprVar(cx, name), new ExprVar(cx, index)),
										automaton.getV(), false));
						asseAux.add((Statement) accessConditionFor(cx, new ExprVar(cx, name), automaton.getV(),
								idA + "_" + accessAux, newParams, index, false));
						Statement converge = (Statement) convergeCondFor(cx, automaton.getHelper(), func.getParams(),
								new ExprArrayRange(cx, new ExprVar(cx, "accept" + idA),
										new ExprVar(cx, inB.getIndex())),
								index);
						convergesAux.add(converge);
						accessAux++;
					}
					Statement bodyForAssu = new StmtBlock(cx, assuAux);
					FEContext curr = cx.getCx();
					FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
							curr.getComment());
					ncontext.setLTL(true);
					assumes.add(
							new StmtFor(ncontext, stmtFor.getInit(), stmtFor.getCond(), stmtFor.getIncr(), bodyForAssu,
							stmtFor.isCanonical()));
					assumes.add(new StmtVarDecl(ncontext, new TypeArray(TypePrimitive.bittype, inB.getBound()),
							"accept" + idA, new ExprConstInt(cx, 0)));
					Statement bodyForAsse = new StmtBlock(cx, asseAux);
					asserts.add(
							new StmtFor(ncontext, stmtFor.getInit(), stmtFor.getCond(), stmtFor.getIncr(), bodyForAsse,
							stmtFor.isCanonical()));
					Statement bodyForConv = new StmtBlock(cx, convergesAux);
					asserts.add(new StmtFor(ncontext, stmtFor.getInit(), stmtFor.getCond(), stmtFor.getIncr(),
							bodyForConv,
							stmtFor.isCanonical()));
				} else {
					StmtAssert assAux = (StmtAssert) st;
					ExprFunCall funCall = (ExprFunCall) assAux.getCond();
					st = new StmtAssert(st, funCall.getParams().get(0), false);
					LTL2BAFormat stringFormat = new LTL2BAFormat(st, true);
					st.accept(stringFormat);
					String ltlString = stringFormat.getLtlString();
					Graph automaton = null;
					if (!ltlString.equals("")) {
						Graph LTLFA = new Graph(ltlString, idA, funCall.getParams().get(1).toString());
						LTLFA.castAdj(body, stringFormat.getPropNames());
						automaton = LTLFA;
					}
					Iterator<Pair<Statement, Graph>> itstg = mapLTLAut.iterator();
					while (itstg.hasNext()) {
						Pair<Statement, Graph> stg = itstg.next();
						StmtAssert auxStg = (StmtAssert) stg.getFirst();
						if (auxStg.getCond().equals(((StmtAssert) st).getCond())) {
							automaton = stg.getSecond();
						}
					}
					List<Parameter> newParams = new ArrayList<>();
					for (Parameter p : func.getParams()) {
						String nameP = p.getName();
						if (!(nameP.startsWith("st") || nameP.startsWith("stc")))
							newParams.add(p);
					}
					newParams.add(new Parameter(cx, TypePrimitive.inttype, "st"));
					LinkedList<Pair<Expression, Integer>> adjE[] = automaton.transposeE().getAdjE();
					Statement nbody = accessCond(adjE, func);
					FunctionCreator newFCreator = Function.creator(func, "access_" + automaton.getIdA(),
							FcnType.Static);
					newFCreator.body(nbody);
					newFCreator.params(newParams);
					newFCreator.pkg(func.getPkg());
					newFCreator.returnType(TypePrimitive.bittype);
					Function funcAccess = newFCreator.create();
					newFuncs.add(funcAccess);
					String name = "stc" + idA;
					FEContext curr = cx.getCx();
					FEContext ncontext = new FEContext(curr.getFileName(), curr.getLineNumber(), curr.getColumnNumber(),
							curr.getComment());
					ncontext.setLTL(true);
					assumes.add((Statement) declareReachOne(new ExprVar(ncontext, name), automaton.getV(), true));
					assumes.add((Statement) accessCondition(ncontext, new ExprVar(ncontext, name), automaton.getV(),
							automaton.getIdA(), newParams, true));
					assumes.add(
							new StmtVarDecl(ncontext, TypePrimitive.bittype, "accept" + idA, new ExprConstInt(cx, 0)));
					name = "st" + idA;
					asserts.add((Statement) declareReachOne(new ExprVar(ncontext, name), automaton.getV(), false));
					asserts.add((Statement) accessCondition(ncontext, new ExprVar(cx, name), automaton.getV(),
							automaton.getIdA(), newParams, false));
					Statement converge = (Statement) convergeCond(ncontext, automaton.getHelper(), func.getParams(),
							automaton.getIdA());
					converges.add(converge);
				}
				idA++;
			}
			body = (Statement) body.accept(new Incorporate(0, assumes, asserts, converges));
			symtab = oldSymTab;
			func = func.creator().body(body).create();
			return super.visitFunction(func);
		}

		public Statement accessCond(LinkedList<Pair<Expression, Integer>> adjE[], FENode node) {
			int v = adjE.length;
			if (v == 1)
				return new StmtReturn(node, adjE[0].get(0).getFirst());
			List<Statement> ifs = new ArrayList<Statement>();
			for (int i = 0; i < v; i++) {
				Expression left = new ExprVar(node, "st");
				Expression right = new ExprConstInt(i);
				Expression cond = new ExprBinary(node, ExprBinary.BINOP_EQ, left, right);
				List<Statement> innerIfs = new ArrayList<Statement>();
				for (Pair<Expression, Integer> p : adjE[i]) {
					Expression inCond = p.getFirst();
					Statement ret = new StmtReturn(node, new ExprConstInt(1));
					innerIfs.add(new StmtIfThen(node, inCond, ret, null));
				}
				innerIfs.add(new StmtReturn(node, new ExprConstInt(0)));
				Statement body = new StmtBlock(node, innerIfs);
				ifs.add(new StmtIfThen(node, cond, body, null));
			}
			ifs.add(new StmtReturn(node, new ExprConstInt(0)));
			FEContext nCx = node.getContext();
			nCx.setLTL(true);
			return new StmtBlock(nCx, ifs);
		}

		public Object declareReachOne(Expression var, int numStates, boolean isAssume) {
			Expression cond = (Expression) new ExprArrayRange(var, new ExprConstInt(var, 0));
			for (int i = 1; i < numStates; i++) {
				Expression condL = (Expression) new ExprArrayRange(var, new ExprConstInt(var, i));
				cond = new ExprBinary(var, ExprBinary.BINOP_OR, cond, condL);
			}
			Statement ret = null;
			FEContext nCx = var.getContext();
			nCx.setLTL(true);
			if (isAssume) {
				ret = new StmtAssume(nCx, cond, "");
			} else {
				ret = new StmtAssert(nCx, cond, false);
			}
			return ret;
		}

		public Object declareReachOneFor(Expression var, int numStates, boolean isAssume) {
			Expression cond = (Expression) new ExprArrayRange(var, new ExprConstInt(var, 0));
			for (int i = 1; i < numStates; i++) {
				Expression condL = (Expression) new ExprArrayRange(var, new ExprConstInt(var, i));
				cond = new ExprBinary(var, ExprBinary.BINOP_OR, cond, condL);
			}
			Statement ret = null;
			FEContext nCx = var.getContext();
			nCx.setLTL(true);
			if (isAssume) {
				ret = new StmtAssume(nCx, cond, "");
			} else {
				ret = new StmtAssert(nCx, cond, false);
			}
			return ret;
		}

		public Object accessCondition(FEContext cx, Expression var, int v, int idA, List<Parameter> params,
				boolean isAssume) {
			ExprVar varI = new ExprVar(cx,"i");
			StmtVarDecl init = new StmtVarDecl(cx, TypePrimitive.inttype, varI.getName(), new ExprConstInt(0));
			FENode nCx = init;
			ExprBinary cond = new ExprBinary(nCx, ExprBinary.BINOP_LT, varI, new ExprConstInt(v));
			StmtExpr incr = new StmtExpr(nCx, (Expression) new ExprUnary(cx, ExprUnary.UNOP_POSTINC, varI));
			Statement body = null;
			Expression left = (Expression) new ExprArrayRange(var, varI);
			List<Expression> nParams = new ArrayList<Expression>();
			for (Parameter p : params) {
				String nameP = p.getName();
				if (nameP.equals("st"))
					nParams.add(varI);
				else
					nParams.add(new ExprVar(cx, nameP));
			}
			Expression right = (Expression) new ExprUnary(nCx, ExprUnary.UNOP_NOT,
					new ExprFunCall(nCx, "access_" + idA, nParams));
			ExprBinary condA = new ExprBinary(nCx, ExprBinary.BINOP_BXOR, left, right);
			if (isAssume)
				body = (Statement) new StmtAssume(nCx, condA, "");
			else
				body = (Statement) new StmtAssert(nCx, condA, false);
			return new StmtFor(nCx, init, cond, incr, body, true);
		}

		public Object accessConditionFor(FENode cx, Expression var, int v, String idA, List<Parameter> params,
				String index, boolean isAssume) {
			ExprVar varI = new ExprVar(cx, "i");
			StmtVarDecl init = new StmtVarDecl(cx, TypePrimitive.inttype, varI.getName(), new ExprConstInt(0));
			ExprBinary cond = new ExprBinary(cx, ExprBinary.BINOP_LT, varI, new ExprConstInt(v));
			StmtExpr incr = new StmtExpr(cx, (Expression) new ExprUnary(cx, ExprUnary.UNOP_POSTINC, varI));
			Statement body = null;
			Expression left = (Expression) new ExprArrayRange(new ExprArrayRange(var, new ExprVar(cx, index)), varI);
			List<Expression> nParams = new ArrayList<Expression>();
			for (Parameter p : params) {
				String nameP = p.getName();
				if (nameP.equals("st"))
					nParams.add(varI);
				else
					nParams.add(new ExprVar(cx, nameP));
			}
			Expression right = (Expression) new ExprUnary(cx, ExprUnary.UNOP_NOT,
					new ExprFunCall(cx, "access_" + idA, nParams));
			ExprBinary condA = new ExprBinary(cx, ExprBinary.BINOP_BXOR, left, right);
			if (isAssume)
				body = (Statement) new StmtAssume(cx, condA, "");
			else
				body = (Statement) new StmtAssert(cx, condA, false);
			FEContext nCx = cx.getContext();
			nCx.setLTL(true);
			return new StmtFor(nCx, init, cond, incr, body, true);
		}

		public Object convergeCond(FEContext cx, String helper, List<Parameter> ps, int idA) {
			Statement ret = null;
			List<Expression> init = new ArrayList<>();
			List<Expression> post = new ArrayList<>();
			List<Parameter> psp = new ArrayList<>();
			for (Parameter p : ps) {
				if (!p.getName().startsWith("st")) {
					init.add(new ExprVar(cx, "init_" + p.getName()));
					post.add(new ExprVar(cx, p.getName()));
					psp.add(p);
				}
			}
			this.fNamesParams.add(new Pair<String, List<Parameter>>(helper, psp));
			Expression funInit = new ExprFunCall(cx, helper, init);
			Expression funPost = new ExprFunCall(cx, helper, post);
			FENode nCx = funInit;
			Expression guardFinal = new ExprBinary(nCx, ExprBinary.BINOP_EQ, funInit, new ExprConstInt(0));
			Expression condConv1 = new ExprBinary(nCx, ExprBinary.BINOP_GT, funInit, funPost);
			Expression condConv2 = new ExprBinary(nCx, ExprBinary.BINOP_GE, funInit, new ExprConstInt(0));
			StmtAssert condConv = new StmtAssert(nCx, new ExprBinary(nCx, ExprBinary.BINOP_AND, condConv1, condConv2),
					false);
			StmtAssert condFinal = new StmtAssert(nCx, new ExprVar(cx, "accept" + idA), false);
			ret = new StmtIfThen(nCx, guardFinal, condFinal, condConv);
			return ret;
		}

		public Object convergeCondFor(FENode cx, String helper, List<Parameter> ps, Expression idA, String index) {
			Statement ret = null;
			List<Expression> init = new ArrayList<>();
			List<Expression> post = new ArrayList<>();
			List<Parameter> psp = new ArrayList<>();
			for (Parameter p : ps) {
				if (!p.getName().startsWith("st")) {
					init.add(new ExprVar(cx, "init_" + p.getName()));
					post.add(new ExprVar(cx, p.getName()));
					psp.add(p);
				}
			}
			psp.add(new Parameter(cx, TypePrimitive.inttype, index));
			this.fNamesParams.add(new Pair<String, List<Parameter>>(helper, psp));
			init.add(new ExprVar(cx, index));
			post.add(new ExprVar(cx, index));
			Expression funInit = new ExprFunCall(cx, helper, init);
			Expression funPost = new ExprFunCall(cx, helper, post);
			Expression guardFinal = new ExprBinary(cx, ExprBinary.BINOP_EQ, funInit, new ExprConstInt(0));
			Expression condConv1 = new ExprBinary(cx, ExprBinary.BINOP_GT, funInit, funPost);
			Expression condConv2 = new ExprBinary(cx, ExprBinary.BINOP_GE, funInit, new ExprConstInt(0));
			StmtAssert condConv = new StmtAssert(cx, new ExprBinary(cx, ExprBinary.BINOP_AND, condConv1, condConv2),
					false);
			StmtAssert condFinal = new StmtAssert(cx, idA, false);
			FEContext nCx = cx.getContext();
			nCx.setLTL(true);
			ret = new StmtIfThen(nCx, guardFinal, condFinal, condConv);
			return ret;
		}

		public List<Function> getNewFuncs() {
			return newFuncs;
		}
	}

	class Incorporate extends FEReplacer {

		int depth;
		List<Statement> assumes, asserts, converges;

		Incorporate(int depth, List<Statement> assumes, List<Statement> asserts, List<Statement> converges) {
			this.depth = depth;
			this.assumes = assumes;
			this.asserts = asserts;
			this.converges = converges;
		}

		public Object visitStmtBlock(StmtBlock block) {
			if (depth == 1) {
				for (Statement st : assumes)
					this.addStatement(st);
				for (Statement st : block.getStmts())
					this.addStatement(st);
				for (Statement st : asserts)
					this.addStatement(st);
				for (Statement st : converges)
					this.addStatement(st);
				return null;
			}
			depth++;
			return super.visitStmtBlock(block);
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
