package streamit.frontend.tosbit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import streamit.frontend.nodes.ExprArray;
import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;

public class EliminateStar extends PartialEvaluator {
	private ValueOracle oracle;	
	private HasStars starCheck = new HasStars();
	private int LUNROLL;
	private Integer currentSize = null;
	
	public class HasStars extends FEReplacer{
		boolean hasUnknown=false;		
		private Set<Function> visitedFunctions = new HashSet<Function>();;		
		public Object visitExprFunCall(ExprFunCall exp)
	    {	
			Function fun = ss.getFuncNamed(exp.getName());
			assert fun != null : "Calling undefined function!!";
			Object obj = super.visitExprFunCall(exp);
			if(!visitedFunctions.contains(fun)){
				visitedFunctions.add(fun);
				fun.accept(this);			
			}
			return obj;
	    }
		
		public Object visitExprBinary(ExprBinary exp)
	    {
			if(exp.getOp() == ExprBinary.BINOP_SELECT){
				hasUnknown = true;
			}
			return super.visitExprBinary(exp);	        
	    }		
		public Object visitExprStar(ExprStar star) {
			hasUnknown = true;
			return star;
		}
		public boolean testNode(FENode node){
			hasUnknown = false;
			node.accept(this);
			return hasUnknown;
		}
	}
	
