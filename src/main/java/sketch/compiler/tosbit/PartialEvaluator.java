package streamit.frontend.tosbit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import streamit.frontend.nodes.ExprArray;
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
import streamit.frontend.nodes.ExprPeek;
import streamit.frontend.nodes.ExprPop;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprTypeCast;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENullVisitor;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.FEReplacer;

public class PartialEvaluator extends FEReplacer {
	protected StreamSpec ss;
	protected MethodState state;
	public final boolean isReplacer;

    public class LHSvisitor extends FENullVisitor{
    	public Object visitExprArray(ExprArray exp)
 	    {
 	    	Assert(exp.getBase() instanceof ExprVar, "Currently only 1 dimensional arrays are supported. \n" + exp.getContext());
 	    	ExprVar base = (ExprVar)exp.getBase();
 	    	String vname =  base.getName();
 	    	exp.getOffset().accept(PartialEvaluator.this);
 	    	valueClass ofst = state.popVStack();
 	    	if( ofst.hasValue()){
 		    	int ofstV = ofst.getIntValue();
 		    	int size = state.checkArray(vname);
 		    	if(ofstV >= size || ofstV < 0){	 		    		
 		    		Assert(false, "ARRAY OUT OF BOUNDS !(0<=" + ofstV + " < " + size);
 		    		return null;
 		    	}
 		    	vname = vname + "_idx_" + ofstV;
 		    	String rval = vname;	 		    	
 		    	return rval;
 	    	}else{
 	    		Assert( false, "Array indexing of non-deterministic value is only allowed in the RHS of an assignment; sorrry." );	 	    	
 	    	}
 	    	return null;
 	    }
	    public Object visitExprVar(ExprVar exp)
	    {		    	
	    	return exp.getName();		    	
	    }	    	
    }
	
	
	public PartialEvaluator(boolean isReplacer) {
		super();
		this.isReplacer = isReplacer;
	}

	protected boolean intToBool(int v) {
		if(v>0)
			return true;
		else
			return false;
	}

	protected void Assert(boolean t, String s) {
		if(!t){
			System.err.println(s);
			System.err.println( ss.getContext() );
			throw new RuntimeException(s);
		}
	}

	protected int boolToInt(boolean b) {	    	
		if(b)
			return 1;
		else 
			return 0;
	}

	public Object visitExprArrayInit(ExprArrayInit exp) {
		
		List elems = exp.getElements();
		
		List<valueClass> intelems = new ArrayList<valueClass>(elems.size());
		
		List<Expression> newElements = null; 
		
		if(isReplacer) newElements = new ArrayList<Expression>(elems.size());
		
		boolean hasChanged = false;
		for (int i=0; i<elems.size(); i++) {			
			Expression element = ((Expression)elems.get(i));
			Expression newElement = (Expression) element.accept(this);
			valueClass vrhs =  state.popVStack();
			
			if(isReplacer) newElements.add(newElement);			
			if(isReplacer) if (element != newElement) hasChanged = true;
            
	    	intelems.add(vrhs);
		}
		state.pushVStack(new valueClass(intelems));		
		if (!hasChanged || !isReplacer) return exp;
        return new ExprArrayInit(exp.getContext(), newElements);		
	}

