package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstFloat;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprHole;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.structure.GetAssignLHS;

class CollectModifiedVariables extends FEReplacer {
	Set<String> modifiedVariables;

	public CollectModifiedVariables(NameResolver nres) {
		modifiedVariables = new HashSet<String>();
		this.nres = nres;
	}

	public Set<String> getModifiedVariables() {
		return modifiedVariables;
	}

	@Override
	public Object visitStmtVarDecl(StmtVarDecl stmt) {
		modifiedVariables.addAll(stmt.getNames());
		return super.visitStmtVarDecl(stmt);
	}

	@Override
	public Object visitStmtAssign(StmtAssign stmt) {
		modifiedVariables.add(stmt.getLhsBase().getName());
		return super.visitStmtAssign(stmt);
	}

	@Override
	public Object visitExprFunCall(ExprFunCall exp) {
		Function fun = nres.getFun(exp.getName());
		Iterator<Parameter> it = fun.getParams().iterator();
		int diff = fun.getParams().size() - exp.getParams().size();

		for (int i = 0; i < diff; i++) {
			it.next();
		}

		for (Expression e : exp.getParams()) {
			Parameter p = it.next();
			if (p.isParameterOutput() || p.isParameterReference() || p.getType().isArray() || p.getType().isStruct()) {
				ExprVar baseVar = e.accept(new GetAssignLHS());
				modifiedVariables.add(baseVar.getName());
			}
		}

		return super.visitExprFunCall(exp);
	}

}

class ModifiedVariablesChecker extends FEReplacer {
	Set<String> modifiedVariables;
	boolean hasModifiedVariables = false;

	public ModifiedVariablesChecker(Set<String> modifiedVariables) {
		this.modifiedVariables = modifiedVariables;
	}

	public void reset() {
		hasModifiedVariables = false;
	}

	public boolean hasModifiedVariables() {
		return hasModifiedVariables;
	}

	@Override
	public Object visitExprVar(ExprVar exp) {
		if (modifiedVariables.contains(exp.getName())) {
			hasModifiedVariables = true;
		}
		return super.visitExprVar(exp);
	}

}

class CollectDynamicComparisons extends SymbolTableVisitor {
	Set<Expression> dynamicComparisons;
	ModifiedVariablesChecker mvChecker;

	public CollectDynamicComparisons(NameResolver nres, SymbolTable symtab,
			ModifiedVariablesChecker mvChecker) {
		super(symtab);
		this.nres = nres;
		this.dynamicComparisons = new HashSet<Expression>();
		this.mvChecker = mvChecker;
	}

	public Set<Expression> getDynamicComparisons() {
		return dynamicComparisons;
	}

	@Override
	public Object visitExprFunCall(ExprFunCall exp) {
		// TODO: this function should also look into function calls - but not
		// sure what is the best way to deal with this
		return super.visitExprFunCall(exp);
	}

	@Override
	public Object visitExprBinary(ExprBinary exp) {
		if (exp.isComparison()) {
			Type t = getType(exp.getLeft());
			if (t.equals(TypePrimitive.floattype)) {
				mvChecker.reset();
				exp.getLeft().accept(mvChecker);
				boolean left = mvChecker.hasModifiedVariables;
				mvChecker.reset();
				exp.getRight().accept(mvChecker);
				boolean right = mvChecker.hasModifiedVariables;
				if (left || right) {
					// this means the condition can dynamically change across
					// the iterations of the loop
					dynamicComparisons.add(exp);
				}
			}
		}

		return super.visitExprBinary(exp);
	}
}

class ReplaceDynamicComparisons extends FEReplacer {
	Map<Expression, Expression> replaceExprs;
	TempVarGen varGen;

	public ReplaceDynamicComparisons(Map<Expression, Expression> replaceExprs, TempVarGen varGen) {
		this.replaceExprs = replaceExprs;
		this.varGen = varGen;
	}

