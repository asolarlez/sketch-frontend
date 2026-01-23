package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprHole;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;

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
    	  ExprVar tmpvar = new ExprVar(exp, nm);

            Type bvtypeLeft = this.getType(exp.getLeft());
            Type bvtypeRight = this.getType(exp.getRight());
            Type bvtype = bvtypeRight.leastCommonPromotion(bvtypeLeft, nres);

            Type tmptype = bvtype;

    	  while(tmptype instanceof TypeArray){
    		  tmptype = ((TypeArray) tmptype).getBase();
    	  }
    	  assert tmptype.equals(TypePrimitive.bittype) : "The {|} operator is currently only defined for bit vectors: " + exp;
    	  StmtVarDecl decl = new StmtVarDecl(exp, bvtype , nm, null  );
    	  this.addStatement( decl );
    	  ExprHole star = new ExprHole(exp);
    	  star.setType( bvtype );
    	  StmtAssign ass = new StmtAssign(tmpvar, star );
    	  this.addStatement(ass);
    	  Expression left = exp.getLeft().doExpr(this);
    	  Expression right = exp.getRight().doExpr(this);
    	  return new ExprBinary(exp, ExprBinary.BINOP_BOR,
    		new ExprBinary(exp,  ExprBinary.BINOP_BAND,left, tmpvar)
    	  ,
    	    new ExprBinary(exp,  ExprBinary.BINOP_BAND,right, new ExprUnary(exp, ExprUnary.UNOP_BNOT, tmpvar  ) )
    	  );
      }else{
    	  return super.visitExprBinary(exp);
      }
    }


}