	public Object visitExprArray(ExprArray exp) {
		Assert(exp.getBase() instanceof ExprVar, "Currently only 1 dimensional arrays are supported. \n" + exp.getContext());
		//ExprVar base = (ExprVar)exp.getBase();	    		    		    	
		Expression nbase = null;
        Expression offset = null;
        
		nbase = (Expression) exp.getBase().accept(this); //base.getName();
		valueClass baseVal = state.popVStack();		
		Object fu = exp.getOffset().accept(this);;
		offset = (Expression)fu;
		valueClass ofst = state.popVStack();		
		if( ofst.hasValue()){
	    	Assert(ofst != null, "The array index must be computable at compile time. \n" + exp.getContext());
	    	int ofstV = ofst.getIntValue();
	    	List<valueClass> lst = baseVal.getVectValue();	    	
	    	int size = lst.size();
	    	if(ofstV >= size || ofstV < 0){
	    		if(!exp.isUnchecked())throw new ArrayIndexOutOfBoundsException(exp.getContext() + ": ARRAY OUT OF BOUNDS !(0<=" + ofst.getIntValue() + " < " + size);
				state.pushVStack( new valueClass(0) );
				return "0";
	    	}
	    	valueClass rval = lst.get(ofstV);
	    	state.pushVStack(rval);
	    	if(isReplacer && rval.hasValue()){
	    		if(rval.isVect()){
	    			assert false : "NYI";
	    		}else{
	    			return new ExprConstInt(rval.getIntValue());	
	    		}
	    	}
		}else{
			List<valueClass> lst = baseVal.getVectValue();
			int arrSize = lst.size();
			Iterator<valueClass> iter = lst.iterator();
			String vname = "($ ";
			for(int i=0; i< arrSize; ++i ){
				if( i!= 0) vname += " ";
				vname += iter.next().toString();
			}
			vname = vname + "$" +  "[" + ofst + "])";
			state.pushVStack( new valueClass(vname));			
		}
		if ((nbase == exp.getBase() && offset == exp.getOffset() ) || !isReplacer)
            return exp;
        else
            return new ExprArray(exp.getContext(), nbase, offset, exp.isUnchecked());
	}

	public Object visitExprArrayRange(ExprArrayRange exp) {
		assert false : "At this stage, there shouldn't be any ArrayRange expressions";
		return null;
	}

	public Object visitExprComplex(ExprComplex exp) {
	    // This should cause an assertion failure, actually.
		assert false : "NYI";
		Expression real = exp.getReal();
	    if (real != null){
	    	real = (Expression)real.accept(this);
	    	state.popVStack();
	    }
	    Expression imag = exp.getImag();
	    if (imag != null){
	    	imag = (Expression) imag.accept(this);
	    	state.popVStack();
	    }
	    state.pushVStack(null);
	    if (real == exp.getReal() && imag == exp.getImag())
	        return exp;
	    else
	        return new ExprComplex(exp.getContext(), real, imag);
	}

	public Object visitExprConstBoolean(ExprConstBoolean exp) {
	    if (exp.getVal()){
	    	state.pushVStack(new valueClass(1));	        
	    }else{
	    	state.pushVStack(new valueClass(0));	        
	    }
	    return exp;
	}

	public Object visitExprConstFloat(ExprConstFloat exp) {
		Assert(false, "NYS");
	    return exp;
	}

	public Object visitExprConstInt(ExprConstInt exp) {
		state.pushVStack(new valueClass(exp.getVal()));
	    return exp;
	}

	public Object visitExprConstStr(ExprConstStr exp) {
		Assert(false, "NYS");
	    return exp;
	}

	public Object visitExprField(ExprField exp) {
		Assert(false, "NYS");	    
	    return exp;
	}

	public Object visitExprPeek(ExprPeek exp) {
		int poppos = state.varValue("POP_POS");
	    exp.getExpr().accept(this);
	    valueClass arg = state.popVStack();	        
	    Assert(arg.hasValue(), "I can not tell at compile time where you are peeking. " + arg);
	    String result = "INPUT_" + (arg.getIntValue()+poppos);
	    state.pushVStack(new valueClass(result));	    
	    if(this.isReplacer)
			return new ExprVar(exp.getContext(), result);
		else
			return exp;
	}

	public Object visitExprPop(ExprPop exp) {
		int poppos = state.varValue("POP_POS");
		String result = "INPUT_" +  poppos; 
		state.setVarValue("POP_POS", poppos+1);
		state.pushVStack(new valueClass(result));
		if(this.isReplacer)
			return new ExprVar(exp.getContext(), result);
		else
			return exp;
	}