	public EliminateStar(ValueOracle oracle, int LUNROLL){
		super(true);
		this.oracle = oracle;
		this.LUNROLL = LUNROLL;
		oracle.initCurrentVals();
		this.state = new MethodState();
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
			assert isArr: "This should not happen, you are trying to assign an array to a non-array";
			List<valueClass> lst= rhsLst;
			Iterator<valueClass>  it = lst.iterator();
			int idx = 0;
			while( it.hasNext() ){
				valueClass val = it.next(); 
				if(val.hasValue()){
					int i = val.getIntValue();
					state.setVarValue(lhs + "_idx_" + idx, i);
				}else{
					hv = false;
					state.unsetVarValue(lhs + "_idx_" + idx);
				}
				++idx;
			}			
		}else if(hv){			
			state.setVarValue(lhs, vrhsVal.getIntValue());
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
			return new StmtAssign(stmt.getContext(), lvalue, right,
                    stmt.getOp());
		}else{
	        return new StmtAssign(stmt.getContext(), lvalue, 
	        		new ExprBinary(stmt.getContext(), stmt.getOp(), left, right));
		}
	}
	
	public Object visitStmtBlock(StmtBlock stmt)
	{
		// Put context label at the start of the block, too.
		Object rval = null;
		state.pushLevel();
		try{
			rval = super.visitStmtBlock(stmt);
		}finally{
			state.popLevel();
		}
		return rval;
	}
	
	
	public Object visitStmtExpr(StmtExpr stmt)
	{
		Expression exp = stmt.getExpression();
		Expression newExpr = (Expression)exp.accept(this);
		valueClass vc = state.popVStack();	
		if(newExpr == null) return null;
		if(vc.hasValue()) return null;
        if (newExpr == stmt.getExpression()) return stmt;
        return new StmtExpr(stmt.getContext(), newExpr);
	}
	
	
	public Object visitStmtFor(StmtFor stmt)
	{
		
		List<Statement>  oldNewStatements = newStatements;
		newStatements = new ArrayList<Statement> ();
		
		state.pushLevel();
		try{
			if (stmt.getInit() != null)
				stmt.getInit().accept(this);		
			Assert( stmt.getCond() != null , "For now, the condition in your for loop can't be null");
			stmt.getCond().accept(this);
			valueClass vcond = state.popVStack();
			int iters = 0;
			while(vcond.hasValue() && vcond.getIntValue() > 0){
				++iters;
				Statement body = (Statement) stmt.getBody().accept(this);
				addStatement(body);			
				if (stmt.getIncr() != null)
					stmt.getIncr().accept(this);
				stmt.getCond().accept(this);
				vcond = state.popVStack();
				Assert(iters <= (1<<13), "This is probably a bug, why would it go around so many times? ");
			}
		}finally{
			state.popLevel();
		}
		if(!starCheck.testNode(stmt)){
			newStatements = oldNewStatements;
			return stmt;
		}else{
			oldNewStatements.addAll(newStatements);
			newStatements = oldNewStatements;
			return null;
		}		
	}
	
	public Object visitStmtIfThen(StmtIfThen stmt)
	{
		// must have an if part...		
		Expression newCond = (Expression) stmt.getCond().accept(this);
		valueClass vcond = state.popVStack();
		if(vcond.hasValue()){
			if(vcond.getIntValue() != 0){
				Statement cons = (Statement)stmt.getCons().accept(this);
				if(cons != null)
					addStatement(cons);	        		
			}else{
				if (stmt.getAlt() != null){
					Statement alt = (Statement)stmt.getAlt().accept(this);
					if(alt != null)
						addStatement(alt);
				}
			}
			return null;
		}
		state.pushChangeTracker();
		Statement newCons = null;
		try{
			newCons =  (Statement) stmt.getCons().accept(this);
		}catch(RuntimeException e){
        	state.popChangeTracker();
        	throw e;
        }
		ChangeStack ipms = state.popChangeTracker();
		Statement newAlt = null;
		ChangeStack epms = null;				
		if (stmt.getAlt() != null){
			state.pushChangeTracker();
			try{
				newAlt = (Statement) stmt.getAlt().accept(this);
			}catch(RuntimeException e){
	        	state.popChangeTracker();
	        	throw e;
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
        return new StmtIfThen(stmt.getContext(), newCond, newCons, newAlt);
	}
	
	
	public void loopHelper(StmtLoop stmt, int i, Expression cond){
		List<Statement> oldStatements = newStatements;
		StmtIfThen ifStmt;			
        newStatements = new ArrayList<Statement> ();        
        state.pushChangeTracker();
        try{
        	addStatement((Statement)stmt.getBody().accept(this));
		}catch(ArrayIndexOutOfBoundsException er){			        	
			state.popChangeTracker();
			newStatements = oldStatements;  
			return;
		}
        if((i-1)>0)
        	loopHelper(stmt, i-1, cond);
        ChangeStack ms1 = state.popChangeTracker();
        state.procChangeTrackers(ms1, " ");
        Statement result = new StmtBlock(stmt.getContext(), newStatements);
        ifStmt = new StmtIfThen(stmt.getContext(), 
        		new ExprBinary(stmt.getContext(), ExprBinary.BINOP_GT, cond, new ExprConstInt(LUNROLL - i) ), result, null);
        newStatements = oldStatements;    
        addStatement(ifStmt);
	}
	
	public Object visitStmtLoop(StmtLoop stmt)
	{
		List<Statement>  oldNewStatements = newStatements;
		newStatements = new ArrayList<Statement> ();
		
		Expression newIter = (Expression)stmt.getIter().accept(this);
		valueClass vcond = state.popVStack();
		
		if(!vcond.hasValue()){			
			String nvar = state.varDeclare();
			state.varGetLHSName(nvar);
	        this.addStatement( new StmtVarDecl(stmt.getContext(),TypePrimitive.inttype, nvar, newIter));	        
			loopHelper(stmt, LUNROLL, new ExprVar(stmt.getContext(), nvar) );			
		}else{			
			for(int i=0; i<vcond.getIntValue(); ++i){
				addStatement( (Statement)stmt.getBody().accept(this) );				
			}
		}
		if(!starCheck.testNode(stmt.getBody())){
			newStatements = oldNewStatements;
			if(newIter == stmt.getIter())
				return stmt;
			return new StmtLoop(stmt.getContext(), newIter, stmt.getBody());
		}else{
			oldNewStatements.addAll(newStatements);
			newStatements = oldNewStatements;
			return null;
		}
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
				valueClass tmp = state.popVStack();
				Assert(tmp.hasValue(), "The array size must be a compile time constant !! \n" + stmt.getContext());
				state.makeArray(nm, tmp.getIntValue());
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
						//continue;
					}else{
						Integer val = null;
						if(vclass.hasValue())
							val = vclass.getIntValue();
						for(int tt=0; tt<tmp.getIntValue(); ++tt){
							String nnm = nm + "_idx_" + tt;
							state.varDeclare(nnm);
							String tmplhsn = state.varGetLHSName(nnm);
							if(val != null){
								state.setVarValue(tmplhsn, val.intValue());
							}
						}	
						//if(val != null) continue;
					}
					addStatement( new StmtVarDecl(stmt.getContext(), new TypeArray(at.getBase(), arLen),
							nm, init) );
				}else{
					for(int tt=0; tt<tmp.getIntValue(); ++tt){
						String nnm = nm + "_idx_" + tt;
						state.varDeclare(nnm);
						state.varGetLHSName(nnm);		            		
					}
					addStatement( new StmtVarDecl(stmt.getContext(), new TypeArray(at.getBase(), arLen),
							nm, null) );
				}
			}else{				
				if (stmt.getInit(i) != null){     
					Expression init = (Expression) stmt.getInit(i).accept(this);
					valueClass tmp = state.popVStack();
					String asgn = lhsn + " = " + tmp + "; \n";		                
					if(tmp.hasValue()){
						state.setVarValue(nm, tmp.getIntValue());
						addStatement( new StmtVarDecl(stmt.getContext(), vt, nm, new ExprConstInt(tmp.getIntValue())) );
					}else{//Because the variable is new, we don't have to unset it if it is null. It must already be unset.
						result += asgn;
						addStatement( new StmtVarDecl(stmt.getContext(), vt, nm, init) );
					} 	                
				}else{
					addStatement( new StmtVarDecl(stmt.getContext(), vt, nm, null) );
				}
			}
		}
		return null;
	}
	
	
	protected Object handleBinarySelect(ExprBinary exp, Expression left, valueClass lhs, Expression right, valueClass rhs){
		Expression rvalE = exp;			
		boolean hasv = lhs.hasValue() && rhs.hasValue();		        
		if(hasv && lhs.getIntValue() == rhs.getIntValue()){
			int newv=0;	        		
			newv = lhs.getIntValue();
			state.pushVStack(new valueClass(newv));			        	
			if( this.isReplacer ){
				rvalE = new ExprConstInt(newv);
			}
			return rvalE;
		}else{
			hasv = false;
			ExprConstInt newExp = (ExprConstInt) oracle.popValueForNode(exp.getAlias());			
			if(newExp.getVal() == 1){
				if(lhs.hasValue()){
					rvalE = new ExprConstInt(lhs.getIntValue());
				}else{
					rvalE = left;
				}
			}else{
				if(rhs.hasValue()){
					rvalE = new ExprConstInt(rhs.getIntValue());
				}else{
					rvalE = right;
				}
			}
			state.pushVStack(new valueClass(" "));
			return rvalE;
		}
	}
	
	
	
	protected Object handleVectorBinarySelect(ExprBinary exp, Expression left, List<valueClass> lhsVect, Expression right, List<valueClass> rhsVect){
		Iterator<valueClass> lhsIt = lhsVect.iterator();
		Iterator<valueClass> rhsIt = rhsVect.iterator();
		Expression rvalE = exp;
		boolean globalHasV = true;
		List<ExprConstInt> vals =   new ArrayList<ExprConstInt>(lhsVect.size());
		List<ExprConstInt> orac =   new ArrayList<ExprConstInt>(lhsVect.size());
        for( ; lhsIt.hasNext(); ){
        	valueClass lhs = lhsIt.next();
        	valueClass rhs = rhsIt.next();
        	boolean hasv = lhs.hasValue() && rhs.hasValue();
        	if(hasv && lhs.getIntValue() == rhs.getIntValue()){
    			int newv=0;	        		
    			newv = lhs.getIntValue();    
    			vals.add(new ExprConstInt(lhs.getIntValue()));
    			orac.add(new ExprConstInt(0));
    		}else{    			
    			ExprConstInt newExp = (ExprConstInt)oracle.popValueForNode(exp.getAlias());
    			orac.add(newExp);
    			if(newExp.getVal() == 1){
    				if(lhs.hasValue()){
    					vals.add(new ExprConstInt(lhs.getIntValue()));
    				}else{
    					globalHasV = false;
    				}
    			}else{
    				if(rhs.hasValue()){
    					vals.add(new ExprConstInt(rhs.getIntValue()));
    				}else{
    					globalHasV = false;
    				}
    			}    			
    		}
        }        
        state.pushVStack(new valueClass(" "));
        if(globalHasV){
        	return new ExprArrayInit(exp.getContext(), vals);
        }else{
        	ExprArrayInit oracExp = new ExprArrayInit(exp.getContext(), orac);
        	ExprBinary lres = new ExprBinary(exp.getContext(), ExprBinary.BINOP_AND, left, oracExp, exp.getAlias());
        	ExprBinary rres = new ExprBinary(exp.getContext(), ExprBinary.BINOP_AND, right, new ExprUnary(exp.getContext(), ExprUnary.UNOP_NOT, oracExp), exp.getAlias());
        	return new ExprBinary(exp.getContext(), ExprBinary.BINOP_BOR, lres, rres, exp.getAlias());
        }		
	}
	
	public Object visitExprBinary(ExprBinary exp)
	{
		
		if( exp.getOp() == ExprBinary.BINOP_LSHIFT ||  exp.getOp() == ExprBinary.BINOP_RSHIFT){
			Expression left = (Expression) exp.getLeft().accept(this); 	        
	        valueClass lhs = state.popVStack();	       
	        Integer tmpInt = this.currentSize;
	        currentSize = null;
	        Expression right = (Expression) exp.getRight().accept(this);
	        valueClass rhs = state.popVStack();
	        currentSize = tmpInt;
	        return this.ExprBinaryHelper(exp, left, lhs, right, rhs);
		}else{
			return super.visitExprBinary(exp);
		}	
	}
		
	
	
	public Object visitExprFunCall(ExprFunCall exp)
	{    	
    	String name = exp.getName();
    	// Local function?
    	if (ss.getFuncNamed(name) != null) {     
    		Function fun = ss.getFuncNamed(name);
    		if(!this.starCheck.testNode(fun)){    			
    			return super.visitExprFunCall(exp);
    		}   
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
    		
    		state.pushVStack( new valueClass(exp.toString()) );
    		return null;
    	}
    	state.pushVStack( new valueClass(exp.toString()));
    	return exp;    	
    }
	
	public Object visitExprStar(ExprStar star) {							
		state.pushVStack(new valueClass("{*}"));
		if(currentSize == null || currentSize <=1){
			return oracle.popValueForNode(star);
		}else{
			int N = currentSize;
			List<Expression> newElements = new ArrayList<Expression>(N);
			for(int i=0; i<N; ++i){
				newElements.add(oracle.popValueForNode(star));
			}
			return new ExprArrayInit(star.getContext(), newElements);
		}
	}
	
	
	 public String postDoParams(List params, List<Statement> stmts){
	    	String result = "";	        
	        for (Iterator iter = params.iterator(); iter.hasNext(); )
	        {
	            Parameter param = (Parameter)iter.next();
	            if(param.isParameterOutput()){
	            	String lhs = param.getName();
		            if( param.getType() instanceof TypeArray ){
		            	stmts.add(new StmtAssign(null, new ExprVar(null, "_p_"+lhs), new ExprVar(null, lhs)));
		            }else{		            	
		            	if(state.varHasValue(lhs)){
		            		stmts.add(new StmtAssign(null, new ExprVar(null, "_p_"+lhs), new ExprConstInt(state.varValue(lhs))));
		            	}else{
		            		stmts.add(new StmtAssign(null, new ExprVar(null, "_p_"+lhs), new ExprVar(null, lhs)));
		            	}
		            }
	            }
	        }
	        return result;
	    }
	
	public Object visitFunction(Function func)
    {
        if(starCheck.testNode(func)){
	        if(func.getCls() != Function.FUNC_INIT && func.getCls() != Function.FUNC_WORK && func.getSpecification() != null ){
	        	doParams(func.getParams(), "");
	        	Statement body = null;
	        	this.state.pushLevel();   
	        	try{
	        		body = (Statement)func.getBody().accept(this);
	        	}finally{
	        		this.state.popLevel();
	        	}
	        	List<Statement> theList = new ArrayList<Statement>(func.getParams().size() + 1);
	        	theList.add(body);
	        	//postDoParams(func.getParams(), theList);
	        	body = new StmtBlock(func.getContext(), theList);
	        	func = new Function(func.getContext(), func.getCls(),
                        func.getName(), func.getReturnType(),
                        func.getParams(), func.getSpecification(), body);
	        }
        }
        return func;
    }
	
}
