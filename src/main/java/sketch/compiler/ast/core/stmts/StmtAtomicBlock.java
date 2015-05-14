/**
 *
 */
package sketch.compiler.ast.core.stmts;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;

/**
 * Not used now.
 * 
 * @deprecated
 * @author Chris Jones
 */
public class StmtAtomicBlock extends  Statement{

	StmtBlock block;
	Expression cond;

	/**
	 * @param context
	 * @param stmts
	 */
	public StmtAtomicBlock (FENode context, List<? extends Statement> stmts) {
		super (context);
		block = new StmtBlock(context, stmts);
	}

	/**
	 * @param context
	 * @param stmts
	 */
	public StmtAtomicBlock (FENode context, List<? extends Statement> stmts, Expression cond) {
		super (context);
		block = new StmtBlock(context, stmts);
		this.cond = cond;
	}

	public StmtAtomicBlock (FENode context, StmtBlock stmt, Expression cond) {		
		super (context);
		block = stmt;
		this.cond = cond;
	}

	/**
	 * @param context
	 * @param stmts
	 */
	public StmtAtomicBlock (FENode context, Statement stmt, Expression cond) {
		super (context);
		block = new StmtBlock(stmt);
		this.cond = cond;
	}

	/**
	 * @param context
	 * @param stmts
	 * @deprecated
	 */
	public StmtAtomicBlock (FEContext context, List<? extends Statement> stmts) {
		super (context);
		assert context != null;
		block = new StmtBlock(context, stmts);
	}

	/**
	 * @param context
	 * @param stmts
	 * @deprecated
	 */
	public StmtAtomicBlock (FEContext context, List<? extends Statement> stmts, Expression cond) {
		super (context);
		assert context != null;
		block = new StmtBlock(context, stmts);
		this.cond = cond;
	}

    @Override
    public int size() {
        return block == null ? 0 : block.size();
    }

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v) {
        return v.visitStmtAtomicBlock(this);
    }

    public boolean isCond(){
    	return cond != null;
    }

    public StmtBlock getBlock(){
    	return block;
    }

    public Expression getCond(){
    	return cond;
    }

    public String toString () {
    	if(isCond ())return "atomic(" + cond + ") {\n"+ block.toString () +"}";
    	return "atomic {\n"+ block.toString () +"}";
    }
}