	public Object visitExprTernary(ExprTernary exp) {
		Expression expA = (Expression) exp.getA().accept(this);
	    valueClass aval = state.popVStack();	        
	    switch (exp.getOp())
	    {
	    case ExprTernary.TEROP_COND:	        	
			if(aval.hasValue()){
				if( intToBool(aval.getIntValue()) ){
					Expression expret = (Expression) exp.getB().accept(this);
					valueClass bval = state.popVStack();        		        
					state.pushVStack(bval);
					return expret;
				}else{
					Expression expret = (Expression) exp.getC().accept(this);
					valueClass cval = state.popVStack();
					state.pushVStack( cval );
					return expret;
				}
			}else{
				Expression expB = (Expression) exp.getB().accept(this);
		        valueClass bval = state.popVStack();
		        Expression expC = (Expression)exp.getC().accept(this);
		        valueClass cval = state.popVStack();        			
				String rval = "(" + aval + " ? " + bval + " : " + cval + ")";
				state.pushVStack( new valueClass(rval) );
				if (!isReplacer ||(expA == exp.getA() && expB == exp.getB() && expC == exp.getC()))
		            return exp;
		        else
		            return new ExprTernary(exp.getContext(), exp.getOp(), expA, expB, expC);
			}
	    }
		state.pushVStack(new valueClass((String)null));
	    return exp;
	}

	public Object visitExprTypeCast(ExprTypeCast exp) {
		if(! exp.getType().equals(TypePrimitive.inttype) ){
			Assert( exp.getType() instanceof TypeArray, "WHAT ARE YOU TRYING TO DO!!!");
			return (String)exp.getExpr().accept(this);
		}
		Expression param = (Expression)exp.getExpr().accept(this);
		Expression rval = exp;
		valueClass rhsVal = state.popVStack();
		if( rhsVal.isVect() ){
			String result = "( $$";
			List<valueClass> rhsLst;
	    	rhsLst= rhsVal.getVectValue();	        	
	    	Iterator<valueClass> it = rhsLst.iterator();
	    	int i = 0;
	    	int val=0;
	    	boolean hasValue=true;
	    	while(it.hasNext()){
	    		valueClass o = it.next();
	    		if(!o.hasValue()){	        			
	    			result += " " + o;
	    			hasValue = false;
	    		}else{	        			
	    			int curv =  o.getIntValue();
	    			result += " " + o.getIntValue();
	    			Assert(curv == 1 || curv == 0, "Only boolean arrays please!!");
	    			val = val*2;
	    			val = val + curv;
	    		}
	    		++i;
	    	}
	    	result += " $$ )";
	    	if(hasValue){
	    		state.pushVStack(new valueClass(val));
	    		if(isReplacer) rval = new ExprConstInt(val);
	    		result = " " + val;
	    	}else{
	    		state.pushVStack(new valueClass(result));
	    		if(isReplacer && param != exp.getExpr())  rval = new ExprTypeCast(exp.getContext(), exp.getType(), param);
	    	}
	    	return rval;
	    }else{	        	
	    	Assert(false, "We only allow casting of array expressions");
	    }
	    return null;
	}
	public Object visitExprVar(ExprVar exp) {
		String vname =  exp.getName();
		valueClass intValue;
		if( state.varHasValue( vname ) ){
			intValue = new valueClass(state.varValue(vname)) ;	    		
		}else{
			intValue = new valueClass(state.varGetRHSName( exp.getName() ));
		}
		int sz = state.checkArray(vname);
		if( sz >= 0 ){
			List<valueClass> nlist = new LinkedList<valueClass>();
			for(int i=0; i<sz; ++i){
				String lnm = vname + "_idx_" + i;
				if( state.varHasValue( lnm) ){
					nlist.add(new valueClass( state.varValue(lnm )));
				}else{
					nlist.add(new valueClass(state.varGetRHSName(lnm)));
				}
			}
			state.pushVStack( new valueClass(nlist) );
			return exp;
		}else{
			state.pushVStack(intValue);
			if(this.isReplacer && intValue.hasValue()){
				return new ExprConstInt(intValue.getIntValue());
			}else{
				return exp;
			}
		}
	}
	
	public Object visitExprConstChar(ExprConstChar exp)
    {
    	Assert(false, "NYS");
        return "'" + exp.getVal() + "'";
    }

