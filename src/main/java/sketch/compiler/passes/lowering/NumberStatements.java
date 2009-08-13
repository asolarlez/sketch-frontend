package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.promela.stmts.StmtFork;

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
    public Object visitStmtFork(StmtFork loop){
    	Object o = super.visitStmtFork(loop);
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

	@Override
	public Object visitStmtExpr(StmtExpr stmt){
		Object o = super.visitStmtExpr(stmt);
		return number(o);
	}

}
