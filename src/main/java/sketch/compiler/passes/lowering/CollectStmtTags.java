package streamit.frontend.passes;

import java.util.HashSet;
import java.util.Set;

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

public class CollectStmtTags extends FEReplacer {

	public Set<Object> oset = new HashSet<Object>();
	
	public Statement collectTag(Object o){
		Statement s = (Statement) o;
		if(s.getTag() != null){
			oset.add(s.getTag());
		}
		return s;
	}
	@Override
    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
    	Object o = super.visitStmtVarDecl(stmt);    	
    	return collectTag(o);
    }
	@Override
    public Object visitStmtPloop(StmtPloop loop){    	
    	Object o = super.visitStmtPloop(loop);    	
    	return collectTag(o);
    }
	@Override
    public Object visitStmtLoop(StmtLoop loop){    	
    	Object o = super.visitStmtLoop(loop);    	
    	return collectTag(o);
    }
	
	

	@Override
    public Object visitStmtIfThen(StmtIfThen stmt){    	
    	Object o = super.visitStmtIfThen(stmt);    	
    	return collectTag(o);
    }
	
	@Override
    public Object visitStmtFor(StmtFor stmt){    	
    	Object o = super.visitStmtFor(stmt);    	
    	return collectTag(o);
    }	
	
	@Override
    public Object visitStmtBlock(StmtBlock stmt){    	
    	Object o = super.visitStmtBlock(stmt);    	
    	return collectTag(o);
    }
	
	@Override
	public Object visitStmtAtomicBlock (StmtAtomicBlock ab) {
		Object o = super.visitStmtAtomicBlock (ab);    	
    	return collectTag(o);
	}
	
	
	@Override
	public Object visitStmtAssign(StmtAssign stmt){
		Object o = super.visitStmtAssign(stmt);    	
    	return collectTag(o);
	}
	

	@Override
    public Object visitStmtAssert(StmtAssert stmt){
		Object o = super.visitStmtAssert(stmt);    	
		return collectTag(o);
	}
}