	public Object visitExprUnary(ExprUnary exp) {
		
		Expression childExp = (Expression) exp.getExpr().accept(this);
	    valueClass vchild = state.popVStack();
	    String child = vchild.toString();
	    Expression returnVal=exp;
	    boolean hv = vchild.hasValue(); 	        
	    switch(exp.getOp())
	    {
	    case ExprUnary.UNOP_NOT: 
	    	if( hv ){ 
	    		state.pushVStack(new valueClass(1-vchild.getIntValue()));
	    		if(this.isReplacer) returnVal = new ExprConstInt(1-vchild.getIntValue());
	    	}else{
	    		state.pushVStack( new valueClass("!" + child) );
	    		if(this.isReplacer && childExp != exp.getExpr()) returnVal = new ExprUnary(exp.getContext(), exp.getOp(), childExp );
	    	}
	    return returnVal;
	    
	    case ExprUnary.UNOP_NEG:
	    	if( hv ){
	    		state.pushVStack(new valueClass(-vchild.getIntValue()));
	    		if(this.isReplacer) returnVal = new ExprConstInt(-vchild.getIntValue());
	    	}else{
	    		state.pushVStack(new valueClass("-" + child));
	    		if(this.isReplacer && childExp != exp.getExpr()) returnVal = new ExprUnary(exp.getContext(), exp.getOp(), childExp );
	    	}
	    return returnVal;
	    case ExprUnary.UNOP_PREINC:  
	    	if( hv ){    		
	    		String childb = (String)exp.getExpr().accept( new LHSvisitor());
	    		state.pushVStack(new valueClass(vchild.getIntValue()+1));
	    		state.setVarValue(childb, vchild.getIntValue()+1 );	    		
	    		if(this.isReplacer){
	    			this.addStatement(new StmtExpr(exp.getContext(), exp));
	    			returnVal = new ExprConstInt(vchild.getIntValue()+1);
	    		}
	    		return returnVal;
	    	}else{
	    		String newName = state.varGetLHSName(child);
	    		state.pushVStack(new valueClass("( "  + newName + " = " + child + " + 1 )"));
	    		if(this.isReplacer && childExp != exp.getExpr()) returnVal = new ExprUnary(exp.getContext(), exp.getOp(), childExp );
	    	}
	    	return returnVal;
	    case ExprUnary.UNOP_POSTINC:
	    	if( hv ){ 	        		
	    		String childb = (String)exp.getExpr().accept( new LHSvisitor());
	    		exp.getExpr().accept(this);
	    		vchild = state.popVStack();
	    		state.pushVStack(new valueClass(vchild.getIntValue()));
	    		state.setVarValue(childb, vchild.getIntValue()+1 );
	    		return null; 
	    		//TODO NYI;
	    	}else{
	    		state.pushVStack(new valueClass(child + "++"));
	    	}
	    	return null;
	    case ExprUnary.UNOP_PREDEC:  
	    	if( hv ){    		
	    		String childb = (String)exp.getExpr().accept( new LHSvisitor());
	    		state.pushVStack(new valueClass(vchild.getIntValue()-1));
	    		state.setVarValue(childb, vchild.getIntValue()-1 );	    		
	    		if(this.isReplacer){
	    			this.addStatement(new StmtExpr(exp.getContext(), exp));
	    			returnVal = new ExprConstInt(vchild.getIntValue()-1);
	    		}
	    		return returnVal;
	    	}else{
	    		String newName = state.varGetLHSName(child);
	    		state.pushVStack(new valueClass("( "  + newName + " = " + child + " - 1 )"));
	    		if(this.isReplacer && childExp != exp.getExpr()) returnVal = new ExprUnary(exp.getContext(), exp.getOp(), childExp );
	    	}
	    	return returnVal;
	    case ExprUnary.UNOP_POSTDEC: 
	    	if( hv ){ 
	    		String childb = (String)exp.getExpr().accept( new LHSvisitor() );
	    		exp.getExpr().accept(this);
	    		vchild = state.popVStack();
	    		state.pushVStack(new valueClass(vchild.getIntValue()));
	    		state.setVarValue(childb, vchild.getIntValue()-1 );
	    		return null; 
	    		//TODO NYI;
	    	}else{
	    		state.pushVStack(new valueClass(child + "--"));
	    	}
	    	return null; 
    		//TODO NYI;
	    }
	    return null;
	}
	
