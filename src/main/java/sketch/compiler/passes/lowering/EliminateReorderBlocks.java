package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtInsertBlock;
import streamit.frontend.nodes.StmtReorderBlock;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
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
public class EliminateReorderBlocks extends FEReplacer {

	private boolean insertBlockRewrite;
	private TempVarGen varGen;

	public EliminateReorderBlocks(TempVarGen varGen){
		this (varGen, false);
	}

	public EliminateReorderBlocks (TempVarGen varGen, boolean insertBlockRewrite) {
		this.varGen = varGen;
		this.insertBlockRewrite = insertBlockRewrite;
	}

	public Object visitStmtReorderBlock (StmtReorderBlock stmt) {
		return insertBlockRewrite ? lowerToInsertBlocks (stmt)
				: lowerToSwitchedLoop (stmt);
	}

	protected Statement lowerToInsertBlocks (StmtReorderBlock srb) {
		List<Statement> B = srb.getStmts ();

		if (1 >= B.size ())
			return (Statement) srb.getBlock ().accept (this);	// no reorder for 1 stmt

		StmtBlock into = new StmtBlock (srb,
				Collections.singletonList ((Statement)
						(new StmtReorderBlock (srb, B.subList (1, B.size ()))).accept (this)));
		Statement insert = (Statement) B.get (0).accept (this);

		return new StmtInsertBlock (srb, insert, into);
	}

	public Statement lowerToSwitchedLoop (StmtReorderBlock stmt) {
		Iterator<Statement> iter = stmt.getStmts().iterator();
		String name = varGen.nextVar();
		int len = stmt.getStmts().size();
		Statement s = recursiveCondGenerator(iter, name, 0);
		if(s==null) return null;
		assert len > 0;
		Expression elen = new ExprConstInt(len);
		StmtVarDecl svd = new StmtVarDecl(s, new TypeArray(TypePrimitive.bittype, elen), name, ExprConstInt.zero );
		StmtLoop sloop = new StmtLoop(s, elen, s);
		Expression var = new ExprVar(s, name);
		Expression fex = new ExprArrayRange(s, var, ExprConstInt.zero);
		for(int i=1; i<len; ++i){
			fex = new ExprBinary(s, ExprBinary.BINOP_AND,  fex  , new ExprArrayRange(s, var, new ExprConstInt(i)));
		}
		List<Statement> slist = new ArrayList<Statement>(2);
		slist.add(svd);
		slist.add(sloop);
		slist.add(new StmtAssert(s, fex));
		return new StmtBlock(s, slist);
	}

	private Statement recursiveCondGenerator(Iterator<Statement> iter, String iname, int i){
		if(!iter.hasNext()){ return null; }
		Statement s = iter.next();
		Expression var = new ExprArrayRange(s, new ExprVar(s, iname), new ExprConstInt(i));
		Statement set = new StmtAssign(var,  ExprConstInt.one);
		List<Statement> slist = new ArrayList<Statement>(2);
		s = (Statement)s.accept(this);
		if( s != null ){
			slist.add(s);
		}
		slist.add(set);
		Statement elsebranch = recursiveCondGenerator(iter, iname, i+1);
		if(elsebranch == null){
			return new StmtBlock(s, slist);
		}else{
			Expression cond = new ExprBinary(s, ExprBinary.BINOP_AND, new ExprStar(s), new ExprUnary(s, ExprUnary.UNOP_NOT,var));
			return new StmtIfThen(s, cond, new StmtBlock(s, slist), elsebranch);
		}
	}
}