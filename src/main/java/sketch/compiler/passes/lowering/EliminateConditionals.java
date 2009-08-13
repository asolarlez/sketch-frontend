/**
 *
 */
package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;

/**
 * @author higgins
 *
 */
public class EliminateConditionals extends SymbolTableVisitor {
	protected TempVarGen varGen;
	protected Type nulltype;

	/**
	 * @param symtab
	 */
	public EliminateConditionals (TempVarGen varGen) {
		this (varGen, TypePrimitive.inttype);
	}

	public EliminateConditionals (TempVarGen varGen, Type nulltype) {
		super (null);
		this.varGen = varGen;
		this.nulltype = nulltype;

	}

	public Object visitExprTernary (ExprTernary et) {
		FENode cx = et;
		ExprVar tmpVar =
			new ExprVar (cx, varGen.nextVar ("_tmp_cond_elim_"));
		Type t = getType (et, nulltype);

		StmtVarDecl tmpDecl =
			new StmtVarDecl (cx, t, tmpVar.getName (), t.defaultValue ());

		List<Statement> oldStatements = newStatements;

		newStatements = new ArrayList<Statement> ();
		newStatements.add (
				new StmtAssign (tmpVar, (Expression)et.getB ().accept (this)));
		Statement thenBlock = new StmtBlock (cx, newStatements);

		newStatements = new ArrayList<Statement> ();
		newStatements.add (
				new StmtAssign (tmpVar, (Expression)et.getC ().accept (this)));
		Statement elseBlock = new StmtBlock (cx, newStatements);

		newStatements = oldStatements;

		StmtIfThen condReplace =
			new StmtIfThen (cx, (Expression) et.getA ().accept (this),
							thenBlock, elseBlock);

		addStatement (tmpDecl);
		addStatement (condReplace);

		return tmpVar;
	}

	/** Ignores the cond; it will be handled later */
	public Object visitStmtAtomicBlock (StmtAtomicBlock sab) {
		StmtBlock newBlock = (StmtBlock) sab.getBlock ().accept (this);
		return (newBlock == sab.getBlock ()) ? sab
				: new StmtAtomicBlock (sab, newBlock, sab.getCond ());
	}
}
