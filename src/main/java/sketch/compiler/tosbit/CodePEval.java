package streamit.frontend.tosbit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.tosbit.PartialEvaluator.CheckSize;
import streamit.frontend.tosbit.PartialEvaluator.LHSvisitor;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class CodePEval extends PartialEvaluator {
	/**
	 * 0 means to visit only things that have stars. </br>
	 * 1 means visit all functions, but only unroll their loops and inline their functions if they have stars </br>
	 * 2 visit all functions that have specs, unroll their loops </br>
	 * 3 visit all functions that have specs, unroll their loops inline their calls. </br>
	 */
	protected int inlineLevel;   /* Default value given by constructor. */
	protected Integer currentSize = null;	
	public Map<String, Function> newFuns;
	
	public boolean askIfPEval(FENode node){	
		switch( inlineLevel ){
		case 0: return false;
		case 1: return false;
		case 2: return true;
		case 3: return true;			
		default :
			return false;
		}
	}
	
	public CodePEval(int maxUnroll, RecursionControl rcontrol, int inlineLevel){
		super(true, maxUnroll, rcontrol);		
		this.state = new MethodState();
		this.newFuns = new HashMap<String, Function>();
		this.inlineLevel = inlineLevel;
	}


	public CodePEval(int maxUnroll, RecursionControl rcontrol)
	{
		this (maxUnroll, rcontrol, 0);
	}


	public Object visitStmtAssign(StmtAssign stmt)
	{
		String op;
		
		currentSize =  (new CheckSize()).checkSize(stmt.getLHS()); 
		
		Expression right = (Expression)stmt.getRHS().accept(this);
		valueClass vrhsVal = state.popVStack();
		List<valueClass> rhsLst = null;		
		if( vrhsVal.isVect() ){
			rhsLst= vrhsVal.getVectValue();
		}
		
		
		Expression left = stmt.getLHS(); 
		valueClass vlhsVal = null;
		if(stmt.getOp() != 0){
			left =(Expression)stmt.getLHS().accept(this);
			vlhsVal = state.popVStack();
		}
		
		LHSvisitor lhsvisit = new LHSvisitor();
		String lhs = (String)stmt.getLHS().accept( lhsvisit);
		Expression lvalue = lhsvisit.lhsExp;
		
		
		int arrSize = -1;
		boolean isArr = false;		
		if(! lhsvisit.isNDArracc()){
			state.varGetLHSName(lhs);
			arrSize = state.checkArray(lhs);
			isArr = arrSize > 0;
		}
		
		
		currentSize = null;
		
		
		boolean hv = (vlhsVal == null || vlhsVal.hasValue()) && vrhsVal.hasValue() && !lhsvisit.isNDArracc();
		
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
			List<valueClass> lst= rhsLst;
			Iterator<valueClass>  it = lst.iterator();
			
			
			for(int idx = 0; idx<arrSize; ++idx){
				if( it.hasNext() ){
					valueClass val = it.next(); 
					if(val.hasValue()){
						int i = val.getIntValue();
						state.setVarValue(lhs + "_idx_" + idx, i);
					}else{
						hv = false;
						state.unsetVarValue(lhs + "_idx_" + idx);
					}
				}else{
					state.setVarValue(lhs + "_idx_" + idx, 0);
				}				
			}			
		}else if(hv){
			if(isArr){
				for(int i=0; i<arrSize; ++i){
					state.setVarValue(lhs + "_idx_" + i, vrhsVal.getIntValue());
				}
			}else{
				state.setVarValue(lhs, vrhsVal.getIntValue());
			}
		}
		}
		// Assume both sides are the right type.
		if(!hv){
			if(lhsvisit.isNDArracc()){
				lhsvisit.unset();
			}else{
				state.unsetVarValue(lhs);
			}
		}
		if (right == stmt.getRHS() && left == stmt.getLHS() && lvalue == stmt.getLHS()){
            return stmt;
		}
		if(stmt.getOp() == 0){
			return new StmtAssign(stmt.getCx(), lvalue, right,
                    stmt.getOp());
		}else{
	        return new StmtAssign(stmt.getCx(), lvalue, 
	        		new ExprBinary(stmt.getCx(), stmt.getOp(), left, right));
		}
	}
	
	public Object visitStmtIfThen(StmtIfThen stmt)
	{
		// must have an if part...		
		Expression newCond = (Expression) stmt.getCond().accept(this);
		valueClass vcond = state.popVStack();
		if(vcond.hasValue()){
			
			
			if(vcond.getIntValue() != 0){
				if( rcontrol.testBlock(stmt.getCons()) ){
					Statement cons = (Statement)stmt.getCons().accept(this);
					if(cons != null)
						addStatement(cons);	 
					rcontrol.doneWithBlock(stmt.getCons());
				}else{
					addStatement( new StmtAssert(stmt.getCx(), new ExprConstInt(0)) );
				}
			}else{
				if (stmt.getAlt() != null){
					if( rcontrol.testBlock(stmt.getAlt()) ){
						Statement alt = (Statement)stmt.getAlt().accept(this);
						if(alt != null)
							addStatement(alt);
						rcontrol.doneWithBlock(stmt.getAlt());
					}else{
						addStatement( new StmtAssert(stmt.getCx(), new ExprConstInt(0)) );
					}
				}
			}
			
			
			return null;
		}
		state.pushChangeTracker(newCond, vcond, false);
		Statement newCons = null;
		
		if( rcontrol.testBlock(stmt.getCons()) ){
			try{
				newCons =  (Statement) stmt.getCons().accept(this);
			}catch(RuntimeException e){
	        	state.popChangeTracker();
	        	throw e;
	        }
			rcontrol.doneWithBlock(stmt.getCons());
		}else{
			newCons = new StmtAssert(stmt.getCx(), new ExprConstInt(0));
		}
		
		ChangeStack ipms = state.popChangeTracker();
		
		
		Statement newAlt = null;
		ChangeStack epms = null;				
		if (stmt.getAlt() != null){
			state.pushChangeTracker(newCond, vcond, true);
			
			if( rcontrol.testBlock(stmt.getAlt()) ){
				try{
					newAlt = (Statement) stmt.getAlt().accept(this);
				}catch(RuntimeException e){
		        	state.popChangeTracker();
		        	throw e;
		        }
				rcontrol.doneWithBlock(stmt.getAlt());
			}else{
				newAlt = new StmtAssert(stmt.getCx(), new ExprConstInt(0));
			}
			
			epms = state.popChangeTracker();
		}	        
		if(epms != null){		
			state.procChangeTrackers(ipms, epms, vcond.toString());
		}else{
			state.procChangeTrackers(ipms, vcond.toString());
		}
		
        if (newCond == stmt.getCond() && newCons == stmt.getCons() &&
            newAlt == stmt.getAlt())
            return stmt;
        return new StmtIfThen(stmt.getCx(), newCond, newCons, newAlt);
	}
	
	public Object visitStmtVarDecl(StmtVarDecl stmt)
	{
		String result = "";
		for (int i = 0; i < stmt.getNumVars(); i++)
		{
			String nm = stmt.getName(i);
			state.varDeclare(nm);
			String lhsn = state.varGetLHSName(nm);
			Type vt = stmt.getType(i);
			if( vt instanceof TypeArray){
				TypeArray at = (TypeArray)vt;
				Expression arLen = (Expression) at.getLength().accept(this);
				valueClass arrSizeVal = state.popVStack();
				
				if(arrSizeVal.hasValue() || isComplete ){
					Assert(arrSizeVal.hasValue(), "The array size must be a compile time constant !! \n" + stmt.getCx());
					state.makeArray(nm, arrSizeVal.getIntValue());
					//this.state.markVectorStack();
					if( stmt.getInit(i) != null){
						Expression init = (Expression)stmt.getInit(i).accept(this);
						valueClass vclass = state.popVStack();
						if( vclass.isVect() ){
							List<valueClass> lst= vclass.getVectValue();
							Iterator<valueClass> it = lst.iterator();
							int tt = 0;
							while( it.hasNext() ){
								valueClass ival =  it.next();
								String nnm = nm + "_idx_" + tt;
								state.varDeclare(nnm);
								state.varGetLHSName(nnm);
								if(ival.hasValue()){
									state.setVarValue(nnm, ival.getIntValue());
								}
								++tt;
							} 
							for(; tt<arrSizeVal.getIntValue(); ++tt){
								String nnm = nm + "_idx_" + tt;
								state.varDeclare(nnm);
								state.varGetLHSName(nnm);	
								state.setVarValue(nnm,0);						
							}
							//continue;
						}else{
							Integer val = null;
							if(vclass.hasValue())
								val = vclass.getIntValue();
							for(int tt=0; tt<arrSizeVal.getIntValue(); ++tt){
								String nnm = nm + "_idx_" + tt;
								state.varDeclare(nnm);
								state.varGetLHSName(nnm);
								if(val != null){
									state.setVarValue(nnm, val.intValue());
								}
							}	
							//if(val != null) continue;
						}
						addStatement( new StmtVarDecl(stmt.getCx(), new TypeArray(at.getBase(), arLen),
								state.transName(nm), init) );
					}else{
						for(int tt=0; tt<arrSizeVal.getIntValue(); ++tt){
							String nnm = nm + "_idx_" + tt;
							state.varDeclare(nnm);
							state.varGetLHSName(nnm);		            		
						}
						addStatement( new StmtVarDecl(stmt.getCx(), new TypeArray(at.getBase(), arLen),
								state.transName(nm), null) );
					}
				}else{
					if( stmt.getInit(i) != null){
						Expression init = (Expression)stmt.getInit(i).accept(this);
						valueClass vclass = state.popVStack();
						addStatement( new StmtVarDecl(stmt.getCx(), new TypeArray(at.getBase(), arLen),
								state.transName(nm), init) );
					}else{
						addStatement( new StmtVarDecl(stmt.getCx(), new TypeArray(at.getBase(), arLen),
								state.transName(nm), null) );
					}
				}
			}else{				
				if (stmt.getInit(i) != null){     
					Expression init = (Expression) stmt.getInit(i).accept(this);
					valueClass tmp = state.popVStack();
					String asgn = lhsn + " = " + tmp + "; \n";		                
					if(tmp.hasValue()){
						state.setVarValue(nm, tmp.getIntValue());
						return ( new StmtVarDecl(stmt.getCx(), vt, state.transName(nm), new ExprConstInt(tmp.getIntValue())) );
					}else{//Because the variable is new, we don't have to unset it if it is null. It must already be unset.
						result += asgn;
						addStatement( new StmtVarDecl(stmt.getCx(), vt, state.transName(nm), init) );
					} 	                
				}else{
					addStatement( new StmtVarDecl(stmt.getCx(), vt, state.transName(nm), null) );
				}
			}
		}
		return null;
	}
	
	public Object visitExprFunCall(ExprFunCall exp)
	{
    	String name = exp.getName();
    	// Local function?
		Function fun = ss.getFuncNamed(name);
		boolean madeSubstitution=false;
		if(fun.getSpecification()!= null){
			String specName = fun.getSpecification();
			if( newFuns.containsKey(specName)){
				fun = newFuns.get(specName);
			}else{
				Function newFun = ss.getFuncNamed(specName);
				fun = (Function)this.visitFunction(newFun);
			}
			madeSubstitution=true;
		}
    	if (fun != null) {    		
    		if(!askIfPEval(exp)){
                /* Check inline counters. */                
                if (rcontrol.testCall(exp)) {
                    /* Increment inline counter. */
                	rcontrol.pushFunCall(exp, fun);                    

                    // if the called function contains no stars, keep the call
                    // but run the partial evaluator
                    List<Statement>  oldNewStatements = newStatements;
                    newStatements = new ArrayList<Statement> ();
                    if (madeSubstitution) {
                        ExprFunCall exp2 =
                            new ExprFunCall (exp.getCx(), fun.getName(),
                                             exp.getParams());
                        super.visitExprFunCall(exp2);
                    } else {
                        super.visitExprFunCall(exp);
                    }
                    newStatements = oldNewStatements;

                    /* Decrement inline counter. */
                    rcontrol.popFunCall(exp);
                    

                    //return exp;
                    boolean hasChanged = false;
                    List<Expression> newParams = new ArrayList<Expression>();
                    for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
                    {
                        Expression param = (Expression)iter.next();
                        Expression newParam = doExpression(param);
                        state.popVStack();
                        if (param instanceof ExprVar
                            && newParam instanceof ExprArrayInit)
                        {
                            Expression renamedParam =
                                new ExprVar (exp.getCx (),
                                             state.transName (
                                                 ((ExprVar) param).getName()));
                            newParams.add (renamedParam);
                        } else {
                            newParams.add (newParam);
                        }
                        if (param != newParam)
                            hasChanged = true;
                    }
                    if (hasChanged)
                        return new ExprFunCall (exp.getCx(), exp.getName(),
                                                newParams);
                }// if (rcontrol.testCall(exp))
                // If the if rcontrol.testCall(exp) returns false, then this call better not be made ever, 
                // so it really doesn't matter what we return. 
                return exp;
    		}
			//....else inline the called function
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
		    		result = new StmtBlock(exp.getCx(), newStatements);
	    		}finally{
	    			state.popLevel();
	    			newStatements = oldNewStatements;
	    		}
	            addStatement(result);    		
	    		state.pushVStack( new valueClass(exp.toString()) );
	    		
	    		rcontrol.popFunCall(exp);
    		}else{
    			StmtAssert sas = new StmtAssert(exp.getCx(), new ExprConstInt(0));
    			addStatement(sas);    		
	    		state.pushVStack( new valueClass(sas.toString()) );
    		}
    		return null;
    	}
    	state.pushVStack( new valueClass(exp.toString()));
    	return exp;    	
    }
	
}
