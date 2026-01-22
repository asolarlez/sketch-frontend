package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAssume;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.passes.lowering.MakeBodiesBlocks;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class LTLInitValues extends SymbolTableVisitor {

	Set<ExprVar> initVars;

	public LTLInitValues() {
		super(null);
		initVars = new HashSet<ExprVar>();
	}

	public Object visitFunction(Function func) {
		SymbolTable oldSymTab = symtab;
		symtab = new SymbolTable(symtab);
		String name = func.getName();
		if (name.startsWith("forever")) {
			for (Parameter p : func.getParams()) {
				String pname = p.getName();
				symtab.registerVar(pname, p.getType());
				if (!pname.startsWith("st")) {
					initVars.add(new ExprVar(p.getContext(), pname));
				}
			}
			Statement body = func.getBody();
			body = (Statement) body.accept(new GetInitVars(symtab));
			body = (StmtBlock) body.accept(new MakeBodiesBlocks());
			body = (Statement) body.accept(new IncorporateInits(symtab, initVars));
			return func.creator().body(body).create();
		}
		symtab = oldSymTab;
		return super.visitFunction(func);
	}

	class GetInitVars extends SymbolTableVisitor {

		GetInitVars(SymbolTable symtab) {
			super(symtab);
		}

		public Object visitStmtAssign(StmtAssign assign) {
			Expression left = assign.getLHS();
			Expression right = assign.getRHS();
			HasInput hasIn = new HasInput();
			right.accept(hasIn);
			String name = left.toString();
			if (hasIn.getHasIn()) {
				left.accept(new GetVars());
				Type initT = symtab.lookupVar(name, assign);
				String initName = "init_" + name;
				StmtVarDecl initDecl = new StmtVarDecl(assign, initT, initName, left);
				this.addStatement(assign);
				this.addStatement(initDecl);
				return null;
			}
			return super.visitStmtAssign(assign);
		}

	}

	class HasInput extends FEReplacer {

		boolean hasIn;

		HasInput() {
			this.hasIn = false;
		}

		public Object visitExprFunCall(ExprFunCall call) {
			hasIn = call.getName().startsWith("input");
			return super.visitExprFunCall(call);
		}

		public boolean getHasIn() {
			return hasIn;
		}
	}

	class GetVars extends FEReplacer {
		public Object visitExprVar(ExprVar var) {
			initVars.remove(var);
			return super.visitExprVar(var);
		}
	}

	class IncorporateInits extends SymbolTableVisitor {

		Set<ExprVar> initVars;

		IncorporateInits(SymbolTable symtab, Set<ExprVar> initVars) {
			super(symtab);
			this.initVars = initVars;
		}

		public Object visitStmtBlock(StmtBlock block) {
			List<Statement> oldStmts = block.getStmts();
			List<Statement> assumesIn = new ArrayList<>();
			List<Statement> newStmts1 = new ArrayList<>();
			List<Statement> newStmts2 = new ArrayList<>();
			int i = 0;
			for (Statement st : oldStmts) {
				if (st.getContext().getLTL()) {
					newStmts1.add(st);
					i++;
				} else
					break;
			}
			for (; i < oldStmts.size(); i++) {
				if (oldStmts.get(i) instanceof StmtAssume || oldStmts.get(i) instanceof StmtFor) {
					assumesIn.add(oldStmts.get(i));
				} else
					break;
			}
			List<ExprVar> vars = new ArrayList<ExprVar>();
			vars.addAll(initVars);
			for (ExprVar var : vars) {
				Type tp = symtab.lookupVar(var);
				String name = var.getName();
				StmtVarDecl initNew = new StmtVarDecl(block, tp, "init_" + name, var);
				newStmts1.add(initNew);
			}
			for (; i < oldStmts.size(); i++) {
				newStmts2.add(oldStmts.get(i));
			}
			List<Statement> newStmts = new ArrayList<>();
			newStmts.addAll(assumesIn);
			newStmts.addAll(newStmts1);
			newStmts.addAll(newStmts2);
			return new StmtBlock(block, newStmts);
		}

	}

}
