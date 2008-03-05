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
	boolean isCond = false;
	
	/**
	 * @param context
	 * @param stmts
	 */
	public StmtAtomicBlock (FENode context, List<? extends Statement> stmts) {
		super (context);
		block = new StmtBlock(context, stmts);
		cond = ExprConstInt.one;
	}
	
	/**
	 * @param context
	 * @param stmts
	 */
	public StmtAtomicBlock (FENode context, List<? extends Statement> stmts, Expression cond, boolean isCond) {
		super (context);
		block = new StmtBlock(context, stmts);
		this.cond = cond;
		this.isCond = isCond;
	}
	
	public StmtAtomicBlock (FENode context, StmtBlock stmt, Expression cond, boolean isCond) {
		super (context);
		block = stmt;
		this.cond = cond;
		this.isCond = isCond;
	}
	
	/**
	 * @param context
	 * @param stmts
	 */
	public StmtAtomicBlock (FENode context, Statement stmt, Expression cond) {
		super (context);
		block = new StmtBlock(stmt);
		this.cond = cond;
		isCond = true;
	}
	
	/**
	 * @param context
	 * @param stmts
	 */
	public StmtAtomicBlock (FENode context, List<? extends Statement> stmts, Expression cond) {
		super (context);
		block = new StmtBlock(context, stmts);
		this.cond = cond;
		isCond = true;
	}

	/**
	 * @param context
	 * @param stmts
	 * @deprecated
	 */
	public StmtAtomicBlock (FEContext context, List<? extends Statement> stmts) {
		super (context);
		block = new StmtBlock(context, stmts);
		cond = ExprConstInt.one;
	}

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v) {
        return v.visitStmtAtomicBlock(this);
    }
    
    public boolean isCond(){
    	return isCond;
    }

    public StmtBlock getBlock(){
    	return block;
    }
    
    public Expression getCond(){
    	return cond;
    }
    
    public String toString () {
    	if(isCond)return "atomic(" + cond + ") {\n"+ block.toString () +"}";
    	return "atomic {\n"+ block.toString () +"}";
    }
}
