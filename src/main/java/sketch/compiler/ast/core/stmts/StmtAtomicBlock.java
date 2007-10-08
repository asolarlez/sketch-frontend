/**
 *
 */
package streamit.frontend.nodes;

import java.util.List;

/**
 * @author Chris Jones
 *
 */
public class StmtAtomicBlock extends StmtBlock {

	/**
	 * @param context
	 * @param stmts
	 */
	public StmtAtomicBlock (FEContext context, List stmts) {
		super (context, stmts);
	}

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v) {
        return v.visitStmtAtomicBlock(this);
    }

    public String toString () {
    	return "atomic {\n"+ super.toString () +"}";
    }
}
