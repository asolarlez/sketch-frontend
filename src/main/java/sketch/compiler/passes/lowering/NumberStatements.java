package streamit.frontend.passes;

import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtPloop;
import streamit.frontend.nodes.StmtVarDecl;

/**
 * The purpose of this class is to number all the statements in the program. 
 * This is useful when later matching different versions of the AST.
 * 
 * @author asolar
 *
 */
public class NumberStatements extends FEReplacer {
	int idx=0;
	
	public Statement number(Object o){
		Statement s = (Statement) o;
		s.setTag(new Integer(idx++));
		return s;
	}
	@Override
    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
    	Object o = super.visitStmtVarDecl(stmt);    	
    	return number(o);
    }
	@Override
    public Object visitStmtPloop(StmtPloop loop){    	
    	Object o = super.visitStmtPloop(loop);    	
    	return number(o);
    }
	@Override
    public Object visitStmtLoop(StmtLoop loop){    	
    	Object o = super.visitStmtLoop(loop);    	
    	return number(o);
    }
	
	

	@Override
    public Object visitStmtIfThen(StmtIfThen stmt){    	
    	Object o = super.visitStmtIfThen(stmt);    	
    	return number(o);
    }
	
	@Override
    public Object visitStmtFor(StmtFor stmt){    	
    	Object o = super.visitStmtFor(stmt);    	
    	return number(o);
    }	
	
	@Override
    public Object visitStmtBlock(StmtBlock stmt){    	
    	Object o = super.visitStmtBlock(stmt);    	
    	return number(o);
    }
	
	@Override
	public Object visitStmtAtomicBlock (StmtAtomicBlock ab) {
		Object o = super.visitStmtAtomicBlock (ab);    	
    	return number(o);
	}
	
	
	@Override
	public Object visitStmtAssign(StmtAssign stmt){
		Object o = super.visitStmtAssign(stmt);    	
    	return number(o);
	}
	

	@Override
    public Object visitStmtAssert(StmtAssert stmt){
		Object o = super.visitStmtAssert(stmt);    	
		return number(o);
	}

	
}