	@Override
	public Object visitExprBinary(ExprBinary exp) {
		if (replaceExprs.containsKey(exp)) {
			Expression newBool = replaceExprs.get(exp);
			Expression newExp = (Expression) super.visitExprBinary(exp);
			// Create assertions to make sure the value of the new bool
			// variable matches the actual comparison condition.
			// bit a
			// if (b) {
			// a = exp
			// } else {
			// a = ~exp
			// }
			// assert(a)
			String newVarName = varGen.nextVar("_a_");
			StmtVarDecl newVarDecl = new StmtVarDecl(exp,
					TypePrimitive.bittype, newVarName, null);
			addStatement(newVarDecl);
			ExprVar newVar = new ExprVar(exp, newVarName);
			Statement s = new StmtIfThen(exp, newBool, new StmtAssign(exp,
					newVar, newExp), new StmtAssign(exp, newVar, new ExprUnary(
					exp, ExprUnary.UNOP_NOT, newExp)));
			addStatement(s);
			addStatement(new StmtAssert(exp, newVar, 2));
			// Add this above assert before the statement containing the actual
			// expression.
			return newBool;
		} else {
			return super.visitExprBinary(exp);
		}
	}
}

public class PreprocessForLoop extends SymbolTableVisitor {
	int MAX_UNROLL;
	TempVarGen varGen;

	double it_incr = 0.1;
	int MAX_NUM_MODES = 1;

	public PreprocessForLoop(TempVarGen varGen, SketchOptions options) {
		super(null);
		MAX_UNROLL = options.bndOpts.unrollAmnt;
		this.varGen = varGen;
	}

