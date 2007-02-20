package streamit.frontend.experimental.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import streamit.frontend.experimental.PartialEvaluator;
import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.MethodState.ChangeTracker;
import streamit.frontend.experimental.nodesToSB.IntVtype;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class PreprocessSketch extends PartialEvaluator {

	public Map<String, Function> newFuns;
	
	
	@Override
	public String transName(String name){
		return state.transName(name);
	}
	
	public PreprocessSketch(TempVarGen vargen, int maxUnroll, RecursionControl rcontrol){
		super(new IntVtype(), vargen,true, maxUnroll, rcontrol );
		newFuns = new HashMap<String, Function>();
	}
	

	
    void inParameterSetter(Iterator<Parameter> formalParamIterator, Iterator<Expression> actualParamIterator, boolean checkError){    	
    	List<Expression> actualsList = new ArrayList<Expression>();
    	List<abstractValue> actualsValList = new ArrayList<abstractValue>();
    	while(actualParamIterator.hasNext()){
    		Expression actualParam = actualParamIterator.next();
    		abstractValue actualParamValue = (abstractValue) actualParam.accept(this);
    		actualParam = exprRV;
        	actualsList.add(actualParam);
        	actualsValList.add(actualParamValue);
    	}
    	
    	state.pushLevel();
    	
    	Iterator<Expression> actualIterator = actualsList.iterator();
    	Iterator<abstractValue> actualValIterator = actualsValList.iterator();
    	
        while(actualIterator.hasNext()){	        	
        	Expression actualParam = actualIterator.next();			        	
        	Parameter formalParam = (Parameter) formalParamIterator.next();
        	        	
        	//abstractValue actualParamValue = actualValIterator.next();
    		
        	String formalParamName = formalParam.getName();
        	state.varDeclare(formalParamName, formalParam.getType());
    		if( !formalParam.isParameterOutput() ){
    			
    	    	Statement varDecl=new StmtVarDecl(null,formalParam.getType(),state.transName(formalParam.getName()),actualParam);
    	    	addStatement((Statement)varDecl);
    		}else{
    			Statement varDecl=new StmtVarDecl(null,formalParam.getType(),state.transName(formalParam.getName()),null);
    	    	addStatement((Statement)varDecl);
    		}
        }
    }
    
    
    String outParameterSetter(Iterator formalParamIterator, Iterator actualParamIterator, boolean checkError){
    	String result = "";
    	FEContext context = null;
    	List<abstractValue> formalList = new ArrayList<abstractValue>();
    	List<String> formalTransNames = new ArrayList<String>();
    	while(formalParamIterator.hasNext()){
    		Parameter formalParam = (Parameter) formalParamIterator.next();
    		if( formalParam.isParameterOutput() ){
    			String formalParamName = formalParam.getName();    			    			
    			formalTransNames.add(transName(formalParamName));
    			abstractValue av = state.varValue(formalParamName);
    			formalList.add(av);
    		}else{
    			formalList.add(null);
    			formalTransNames.add(null);
    		}
    	}
    	
    	state.popLevel();
    	
    	Iterator<abstractValue> vcIt = formalList.iterator();
    	Iterator<String> fTransNamesIt = formalTransNames.iterator(); 
    	
        while(actualParamIterator.hasNext()){	        	
        	Expression actualParam = (Expression)actualParamIterator.next();			        	        	
        	abstractValue formal = vcIt.next();
        	String fTransName = fTransNamesIt.next();
        	if( formal != null ){
        		
        		
        		String lhsName = null;
                abstractValue lhsIdx = null;
                Expression nlhs = null;
                if( actualParam instanceof ExprVar){
                	lhsName = ((ExprVar)actualParam).getName();  
                	nlhs = new ExprVar(actualParam.getCx(), transName(lhsName));
                }
                
                if( actualParam instanceof ExprArrayRange){
                	ExprArrayRange ear = ((ExprArrayRange)actualParam);
                	Expression base = ear.getBase();
                	assert base instanceof ExprVar;        	
                	lhsName = ((ExprVar)base).getName();
                	nlhs = new ExprVar(actualParam.getCx(), transName(lhsName));
                	assert ear.getMembers().size() == 1 && ear.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
            		RangeLen rl = (RangeLen)ear.getMembers().get(0);
            		lhsIdx = (abstractValue)rl.start().accept(this);
            		nlhs = new ExprArrayRange(actualParam.getCx(), nlhs, exprRV);
            		assert rl.len() == 1 ;
                }
        		
        		state.setVarValue(lhsName, lhsIdx, formal);
        		addStatement(new StmtAssign(context, nlhs, new ExprVar(context, fTransName) ));
        		

        	}
        }
        return result;
    }
	
	
	
	
	
	public Object visitExprFunCall(ExprFunCall exp)
	{
    	String name = exp.getName();
    	// Local function?
		Function fun = ss.getFuncNamed(name);
		if(fun.getSpecification()!= null){
			String specName = fun.getSpecification();
			if( newFuns.containsKey(specName)){
				fun = newFuns.get(specName);
			}else{
				Function newFun = ss.getFuncNamed(specName);
				fun = (Function)this.visitFunction(newFun);
			}
		}
    	if (fun != null) {    		
    		if (rcontrol.testCall(exp)) {
                /* Increment inline counter. */
            	rcontrol.pushFunCall(exp, fun);  
    		
				List<Statement>  oldNewStatements = newStatements;
				newStatements = new ArrayList<Statement> ();
				Statement result = null;
				state.pushLevel();
				try{
		    		{
		    			Iterator actualParams = exp.getParams().iterator();	        		        	       	
		    			Iterator formalParams = fun.getParams().iterator();
		    			inParameterSetter(formalParams, actualParams, false);
		    		}
		    		Statement body = (Statement) fun.getBody().accept(this);
		    		addStatement(body);
		    		{
		    			Iterator actualParams = exp.getParams().iterator();	        		        	       	
		    			Iterator formalParams = fun.getParams().iterator();
		    			outParameterSetter(formalParams, actualParams, false);
		    		}
		    		result = new StmtBlock(exp.getContext(), newStatements);
	    		}finally{
	    			state.popLevel();
	    			newStatements = oldNewStatements;
	    		}
	            addStatement(result);
	    		
	    		rcontrol.popFunCall(exp);
    		}else{
    			StmtAssert sas = new StmtAssert(exp.getContext(), new ExprConstInt(0));
    			addStatement(sas);    			    		
    		}
    		exprRV = null;
    		return vtype.BOTTOM();
    	}
    	exprRV = null;
    	return vtype.BOTTOM();    	
    }

	
	
	public Object visitStmtFor(StmtFor stmt)
    {
    	state.pushLevel();
    	Statement ninit = null;
		Expression ncond = null;
		Statement nincr = null;
		Statement nbody = null;
    	try{    		
	        if (stmt.getInit() != null)
	            ninit = (Statement) stmt.getInit().accept(this);
	        boolean goOn = true;
	        int iters = 0;	
	        while(goOn){
	        	state.pushChangeTracker(null, false);
	        	boolean lisReplacer = isReplacer;
	        	isReplacer = false;
	        	abstractValue vcond = (abstractValue) stmt.getCond().accept(this);
	        	if(!vcond.isBottom() && vcond.getIntVal() == 0){
	        		isReplacer = lisReplacer;	        		
	        		break;
	        	}
	        	stmt.getBody().accept(this);
	        	if (stmt.getIncr() != null){
		        	stmt.getIncr().accept(this);
	        	}
	        	ChangeTracker ct = state.popChangeTracker();
	        	state.pushChangeTracker(null, false);
	        	ChangeTracker ct2 = state.popChangeTracker();
	        	
	        	goOn = !state.compareChangeTrackers(ct, ct2);
	        	
	        	state.procChangeTrackers(ct, ct2);	        	
	        	isReplacer = lisReplacer;
	        	++iters;
	        }
	        stmt.getCond().accept(this);
	        ncond = exprRV;
	        nbody = (Statement) stmt.getBody().accept(this);
	        if (stmt.getIncr() != null){
	        	nincr = (Statement) stmt.getIncr().accept(this);
        	}
    	}finally{
    		state.popLevel();	        	
    	}    	
    	if(nbody == null) return stmt;
    	return new StmtFor(stmt.getCx(), ninit, ncond, nincr, nbody);
    }
	
}
