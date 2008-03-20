package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypePrimitive;


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

	public Expression replWithLocal(Expression exp){

		String nname = varGen.nextVar("_atomize");
		addStatement(new StmtVarDecl(exp, TypePrimitive.inttype, nname,  exp));
		ExprVar ev = new ExprVar(exp, nname);
		return ev;
	}


	public Object visitExprBinary (ExprBinary exp) {
		String op = exp.getOpString ();
		if (op.equals ("&&"))
			return doLogicalExpr (exp, true);
		else if (op.equals ("||"))
			return doLogicalExpr (exp, false);

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

	protected Expression doLogicalExpr (ExprBinary eb, boolean isAnd) {
		Expression left = eb.getLeft (), right = eb.getRight ();

		//if (!(isGlobal (left) || isGlobal (right)))
		//	return eb;

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
