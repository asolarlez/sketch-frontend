package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.List;

import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.tosbit.ChangeStack;
import streamit.frontend.tosbit.CodePEval;
import streamit.frontend.tosbit.valueClass;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class StencilPreprocessor extends CodePEval{

	public StencilPreprocessor(int maxUnroll, RecursionControl maxInline){
		super(maxUnroll, maxInline);
		this.isComplete = false;
		this.inlineLevel = 3;
	}
	
	 public Object visitStmtFor(StmtFor stmt)
	    {
		 	List<Statement> tmpslist = newStatements;
		 	newStatements = new ArrayList<Statement>(); 		 	
	        Statement newInit = (Statement)stmt.getInit().accept(this);
	        if( newInit == null){
	        	assert newStatements.size() == 1;
	        	newInit = newStatements.get(0);
	        }else{
	        	assert newStatements.size() == 0;
	        }
	        newStatements = tmpslist;       	        
	        Statement newBody = null;
	        Statement newIncr = null;
	        Expression newCond = null;
	        int i=0;
	        do{
	        state.pushChangeTracker(null, null, false);	 
	        newCond = doExpression(stmt.getCond());
	        valueClass vc = state.popVStack();
	        if( vc.hasValue() && vc.getIntValue() == 0 ){
	        	ChangeStack ipms = state.popChangeTracker();
		        state.procChangeTrackers(ipms, "  ");
	        	break;
	        }
	        newBody = (Statement)stmt.getBody().accept(this);
	        newIncr = (Statement)stmt.getIncr().accept(this);
	        ChangeStack ipms = state.popChangeTracker();
	        state.procChangeTrackers(ipms, "  ");
	        ++i;
	        //TODO, need a real convergence test.
	        }while(i<3);
	        
	        return new StmtFor(stmt.getCx(), newInit, newCond, newIncr,
	                           newBody);
	    }
	
	
}
