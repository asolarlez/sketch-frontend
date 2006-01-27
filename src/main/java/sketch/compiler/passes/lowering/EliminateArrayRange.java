package streamit.frontend.passes;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.SymbolTable;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;

public class EliminateArrayRange extends SymbolTableVisitor {

	public EliminateArrayRange() {
		super(null);	
	}	
	
	public Object visitExprArrayRange(ExprArrayRange exp){
    	assert exp.getMembers().size() == 1 && exp.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";    	    	
		Expression newBase=doExpression(exp.getBase());
		RangeLen rl = (RangeLen)exp.getMembers().get(0);
		assert rl.len == 1 : "Complex indexing not yet implemented.";
		Expression newIndex = doExpression(rl.start);
		return new ExprArray(exp.getContext(), newBase, newIndex);
    }
}
