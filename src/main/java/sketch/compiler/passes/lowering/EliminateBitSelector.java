package streamit.frontend.passes;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;

public class EliminateBitSelector extends SymbolTableVisitor {
	
	TempVarGen varGen;
	public EliminateBitSelector(TempVarGen varGen){
		super(null);
		this.varGen = varGen;
	}
	
    public Object visitExprBinary(ExprBinary exp)
    {
      if(exp.getOp() == ExprBinary.BINOP_SELECT){
    	  String nm = varGen.nextVar();
    	  ExprVar tmpvar = new ExprVar(exp.getCx(), nm);
    	  Type bvtype = this.getType(exp.getRight());
    	  Type tmptype = bvtype;
    	  
    	  while(tmptype instanceof TypeArray){
    		  tmptype = ((TypeArray) tmptype).getBase();
    	  }
    	  assert tmptype.equals(TypePrimitive.bittype) : "The {|} operator is currently only defined for bit vectors: " + exp.getCx();
    	  StmtVarDecl decl = new StmtVarDecl(exp.getCx(), bvtype , nm, null  );    	  
    	  this.addStatement( decl );
    	  ExprStar star = new ExprStar(exp.getCx());
    	  star.setType( bvtype );
    	  StmtAssign ass = new StmtAssign(exp.getCx(), tmpvar, star );
    	  this.addStatement(ass);

    	  FEContext c = exp.getCx();
    	  
    	  return new ExprBinary(c, ExprBinary.BINOP_BOR,
    		new ExprBinary(c,  ExprBinary.BINOP_BAND,exp.getLeft(), tmpvar)
    	  ,
    	    new ExprBinary(c,  ExprBinary.BINOP_BAND,exp.getRight(), new ExprUnary(c, ExprUnary.UNOP_BNOT, tmpvar  ) )
    	  );
      }else{
    	  return super.visitExprBinary(exp);
      }
    }
	

}
