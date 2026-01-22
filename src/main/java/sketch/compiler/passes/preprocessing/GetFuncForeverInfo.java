package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.Function.FunctionCreator;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.main.seq.LTLWhileForever;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.Pair;

public class GetFuncForeverInfo extends SymbolTableVisitor {

	List<Function> nf;

	public GetFuncForeverInfo() {
		super(null);
		this.nf = new ArrayList<Function>();
	}

	public Object visitPackage(Package pkg) {
		for (ListIterator<Function> it = pkg.getFuncs().listIterator(); it.hasNext();) {
			Function func = it.next();
			func = (Function) func.accept(new ReachFunctions(nf));
			nf.add(func);
		}
		return pkg.newFromFcns(nf);
	}

	class ReachFunctions extends SymbolTableVisitor {

		List<Function> nf;

		public ReachFunctions(List<Function> nf) {
			super(null);
			this.nf = nf;
		}

		public Object visitFunction(Function func) {
			SymbolTable oldSymTab = symtab;
			symtab = new SymbolTable(symtab);

			Statement body = (Statement) func.getBody();
			if (body == null) {
				symtab = oldSymTab;
				return super.visitFunction(func);
			}
			LTLWhileForever forever = new LTLWhileForever();
			body.accept(forever);
			boolean anyForever = forever.getIsInf();
			if (anyForever) {
				for (Parameter p : func.getParams()) {
					symtab.registerVar(p.getName(), p.getType());
				}
				ReachLoop reached = new ReachLoop(symtab, func.getPkg());
				body = (Statement) body.accept(reached);
				symtab = oldSymTab;
				nf.add(reached.getNewFunc());
				return func.creator().body(body).create();
			}
			symtab = oldSymTab;
			return super.visitFunction(func);
		}

	}
	
	class ReachLoop extends SymbolTableVisitor {

		Function newFunc;
		String pkg;

		ReachLoop(SymbolTable symtab, String pkg) {
			super(symtab);
			newFunc = null;
			this.pkg = pkg;
		}

		public Object visitStmtWhile(StmtWhile loop) {
			Statement body = loop.getBody();
			FENode cx = loop;
			List<Parameter> nPs = new ArrayList<Parameter>();
			List<Pair<String, Type>> newPs = new ArrayList<>();
			List<Pair<String, Type>> remVs = new ArrayList<>();
			GetVars getVs = new GetVars(symtab, newPs, remVs);
			body.accept(getVs);
			Iterator<Pair<String, Type>> it = newPs.iterator();
			while (it.hasNext()) {
				Pair<String, Type> p = it.next();
				for (Pair<String, Type> r : remVs) {
					if (p.getFirst().equals(r.getFirst())) {
						it.remove();
					}
				}
			}
			for (Pair<String, Type> p : newPs) {
				nPs.add(new Parameter(cx, p.getSecond(), p.getFirst()));
			}
			FunctionCreator newFCreator = Function.creator(loop, "forever" + loop.getCx().getLineNumber(),
					FcnType.Harness);
			newFCreator.params(nPs);
			newFCreator.body(body);
			newFCreator.pkg(pkg);
			newFunc = newFCreator.create();
			return loop;
		}

		public Function getNewFunc() {
			return newFunc;
		}

	}

	private class GetVars extends SymbolTableVisitor {

		List<Pair<String, Type>> newPs, remVs;

		public GetVars(SymbolTable symtab, List<Pair<String, Type>> newPs, List<Pair<String, Type>> remVs) {
			super(symtab);
			this.newPs = newPs;
			this.remVs = remVs;
		}

		public Object visitExprVar(ExprVar var) {
			newPs.add(new Pair<String, Type>(var.getName(), symtab.lookupVar(var)));
			List<Pair<String, Type>> news = new ArrayList<>();
			for (Pair<String, Type> p : newPs) {
				if (!containsAux(news, p))
					news.add(p);
			}
			newPs.clear();
			newPs.addAll(news);
			return super.visitExprVar(var);
		}

		public Object visitStmtVarDecl(StmtVarDecl decl) {
			List<String> names = decl.getNames();
			List<Type> types = decl.getTypes();
			for(int i = 0; i < names.size(); i++) {
				remVs.add(new Pair<String, Type>(names.get(i), types.get(i)));
				List<Pair<String, Type>> news = new ArrayList<>();
				for (Pair<String, Type> p : remVs) {
					if (!containsAux(news, p))
						news.add(p);
				}
				remVs.clear();
				remVs.addAll(news);
			}
			return super.visitStmtVarDecl(decl);
		}

	}

	private boolean containsAux(List<Pair<String, Type>> lst, Pair<String, Type> elem) {
		for (Pair<String, Type> p : lst) {
			if (p.getFirst().equals(elem.getFirst())) {
				return true;
			}
		}
		return false;
	}
}