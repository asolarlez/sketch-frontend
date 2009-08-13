package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;


/**
 *
 * Each statement within a parallel section is decomposed into
 * atomic substatements.
 * For example, if a,b and c are global,
 *
 * a = b + c
 *
 * gets partitioned into
 *
 * t1 = c;
 * t2 = b;
 * a = t1 + t2;
 *
 * Assumes that conditional expressions have been eliminated.
 *
 * @author asolar
 *
 */
public class AtomizeStatements extends SymbolTableVisitor {
	TempVarGen varGen;
	public AtomizeStatements(TempVarGen varGen){
		super(null);
		this.varGen = varGen;
	}


	public Object visitStmtAtomicBlock(StmtAtomicBlock atom){
		return atom;
	}


	public Expression replWithLocal(Expression exp){

		String nname = varGen.nextVar("_atomize");
		addStatement(new StmtVarDecl(exp, getType (exp), nname,  exp));
		ExprVar ev = new ExprVar(exp, nname);
		return ev;
	}


	public Object visitExprBinary (ExprBinary exp) {
		String op = exp.getOpString ();
		if (op.equals ("&&") || op.equals ("||"))
			return doLogicalExpr (exp, op);

		Expression left = doExpression (exp.getLeft ());
		if (left instanceof ExprVar && isGlobal ((ExprVar) left)) {
			left = replWithLocal (left);
		}

		Expression right = doExpression (exp.getRight ());
		if (right instanceof ExprVar && isGlobal ((ExprVar) right)) {
			right = replWithLocal (right);
		}

		if (left == exp.getLeft () && right == exp.getRight ())
			return exp;
		else
			return new ExprBinary (exp, exp.getOp (), left, right, exp
					.getAlias ());
	}

	protected Expression doLogicalExpr (ExprBinary eb, String op) {
		boolean isAnd = op.equals ("&&");
		Expression left = eb.getLeft (), right = eb.getRight ();

		if (!(isGlobal (left) || isGlobal (right)))
			return eb;

		left = doExpression (left);

		String resName = varGen.nextVar ("_atomize_sc");
		addStatement (new StmtVarDecl (eb, TypePrimitive.bittype, resName, left));
		ExprVar res = new ExprVar (eb, resName);

		List<Statement> oldStatements = newStatements;
        newStatements = new ArrayList<Statement>();

        right = doExpression (right);
        newStatements.add (new StmtAssign (res, right));

        StmtBlock thenBlock = new StmtBlock (eb, newStatements);
        newStatements = oldStatements;

        // What is the condition on 'res' that causes us to fully evaluate
        // the expression?  If it's a logical AND, and 'res' is true, then we
        // need to evaluate the right expr.  If it's a logical OR, and 'res' is
        // false, then we need to evaluate the right expr.
        Expression cond = isAnd ? res : new ExprUnary ("!", res);

        addStatement (new StmtIfThen (eb, cond, thenBlock, null));

        return res;
	}
	
	public Object visitStmtIfThen(StmtIfThen stmt){
		Expression newCond;
		if(stmt.getCond() instanceof ExprVar && isGlobal ((ExprVar) stmt.getCond())){
			newCond = replWithLocal (stmt.getCond());
		}else{
			newCond = doExpression(stmt.getCond());
		}
		 
        Statement newCons = stmt.getCons() == null ? null :
            (Statement)stmt.getCons().accept(this);
        Statement newAlt = stmt.getAlt() == null ? null :
            (Statement)stmt.getAlt().accept(this);
        if (newCond == stmt.getCond() && newCons == stmt.getCons() &&
            newAlt == stmt.getAlt())
            return stmt;
        if(newCons == null && newAlt == null){
        	return new StmtExpr(stmt, newCond);
        }

        return new StmtIfThen(stmt, newCond, newCons, newAlt);
	}
	

	@Override
	public Object visitExprArrayRange(ExprArrayRange ear){
		assert ear.hasSingleIndex() : "Array ranges not allowed in parallel code.";
		Expression nofset = (Expression) ear.getOffset().accept(this);
		if(nofset instanceof ExprVar && isGlobal((ExprVar) nofset)){
			nofset = replWithLocal(nofset);
		}
		Expression base = (Expression) ear.getBase().accept(this);
		Expression near = new ExprArrayRange(base, nofset);
		return near;
	}

}
