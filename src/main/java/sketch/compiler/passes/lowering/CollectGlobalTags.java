package streamit.frontend.passes;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtVarDecl;

public class CollectGlobalTags extends FEReplacer {
	public Set<Object> oset = new HashSet<Object>();
	public Set<String> globals;
	
	public  boolean isGlobal = false;
	
	public Statement collectTag(Object o){
		Statement s = (Statement) o;
		if(s.getTag() != null){
			oset.add(s.getTag());
		}
		return s;
	}
	
	public CollectGlobalTags(Set<StmtVarDecl> svd){
		globals = new HashSet<String>();
		for(Iterator<StmtVarDecl> it = svd.iterator(); it.hasNext(); ){
			StmtVarDecl sv = it.next();
			for(int i=0; i<sv.getNumVars(); ++i){
				globals.add( sv.getName(i) );
			}
		}
	}
	
	
	public Object visitExprVar(ExprVar ev){
		
		if(globals.contains(ev.getName())){
			isGlobal = true;
		}
		
		return ev;		
	}
	

	@Override
	public Object visitStmtAssign(StmtAssign stmt){
		isGlobal = false;
		Object o = super.visitStmtAssign(stmt); 
		if( isGlobal )
			return collectTag(o);
		else
			return o;
	}
	
	@Override
	public Object visitStmtAtomicBlock(StmtAtomicBlock stmt){
		isGlobal = false;
		int sz = oset.size();
		Object o = super.visitStmtAtomicBlock(stmt); 
		if( isGlobal || sz != oset.size())
			return collectTag(o);
		else
			return o;
	}
	
	
	public Object visitExprFunCall(ExprFunCall efc){
		assert false :"NYI";
		return efc;
	}

	@Override
    public Object visitStmtAssert(StmtAssert stmt){
		Object o = super.visitStmtAssert(stmt);    	
		return collectTag(o);
	}
	
	

}
