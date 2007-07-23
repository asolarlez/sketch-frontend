package streamit.frontend.passes;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.FEReplacer;

public class EliminateBitSelector extends SymbolTableVisitor {
	
	
	public EliminateBitSelector(){
		super(null);
	}
	
    public Object visitExprBinary(ExprBinary exp)
    {
      if(exp.getOp() == ExprBinary.BINOP_SELECT){
    	  ExprStar star = new ExprStar(exp.getCx());    	  
    	  star.setType( this.getType(exp.getRight()) );
    	  return new ExprTernary(exp.getCx(), ExprTernary.TEROP_COND,
    			  star, exp.getLeft(), exp.getRight()); 
      }else{
    	  return super.visitExprBinary(exp);
      }
    }
	

}
