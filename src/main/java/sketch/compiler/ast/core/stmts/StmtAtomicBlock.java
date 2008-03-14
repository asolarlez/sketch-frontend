/**
 *
 */
package streamit.frontend.nodes;

import java.util.List;

/**
 * @author Chris Jones
 *
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
		block = new StmtBlock(context, stmts);
	}

	/**
	 * @param context
	 * @param stmts
	 * @deprecated
	 */
	public StmtAtomicBlock (FEContext context, List<? extends Statement> stmts, Expression cond) {
		super (context);
		block = new StmtBlock(context, stmts);
		this.cond = cond;
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
