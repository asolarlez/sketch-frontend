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
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprPeek;
import streamit.frontend.nodes.ExprPop;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprTernary;
import streamit.frontend.nodes.ExprTypeCast;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FENullVisitor;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePortal;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.TypeStruct;
import streamit.frontend.nodes.TypeStructRef;

public class PartialEvaluator extends FEReplacer {
	protected StreamSpec ss;
	protected MethodState state;
	public final boolean isReplacer;	

	public class CheckSize extends FENullVisitor{
		int size = -1;
		 public Object visitExprVar(ExprVar exp) { 
			 size = state.checkArray(exp.getName());
			 return exp.getName();
		 }
		 public Object visitExprArray(ExprArray exp)
 	    {	 	    	
 	    	String vname =  (String) exp.getBase().accept(this);	 	    	
 		    vname = vname + "_idx_" + 0;
 		    size = state.checkArray(vname);
 		    return vname;
 	    }
		public int checkSize(FENode node){
			size = -1;
			node.accept(this);
			return size;
		}
	}
	
    public class LHSvisitor extends FENullVisitor{
    	public Expression lhsExp=null;
    	private List<String> lhsVals=null;
    	private List<String> oldVals=null;
    	private List<String> names=null;
    	public String offset = null;
    	public String base = null;
    	private boolean NDArracc=false;
    	
    	public boolean isNDArracc(){
    		return NDArracc;
    	}
    	
    	public void unset(){
    		for(Iterator<String> it = names.iterator(); it.hasNext(); ){
    		    state.unsetVarValue(it.next());
    		}
    	}
    	
    	public String getLHSString(){
    		assert NDArracc;
    		String rval = "$";    		
    		for(Iterator<String> it = lhsVals.iterator(); it.hasNext(); ){
    			rval += " " + it.next();    			
    		}
    		rval += "$$";
    		for(Iterator<String> it = oldVals.iterator(); it.hasNext(); ){
    			rval += " " + it.next();    			
    		}
    		rval += "$[" + offset + "]";    		
    		return rval;    		
    	}
    	
