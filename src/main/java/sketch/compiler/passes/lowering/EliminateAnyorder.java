package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAnyOrderBlock;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;

/**
 * 
 * Replaces the Anyorder nodes with basic integer holes.
 * 
 * @author asolar
 *
 */
public class EliminateAnyorder extends FEReplacer {

	private TempVarGen varGen;	
	
	public EliminateAnyorder(TempVarGen varGen){
		this.varGen = varGen;
	}
	
	private Statement recursiveCondGenerator(Iterator<Statement> iter, String iname, int i){
		if(!iter.hasNext()){ return null; }
		Statement s = iter.next();
		FEContext cx = s.getCx();
		Expression var = new ExprArrayRange(cx, new ExprVar(cx, iname), new ExprConstInt(i));		
		Statement set = new StmtAssign(cx, var,  ExprConstInt.one);
		List<Statement> slist = new ArrayList<Statement>(2);
		s = (Statement)s.accept(this);
		if( s != null ){
			slist.add(s);
		}
		slist.add(set);
		Statement elsebranch = recursiveCondGenerator(iter, iname, i+1);
		if(elsebranch == null){
			return new StmtBlock(cx, slist);
		}else{
			Expression cond = new ExprBinary(cx, ExprBinary.BINOP_AND, new ExprStar(cx), new ExprUnary(cx, ExprUnary.UNOP_NOT,var));
			return new StmtIfThen(cx, cond, new StmtBlock(cx, slist), elsebranch);
		}
	}

	 public Object visitStmtAnyOrderBlock(StmtAnyOrderBlock stmt){
	    
		 Iterator<Statement> iter = stmt.getStmts().iterator();
		 String name = varGen.nextVar();
		 int len = stmt.getStmts().size();
		 Statement s = recursiveCondGenerator(iter, name, 0);
		 if(s==null) return null;
		 assert len > 0;
		 FEContext cx = s.getCx();
		 Expression elen = new ExprConstInt(len);
		 StmtVarDecl svd = new StmtVarDecl(cx, new TypeArray(TypePrimitive.bittype, elen), name, ExprConstInt.zero );
		 StmtLoop sloop = new StmtLoop(cx, elen, s);		 
		 Expression var = new ExprVar(cx, name);
		 Expression fex = new ExprArrayRange(cx, var, ExprConstInt.zero);
		 for(int i=1; i<len; ++i){
			 fex = new ExprBinary(cx, ExprBinary.BINOP_AND,  fex  , new ExprArrayRange(cx, var, new ExprConstInt(i)));
		 }		 
		 List<Statement> slist = new ArrayList<Statement>(2);
		 slist.add(svd);
		 slist.add(sloop);
		 slist.add(new StmtAssert(cx, fex));		 
		 return new StmtBlock(cx, slist);    	
    }
}
