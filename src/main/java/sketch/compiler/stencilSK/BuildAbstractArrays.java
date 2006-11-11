package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtVarDecl;


public class BuildAbstractArrays extends FEReplacer {
	Map<String, AssignStruct> assignMap;
	
	Map<String, AbstractArray> inVars;
    
    
    class setPredecessors extends FEReplacer{
    	AssignStruct as;
    	List<AssignStruct> asList=null;
    	setPredecessors(AssignStruct as){
    		this.as = as;
    		as.rhs.accept(this);
    	}
    	
    	public Object visitExprArrayRange(ExprArrayRange exp) {
    		List<Expression> indices = exp.getArrayIndices();
    		String name = exp.getAbsoluteBase().getName();
    		boolean isInput = false;
    		if( inVars.containsKey(name) ){
    			isInput = true;
    		}
    		if(isInput) asList = new ArrayList<AssignStruct>();
    		for(Iterator<Expression> it = indices.iterator(); it.hasNext(); ){    			
    			Expression idxExp = it.next();
    			idxExp.accept(this);    			
    		}
    		asList = null;
    		if(isInput){
    			as.addInput(inVars.get(name), indices);
    			// inVars.get(name).addAssignStruct(as, indices);
    		}
    		
    		return exp;
    	}
    	
    	public Object visitExprVar(ExprVar exp) { 
    		if( inVars.containsKey(exp.getName()) ){
    			return exp;
    		}
    		if( assignMap.containsKey(exp.getName()) ){
    			AssignStruct as2 = assignMap.get(exp.getName());
    			as.predecessors.add(as2);
    			if( asList != null) asList.add(as2);
    		}
    		return exp;
    	}
    }
    
    
    

    
    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        for (int i = 0; i < stmt.getNumVars(); i++){
            if (stmt.getInit(i) != null){
            	Expression exp = (Expression) stmt.getInit(i).accept(this);
            	AssignStruct as = new AssignStruct(new ExprVar(null, stmt.getName(i)), exp);
            	new setPredecessors(as);
            	this.assignMap.put(stmt.getName(i), as);
            }
        }
        return super.visitStmtVarDecl(stmt);
    }
    
    
    
	public BuildAbstractArrays(Map<String, AbstractArray> inVars){
		super();
		this.inVars = inVars;
		assignMap = new HashMap<String, AssignStruct>();
	}
	
	
	public void makeDefault(){
		for(Iterator<Entry<String, AbstractArray>> it = inVars.entrySet().iterator(); it.hasNext(); ){
			it.next().getValue().makeDefault(3);
		}
	}
	
	
	private void addASForPreds(AssignStruct base, AssignStruct current){
		for(int i=0; i<current.inputs.size(); ++i){
			AbstractArray inx = current.inputs.get(i);
			List<Expression> idxs = current.inputIndices.get(i);			
			inx.addAssignStruct(base, idxs);
		}
		for(int i=0; i< current.predecessors.size(); ++i){
			addASForPreds(base, current.predecessors.get(i));
		}
	}
	
    public Object visitStmtAssign(StmtAssign stmt){
    	
    	assert (stmt.getOp() == 0);
    	AssignStruct as = new AssignStruct(stmt.getLHS(), stmt.getRHS());
    	new setPredecessors(as);
    	this.assignMap.put(as.lhsName, as);
    	
    	if( stmt.getLHS() instanceof ExprArrayRange ){
    		//addASForPreds.
    		addASForPreds(as, as);
    	}
    	
    	
    	return stmt;
    	/**
    	 * if( isArrayAccess){
    	 *   if( Array is grid ){
    	 *   	if( the rhs has an input grid){
    	 *   		Extract expression, get all dependencies, feed to the AbstractArray.
    	 *      }else{
    	 *      	ignore
    	 *      }
    	 *   }else{
    	 *   	assert false.
    	 *   }
    	 * }else{
    	 *   record the assignment, move on.
    	 * }
    	 */
    	
    	
    	/*
        String op;
        //this.state.markVectorStack();	        
        stmt.getRHS().accept(this);
        valueClass vrhsVal = state.popVStack();     
        
        LHSvisitor lhsvisitor = new LHSvisitor();
        String lhs = (String)stmt.getLHS().accept( lhsvisitor );
        
        stmt.getLHS(); 
		valueClass vlhsVal = null;
		if(stmt.getOp() != 0){
			stmt.getLHS().accept(this);
			vlhsVal = state.popVStack();
		}
        
        String lhsnm = null;
        
        
        boolean isArr = false;
        int arrSize = -1;
        if(! lhsvisitor.isNDArracc()){
        	lhsnm = state.varGetLHSName(lhs);
        	arrSize = state.checkArray(lhs);
	        isArr = arrSize > 0;	
        }
                
        
        boolean hv = (vlhsVal == null || vlhsVal.hasValue()) && vrhsVal.hasValue() && !lhsvisitor.isNDArracc();
        
        switch(stmt.getOp())
        {
        case ExprBinary.BINOP_ADD: 	        	
        	op = " = " + vlhsVal + "+";
        	assert !isArr : "Operation not yet defined for arrays:" + op;
        	if(hv){
        		state.setVarValue(lhs, vlhsVal.getIntValue() + vrhsVal.getIntValue());
        	}
        break;
        case ExprBinary.BINOP_SUB: 
        	op = " = "+ vlhsVal + "-";
        	assert !isArr : "Operation not yet defined for arrays:" + op;
	        if(hv){
        		state.setVarValue(lhs, vlhsVal.getIntValue() - vrhsVal.getIntValue());
        	}
        break;
        case ExprBinary.BINOP_MUL:
        	op = " = "+ vlhsVal + "*";
        	assert !isArr : "Operation not yet defined for arrays:" + op;
	        if(hv){
        		state.setVarValue(lhs, vlhsVal.getIntValue() * vrhsVal.getIntValue());
        	}
        break;
        case ExprBinary.BINOP_DIV:
        	op = " = "+ vlhsVal + "/";
        	assert !isArr : "Operation not yet defined for arrays:" + op;
	        if(hv){
        		state.setVarValue(lhs, vlhsVal.getIntValue() / vrhsVal.getIntValue());
        	}
        break;
        default: op = " = ";
	        if( rhsLst != null ){
	        	assert isArr: "This should not happen, you are trying to assign an array to a non-array";
	    		List<valueClass>  lst= rhsLst;
	    		Iterator<valueClass>  it = lst.iterator();
	    		int idx = 0;
	    		while( it.hasNext() ){
	    			int i = it.next().getIntValue();
	    			state.setVarValue(lhs + "_idx_" + idx, i);
	    			++idx;
	    		}
	    		return "";
	    	}else if(hv){
        		state.setVarValue(lhs, vrhsVal.getIntValue());	
        		return "";
        	}
        }
        // Assume both sides are the right type.
        if(hv) 
        	return "";
        else{
        	if(lhsvisitor.isNDArracc()){
        		lhsnm = lhsvisitor.getLHSString();
        		lhsvisitor.unset();
        	}else{
        		state.unsetVarValue(lhs);
        	}
        }
        out.print(lhsnm + op + vrhsVal);
        return null;
        */
    }

	
	
}
