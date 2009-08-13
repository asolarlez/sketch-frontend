package sketch.compiler.passes.lowering;
import java.util.HashSet;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.promela.stmts.StmtFork;

public class UnrollLocalLoops extends SimpleLoopUnroller {

	Set<String> globals = new HashSet<String>();
	boolean isInFork = false;
	@Override
	public Object visitStmtVarDecl(StmtVarDecl stmt){
		
		if(!isInFork){
			for(int i=0; i<stmt.getNumVars(); ++i  ){
				globals.add( stmt.getName(i) );
			}
		}
		return stmt;
	}
	
	
	public boolean unrollThisLoop(StmtFor stmt){
		
		class GlobalEffects extends FEReplacer{
			public int gcnt = 0;
			public Object visitStmtAssert(StmtAssert stmt){
				gcnt++;
				return stmt;
			}
			
			public Object visitStmtAtomicBlock(StmtAtomicBlock stmt){
				gcnt++;
				return stmt;
			}
			
			public Object visitExprVar(ExprVar exp){
				if(globals.contains(exp.getName())){
					gcnt++;
				}
				return exp;
			}						
		}
		
		GlobalEffects ge = new GlobalEffects();
		stmt.accept(ge);
		return ge.gcnt<5;
	}
	
	
	public Object visitStmtFork(StmtFork stmt){		
		boolean tmp = isInFork;
		isInFork = true;
		Object o = super.visitStmtFork(stmt);
		isInFork = tmp;
		return o;
	}
	
	
	
}
