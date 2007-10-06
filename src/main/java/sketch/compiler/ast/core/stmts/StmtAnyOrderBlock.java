package streamit.frontend.nodes;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class StmtAnyOrderBlock extends StmtBlock {
	 /** Create a new StmtBlock with the specified ordered list of
     * statements. */
    public StmtAnyOrderBlock(FEContext context, List stmts)
    {
        super(context, stmts);
    }
    
    /** Accepts a front-end visitor. */
    public Object accept(FEVisitor v)
    {
        return v.visitStmtAnyOrderBlock(this);
    }
    
    public String toString(){
    	String result = "anyorder{\n";
    	Iterator it = stmts.iterator();
    	while(it.hasNext()){
    		result += it.next().toString() + "\n";
    	}
    	return result + "\n}";
    }
}