    	public Object visitExprArray(ExprArray exp)
 	    { 	    	
 	    	String vname =  (String) exp.getBase().accept(this); 
 	    	Expression offsetE = (Expression) exp.getOffset().accept(PartialEvaluator.this);
 	    	valueClass ofst = state.popVStack();
 	    	offset = ofst.toString();
 	    	if( ofst.hasValue()){
 		    	int ofstV = ofst.getIntValue();
 		    	int size = state.checkArray(vname);
 		    	if(ofstV >= size || ofstV < 0){	 		    		
 		    		Assert(false, "ARRAY OUT OF BOUNDS !(0<=" + ofstV + " < " + size);
 		    		return null;
 		    	}
 		    	vname = vname + "_idx_" + ofstV;
 		    	String rval = vname;
 		    	if(isReplacer){
 		    		if(offsetE != exp.getOffset()){
 		    			lhsExp = new ExprArray(exp.getContext(), lhsExp, offsetE);
 		    		}else{
 		    			lhsExp = exp;
 		    		}
 		    	}else{
 		    		lhsExp = exp;
 		    	}
 		    	return rval;
 	    	}else{ 	    		
 	    		int size = state.checkArray(vname);
 	    		lhsVals = new ArrayList<String>(size);
 	    		oldVals = new ArrayList<String>(size);
 	    		names = new ArrayList<String>(size);
 	    		for(int i=0; i<size; ++i){
 	    			String nm = vname + "_idx_" + i;
 	    			oldVals.add(state.varGetRHSName(nm));
 	    			lhsVals.add(state.varGetLHSName(nm));
 	    			names.add(nm);
 	    		}
 	    		NDArracc = true;
 	    		if(isReplacer){
 		    		if(offsetE != exp.getOffset()){
 		    			lhsExp = new ExprArray(exp.getContext(), lhsExp, offsetE);
 		    		}else{
 		    			lhsExp = exp;
 		    		}
 		    	}else{
 		    		lhsExp = exp;
 		    	}
 	    	}
 	    	return null;
 	    }
	    public Object visitExprVar(ExprVar exp)
	    {		    	
	    	lhsExp = exp;
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
				return new ExprConstInt(0);
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
			List<valueClass> nlist = new ArrayList<valueClass>(sz);
			boolean isAllValues=true;
			List<ExprConstInt> olist = new ArrayList<ExprConstInt>(sz);
			for(int i=0; i<sz; ++i){
				String lnm = vname + "_idx_" + i;
				if( state.varHasValue( lnm) ){
					int v = state.varValue(lnm );
					olist.add( new ExprConstInt(v));
					nlist.add(new valueClass(v));
				}else{
					isAllValues = false;
					nlist.add(new valueClass(state.varGetRHSName(lnm)));
				}
			}
			state.pushVStack( new valueClass(nlist) );
			if(this.isReplacer && isAllValues){
				return new ExprArrayInit(exp.getContext(), olist);
			}else{
				return exp;
			}
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
	    			//this.addStatement(new StmtExpr(exp.getContext(), exp));
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
	    			//this.addStatement(new StmtExpr(exp.getContext(), exp));
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
	
	protected valueClass doOps(ExprBinary exp, valueClass lhs,valueClass rhs){
		valueClass newv=null;
		boolean hasv = lhs.hasValue() && rhs.hasValue();
		switch (exp.getOp())
        {                
        case ExprBinary.BINOP_AND:  if(hasv) newv = new valueClass(boolToInt( intToBool(lhs.getIntValue()) && intToBool(rhs.getIntValue()))); break;
        case ExprBinary.BINOP_OR:   if(hasv) newv = new valueClass(boolToInt( intToBool(lhs.getIntValue()) || intToBool(rhs.getIntValue()))); break;
        case ExprBinary.BINOP_EQ:   if(hasv) newv = new valueClass(boolToInt(lhs.getIntValue() == rhs.getIntValue())); break;
        case ExprBinary.BINOP_NEQ:  if(hasv) newv = new valueClass(boolToInt(lhs.getIntValue() != rhs.getIntValue())); break;
        case ExprBinary.BINOP_BAND: if(hasv) newv = new valueClass(boolToInt( intToBool(lhs.getIntValue()) && intToBool(rhs.getIntValue()))); break;
        case ExprBinary.BINOP_BOR:  if(hasv) newv = new valueClass(boolToInt( intToBool(lhs.getIntValue()) || intToBool(rhs.getIntValue()))); break;
        case ExprBinary.BINOP_BXOR:  if(hasv) newv = new valueClass(boolToInt(lhs.getIntValue() != rhs.getIntValue())); break;        
        default: assert false : exp; break;
        }
		if(newv == null){
			newv = new valueClass(lhs.toString() + " " + exp.getOpString() + " " + rhs.toString());
		}
		return newv;
	}
	
	
	protected Object ExprVectorBinaryHelper(ExprBinary exp, Expression left, valueClass lhs, Expression right, valueClass rhs){
		List<valueClass> lhsList=null;
		List<valueClass> rhsList=null;
		if(lhs.isVect()){
			lhsList = lhs.getVectValue();
		}else{
			int N = rhs.getVectValue().size();
			lhsList = new ArrayList<valueClass>(N);			
			for(int i=0; i<N; ++i){
				lhsList.add(lhs);
			}
		}
		if(rhs.isVect()){
			rhsList = rhs.getVectValue();	
		}else{
			int N = lhsList.size();
			rhsList = new ArrayList<valueClass>(N);			
			for(int i=0; i<N; ++i){
				rhsList.add(rhs);
			}
		}		
		
		while(rhsList.size() < lhsList.size()){
			rhsList.add(new valueClass(0));
		}
		
		while(lhsList.size() < rhsList.size()){
			lhsList.add(new valueClass(0));
		}
		
		assert rhsList.size() == lhsList.size() : "NYI : List sizes " + rhsList.size() + " and " + lhsList.size();
		
		List<valueClass> result = new ArrayList<valueClass>(lhsList.size());	;
		
		switch (exp.getOp())
        {        
        case ExprBinary.BINOP_AND: 
        case ExprBinary.BINOP_OR:          
        case ExprBinary.BINOP_BAND: 
        case ExprBinary.BINOP_BOR:  
        case ExprBinary.BINOP_BXOR:{
        	Iterator<valueClass> rhsIt = rhsList.iterator();
        	Iterator<valueClass> lhsIt = lhsList.iterator();
        	for(int i=0; rhsIt.hasNext(); ++i){
        		result.add(this.doOps(exp, lhsIt.next(), rhsIt.next()));
        	}      
        } break;
        
        case ExprBinary.BINOP_LSHIFT:{
        	Iterator<valueClass> lhsIt = lhsList.iterator();
        	int N = lhsList.size();
        	if(rhs.hasValue()){
        		int shamt = rhs.getIntValue();        		
        		int i;
        		for(i=0; i<shamt; ++i){
        			lhsIt.next();
        		}
        		for( ; lhsIt.hasNext(); ++i){
        			result.add(lhsIt.next());
        		}
        		for(; i<N; ++i){
        			result.add( new valueClass(0));
        		}
        	}else{
        		for(int i=0; i<N; ++i){
        			result.add(new valueClass());
        		}
        	}
        } break;
        
        case ExprBinary.BINOP_RSHIFT:{
        	Iterator<valueClass> lhsIt = lhsList.iterator();
        	int N = lhsList.size();
        	if(rhs.hasValue()){
        		int shamt = rhs.getIntValue();        		
        		int i;
        		for(i=0; i<shamt; ++i){
        			result.add( new valueClass(0));
        		}
        		for( ; i<N; ++i){
        			result.add(lhsIt.next());
        		}        		
        	}else{
        		for(int i=0; i<N; ++i){
        			result.add(new valueClass());
        		}
        	}
        } break;
        
        case ExprBinary.BINOP_ADD:{
        	Iterator<valueClass> rhsIt = rhsList.iterator();
        	Iterator<valueClass> lhsIt = lhsList.iterator();
        	valueClass carry = new valueClass(0);
        	for(int i=0; rhsIt.hasNext(); ++i){
        		valueClass op1 = rhsIt.next();
        		valueClass op2 = lhsIt.next();
        		if(op1.hasValue() && op2.hasValue() && carry.hasValue()){
        			int op1v = op1.getIntValue();
        			int op2v = op2.getIntValue();
        			int c = carry.getIntValue();
        			result.add(new valueClass((op1v + op2v +c ) % 2));
        			carry = new valueClass( (op1v + op2v +c ) >1 ? 1 : 0  );
        		}else{
        			carry = new valueClass();
        			result.add(carry);        			
        		}
        	}
        }break;
        case ExprBinary.BINOP_SELECT:{
        	return this.handleVectorBinarySelect(exp, left, lhsList, right, rhsList);
        }
        case ExprBinary.BINOP_EQ: 
        case ExprBinary.BINOP_NEQ:
        case ExprBinary.BINOP_LT: 
        case ExprBinary.BINOP_LE: 
        case ExprBinary.BINOP_GT: 
        case ExprBinary.BINOP_GE: 
        case ExprBinary.BINOP_SUB:
        case ExprBinary.BINOP_MUL:
        case ExprBinary.BINOP_DIV:
        case ExprBinary.BINOP_MOD:
        default: assert false : exp; break;
        }
		state.pushVStack(new valueClass(result));
		if (left == exp.getLeft() && right == exp.getRight())
            return exp;
        else
            return new ExprBinary(exp.getContext(), exp.getOp(), left, right, exp.getAlias());		
	}
	
	protected Object handleVectorBinarySelect(ExprBinary exp, Expression left, List<valueClass> lhsVect, Expression right, List<valueClass> rhsVect){
		Iterator<valueClass> lhsIt = lhsVect.iterator();
		Iterator<valueClass> rhsIt = rhsVect.iterator();
		
        Expression rval = exp;
        
        boolean globalHasV = true;
        List<valueClass> results = new ArrayList<valueClass>();
        List<ExprConstInt> vals =   new ArrayList<ExprConstInt>();
        for( ; lhsIt.hasNext(); ){
        	valueClass lhs = lhsIt.next();
        	valueClass rhs = rhsIt.next();
        	boolean hasv = lhs.hasValue() && rhs.hasValue();                    
    		if(hasv && lhs.getIntValue() == rhs.getIntValue()){
        		results.add( lhs );
        		if( this.isReplacer ){
        			vals.add(new ExprConstInt(lhs.getIntValue()));
        		}
        	}else{
        		hasv = false;
        		globalHasV = false;
        	}
        }    
		if(globalHasV){
        	state.pushVStack(new valueClass(results));
        	if( this.isReplacer ){
        		rval = new ExprArrayInit(exp.getContext(), vals);
        	}
        }else{
        	state.pushVStack(new valueClass(""));
        	assert false : "NOT IMPLEMENTED";
        }
        return rval;		
	}
	
	protected Object handleBinarySelect(ExprBinary exp, Expression left, valueClass lhs, Expression right, valueClass rhs){
		String result;
        Expression rval = exp;
        String op = exp.getOpString();
        result = "(";
        boolean hasv = lhs.hasValue() && rhs.hasValue();        
        int newv=0;
		if(hasv && lhs.getIntValue() == rhs.getIntValue()){
    		newv = lhs.getIntValue();
    	}else{
    		hasv = false;
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
        			rval = new ExprBinary(exp.getContext(), exp.getOp(), left, right, exp.getAlias());
        		}        		
        	}
        }
        return rval;
	}
	
	
	protected Object ExprBinaryHelper(ExprBinary exp, Expression left, valueClass lhs, Expression right, valueClass rhs){
		String result;
        Expression rval = exp;
        String op = exp.getOpString();
        result = "(";
	        
        if(lhs.isVect() || rhs.isVect()){
        	return ExprVectorBinaryHelper(exp, left, lhs, right, rhs);
        }
        
        boolean hasv = lhs.hasValue() && rhs.hasValue();	        
        
        int newv=0;
               
        
        switch (exp.getOp())
        {
        case ExprBinary.BINOP_ADD:  if(hasv) newv = lhs.getIntValue() + rhs.getIntValue(); break;
        case ExprBinary.BINOP_SUB:  if(hasv) newv = lhs.getIntValue() - rhs.getIntValue(); break;
        case ExprBinary.BINOP_MUL:  if(hasv) newv = lhs.getIntValue() * rhs.getIntValue(); break;
        case ExprBinary.BINOP_DIV:  if(hasv) newv = lhs.getIntValue() / rhs.getIntValue(); break;
        case ExprBinary.BINOP_MOD:  if(hasv) newv = lhs.getIntValue() % rhs.getIntValue(); break;
        
        case ExprBinary.BINOP_AND:  if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) && intToBool(rhs.getIntValue())); break;
        case ExprBinary.BINOP_OR:   if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) || intToBool(rhs.getIntValue())); break;
        case ExprBinary.BINOP_EQ:   if(hasv) newv = boolToInt(lhs.getIntValue() == rhs.getIntValue()); break;
        case ExprBinary.BINOP_NEQ:  if(hasv) newv = boolToInt(lhs.getIntValue() != rhs.getIntValue()); break;
        case ExprBinary.BINOP_LT:   if(hasv) newv = boolToInt(lhs.getIntValue() < rhs.getIntValue()); break;
        case ExprBinary.BINOP_LE:   if(hasv) newv = boolToInt(lhs.getIntValue() <= rhs.getIntValue()); break;
        case ExprBinary.BINOP_GT:   if(hasv) newv = boolToInt(lhs.getIntValue() > rhs.getIntValue()); break;
        case ExprBinary.BINOP_GE:   if(hasv) newv = boolToInt(lhs.getIntValue() >= rhs.getIntValue()); break;
        case ExprBinary.BINOP_BAND: if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) && intToBool(rhs.getIntValue())); break;
        case ExprBinary.BINOP_BOR:  if(hasv) newv = boolToInt( intToBool(lhs.getIntValue()) || intToBool(rhs.getIntValue()));; break;
        case ExprBinary.BINOP_BXOR: if(hasv) newv = boolToInt(lhs.getIntValue() != rhs.getIntValue()); break;
        case ExprBinary.BINOP_SELECT:{        	
        	return handleBinarySelect(exp, left, lhs, right, rhs);
        }
        default: assert false : exp; break;
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
        			rval = new ExprBinary(exp.getContext(), exp.getOp(), left, right, exp.getAlias());
        		}        		
        	}
        }
        return rval;
	}
	
	
    public Object visitExprBinary(ExprBinary exp)
    {
       
        Expression left = (Expression) exp.getLeft().accept(this); 	        
        valueClass lhs = state.popVStack();	       
        
        Expression right = (Expression) exp.getRight().accept(this);
        valueClass rhs = state.popVStack();
        return this.ExprBinaryHelper(exp, left, lhs, right, rhs);
        
    }
    public Object visitExprStar(ExprStar star) {    	
		state.pushVStack(new valueClass());
		return star;
	}
    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    String inParameterSetter(Iterator formalParamIterator, Iterator actualParamIterator, boolean checkError){
    	String result = "";
    	List<Expression> actualsList = new ArrayList<Expression>();
    	List<valueClass> actualsValList = new ArrayList<valueClass>();
    	while(actualParamIterator.hasNext()){
    		Expression actualParam = (Expression)actualParamIterator.next();
    		actualParam.accept(this);
        	valueClass actualParamValue = state.popVStack();
        	actualsList.add(actualParam);
        	actualsValList.add(actualParamValue);
    	}
    	
    	Iterator<Expression> actualIterator = actualsList.iterator();
    	Iterator<valueClass> actualValIterator = actualsValList.iterator();
    	
        while(actualIterator.hasNext()){	        	
        	Expression actualParam = (Expression)actualIterator.next();			        	
        	Parameter formalParam = (Parameter) formalParamIterator.next();
        	        	
        	valueClass actualParamValue = actualValIterator.next();
    		
        	if( actualParamValue.isVect() ){	
        		List<valueClass> lst= actualParamValue.getVectValue();
        		assert formalParam.getType() instanceof TypeArray : "This should never happen!!";
        		((TypeArray)formalParam.getType()).getLength().accept(this);
        		valueClass sizeInt = state.popVStack();
        		Assert(sizeInt.hasValue(), "I must know array bounds for parameters at compile time");
        		int size = sizeInt.getIntValue();	        			        		
        		
        		if(lst.size()<size){
        			while(lst.size()<size){
        				lst.add(new valueClass(0));	        				
        			}
        			assert lst.size() == size :"Just to make sure";
        		}
        		
        		String formalParamName = formalParam.getName();
	        	state.varDeclare(formalParamName);
	    		String lhsname = state.varGetLHSName(formalParamName);
	    		
	    		
	    		Iterator<valueClass> actualParamValueIt = lst.iterator();		    		
	    		int idx = 0;
	    		state.makeArray(formalParamName, lst.size());
	    		while( actualParamValueIt.hasNext() ){
	    			valueClass currentVal =  actualParamValueIt.next();
	    			String currentName = formalParamName + "_idx_" + idx;
	    			state.varDeclare(currentName);
		    		lhsname = state.varGetLHSName(currentName);			    		
		    		if( !formalParam.isParameterOutput() ){
			    		if(!currentVal.hasValue()){
			    			result += lhsname + " = " + currentVal + ";\n";
			    		}else{
			    			state.setVarValue(currentName, currentVal.getIntValue());
			    		}
		    		}
	    			++idx;
	    		}
	    		if(this.isReplacer && !formalParam.isParameterOutput()){
		    		addStatement( new StmtVarDecl(actualParam.getContext(), formalParam.getType(),
	                        formalParam.getName(), actualParam) );
	    		}
	    	}else{
	    		String formalParamName = formalParam.getName();
	        	state.varDeclare(formalParamName);
	    		String lhsname = state.varGetLHSName(formalParamName);		    		
	    		Assert(actualParamValue.hasValue() || !checkError, "I must be able to determine the values of the parameters at compile time.");
	    		if( !formalParam.isParameterOutput() ){
		    		if(!actualParamValue.hasValue()){
		    			result += lhsname + " = " + actualParamValue + ";\n";
		    			if(isReplacer){
			    			addStatement( new StmtVarDecl(actualParam.getContext(), formalParam.getType(),
			                        formalParam.getName(), actualParam) );
		    			}
		    		}else{
		    			int value = actualParamValue.getIntValue();
		    			state.setVarValue(formalParamName, value);
		    			if(isReplacer){
			    			addStatement( new StmtVarDecl(actualParam.getContext(), formalParam.getType(),
			                        formalParam.getName(), new ExprConstInt(value)) );
		    			}
		    		}
	    		}
	    	}
        }
        return result;
    }
    
    
    String outParameterSetter(Iterator formalParamIterator, Iterator actualParamIterator, boolean checkError){
    	String result = "";
        while(actualParamIterator.hasNext()){	        	
        	Expression actualParam = (Expression)actualParamIterator.next();			        	
        	Parameter formalParam = (Parameter) formalParamIterator.next();
        	
        	String apnm = (String) actualParam.accept( new LHSvisitor());	        	
        	if( formalParam.isParameterOutput() ){
        		String formalParamName = formalParam.getName();
	        	int sz = state.checkArray(formalParamName);
	        	if( sz > 0 ){
	        		for(int i=0; i<sz; ++i){
	        			String formalName = formalParamName + "_idx_" + i;		        			
        				if(state.varHasValue(formalName)){
        					state.setVarValue(apnm+"_idx_"+i, state.varValue(formalName));
        				}else{
        					String rhsname = state.varGetRHSName(formalName);
			    			result += state.varGetLHSName(apnm+"_idx_"+i) + " = " + rhsname + ";\n";
        				}				    		
	        		}
	        		if(this.isReplacer){
	        			addStatement(new StmtAssign(actualParam.getContext(), actualParam, new ExprVar(actualParam.getContext(), formalParam.getName()) ));
	        		}
		    	}else{
	    			if(state.varHasValue(formalParamName)){
	    				int val = state.varValue(formalParamName);
    					state.setVarValue(apnm, val);
    					if(this.isReplacer){
    	        			addStatement(new StmtAssign(actualParam.getContext(), actualParam, new ExprConstInt(actualParam.getContext(), val) ));
    	        		}
    				}else{
    					String rhsname = state.varGetRHSName(formalParamName);
		    			result += state.varGetLHSName(apnm) + " = " + rhsname + ";\n";
		    			if(this.isReplacer){
		        			addStatement(new StmtAssign(actualParam.getContext(), actualParam, new ExprVar(actualParam.getContext(), formalParam.getName()) ));
		        		}
    				}
		    	}
        	}
        }
        return result;
    }
    
    
    
    public Object visitExprFunCall(ExprFunCall exp)
    {	    	
    	String result = " ";
    	String name = exp.getName();
    	// Local function?
    	if (ss.getFuncNamed(name) != null) {        	
    		state.pushLevel();        	
    		Function fun = ss.getFuncNamed(name);	 
    		{
    			Iterator actualParams = exp.getParams().iterator();	        		        	       	
    			Iterator formalParams = fun.getParams().iterator();
    			result += inParameterSetter(formalParams, actualParams, false);
    		}
    		result += "// BEGIN CALL " + fun.getName() + "\n";
    		Object obj = fun.getBody().accept(this);
    		if(obj instanceof String)
    			result += (String) obj; 
    		result += "// END CALL " + fun.getName() + "\n";
    		{
    			Iterator actualParams = exp.getParams().iterator();	        		        	       	
    			Iterator formalParams = fun.getParams().iterator();
    			result += outParameterSetter(formalParams, actualParams, false);
    		}
    		state.popLevel();
    	}else{ 
    		// look for print and println statements; assume everything
    		// else is a math function
    		if (name.equals("print")) {
    			System.err.println("The StreamBit compiler currently doesn't allow print statements in bit->bit filters.");
    			return "";
    		} else if (name.equals("println")) {
    			result = "System.out.println(";
    			System.err.println("The StreamBit compiler currently doesn't allow print statements in bit->bit filters.");
    			return "";
    		} else if (name.equals("super")) {
    			result = "";
    		} else if (name.equals("setDelay")) {
    			result = "";
    		} else if (name.startsWith("enqueue")) {	        	
    			result = "";
    		} else {
    			Assert(false, "The streamBit compiler currently doesn't allow bit->bit filters to call other functions. You are trying to call the function" + name);
    			// Math.sqrt will return a double, but we're only supporting
    			// float's now, so add a cast to float.  Not sure if this is
    			// the right thing to do for all math functions in all cases?
    			result = "(float)Math." + name + "(";
    		}
    	}
    	state.pushVStack( new valueClass(result) );
    	return exp;    	
    }

	public String convertType(Type type) {
	    // This is So Wrong in the greater scheme of things.
	    if (type instanceof TypeArray)
	    {
	        TypeArray array = (TypeArray)type;
	        String base = convertType(array.getBase());
	        array.getLength().accept(this);
	        int i = state.popVStack().getIntValue();	            
	        return base + "[" + i + "]";
	    }
	    else if (type instanceof TypeStruct)
	{
	    return ((TypeStruct)type).getName();
	}
	else if (type instanceof TypeStructRef)
	    {
	    return ((TypeStructRef)type).getName();
	    }
	    else if (type instanceof TypePrimitive)
	    {
	        switch (((TypePrimitive)type).getType())
	        {
	        case TypePrimitive.TYPE_BOOLEAN: return "boolean";
	        case TypePrimitive.TYPE_BIT: return "bit";
	        case TypePrimitive.TYPE_INT: return "int";
	        case TypePrimitive.TYPE_FLOAT: return "float";
	        case TypePrimitive.TYPE_DOUBLE: return "double";
	        case TypePrimitive.TYPE_COMPLEX: return "Complex";
	        case TypePrimitive.TYPE_VOID: return "void";
	        }
	    }
	    else if (type instanceof TypePortal)
	    {
	        return ((TypePortal)type).getName() + "Portal";
	    }
	    return null;
	}

	public String doParams(List params, String prefix) {
	    String result = "(";
	    boolean first = true;
	    for (Iterator iter = params.iterator(); iter.hasNext(); )
	    {
	        Parameter param = (Parameter)iter.next();
	        if (!first) result += ", ";
	        
	        if(param.isParameterOutput()) result += "! ";
	        
	        if (prefix != null) result += prefix + " ";
	        result += convertType(param.getType());
	        result += " ";
	
	        String lhs = param.getName();
	        state.varDeclare(lhs);
	        String lhsn = state.varGetLHSName(lhs);	            
	        if( param.getType() instanceof TypeArray ){
	        	TypeArray ta = (TypeArray) param.getType();
	        	ta.getLength().accept(this);
	        	valueClass tmp = state.popVStack();
	        	Assert(tmp.hasValue(), "The array size must be a compile time constant !! \n" );
	        	state.makeArray(lhs, tmp.getIntValue());
	        	for(int tt=0; tt<tmp.getIntValue(); ++tt){
	        		String nnm = lhs + "_idx_" + tt;
	        		state.varDeclare(nnm);
	        		String tmplhsn = state.varGetLHSName(nnm);
	        		if(param.isParameterOutput()){
	        			state.varDeclare("_p_"+nnm);
	            		String tmplhsn2 = state.varGetLHSName("_p_"+nnm);
	            		result += tmplhsn2 + "  ";
	        		}else{
	        			result += tmplhsn + "  ";
	        		}
	        	}
	        }else{
	        	if(param.isParameterOutput()){
	        		state.varDeclare("_p_"+lhs);
	        		String tmplhsn2 = state.varGetLHSName("_p_"+lhs);
	        		result += tmplhsn2;
	        	}else{
	        		result += lhsn;
	        	}
	        }
	        first = false;
	    }
	    result += ")";
	    return result;
	}
	
	public Object visitStreamSpec(StreamSpec spec)
    {
		ss = spec;
		return super.visitStreamSpec(spec);
    }
    
}
