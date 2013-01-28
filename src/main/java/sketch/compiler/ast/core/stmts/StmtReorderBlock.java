package sketch.compiler.ast.core.stmts;
import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;

/**
 * The "reorder{ statements }" construct executes a list of statements in some unknown
 * order, and the Sketch Solver will resolve the correct order to satisfy the constraints.
 * Not very often used.
 */
public class StmtReorderBlock extends Statement {

	StmtBlock block;

	 /** Create a new StmtReorderBlock with the specified list of
     * statements. */
    public StmtReorderBlock(FENode context, List<? extends Statement> stmts)
    {
    	super(context);
    	block = new StmtBlock(context, stmts);
    }

	 /** Create a new StmtReorderBlock with the specified list of
	  * statements.
	  * @deprecated
	  */
    public StmtReorderBlock(FEContext context, List<? extends Statement> stmts)
    {
    	super(context);
    	block = new StmtBlock(context, stmts);
    }

    /** Create a new StmtReorderBlock that orders the statements within the
     * specified StmtBlock. */
    public StmtReorderBlock (FENode context, StmtBlock block) {
    	super (context);
    	this.block = block;
    }

    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtReorderBlock(this);
    }

    public List<Statement> getStmts()
    {
        return block.getStmts();
    }

    public StmtBlock getBlock(){
    	return block;
    }

    public String toString(){
    	String result = "reorder{\n";
    	Iterator it = block.getStmts().iterator();
    	while(it.hasNext()){
    		result += it.next().toString() + "\n";
    	}
    	return result + "\n}";
    }
}