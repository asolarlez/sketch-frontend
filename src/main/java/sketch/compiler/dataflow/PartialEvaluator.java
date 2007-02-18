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
	
	
	public PartialEvaluator(abstractValueType vtype, TempVarGen varGen,  boolean isReplacer, int maxUnroll, RecursionControl rcontrol) {
		super();		
        this.MAX_UNROLL = maxUnroll;
        this.rcontrol = rcontrol;  
        this.varGen = varGen;
        this.vtype = vtype;
        vtype.setPeval(this);
        this.state = new MethodState(vtype);
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
        return vtype.ARR(newElements);		
	}


	public Object visitExprArrayRange(ExprArrayRange exp) {
		assert exp.getMembers().size() == 1 && exp.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
		RangeLen rl = (RangeLen)exp.getMembers().get(0);
		abstractValue newStart = (abstractValue) rl.start().accept(this);		
		abstractValue newBase = (abstractValue) exp.getBase().accept(this);
		return vtype.arracc(newBase, newStart, vtype.CONST( rl.len() ) );
	}

	public Object visitExprComplex(ExprComplex exp) {
	    // This should cause an assertion failure, actually.
		assert false : "NYI"; return null;
	}

	
	public Object visitExprConstBoolean(ExprConstBoolean exp) {
		return vtype.CONST(  boolToInt(exp.getVal()) );	
	}

	public Object visitExprConstFloat(ExprConstFloat exp) {
		report(false, "NYS");
	    return exp;
	}

	public Object visitExprConstInt(ExprConstInt exp) {
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
		abstractValue vtrue = (abstractValue) exp.getB().accept(this);
		abstractValue vfalse = (abstractValue) exp.getC().accept(this);
	    switch (exp.getOp())
	    {
	    case ExprTernary.TEROP_COND:	        	
			return vtype.condjoin(cond, vtrue, vfalse);
	    }
		assert false;
	    return null;
	}

	public Object visitExprTypeCast(ExprTypeCast exp) {
		abstractValue childExp = (abstractValue) exp.getExpr().accept(this);		
	    return vtype.cast(childExp, exp.getType());
	}
	
	public Object visitExprVar(ExprVar exp) {		
		String vname =  exp.getName();
		return state.varValue(vname);		
	}
	
	public Object visitExprConstChar(ExprConstChar exp)
    {
    	report(false, "NYS");
        return "'" + exp.getVal() + "'";
    }

	public Object visitExprUnary(ExprUnary exp) {
		
		abstractValue childExp = (abstractValue) exp.getExpr().accept(this);
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
        	       
        
        abstractValue right = (abstractValue) exp.getRight().accept(this);
        
        switch (exp.getOp())
        {
        	case ExprBinary.BINOP_ADD: return vtype.plus(left, right);
        	case ExprBinary.BINOP_SUB: return vtype.minus(left, right);
        	case ExprBinary.BINOP_MUL: return vtype.times(left, right);
        	case ExprBinary.BINOP_DIV: return vtype.over(left, right);
        	case ExprBinary.BINOP_MOD: return vtype.mod(left, right);
        	case ExprBinary.BINOP_AND: return vtype.and(left, right);
        	case ExprBinary.BINOP_OR:  return vtype.or(left, right);
        	case ExprBinary.BINOP_EQ:  return vtype.eq(left, right);
        	case ExprBinary.BINOP_NEQ: return vtype.not(vtype.eq(left, right));
        	case ExprBinary.BINOP_LT: return vtype.lt(left, right);
        	case ExprBinary.BINOP_LE: return vtype.le(left, right);
        	case ExprBinary.BINOP_GT: return vtype.gt(left, right);
        	case ExprBinary.BINOP_GE: return vtype.ge(left, right);
        	case ExprBinary.BINOP_BAND: return vtype.and(left, right);
        	case ExprBinary.BINOP_BOR: return vtype.or(left, right);
        	case ExprBinary.BINOP_BXOR: return vtype.xor(left, right);
        	case ExprBinary.BINOP_LSHIFT: 
        	case ExprBinary.BINOP_RSHIFT: 
        	case ExprBinary.BINOP_SELECT: 
        		assert false : "NYI";        	
        }
        
                
        assert false : "????"; 
        
        return null;
        
    }
    public Object visitExprStar(ExprStar star) {		
		return vtype.STAR(star);
	}
    
    public Object visitExprFunCall(ExprFunCall exp)
    {	    	
    	
    	
    	String name = exp.getName();
    	
    	Iterator actualParams = exp.getParams().iterator();
    	List<abstractValue> avlist = new ArrayList<abstractValue>(exp.getParams().size());
    	List<String> outlist = new ArrayList<String>(exp.getParams().size());
    	Function fun = ss.getFuncNamed(name);
    	Iterator formalParams = fun.getParams().iterator();
    	while(actualParams.hasNext()){
    		Expression actual = (Expression) actualParams.next();
    		Parameter param = (Parameter) formalParams.next();    	
    		if( param.isParameterOutput()){
    			assert actual instanceof ExprVar;
    			outlist.add(((ExprVar)actual).getName());
    		}else{
    			abstractValue av = (abstractValue)actual.accept(this);
    			avlist.add(av);
    		}
    	}
    	
    	return vtype.funcall(name, avlist, outlist);
    }


    public Object visitStmtAssign(StmtAssign stmt)
    {
    	
        String op;
        	        
        abstractValue rhs = (abstractValue) stmt.getRHS().accept(this);
         
        Expression lhs = stmt.getLHS();
        
        
        String lhsName = null;
        abstractValue lhsIdx = null;
        
        if( lhs instanceof ExprVar){
        	lhsName = ((ExprVar)lhs).getName();        	
        }
        
        if( lhs instanceof ExprArrayRange){
        	ExprArrayRange ear = ((ExprArrayRange)lhs);
        	Expression base = ear.getBase();
        	assert base instanceof ExprVar;        	
        	lhsName = ((ExprVar)base).getName();
        	assert ear.getMembers().size() == 1 && ear.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
    		RangeLen rl = (RangeLen)ear.getMembers().get(0);
    		lhsIdx = (abstractValue)rl.start().accept(this);
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
        return stmt;
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
    	
    	for(Iterator<Parameter> it = params.iterator(); it.hasNext(); ){
    		Parameter param = it.next();
    		state.varDeclare(param.getName() , param.getType());
    	}
    	
    	
    	state.beginFunction(func.getName());
    	
    	Statement newBody = (Statement)func.getBody().accept(this);
    	
    	state.endFunction();
    	
    	if (newBody == func.getBody()) return func;            
        return new Function(func.getContext(), func.getCls(),
                            func.getName(), func.getReturnType(),
                            func.getParams(), func.getSpecification(), newBody);
    	
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
	    return stmt;
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
    	return stmt;
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        // must have an if part...
    		        
        Expression cond = stmt.getCond();
        abstractValue vcond = (abstractValue)cond.accept(this);        
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
        	return null;   	
        }

        /* Attach conditional to change tracker. */
        state.pushChangeTracker (vcond, false);
        
        if( rcontrol.testBlock(stmt.getCons()) ){
	        try{
	        	stmt.getCons().accept(this);
	        }catch(RuntimeException e){
	        	state.popChangeTracker();
	        	throw e;
	        }
	        rcontrol.doneWithBlock(stmt.getCons());
        }else{
			( new StmtAssert(stmt.getContext(), new ExprConstInt(0)) ).accept(this);
		}	        
        ChangeTracker ipms = state.popChangeTracker();
        
        ChangeTracker epms = null;	        
        if (stmt.getAlt() != null){
            /* Attach inverse conditional to change tracker. */
            state.pushChangeTracker (vcond, true);
            if( rcontrol.testBlock(stmt.getAlt()) ){
	        	try{
	        		stmt.getAlt().accept(this);
	        	}catch(RuntimeException e){
		        	state.popChangeTracker();
		        	throw e;
		        }
	        	rcontrol.doneWithBlock(stmt.getAlt());
            }else{
				( new StmtAssert(stmt.getContext(), new ExprConstInt(0)) ).accept(this);
			}
            epms = state.popChangeTracker();
        }
        if(epms != null){
        	state.procChangeTrackers(ipms, epms);
        }else{        	
        	state.procChangeTrackers(ipms);
        }
        return null;
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
        state.Assert(vcond);
        return stmt;
    }    
    
    public Object visitStmtLoop(StmtLoop stmt)
    {
        /* Generate a new variable, initialized with loop expression. */
        FEContext nvarContext = stmt.getContext ();
        String nvar = varGen.nextVar ();
        StmtVarDecl nvarDecl =
            new StmtVarDecl (nvarContext,
                             new TypePrimitive (TypePrimitive.TYPE_INT),
                             nvar,
                             stmt.getIter ());
        nvarDecl.accept (this);

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
            nvarAssert.accept (this);

    		int iters;    		
    		for (iters=0; iters < MAX_UNROLL; ++iters) {
                /* Generate context condition to go with change tracker. */
                Expression guard =
                    new ExprBinary (nvarContext,
                                    ExprBinary.BINOP_GT,
                                    new ExprVar (nvarContext, nvar),
                                    new ExprConstInt (nvarContext, iters));
                abstractValue vguard = (abstractValue) guard.accept (this);                
                assert (vguard.isBottom());
		        state.pushChangeTracker (vguard, false);

		        ByteArrayOutputStream baos = new ByteArrayOutputStream();
		        try{
		        	stmt.getBody().accept(this);
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
	                nvarAssert2.accept (this);
		        	break;
	    		}
    		}
    		
    		for(int i=iters-1; i>=0; --i){        		    			
    			ChangeTracker ipms = state.popChangeTracker();
    			state.procChangeTrackers(ipms);
    		}
	        return null;
    	}else{
    		for(int i=0; i<vcond.getIntVal(); ++i){
    			stmt.getBody().accept(this);
    		}
    	}
    	return null;
    }

    public Object visitStmtReturn(StmtReturn stmt)
    {
    	assert false :"This opperation should not appear here!!";
        if (stmt.getValue() == null) return "return";
        return "return " + (String)stmt.getValue().accept(this);
    }



    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        	        
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            String nm = stmt.getName(i);
            Type vt = stmt.getType(i);
            state.varDeclare(nm, vt);
            
            if( stmt.getInit(i) != null){
            	abstractValue init = (abstractValue) stmt.getInit(i).accept(this);
            	state.setVarValue(nm, init);
            }
            
        }
        return null;
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
    	assert false : "NYI";
        return "while (" + (String)stmt.getCond().accept(this) +
            ") " + (String)stmt.getBody().accept(this);
    }

    
    
    public Object visitFieldDecl(FieldDecl field)
    {
        for (int i = 0; i < field.getNumFields(); ++i)
        {
            String lhs = field.getName(i);
            state.varDeclare(lhs, field.getType(i));
            if (field.getInit(i) != null){            	
				(new StmtAssign(field.getContext(),
						new ExprVar(field.getContext(), lhs),
						field.getInit(i))).accept(this);   
            }else{	            	
            	report(false, "Vars should be initialized" + field);
            }
        }
        return field;	        
    }
    
    
    

    public Object visitStreamSpec(StreamSpec spec)
    {    	
                        
        state.pushLevel();
        
        // At this point we get to ignore wholesale the stream type, except
        // that we want to save it.
        
        StreamSpec oldSS = ss;
        ss = spec;
        // Output field definitions:
        
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            FieldDecl varDecl = (FieldDecl)iter.next();
            varDecl.accept(this);
        }
         		
        
		SelectFunctionsToAnalyze funSelector = new SelectFunctionsToAnalyze();
	    List<Function> funcs = funSelector.selectFunctions(spec);
		
	    Function f = null;
        for (Iterator<Function> iter = funcs.iterator(); iter.hasNext(); ){
        	f = iter.next();
        	if( ! f.getName().equals("init") ){
        		f.accept(this);
        	}
        }
        
        ss = oldSS;
                
        state.popLevel();
                
    	
        //assert preFil.size() == 0 : "This should never happen";        
    	
        return null;
    }
}