    public Object visitExprBinary(ExprBinary exp)
    {
        String result;
        Expression rval = exp;
        String op = null;
        result = "(";
        Expression left = (Expression) exp.getLeft().accept(this); 	        
        valueClass lhs = state.popVStack();	        
        Expression right = (Expression) exp.getRight().accept(this);
        valueClass rhs = state.popVStack();
        boolean hasv = lhs.hasValue() && rhs.hasValue();	        
        
        int newv=0;
        
        switch (exp.getOp())
        {
        case ExprBinary.BINOP_ADD: op = "+"; if(hasv) newv = lhs.getIntValue() + rhs.getIntValue(); break;
        case ExprBinary.BINOP_SUB: op = "-"; if(hasv) newv = lhs.getIntValue() - rhs.getIntValue(); break;
        case ExprBinary.BINOP_MUL: op = "*"; if(hasv) newv = lhs.getIntValue() * rhs.getIntValue(); break;
        case ExprBinary.BINOP_DIV: op = "/"; if(hasv) newv = lhs.getIntValue() / rhs.getIntValue(); break;
        case ExprBinary.BINOP_MOD: op = "%"; if(hasv) newv = lhs.getIntValue() % rhs.getIntValue(); break;
        case ExprBinary.BINOP_AND: op = "&&"; if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) && intToBool(rhs.getIntValue())); break;
        case ExprBinary.BINOP_OR:  op = "||"; if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) || intToBool(rhs.getIntValue())); break;
        case ExprBinary.BINOP_EQ:  op = "=="; if(hasv) newv = boolToInt(lhs.getIntValue() == rhs.getIntValue()); break;
        case ExprBinary.BINOP_NEQ: op = "!="; if(hasv) newv = boolToInt(lhs.getIntValue() != rhs.getIntValue()); break;
        case ExprBinary.BINOP_LT:  op = "<"; if(hasv) newv = boolToInt(lhs.getIntValue() < rhs.getIntValue()); break;
        case ExprBinary.BINOP_LE:  op = "<="; if(hasv) newv = boolToInt(lhs.getIntValue() <= rhs.getIntValue()); break;
        case ExprBinary.BINOP_GT:  op = ">"; if(hasv) newv = boolToInt(lhs.getIntValue() > rhs.getIntValue()); break;
        case ExprBinary.BINOP_GE:  op = ">="; if(hasv) newv = boolToInt(lhs.getIntValue() >= rhs.getIntValue()); break;
        case ExprBinary.BINOP_BAND:op = "&"; if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) && intToBool(rhs.getIntValue())); break;
        case ExprBinary.BINOP_BOR: op = "|"; if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) || intToBool(rhs.getIntValue()));; break;
        case ExprBinary.BINOP_BXOR:op = "^"; if(hasv) newv = boolToInt(lhs.getIntValue() != rhs.getIntValue()); break;
        case ExprBinary.BINOP_SELECT:{
        	op = "{|}";
        	if(hasv && lhs.getIntValue() == rhs.getIntValue()){
        		newv = lhs.getIntValue();
        	}else{
        		hasv = false;
        	}
        }
        }	                
        if(hasv){
        	state.pushVStack(new valueClass(newv));
        	if( this.isReplacer ){
        		rval = new ExprConstInt(newv);
        	}
        }else{
        	result += lhs + " " + op + " ";
            result += rhs + ")";
        	state.pushVStack(new valueClass(result));
        	if( this.isReplacer ){
        		if(left != exp.getLeft() || right != exp.getRight()){
        			rval = new ExprBinary(exp.getContext(), exp.getOp(), left, right);
        		}        		
        	}
        }
        return rval;
    }
    public Object visitExprStar(ExprStar star) {    	
		state.pushVStack(new valueClass());
		return star;
	}
}
