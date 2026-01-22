package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FunctionCreator;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.main.seq.LTLWhileForever;

public class LTLAddWhiles extends FEReplacer {

	Program sketch;

	public LTLAddWhiles(Program sketch) {
		this.sketch = sketch;
	}

	public Object visitPackage(Package pkg) {
		List<Function> nf = new ArrayList<>();
		for (ListIterator<Function> it = pkg.getFuncs().listIterator(); it.hasNext();) {
			Function func = it.next();
			if (func.getName().startsWith("forever")) {
				String name = func.getName();
				FEContext node = func.getCx();
				Statement body = func.getBody();
				body = (Statement) body.accept(new FixTempVars());
				GetFuncWithForever getFuncs = new GetFuncWithForever(node, body);
				sketch.accept(getFuncs);
				if (!name.contains("Wrapper")) {
					Function solvedForever = getFuncs.getNewFunc();
					nf.add(solvedForever);
				} else {
					String newName = getFuncs.getNewFunc().getName();
					FunctionCreator solvC = func.creator();
					if (name.contains("WrapperNospec")) {
						solvC.name(newName + "__WrapperNospec");
					} else {
						solvC.name(newName + "__Wrapper");
						solvC.spec(newName + "__WrapperNospec");
					}
					List<Parameter> newParams = new ArrayList<>();
					for(Parameter p : func.getParams()) {
						if (!p.getName().contains("st")) {
							newParams.add(p);
						}
					}
					solvC.params(newParams);
					nf.add(solvC.create());
				}
			} else {
				nf.add(func);
			}
		}
		return pkg.newFromFcns(nf);
	}

	class GetFuncWithForever extends FEReplacer {

		FEContext node;
		Statement newBody;
		Function newFunc;

		GetFuncWithForever(FEContext node, Statement newBody) {
			this.node = node;
			this.newBody = newBody;
			this.newFunc = null;
		}

		public Object visitPackage(Package pkg) {
			for (ListIterator<Function> it = pkg.getFuncs().listIterator(); it.hasNext();) {
				Function func = it.next();
				LTLWhileForever forever = new LTLWhileForever();
				func.accept(forever);
				if (forever.getIsInf()) {
					Statement body = func.getBody();
					body = (Statement) body.accept(new InnerWhile(node, newBody));
					newFunc = func.creator().body(body).create();
				}
			}
			return pkg;
		}

		public Function getNewFunc() {
			return newFunc;
		}

	}

	class InnerWhile extends FEReplacer {

		FEContext node;
		Statement newBody;

		InnerWhile(FEContext node, Statement newBody) {
			this.node = node;
			this.newBody = newBody;
		}

		public Object visitStmtWhile(StmtWhile loop) {
			if (loop.getCond().toString().equals("1")) {
				if (loop.getCx().getLineNumber() == this.node.getLineNumber()) {
					return new StmtWhile(loop, loop.getCond(), this.newBody);
				}
			}
			return super.visitStmtWhile(loop);
		}

	}

	class FixTempVars extends FEReplacer {

		public Object visitExprVar(ExprVar var) {
			String name = var.getName();
			String[] partsName = name.split("_");
			int len = partsName.length;
			if (len > 1 && !name.contains("_s")) {
				String newName = "";
				for (int i = 0; i < len - 1; i++) {
					newName += partsName[i];
				}
				return new ExprVar(var, newName);
			}
			return super.visitExprVar(var);
		}

		public Object visitStmtVarDecl(StmtVarDecl decl) {
			List<Type> types = new ArrayList<>();
			List<String> names = new ArrayList<>();
			List<Expression> inits = new ArrayList<>();
			for (int i = 0; i < decl.getNumVars(); i++) {
				String name = decl.getName(i);
				String[] partsName = name.split("_");
				int len = partsName.length;
				if (len > 1 && !name.contains("_s")) {
					String newName = "";
					for (int j = 0; j < len - 1; j++) {
						newName += partsName[j];
					}
					names.add(newName);
					types.add(decl.getType(i));
					inits.add(decl.getInit(i));
					return new StmtVarDecl(decl, types, names, inits);
				}
			}
			return super.visitStmtVarDecl(decl);
		}

	}

}
