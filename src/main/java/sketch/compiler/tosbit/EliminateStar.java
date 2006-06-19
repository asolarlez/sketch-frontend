package streamit.frontend.tosbit;

import java.util.*;

import streamit.frontend.nodes.*;

public class EliminateStar extends PartialEvaluator {
	private ValueOracle oracle;	
	private HasStars starCheck;
	private int LUNROLL;
	private Integer currentSize = null;	
	private boolean checkForStars = true;
	
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

	public EliminateStar(ValueOracle oracle, int LUNROLL, boolean checkForStars){
		super(true);
		this.oracle = oracle;
		this.LUNROLL = LUNROLL;
		oracle.initCurrentVals();
		this.state = new MethodState();
		this.checkForStars = checkForStars;
	}

	
	public EliminateStar(ValueOracle oracle, int LUNROLL){
		super(true);
		this.oracle = oracle;
		this.LUNROLL = LUNROLL;
		oracle.initCurrentVals();
		this.state = new MethodState();
	}
		
	public boolean askIfPEval(Function node){
		if( !checkForStars) return true;
		return starCheck.testNode(node);
	}
	
	public boolean askIfPEval(ExprFunCall exp)
	{    	
		if( !checkForStars ) return false;
    	String name = exp.getName();
    	// Local function?
		Function fun = ss.getFuncNamed(name);
		if( fun == null ) return false;
		return askIfPEval(fun);
	}
	
	public boolean askIfPEval(FENode node){	
		if( !checkForStars ) return false;
		return starCheck.testNode(node);
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
				Assert(arrSizeVal.hasValue(), "The array size must be a compile time constant !! \n" + stmt.getContext());
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
					addStatement( new StmtVarDecl(stmt.getContext(), new TypeArray(at.getBase(), arLen),
							state.transName(nm), init) );
				}else{
					for(int tt=0; tt<arrSizeVal.getIntValue(); ++tt){
						String nnm = nm + "_idx_" + tt;
						state.varDeclare(nnm);
						state.varGetLHSName(nnm);		            		
					}
					addStatement( new StmtVarDecl(stmt.getContext(), new TypeArray(at.getBase(), arLen),
							state.transName(nm), null) );
				}
			}else{				
				if (stmt.getInit(i) != null){     
					Expression init = (Expression) stmt.getInit(i).accept(this);
					valueClass tmp = state.popVStack();
					String asgn = lhsn + " = " + tmp + "; \n";		                
					if(tmp.hasValue()){
						state.setVarValue(nm, tmp.getIntValue());
						addStatement( new StmtVarDecl(stmt.getContext(), vt, state.transName(nm), new ExprConstInt(tmp.getIntValue())) );
					}else{//Because the variable is new, we don't have to unset it if it is null. It must already be unset.
						result += asgn;
						addStatement( new StmtVarDecl(stmt.getContext(), vt, state.transName(nm), init) );
					} 	                
				}else{
					addStatement( new StmtVarDecl(stmt.getContext(), vt, state.transName(nm), null) );
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
		
	
	
	public Object visitExprFunCall(ExprFunCall exp)
	{    	
    	String name = exp.getName();
    	// Local function?
		Function fun = ss.getFuncNamed(name);
    	if (fun != null) {    		
    		if(!askIfPEval(exp)){
    			//if the called function contains no stars, keep the call but run the partial evaluator
    			List<Statement>  oldNewStatements = newStatements;
        		newStatements = new ArrayList<Statement> ();
        		super.visitExprFunCall(exp);
        		newStatements = oldNewStatements;
        		//return exp;
        		boolean hasChanged = false;
                List<Expression> newParams = new ArrayList<Expression>();
                for (Iterator iter = exp.getParams().iterator(); iter.hasNext(); )
                {
                    Expression param = (Expression)iter.next();
                    Expression newParam = doExpression(param);
                    state.popVStack();
                    if( param instanceof ExprVar && newParam instanceof ExprArrayInit){
                    	Expression renamedParam = new ExprVar(exp.getContext(), state.transName(  ((ExprVar)param).getName()  ));
                    	newParams.add(renamedParam);
                    }else{
                    	newParams.add(newParam);
                    }
                    if (param != newParam) hasChanged = true;
                }
                if (!hasChanged) return exp;
                return new ExprFunCall(exp.getContext(), exp.getName(), newParams);
    		}
			//....else inline the called function
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
        return func;
    }

	@Override
	public Object visitStreamSpec(StreamSpec spec)
	{
		starCheck=new HasStars(spec);
		return super.visitStreamSpec(spec);
	}




	public void setCheckForStars(boolean checkForStars) {
		this.checkForStars = checkForStars;
	}




	public boolean getCheckForStars() {
		return checkForStars;
	}
	
}
