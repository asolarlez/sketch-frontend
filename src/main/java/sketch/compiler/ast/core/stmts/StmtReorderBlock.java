package streamit.frontend.nodes;

import java.util.Iterator;
import java.util.List;

public class StmtReorderBlock extends Statement {

	StmtBlock block;
	 /** Create a new StmtBlock with the specified ordered list of
     * statements. */
    public StmtReorderBlock(FEContext context, List stmts)
    {
    	super(context);
    	block = new StmtBlock(context, stmts);
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
    	String result = "anyorder{\n";
    	Iterator it = block.getStmts().iterator();
    	while(it.hasNext()){
    		result += it.next().toString() + "\n";
    	}
    	return result + "\n}";
    }
}