package streamit.frontend.tosbit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class EliminateStar extends CodePEval {
	private ValueOracle oracle;	
	private HasStars starCheck;
	
	/**
	 * 0 means to visit only things that have stars. </br>
	 * 1 means visit all functions, but only unroll their loops and inline their functions if they have stars </br>
	 * 2 visit all functions that have specs, unroll their loops </br>
	 * 3 visit all functions that have specs, unroll their loops inline their calls. </br>
	 */
	
	
	public class ChangeNames extends FEReplacer{		
		public Object visitExprVar(ExprVar exp) { return new ExprVar(exp.getContext(), state.transName(exp.getName()) ); }		
	}
	
	public static class HasStars extends FEReplacer{
		private StreamSpec ss;
		boolean hasUnknown=false;		
		private Set<Function> visitedFunctions = new HashSet<Function>();;		
		public HasStars(StreamSpec ss) {
			this.ss=ss;			
		}
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
			this.visitedFunctions.clear();
			hasUnknown = false;
			node.accept(this);
			return hasUnknown;
		}
	}

	public EliminateStar(ValueOracle oracle, int maxUnroll,
			RecursionControl rcontrol, int inlineLevel)
    {
		super(maxUnroll, rcontrol, inlineLevel);
		this.oracle = oracle;
		oracle.initCurrentVals();
		this.state = new MethodState();		
	}

	
	public EliminateStar(ValueOracle oracle, int maxUnroll, RecursionControl rcontrol)
    {
        this (oracle, maxUnroll, rcontrol, 0);
	}
		
	public boolean askIfPEval(Function node){
		switch( inlineLevel ){
		case 0: return starCheck.testNode(node);
		case 1: return true;
		case 2: 
		case 3:
			return node.getSpecification()!= null;
		default :
			return false;
		}
	}
	
	public boolean askIfPEval(ExprFunCall exp)
	{   
		
		String name = exp.getName();
    	// Local function?
		Function fun = ss.getFuncNamed(name);
		if( fun == null ) return false;
		
		switch( inlineLevel ){
		case 0:{
			if( fun.getSpecification() != null) return false;
			return askIfPEval(fun);
		}
		case 1: return false;
		case 2: return false;
		case 3:
			return true;
		default :
			return false;
		}
	}
	
	public boolean askIfPEval(FENode node){	
		switch( inlineLevel ){
		case 0: return starCheck.testNode(node);
		case 1: return false;
		case 2: return true;
		case 3: return true;			
		default :
			return false;
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
		
		if(!askIfPEval(stmt)){
			newStatements = oldNewStatements;
			return stmt.accept(new ChangeNames());
		}else{
			oldNewStatements.add(new StmtBlock(stmt.getContext(), newStatements));
			//oldNewStatements.addAll(newStatements);
			newStatements = oldNewStatements;
			return null;
		}
	}
	
	
	
	
	public void loopHelper(StmtLoop stmt, int i, Expression cond){
		List<Statement> oldStatements = newStatements;
		StmtIfThen ifStmt;			
        newStatements = new ArrayList<Statement> ();        

        /* Generate unrolling condition to go with change tracker. */
        Expression guard =
            new ExprBinary (stmt.getContext (),
                            ExprBinary.BINOP_GT,
                            cond,
                            new ExprConstInt (stmt.getContext (), MAX_UNROLL - i));
        guard.accept (this);
        valueClass vguard = state.popVStack ();

        state.pushChangeTracker(guard, vguard, false);
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
        		new ExprBinary(stmt.getContext(), ExprBinary.BINOP_GT, cond, new ExprConstInt(MAX_UNROLL - i) ), result, null);
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
			loopHelper(stmt, MAX_UNROLL, new ExprVar(stmt.getContext(), nvar) );			
		}else{			
			for(int i=0; i<vcond.getIntValue(); ++i){
				addStatement( (Statement)stmt.getBody().accept(this) );				
			}
		}
		
		if(!askIfPEval(stmt.getBody())){
			newStatements = oldNewStatements;
			if(newIter == stmt.getIter())
				return stmt.accept(new ChangeNames());
			return new StmtLoop(stmt.getContext(), newIter, (Statement)stmt.getBody().accept(new ChangeNames()));
		}else{
			oldNewStatements.addAll(newStatements);
			newStatements = oldNewStatements;
			return null;
		}
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
        	ExprBinary lres = new ExprBinary(exp.getContext(), ExprBinary.BINOP_BAND, left, oracExp, exp.getAlias());
        	ExprBinary rres = new ExprBinary(exp.getContext(), ExprBinary.BINOP_BAND, right, new ExprUnary(exp.getContext(), ExprUnary.UNOP_BNOT, oracExp), exp.getAlias());
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
		if( newFuns.containsKey(func.getName()) ){
			return newFuns.get(func.getName());
		}
        if(askIfPEval(func)){
	        if(func.getCls() != Function.FUNC_INIT && func.getCls() != Function.FUNC_WORK && func.getSpecification() != null ){
	        	doParams(func.getParams(), "");
	        	Statement body = null;
	        	this.state.pushLevel();   
	        	try{
	        		body = (Statement)func.getBody().accept(this);
	        	}finally{
	        		this.state.popLevel();
	        	}
	        	
	        	List<Parameter> newParams = new ArrayList<Parameter>(); 
	        	for (Iterator iter = func.getParams().iterator(); iter.hasNext(); )
	    	    {
	    	        Parameter param = (Parameter)iter.next();
	    	        Parameter newparam = new Parameter(param.getType(), state.transName(param.getName()), param.isParameterOutput());
	    	        newParams.add(newparam);
	    	    }
	        	
//	        	List<Statement> theList = new ArrayList<Statement>(func.getParams().size() + 1);
//	        	theList.add(body);
//	        	postDoParams(func.getParams(), theList);
//	        	body = new StmtBlock(func.getContext(), theList);
	        	func = new Function(func.getContext(), func.getCls(),
                        func.getName(), func.getReturnType(),
                        newParams, func.getSpecification(), body);
	        }
        }
        newFuns.put(func.getName(), func);
        return func;
    }

	@Override
	public Object visitStreamSpec(StreamSpec spec)
	{
		starCheck=new HasStars(spec);
		return super.visitStreamSpec(spec);
	}




	public void setInlineLevel(int inlineLevel) {
		this.inlineLevel = inlineLevel;
	}




	public int getInlineLevel() {
		return inlineLevel;
	}
	
}
