package streamit.frontend.experimental;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.experimental.MethodState.ChangeTracker;
import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprComplex;
import streamit.frontend.nodes.ExprConstBoolean;
import streamit.frontend.nodes.ExprConstChar;
import streamit.frontend.nodes.ExprConstFloat;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprConstStr;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprTypeCast;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.FuncWork;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtBreak;
import streamit.frontend.nodes.StmtContinue;
import streamit.frontend.nodes.StmtDoWhile;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtLoop;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StmtWhile;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
import streamit.frontend.tosbit.SelectFunctionsToAnalyze;
import streamit.frontend.tosbit.valueClass;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class PartialEvaluator extends FEReplacer {
	protected StreamSpec ss;
	protected MethodState state;
	protected RecursionControl rcontrol;
    /* Bounds for loop unrolling and function inlining (initialized arbitrarily). */
    protected int MAX_UNROLL = 0;
    private TempVarGen varGen;
    protected abstractValueType vtype;
	protected Expression exprRV=null;
    protected boolean isReplacer;
    
    
    
    
    public String transName(String name){
    	return name;
    }
    
    
	
	public PartialEvaluator(abstractValueType vtype, TempVarGen varGen,  boolean isReplacer, int maxUnroll, RecursionControl rcontrol) {
		super();		
        this.MAX_UNROLL = maxUnroll;
        this.rcontrol = rcontrol;  
        this.varGen = varGen;
        this.vtype = vtype;
        vtype.setPeval(this);
        this.state = new MethodState(vtype);
        this.isReplacer =  isReplacer;
	}

	protected boolean intToBool(int v) {
		if(v>0)
			return true;
		else
			return false;
	}

	protected int boolToInt(boolean b) {	    	
		if(b)
			return 1;
		else 
			return 0;
	}
	
	protected void report(boolean t, String s) {
		if(!t){
			System.err.println(s);
			System.err.println( ss.getContext() );
			throw new RuntimeException(s);
		}
	}



	public Object visitExprArrayInit(ExprArrayInit exp) {
		
		List elems = exp.getElements();		
		List<abstractValue> newElements = new ArrayList<abstractValue>(elems.size());; 
		
		for (int i=0; i<elems.size(); i++) {			
			Expression element = ((Expression)elems.get(i));
			abstractValue newElement = (abstractValue) element.accept(this);
			newElements.add(newElement);						
		}
		
		exprRV = exp;
        return vtype.ARR(newElements);		
	}


	public Object visitExprArrayRange(ExprArrayRange exp) {
		assert exp.getMembers().size() == 1 && exp.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
		RangeLen rl = (RangeLen)exp.getMembers().get(0);
		abstractValue newStart = (abstractValue) rl.start().accept(this);
		Expression nstart = exprRV;
		
		abstractValue newBase = (abstractValue) exp.getBase().accept(this);
		Expression nbase = exprRV;
		
		if(isReplacer ) exprRV = new ExprArrayRange(exp.getContext(), nbase, nstart);
		return vtype.arracc(newBase, newStart, vtype.CONST( rl.len() ) );
	}

	public Object visitExprComplex(ExprComplex exp) {
	    // This should cause an assertion failure, actually.
		assert false : "NYI"; return null;
	}

	
	public Object visitExprConstBoolean(ExprConstBoolean exp) {
		exprRV = exp;
		return vtype.CONST(  boolToInt(exp.getVal()) );	
	}

	public Object visitExprConstFloat(ExprConstFloat exp) {
		report(false, "NYS");
	    return exp;
	}

	public Object visitExprConstInt(ExprConstInt exp) {
		exprRV = exp;
		return vtype.CONST(  exp.getVal() );			
	}

	public Object visitExprConstStr(ExprConstStr exp) {
		report(false, "NYS");
	    return exp;
	}

	public Object visitExprField(ExprField exp) {
		report(false, "NYS");	    
	    return exp;
	}


	public Object visitExprTernary(ExprTernary exp) {
		
		abstractValue cond = (abstractValue) exp.getA().accept(this);
		Expression ncond = exprRV;
		abstractValue vtrue = (abstractValue) exp.getB().accept(this);
		Expression nvtrue = exprRV;
		abstractValue vfalse = (abstractValue) exp.getC().accept(this);
		Expression nvfalse = exprRV;
	    switch (exp.getOp())
	    {
	    case ExprTernary.TEROP_COND:	
	    	if(isReplacer) exprRV = new ExprTernary(exp.getContext(), exp.getOp(), ncond, nvtrue, nvfalse);
			return vtype.condjoin(cond, vtrue, vfalse);
	    }
		assert false;
	    return null;
	}

	public Object visitExprTypeCast(ExprTypeCast exp) {
		abstractValue childExp = (abstractValue) exp.getExpr().accept(this);	
		Expression narg = exprRV;
		if(isReplacer) exprRV = new ExprTypeCast(exp.getContext(), exp.getType(), exprRV );
	    return vtype.cast(childExp, exp.getType());
	}
	
	public Object visitExprVar(ExprVar exp) {		
		String vname =  exp.getName();
		abstractValue val = state.varValue(vname);
		if(isReplacer)if( val.hasIntVal() ){
			exprRV = new ExprConstInt(val.getIntVal());
		}else{
			exprRV = new ExprVar(exp.getCx(), transName(exp.getName()));	
		}
		return 	val;
	}
	
	public Object visitExprConstChar(ExprConstChar exp)
    {
    	report(false, "NYS");
        return "'" + exp.getVal() + "'";
    }

	public Object visitExprUnary(ExprUnary exp) {
		
		abstractValue childExp = (abstractValue) exp.getExpr().accept(this);
		Expression nexp =   exprRV;
		if(isReplacer) exprRV = new ExprUnary(exp.getContext(), exp.getOp(), nexp);
		switch(exp.getOp())
	    {
	    	case ExprUnary.UNOP_NOT:	    		
	    		return  vtype.not( childExp );		
	    	case ExprUnary.UNOP_BNOT:
	    		return  vtype.not( childExp );
		    case ExprUnary.UNOP_NEG:
		    	return  vtype.neg( childExp );
		    case ExprUnary.UNOP_PREINC:  
		    {
		    	assert exp.getExpr() instanceof ExprVar : "Pre and post inc only on variables.";
		    	String name = ((ExprVar)exp.getExpr()).getName();	
		    	childExp =  vtype.plus(childExp, vtype.CONST(1));
		    	state.setVarValue(name, childExp);		    	
		    	return childExp;
		    	
		    }
		    case ExprUnary.UNOP_POSTINC:
		    {
		    	assert exp.getExpr() instanceof ExprVar : "Pre and post inc only on variables.";
		    	String name = ((ExprVar)exp.getExpr()).getName();			    	
		    	state.setVarValue(name, vtype.plus(childExp, vtype.CONST(1)));
		    	return childExp;
		    	
		    }		    
		    case ExprUnary.UNOP_PREDEC:  
		    {
		    	assert exp.getExpr() instanceof ExprVar : "Pre and post inc only on variables.";
		    	String name = ((ExprVar)exp.getExpr()).getName();	
		    	childExp =  vtype.plus(childExp, vtype.CONST(-1));
		    	state.setVarValue(name, childExp);
		    	return childExp;
		    	
		    }
		    case ExprUnary.UNOP_POSTDEC: 
		    {
		    	assert exp.getExpr() instanceof ExprVar : "Pre and post inc only on variables.";
		    	String name = ((ExprVar)exp.getExpr()).getName();			    	
		    	state.setVarValue(name, vtype.plus(childExp, vtype.CONST(-1)));
		    	return childExp;
		    	
		    }
	    }		
		assert false;
		return null;
	}
	
		
    public Object visitExprBinary(ExprBinary exp)
    {
       
    	abstractValue left = (abstractValue) exp.getLeft().accept(this); 	        
    	Expression nleft =   exprRV;
        
        abstractValue right = (abstractValue) exp.getRight().accept(this);
        Expression nright =   exprRV;
                
        abstractValue rv = null; 
        
        
        
        switch (exp.getOp())
        {
        	case ExprBinary.BINOP_ADD: rv = vtype.plus(left, right); break;
        	case ExprBinary.BINOP_SUB: rv = vtype.minus(left, right); break;
        	case ExprBinary.BINOP_MUL: rv = vtype.times(left, right); break;
        	case ExprBinary.BINOP_DIV: rv = vtype.over(left, right); break;
        	case ExprBinary.BINOP_MOD: rv = vtype.mod(left, right); break;
        	case ExprBinary.BINOP_AND: rv = vtype.and(left, right); break;
        	case ExprBinary.BINOP_OR:  rv = vtype.or(left, right); break;
        	case ExprBinary.BINOP_EQ:  rv = vtype.eq(left, right); break;
        	case ExprBinary.BINOP_NEQ: rv = vtype.not(vtype.eq(left, right)); break;
        	case ExprBinary.BINOP_LT: rv = vtype.lt(left, right); break;
        	case ExprBinary.BINOP_LE: rv = vtype.le(left, right); break;
        	case ExprBinary.BINOP_GT: rv = vtype.gt(left, right); break;
        	case ExprBinary.BINOP_GE: rv = vtype.ge(left, right); break;
        	case ExprBinary.BINOP_BAND: rv = vtype.and(left, right); break;
        	case ExprBinary.BINOP_BOR: rv = vtype.or(left, right); break;
        	case ExprBinary.BINOP_BXOR: rv = vtype.xor(left, right); break;
        	case ExprBinary.BINOP_LSHIFT: 
        	case ExprBinary.BINOP_RSHIFT: 
        	case ExprBinary.BINOP_SELECT: 
        		assert false : "NYI";        	
        }
        
        
        if(isReplacer){
        	if(rv.hasIntVal() ){
        		exprRV = new ExprConstInt(rv.getIntVal());
        	}else{
        		exprRV = new ExprBinary(exp.getContext(), exp.getOp(), nleft, nright);
        	}
        }
        
        
        
        return rv;
        
    }
    public Object visitExprStar(ExprStar star) {
    	exprRV = star;
		return vtype.STAR(star);
	}
    
    public Object visitExprFunCall(ExprFunCall exp)
    {	    	
    	
    	
    	String name = exp.getName();
    	
    	Iterator actualParams = exp.getParams().iterator();
    	List<abstractValue> avlist = new ArrayList<abstractValue>(exp.getParams().size());
    	List<String> outNmList = new ArrayList<String>(exp.getParams().size());
    	Function fun = ss.getFuncNamed(name);
    	Iterator<Parameter> formalParams = fun.getParams().iterator();
    	while(actualParams.hasNext()){
    		Expression actual = (Expression) actualParams.next();
    		Parameter param = (Parameter) formalParams.next();    	
    		if( param.isParameterOutput()){
    			assert actual instanceof ExprVar;
    			outNmList.add(((ExprVar)actual).getName());
    		}else{
    			abstractValue av = (abstractValue)actual.accept(this);
    			avlist.add(av);
    		}
    	}
    	List<abstractValue> outSlist = new ArrayList<abstractValue>();    	
    	vtype.funcall(fun, avlist, outSlist);
    	Iterator<String> nmIt = outNmList.iterator();
    	for( Iterator<abstractValue> it = outSlist.iterator(); it.hasNext();   ){
    		state.setVarValue(nmIt.next(), it.next());	
    	}
    	assert !isReplacer : "A replacer should really do something different with function calls.";
    	exprRV = exp;
    	return null;
    }


    public Object visitStmtAssign(StmtAssign stmt)
    {
    	
        String op;
        	        
        abstractValue rhs = (abstractValue) stmt.getRHS().accept(this);
        Expression nrhs = exprRV; 
                
        Expression lhs = stmt.getLHS();
        
        String lhsName = null;
        abstractValue lhsIdx = null;
        Expression nlhs = null;
        
        if( lhs instanceof ExprVar){
        	lhsName = ((ExprVar)lhs).getName();
        	if(isReplacer) nlhs = new ExprVar(stmt.getCx(), transName(lhsName));
        }
        
        if( lhs instanceof ExprArrayRange){
        	ExprArrayRange ear = ((ExprArrayRange)lhs);
        	Expression base = ear.getBase();
        	assert base instanceof ExprVar;        	
        	lhsName = ((ExprVar)base).getName();
        	if(isReplacer) nlhs = new ExprVar(stmt.getCx(), transName(lhsName));
        	
        	assert ear.getMembers().size() == 1 && ear.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
    		RangeLen rl = (RangeLen)ear.getMembers().get(0);
    		lhsIdx = (abstractValue)rl.start().accept(this);
    		if(isReplacer) nlhs = new ExprArrayRange(stmt.getCx(), nlhs, exprRV);
    		assert rl.len() == 1 ;
        }
        
        
        
        switch(stmt.getOp())
        {
        case ExprBinary.BINOP_ADD: 	        	
        	state.setVarValue(lhsName, lhsIdx, vtype.plus((abstractValue) lhs.accept(this), rhs));        	
        	break;
        case ExprBinary.BINOP_SUB: 
        	state.setVarValue(lhsName, lhsIdx, vtype.minus((abstractValue) lhs.accept(this), rhs));        	
        	break;        
        case ExprBinary.BINOP_MUL:
        	state.setVarValue(lhsName, lhsIdx, vtype.times((abstractValue) lhs.accept(this), rhs));        	
        	break;
        case ExprBinary.BINOP_DIV:
        	state.setVarValue(lhsName, lhsIdx, vtype.over((abstractValue) lhs.accept(this), rhs));        	
        	break;       
        default:
        	state.setVarValue(lhsName, lhsIdx, rhs); 	
    		break;
        }
        return isReplacer?  new StmtAssign(stmt.getCx(), nlhs, nrhs, stmt.getOp())  : stmt;
    }

    public Object visitStmtBlock(StmtBlock stmt)
    {
        // Put context label at the start of the block, too.
    	Statement s = null;
    	state.pushLevel();	    	
    	try{
    		s = (Statement)super.visitStmtBlock(stmt);
    	}finally{
    		if( s == null){
    			s = stmt;
    		}
    		state.popLevel();
    	}
        return s;
    }
    
    
    public Object visitFunction(Function func)
    {
        List<Parameter> params = func.getParams();
        List<Parameter> nparams = isReplacer ? new ArrayList<Parameter>() : null;
    	for(Iterator<Parameter> it = params.iterator(); it.hasNext(); ){
    		Parameter param = it.next();
    		state.varDeclare(param.getName() , param.getType());
    		if( isReplacer){
    			nparams.add( new Parameter(param.getType(), transName(param.getName()), param.isParameterOutput()));
    		}
    	}
    	
    	
    	state.beginFunction(func.getName());
    	
    	Statement newBody = (Statement)func.getBody().accept(this);
    	
    	state.endFunction();

        return isReplacer? new Function(func.getContext(), func.getCls(),
                            func.getName(), func.getReturnType(),
                            nparams, func.getSpecification(), newBody) : null;
    	
    	//state.pushVStack(new valueClass((String)null) );
    }
    
    
    public Object visitStmtBreak(StmtBreak stmt)
    {
    	assert false :"NYI";
        return "break";
    }
    
    public Object visitStmtContinue(StmtContinue stmt)
    {
    	assert false :"NYI";
        return "continue";
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
    	report(false, "NYS");
        String result = "do ";
        result += (String)stmt.getBody().accept(this);
        result += "while (" + (String)stmt.getCond().accept(this) + ")";
        return result;
    }
 
    
    public Object visitStmtExpr(StmtExpr stmt)
    {
    	Expression exp = stmt.getExpression();    	
    	exp.accept(this);
    	Expression nexp = exprRV;
	    return isReplacer? ( nexp == null ? null : new StmtExpr(stmt.getCx(), nexp) )  :stmt;
    }

    public Object visitStmtFor(StmtFor stmt)
    {
    	state.pushLevel();	    	
    	try{
	        if (stmt.getInit() != null)
	            stmt.getInit().accept(this);
	        report( stmt.getCond() != null , "For now, the condition in your for loop can't be null");
	        abstractValue vcond = (abstractValue) stmt.getCond().accept(this);
	        int iters = 0;		       
	        while(!vcond.isBottom() && vcond.getIntVal() > 0){
	        	++iters;
	        	stmt.getBody().accept(this);
	        	if (stmt.getIncr() != null){
		        	stmt.getIncr().accept(this);
	        	}
	        	vcond = (abstractValue) stmt.getCond().accept(this);
		        report(iters <= (1<<13), "This is probably a bug, why would it go around so many times? " + stmt.getContext());
	        }
    	}finally{
    		state.popLevel();	        	
    	}
    	assert !isReplacer : "No replacement policy for this yet.";
    	return stmt;
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        // must have an if part...
    		        
        Expression cond = stmt.getCond();
        abstractValue vcond = (abstractValue)cond.accept(this);  
        Expression ncond  = exprRV;        
        if(vcond.hasIntVal()){
        	if(vcond.getIntVal() != 0){
        		Statement rv ;
        		if( rcontrol.testBlock(stmt.getCons()) ){
        			rv =(Statement) stmt.getCons().accept(this);	
        			rcontrol.doneWithBlock(stmt.getCons());
        		}else{
					rv = (Statement)( new StmtAssert(stmt.getContext(), new ExprConstInt(0)) ).accept(this);
				}
        		return rv;
        	}else{
        		if (stmt.getAlt() != null){
        			Statement rv ;
        			if( rcontrol.testBlock(stmt.getAlt()) ){
        				rv =(Statement)stmt.getAlt().accept(this);
        				rcontrol.doneWithBlock(stmt.getAlt());
        			}else{
        				rv =(Statement)( new StmtAssert(stmt.getContext(), new ExprConstInt(0)) ).accept(this);
					}
        			return rv;
        		}
        	}
        	assert false: "Control flow should never get here";
        	return null;   	
        }

        /* Attach conditional to change tracker. */
        state.pushChangeTracker (vcond, false);
        Statement nvtrue = null;
        Statement nvfalse = null;
        if( rcontrol.testBlock(stmt.getCons()) ){
	        try{
	        	nvtrue  = (Statement) stmt.getCons().accept(this);
	        }catch(RuntimeException e){
	        	state.popChangeTracker();
	        	throw e;
	        }
	        rcontrol.doneWithBlock(stmt.getCons());
        }else{
			nvtrue = (Statement)( new StmtAssert(stmt.getContext(), new ExprConstInt(0)) ).accept(this);
		}	        
        ChangeTracker ipms = state.popChangeTracker();
        
        ChangeTracker epms = null;	        
        if (stmt.getAlt() != null){
            /* Attach inverse conditional to change tracker. */
            state.pushChangeTracker (vcond, true);
            if( rcontrol.testBlock(stmt.getAlt()) ){
	        	try{
	        		nvfalse = (Statement) stmt.getAlt().accept(this);
	        	}catch(RuntimeException e){
		        	state.popChangeTracker();
		        	throw e;
		        }
	        	rcontrol.doneWithBlock(stmt.getAlt());
            }else{
            	nvfalse = (Statement)( new StmtAssert(stmt.getContext(), new ExprConstInt(0)) ).accept(this);
			}
            epms = state.popChangeTracker();
        }
        if(epms != null){
        	state.procChangeTrackers(ipms, epms);
        }else{        	
        	state.procChangeTrackers(ipms);
        }
        return isReplacer?  new StmtIfThen(stmt.getCx(),ncond, nvtrue, nvfalse ) : stmt;
    }

    /**
     * Assert statement visitor. Generates a complex assertion expression which
     * takes into consideration the chain of nesting conditional expressions, and
     * composes them as premises to the given expression.
     *
     * @author Gilad Arnold
     */
    public Object visitStmtAssert (StmtAssert stmt) {
        /* Evaluate given assertion expression. */
        Expression assertCond = stmt.getCond();        
        abstractValue vcond  = (abstractValue) assertCond.accept (this);
        Expression ncond = exprRV;
        state.Assert(vcond);
        return isReplacer ?  new StmtAssert(stmt.getContext(), ncond)  : stmt;
    }    
    
    public Object visitStmtLoop(StmtLoop stmt)
    {
        /* Generate a new variable, initialized with loop expression. */
    	
    	List<Statement> slist = isReplacer? new ArrayList<Statement>() : null;
    	
        FEContext nvarContext = stmt.getContext ();
        String nvar = varGen.nextVar ();
        StmtVarDecl nvarDecl =
            new StmtVarDecl (nvarContext,
                             new TypePrimitive (TypePrimitive.TYPE_INT),
                             nvar,
                             stmt.getIter ());
        Statement tmpstmt = (Statement) nvarDecl.accept (this);
        if(isReplacer) slist.add(tmpstmt);
        
        /* Generate and visit an expression consisting of the new variable. */
        ExprVar nvarExp = 
            new ExprVar (nvarContext,
                         nvar);
        abstractValue vcond  = (abstractValue)nvarExp.accept (this);

        /* If no known value, perform conditional unrolling of the loop. */
    	if (vcond.isBottom()) {
            /* Assert loop expression does not exceed max unrolling constant. */
            StmtAssert nvarAssert =
                new StmtAssert (nvarContext,
                                new ExprBinary (
                                    nvarContext,
                                    ExprBinary.BINOP_LE,
                                    new ExprVar (nvarContext, nvar),
                                    new ExprConstInt (nvarContext, MAX_UNROLL)));
            tmpstmt = (Statement)nvarAssert.accept (this);
            if(isReplacer) slist.add(tmpstmt);
            List<Expression> condlist = isReplacer ? new ArrayList<Expression>() : null;
            List<Statement> bodlist = isReplacer ? new ArrayList<Statement>() : null;
    		int iters;    		
    		for (iters=0; iters < MAX_UNROLL; ++iters) {
                /* Generate context condition to go with change tracker. */
                Expression guard =
                    new ExprBinary (nvarContext,
                                    ExprBinary.BINOP_GT,
                                    new ExprVar (nvarContext, nvar),
                                    new ExprConstInt (nvarContext, iters));
                abstractValue vguard = (abstractValue) guard.accept (this);   
                Expression nguard = exprRV;
                
                assert (vguard.isBottom());
		        state.pushChangeTracker (vguard, false);
		        Statement nbody = null;
		        ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        try{
		        	nbody = (Statement) stmt.getBody().accept(this);
		        }catch(ArrayIndexOutOfBoundsException er){
		        	//If this happens, it means that we statically determined that unrolling by (iters+1) leads to an out
		        	//of bounds error. Thus, we will put a new assertion on the loop condition. 
		        	state.popChangeTracker();			        	
		        	StmtAssert nvarAssert2 =
	                    new StmtAssert (nvarContext,
	                                    new ExprBinary (
	                                        nvarContext,
	                                        ExprBinary.BINOP_LE,
	                                        new ExprVar (nvarContext, nvar),
	                                        new ExprConstInt (nvarContext, iters)));
		        	nbody = (Statement) nvarAssert2.accept (this);
		        	
	                if(isReplacer){
			        	condlist.add(nguard);
			        	bodlist.add(nbody);
			        }
		        	break;
	    		}
		        if(isReplacer){
		        	condlist.add(nguard);
		        	bodlist.add(nbody);
		        }
		        
    		}
    		
    		assert (isReplacer? iters == condlist.size() || iters+1 == condlist.size() : true) : "This is wierd";
    		
    		StmtIfThen ifthen = null;
    		
    		if( iters+1 == condlist.size() ){
    			int i=iters;
    			ifthen = new StmtIfThen(stmt.getCx(), condlist.get(i), bodlist.get(i), null);
    		}
    		
    		
    		for(int i=iters-1; i>=0; --i){        		    			
    			ChangeTracker ipms = state.popChangeTracker();
    			state.procChangeTrackers(ipms);
    			
    			if(isReplacer){
    				FEContext cx = stmt.getCx();
    				if(ifthen == null){
    					ifthen = new StmtIfThen(cx, condlist.get(i), bodlist.get(i), null);
    				}else{
    					List<Statement> nlist = new ArrayList<Statement>(2);
    					nlist.add(bodlist.get(i));
    					nlist.add(ifthen);
    					ifthen = new StmtIfThen(cx, condlist.get(i), new StmtBlock(cx, nlist), null);
    				}
    			}
    		}
    		if(isReplacer){
    			slist.add(ifthen);
    		}
	        return isReplacer? new StmtBlock(stmt.getCx(), slist) : stmt;
    	}else{
    		List<Statement> tlist = isReplacer? new ArrayList<Statement>( vcond.getIntVal() ) : null;
    		for(int i=0; i<vcond.getIntVal(); ++i){    			
    			Statement itstmt = (Statement) stmt.getBody().accept(this);
    			if(isReplacer) tlist.add( itstmt );
    		}
    		return isReplacer? new StmtBlock(stmt.getCx(), tlist) : stmt;
    	}    	
    }

    public Object visitStmtReturn(StmtReturn stmt)
    {
    	assert false :"This opperation should not appear here!!";
    	return null;
    }



    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
    	List<Type> types = isReplacer? new ArrayList<Type>() : null;
    	List<String> names = isReplacer? new ArrayList<String>() : null;
    	List<Expression> inits = isReplacer? new ArrayList<Expression>() : null;
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            String nm = stmt.getName(i);
            Type vt = stmt.getType(i);
            state.varDeclare(nm, vt);
            Expression ninit = null;
            if( stmt.getInit(i) != null ){
            	abstractValue init = (abstractValue) stmt.getInit(i).accept(this);
            	ninit = exprRV;
            	state.setVarValue(nm, init);
            }
            if( isReplacer ){
            	types.add(vt);
            	names.add(transName(nm));
            	inits.add(ninit);
            }
        }
        return isReplacer? new StmtVarDecl(stmt.getCx(), types, names, inits) : stmt;
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
    	assert false : "NYI";
        return "while (" + (String)stmt.getCond().accept(this) +
            ") " + (String)stmt.getBody().accept(this);
    }

    
    
    public Object visitFieldDecl(FieldDecl field)
    {
    	List<Type> types = isReplacer? new ArrayList<Type>() : null;
    	List<String> names = isReplacer? new ArrayList<String>() : null;
    	List<Expression> inits = isReplacer? new ArrayList<Expression>() : null;
        for (int i = 0; i < field.getNumFields(); ++i)
        {
            String lhs = field.getName(i);
            state.varDeclare(lhs, field.getType(i));
            Expression nexpr = null;
            if (field.getInit(i) != null){            	
				(new StmtAssign(field.getContext(),
						new ExprVar(field.getContext(), lhs),
						field.getInit(i))).accept(this);   
				nexpr = exprRV; //This may be a bit risky, but will work for now.
            }else{	            	
            	report(false, "Vars should be initialized" + field);
            }
            if(isReplacer){
            	types.add(field.getType(i));
            	names.add(transName(lhs));
            	inits.add( nexpr );
            }
        }
        return isReplacer? new FieldDecl(field.getCx(), types, names, inits) :field;	        
    }
    
    
    

    public Object visitStreamSpec(StreamSpec spec)
    {    	
                        
        state.pushLevel();
        
        // At this point we get to ignore wholesale the stream type, except
        // that we want to save it.
        StreamType newST = null;
        StreamSpec oldSS = ss;
        ss = spec;
        // Output field definitions:
        
        List<FieldDecl> newVars = isReplacer ? new ArrayList<FieldDecl>() : null;
        List<Function> newFuncs = isReplacer ? new ArrayList<Function>() : null;
        
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            FieldDecl varDecl = (FieldDecl)iter.next();
            FieldDecl nstmt = (FieldDecl)varDecl.accept(this);
            if( isReplacer ){ newVars.add(nstmt); }
        }
        	
        
		SelectFunctionsToAnalyze funSelector = new SelectFunctionsToAnalyze();
	    List<Function> funcs = funSelector.selectFunctions(spec);
		
	    Function f = null;
        for (Iterator<Function> iter = funcs.iterator(); iter.hasNext(); ){
        	f = iter.next();
        	if( ! f.getName().equals("init") ){
        		Function nstmt =  (Function)f.accept(this);
        		if( isReplacer ){ newFuncs.add(nstmt); }	
        	}
        }
        
        ss = oldSS;
                
        state.popLevel();
                
    	
        //assert preFil.size() == 0 : "This should never happen";        
    	
        return isReplacer? new StreamSpec(spec.getContext(), spec.getType(),
                newST, spec.getName(), spec.getParams(),
                newVars, newFuncs) : spec;
    }
}