	@Override
	public Object visitStmtFor(StmtFor stmt) {
		if (stmt.isCanonical()) {
			List<Statement> newStmts = new ArrayList<Statement>();

			// First determine the number of iterations
			Expression start = stmt.getRangeMin();
			Expression end = stmt.getRangeMax();
			Expression incr = stmt.getIncrVal();
			int numIterations = (int) Math.ceil((end.getIValue()
					- start.getIValue() + 1)
					/ (double) (incr.getIValue()));

			String itVarName = varGen.nextVar("_it");
			StmtVarDecl itVarDecl = new StmtVarDecl(stmt,
					TypePrimitive.inttype, itVarName, null);
			addStatement(itVarDecl);
			ExprVar itVar = new ExprVar(stmt, itVarName);
			StmtAssign itVarInit = new StmtAssign(stmt, itVar,
					ExprConstInt.zero);
			Expression itVarCond = new ExprBinary(stmt, ExprBinary.BINOP_LT,
					itVar, new ExprConstInt(numIterations));
			StmtExpr itIncr = new StmtExpr(stmt, new ExprUnary(stmt,
					ExprUnary.UNOP_POSTINC, itVar));

			// Create a new iteration variable of type float
			String floatItVarName = varGen.nextVar("_t");
			StmtVarDecl floatItVarDecl = new StmtVarDecl(stmt,
					TypePrimitive.floattype, floatItVarName, null);
			addStatement(floatItVarDecl);
			ExprVar floatItVar = new ExprVar(stmt, floatItVarName);
			StmtAssign floatItVarInit = new StmtAssign(stmt, floatItVar,
					ExprConstFloat.ZERO);
			addStatement(floatItVarInit);

			double maxFloatItVar = numIterations * it_incr;

			// Add the old init statement - in case the old iter variable is
			// used inside the loop for any other purpose
			newStmts.add(stmt.getInit());
			// We can ignore the old condition, because it is factored into the
			// numIterations calculations (assuming the loop is in canonical
			// form)

			// Collect variables modified inside the body of the loop
			CollectModifiedVariables mv = new CollectModifiedVariables(nres);
			stmt.getBody().accept(mv);
			Set<String> modifiedVariables = mv.getModifiedVariables();
			// Add the old iteration variable to the list of modifiedVariables
			modifiedVariables.add(stmt.getIndVar());

			// Collect all comparison expressions that are not static in the
			// loop body
			ModifiedVariablesChecker mvChecker = new ModifiedVariablesChecker(
					modifiedVariables);
			CollectDynamicComparisons dc = new CollectDynamicComparisons(nres,
					symtab, mvChecker);
			stmt.getBody().accept(dc);
			Set<Expression> dynamicComparisons = dc.getDynamicComparisons();
			
			// Now, handle each dynamic constraint
			Map<Expression, Expression> replaceExprs = new HashMap<Expression, Expression>();

			for (Expression e : dynamicComparisons) {
				// Create a new boolean variable to replace this dynamic
				// comparison
				String newBoolName = varGen.nextVar("_b");
				StmtVarDecl newBoolDecl = new StmtVarDecl(stmt,
						TypePrimitive.bittype, newBoolName, null);
				ExprVar newBoolVar = new ExprVar(stmt, newBoolName);
				StmtAssign newBoolInit = new StmtAssign(stmt, newBoolVar,
						ExprConstInt.zero);
				addStatement(newBoolDecl);
				addStatement(newBoolInit);
				replaceExprs.put(e, newBoolVar);

				List<ExprVar> switchVars = new ArrayList<ExprVar>();
				// Create new float holes to decide mode switches
				for (int i = 0; i < MAX_NUM_MODES; i++) {
					ExprHole switchHole = new ExprHole(stmt);
					switchHole.makeSpecial();
					switchHole.setType(TypePrimitive.floattype);

					String switchName = varGen.nextVar("_switch");
					StmtVarDecl switchDecl = new StmtVarDecl(stmt,
							TypePrimitive.floattype, switchName, null);
					ExprVar switchVar = new ExprVar(stmt, switchName);
					StmtAssign switchInit = new StmtAssign(stmt, switchVar,
							switchHole);
					addStatement(switchDecl);
					addStatement(switchInit);
					switchVars.add(switchVar);

					// Add assertions on bounds of this switch variable
					if (i == 0) {
						// First switch var should be greater than 0
						Expression cond = new ExprBinary(stmt,
								ExprBinary.BINOP_GT, switchVar,
								ExprConstFloat.ZERO);
						StmtAssert as = new StmtAssert(
								stmt,
								cond,
								"First switch variable should be greater than 0",
								2);
						addStatement(as);
					} else {
						// This switch var should be greater than prev switch
						// var
						// TODO: This kind of a constraint can be hard for
						// numerical solver
						Expression cond = new ExprBinary(stmt,
								ExprBinary.BINOP_GT, switchVar,
								switchVars.get(i - 1));
						StmtAssert as = new StmtAssert(stmt, cond, "Switch "
								+ i + " should be greater that switch "
								+ (i - 1), 2);
						addStatement(as);
					}
					// Final switch should be less than numiterations
					if (i == MAX_NUM_MODES - 1) {
						Expression cond = new ExprBinary(stmt,
								ExprBinary.BINOP_LT, switchVar,
								new ExprConstFloat(maxFloatItVar));
						StmtAssert as = new StmtAssert(stmt, cond, "Switch "
								+ i + " should be less than " + maxFloatItVar,
								2);
						addStatement(as);
					}
				}
				
				// Create a new boolean hole for each mode
				Statement prev;
				ExprHole h = new ExprHole(stmt);
				h.setType(TypePrimitive.bittype);
				prev = new StmtAssign(stmt, newBoolVar, h);
				for (int i = MAX_NUM_MODES - 1; i >= 0; i--) {
					Expression cond = new ExprBinary(stmt, ExprBinary.BINOP_LT, floatItVar, switchVars.get(i));
					h = new ExprHole(stmt);
					h.setType(TypePrimitive.bittype);
					Statement s = new StmtAssign(stmt, newBoolVar, h);
					prev = new StmtIfThen(stmt, cond, s, prev);
				}
				newStmts.add(prev);
			}

			// Now, process the body of the loop and replace each dynamic
			// comparison expression with the corresponding boolean variable.
			ReplaceDynamicComparisons rdc = new ReplaceDynamicComparisons(
					replaceExprs, varGen);
			Statement newBody = (Statement) stmt.getBody().accept(rdc);
			newStmts.add(newBody);

			// Add increments to both the old and the new iteration variable
			newStmts.add(stmt.getIncr());
			newStmts.add(new StmtAssign(stmt, floatItVar, new ExprBinary(stmt,
					ExprBinary.BINOP_ADD, floatItVar, new ExprConstFloat(
							it_incr))));

			StmtFor newStmt = new StmtFor(stmt, itVarInit, itVarCond, itIncr,
					new StmtBlock(newStmts), true);
			return newStmt;
		} else {
			// TODO: Deal with this case by fixing the number of iterations to a
			// max unrolling amount
			return super.visitStmtFor(stmt);
		}
	}

}
