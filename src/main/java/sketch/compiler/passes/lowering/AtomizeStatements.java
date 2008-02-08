package streamit.frontend.passes;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypeArray;
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
		
		String nname = varGen.nextVar();
		addStatement(new StmtVarDecl(exp.getCx(), TypePrimitive.inttype, nname,  exp));
		ExprVar ev = new ExprVar(exp.getCx(), nname);
		return ev;
	}
	
	
   public Object visitExprBinary(ExprBinary exp)
    {
        Expression left = doExpression(exp.getLeft());        
        if(left instanceof ExprVar && isGlobal((ExprVar) left)){
        	left = replWithLocal(left);
        }
        
        Expression right = doExpression(exp.getRight());        
        if(right instanceof ExprVar && isGlobal((ExprVar) right)){
        	right = replWithLocal(right);
        }
        
        if (left == exp.getLeft() && right == exp.getRight())
            return exp;
        else
            return new ExprBinary(exp.getCx(), exp.getOp(), left, right, exp.getAlias());
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
