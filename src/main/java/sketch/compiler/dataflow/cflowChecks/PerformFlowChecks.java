package streamit.frontend.experimental.cflowChecks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.experimental.PartialEvaluator;
import streamit.frontend.experimental.abstractValue;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtFork;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.tosbit.recursionCtrl.BaseRControl;

public class PerformFlowChecks extends PartialEvaluator {
	Cfctype cftype;
	
	public void report(FENode n, String msg){
		System.err.println( n.getCx() + ": " + msg );
		throw new IllegalStateException("Semantic check failed");
	}
	
	public PerformFlowChecks(){		
		super(new Cfctype(), new TempVarGen() , false, 1, (new BaseRControl(10)));
		cftype = (Cfctype) this.vtype;
	}
	
	 public Object visitStmtAssign(StmtAssign stmt)
	    {	        
	        CfcValue rhs = null;	        
	        rhs = (CfcValue) stmt.getRHS().accept(this);
	        
	        if(! rhs.isallinit()){ report(stmt,  "There is a variable in the rhs of the assignment that may not have been initialized. All variables must be statically initialized."); }
	        return super.visitStmtAssign(stmt);
	    }
	 
	 
	 /**
     *
     * This method must necessarily push a parallel section.
     * This implementation is the most conservative implementation for this methods.
     * In some cases, we can be more liberal and pass a subset of the variables
     * that we want to make volatile in the fork. For things like constant propagation,
     * we only need to make volatile those variables that are modified in the fork.
     *
     * For now, I am making this very liberal. Nothing is made volatile.
     *
     */
    protected void startFork(StmtFork loop){
    	state.pushParallelSection(Collections.EMPTY_SET);
    }
	 
	 
	 @Override
	 public Object visitStmtReturn(StmtReturn stmt)
	    {	        
	        CfcValue rhs = null;	        
	        rhs = (CfcValue) stmt.getValue().accept(this);
	        if(! rhs.isallinit()){ report(stmt,  "There is a variable in the return expression that may not have been initialized. All variables must be statically initialized."); }
	        return super.visitStmtReturn(stmt);
	    }
	        	
	        

	    public Object visitStmtVarDecl(StmtVarDecl stmt)
	    {
	    	List<Type> types = isReplacer? new ArrayList<Type>() : null;
	    	List<String> names = isReplacer? new ArrayList<String>() : null;
	    	List<Expression> inits = isReplacer? new ArrayList<Expression>() : null;
	        for (int i = 0; i < stmt.getNumVars(); i++)
	        {
	            String nm = stmt.getName(i);
	            Type vt = (Type)stmt.getType(i).accept(this);
	            state.varDeclare(nm, vt);
	            Expression ninit = null;
	            if( stmt.getInit(i) != null ){
	            	CfcValue init = (CfcValue) stmt.getInit(i).accept(this);
	            	ninit = exprRV;
	    	        if(! init.isallinit()){ report(stmt,  "There is a variable in the initializer that may not have been itself initialized. All variables must be statically initialized."); }
	            	state.setVarValue(nm, init);
	            }
	            /* else{
	            	state.setVarValue(nm, this.vtype.BOTTOM("UNINITIALIZED"));
	            } */
	            if( isReplacer ){
	            	types.add(vt);
	            	names.add(transName(nm));
	            	inits.add(ninit);
	            }
	        }
	        return isReplacer? new StmtVarDecl(stmt, types, names, inits) : stmt;
	    }
	 
	 
	 
    @Override
    public Object visitParameter(Parameter param){
    	state.varDeclare(param.getName() , param.getType());
    	if(param.isParameterInput()){
    		state.setVarValue(param.getName(), Cfctype.allinit);
    	}
    	if(isReplacer){
    		Type ntype = (Type)param.getType().accept(this);
    		 return new Parameter(ntype, transName(param.getName()), param.getPtype());
    	}else{
    		return param;
    	}
    }
	
    
    

    public Object visitFunction(Function func)
    {
    	Object o = super.visitFunction(func);

       
            List<Parameter> params = func.getParams();
        	for(Iterator<Parameter> it = params.iterator(); it.hasNext(); ){
        		Parameter param = it.next();
        		if(!param.isParameterInput()){
        			CfcValue v=  (CfcValue )state.varValue(param.getName());
        	        if(! v.isallinit()){ report(param,  "There are some paths under which the return value will not be set."); }
        		}
        	}
    	return o;        
    }
    
    
    
}
