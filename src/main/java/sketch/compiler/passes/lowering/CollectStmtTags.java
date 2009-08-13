package sketch.compiler.passes.lowering;
import java.util.HashSet;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtLoop;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.promela.stmts.StmtFork;

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
    public Object visitStmtFork(StmtFork loop){    	
    	Object o = super.visitStmtFork(loop);    	
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
