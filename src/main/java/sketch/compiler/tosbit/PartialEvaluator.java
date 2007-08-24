package streamit.frontend.tosbit;

import java.util.*;

import streamit.frontend.nodes.*;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
import streamit.frontend.tosbit.recursionCtrl.RecursionControl;

public class PartialEvaluator extends FEReplacer {
	protected StreamSpec ss;
	protected MethodState state;
	public final boolean isReplacer;	
	protected boolean isComplete = true;
	protected RecursionControl rcontrol;
    /* Bounds for loop unrolling and function inlining (initialized arbitrarily). */
    protected int MAX_UNROLL = 0;
   

	public class CheckSize extends FENullVisitor{
		int size = -1;
		 public Object visitExprVar(ExprVar exp) { 
			 if(isComplete || state.knowsVar(exp.getName())){
				 size = state.checkArray(exp.getName());
			 }else{
				 size = 0;
			 }
			 return exp.getName();
		 }
		 public Object visitExprArrayRange(ExprArrayRange exp)
 	    {	 	    	
 	    	String vname =  (String) exp.getBase().accept(this);	 	    	
 		    vname = vname + "_idx_" + 0;
 		    if(isComplete || state.knowsVar(vname)){
 		    	size = state.checkArray(vname);
 		    }else{
 		    	size = 0;
 		    }
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
    		if( names != null){
	    		for(Iterator<String> it = names.iterator(); it.hasNext(); ){
	    			String lnm = it.next();
	    			if( isComplete || state.knowsVar(lnm)){
	    				state.unsetVarValue(lnm);	
	    			}
	    		}
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
    	
 
    	
    	public Object visitExprArrayRange(ExprArrayRange exp) {
    		assert exp.getMembers().size() == 1 && exp.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
    		RangeLen rl = (RangeLen)exp.getMembers().get(0);
    		Expression newStart = (Expression) rl.start().accept(PartialEvaluator.this);
    		valueClass startVal = state.popVStack();    		
    		String vname = (String) exp.getBase().accept(this);    			
    		assert startVal.hasValue() || rl.len() == 1 : "For now, we require all array range expressions to have a computable start index.";		
    		if(startVal.hasValue()){
    			lhsVals = new ArrayList<String>(rl.len());
 	    		oldVals = new ArrayList<String>(rl.len());
 	    		names = new ArrayList<String>(rl.len());
 	    		int start = startVal.getIntValue(); 	    		
 	    		for(int i=0; i<rl.len(); ++i){
 	    			int ofst = start + i;
 	    			String nm = vname + "_idx_" + ofst;
 	    			if( isComplete || state.knowsVar(nm) ){
 	    				oldVals.add(state.varGetRHSName(nm));
 	 	    			lhsVals.add(state.varGetLHSName(nm));	
 	    			}else{
 	    				oldVals.add("null");
 	 	    			lhsVals.add("null");	
 	    			} 	    			
 	    			names.add(nm);
 	    		}
 	    		NDArracc = true;
 	    		if(isReplacer){
 		    		if(rl.start() != newStart || exp.getBase() != lhsExp){
 		    			List nlst = new ArrayList();
 						nlst.add( new RangeLen(newStart, rl.len()) );
 						lhsExp = new ExprArrayRange(lhsExp, nlst);
 		    		}else{
 		    			lhsExp = exp;
 		    		}
 		    	}else{
 		    		lhsExp = exp;
 		    	}
    		}else{
    			assert rl.len() == 1 : " NYI";
    			offset = startVal.toString();
     	    	if( startVal.hasValue()){
     		    	int ofstV = startVal.getIntValue();
     		    	int size = state.checkArray(vname);
     		    	if(ofstV >= size || ofstV < 0){
     		    		if(!exp.isUnchecked())
     		    			throw new ArrayIndexOutOfBoundsException(exp.getContext() + ": ARRAY OUT OF BOUNDS !(0<=" + startVal.getIntValue() + " < " + size);
     					state.pushVStack( new valueClass(0) );
     					return null;
     		    	} 		    	
     		    	vname = vname + "_idx_" + ofstV;
     		    	String rval = vname;
     		    	if(isReplacer){
     		    		if(newStart != exp.getOffset() || lhsExp != exp.getBase()){
     		    			lhsExp = new ExprArrayRange(exp.getContext(), lhsExp, newStart);
     		    		}else{
     		    			lhsExp = exp;
     		    		}
     		    	}else{
     		    		lhsExp = exp;
     		    	}
     		    	return rval;
     	    	}else{
     	    		int size = state.checkArray(vname);
     	    		NDArracc = true;
     	    		if(  isComplete || size > 0  ){
	     	    		lhsVals = new ArrayList<String>(size);
	     	    		oldVals = new ArrayList<String>(size);
	     	    		names = new ArrayList<String>(size);
	     	    		for(int i=0; i<size; ++i){
	     	    			String nm = vname + "_idx_" + i;
	     	    			oldVals.add(state.varGetRHSName(nm));
	     	    			lhsVals.add(state.varGetLHSName(nm));
	     	    			names.add(nm);
	     	    		}	     	    		
	     	    		if(isReplacer){
	     		    		if(newStart != exp.getOffset() || lhsExp != exp.getBase()){
	     		    			lhsExp = new ExprArrayRange(exp.getContext(), lhsExp, newStart);
	     		    		}else{
	     		    			lhsExp = exp;
	     		    		}
	     		    	}else{
	     		    		lhsExp = exp;
	     		    	}
     	    		}else{
     	    			if(isReplacer){
	     		    		if(newStart != exp.getOffset()|| lhsExp != exp.getBase()){
	     		    			lhsExp = new ExprArrayRange(exp.getContext(), lhsExp, newStart);
	     		    		}else{
	     		    			lhsExp = exp;
	     		    		}
	     		    	}else{
	     		    		lhsExp = exp;
	     		    	}	
     	    		}
     	    	}
    			
    		}
    		return vname;
    	}
    	
	    public Object visitExprVar(ExprVar exp)
	    {		   
	    	if(isReplacer){
	    		lhsExp = new ExprVar(exp.getContext(), state.transName(exp.getName())); //exp;
	    	}else{
	    		lhsExp = exp;
	    	}
	    	return exp.getName();		    	
	    }	    	
    }
	
	
	public PartialEvaluator(boolean isReplacer, int maxUnroll, RecursionControl rcontrol) {
		super();
		this.isReplacer = isReplacer;
        this.MAX_UNROLL = maxUnroll;
        this.rcontrol = rcontrol;        
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

	
	public Object visitExprArrayRange(ExprArrayRange exp) {
		assert exp.getMembers().size() == 1 && exp.getMembers().get(0) instanceof RangeLen : "Complex indexing not yet implemented.";
		RangeLen rl = (RangeLen)exp.getMembers().get(0);
		Expression newStart = (Expression) rl.start().accept(this);
		valueClass startVal = state.popVStack();
		
		Expression newBase = (Expression) exp.getBase().accept(this);
		valueClass baseVal = state.popVStack();				
		if(startVal.hasValue()){
			if( baseVal.isVect()  || isComplete ){
				assert baseVal.isVect() :"This has to be a vector, otherwise, something went wrong.";
				List<valueClass> lst = baseVal.getVectValue();
				int sval = startVal.getIntValue();
				List<valueClass> newLst = lst.subList(sval, sval + rl.len());
				state.pushVStack( new valueClass(newLst));
				if(this.isReplacer && (rl.start() != newStart || exp.getBase() != newBase )){
					if( exp.getBase() instanceof ExprVar && newBase instanceof ExprArrayInit){
						Expression renamedBase = new ExprVar(exp.getContext(), state.transName(  ((ExprVar)exp.getBase()).getName()  ));
		        		List nlst = new ArrayList();
						nlst.add( new RangeLen(newStart, rl.len()) );
						return new ExprArrayRange( renamedBase, nlst);		        		
		        	}else{
		        		List nlst = new ArrayList();
						nlst.add( new RangeLen(newStart, rl.len()) );
						return new ExprArrayRange(newBase, nlst);	        		
		        	}				
				}else{
					return exp;
				}
			}else{
				state.pushVStack(new valueClass(exp.toString()));
				if(this.isReplacer && (rl.start() != newStart || exp.getBase() != newBase )){
					List nlst = new ArrayList();
					nlst.add( new RangeLen(newStart, rl.len()) );
					return new ExprArrayRange(newBase, nlst);	        		
				}else{
					return exp;
				}
			}
		}else{
			state.pushVStack(new valueClass(exp.toString()));
			if(this.isReplacer && (rl.start() != newStart || exp.getBase() != newBase )){
	        	if( exp.getBase() instanceof ExprVar && newBase instanceof ExprArrayInit){
	        		List nlst = new ArrayList();
					nlst.add( new RangeLen(newStart, rl.len()) );
					Expression renamedBase = new ExprVar(exp.getContext(), state.transName(  ((ExprVar)exp.getBase()).getName()  ));
					return new ExprArrayRange( renamedBase, nlst);		        		
	        	}else{
	        		List nlst = new ArrayList();
					nlst.add( new RangeLen(newStart, rl.len()) );
					return new ExprArrayRange(newBase, nlst);	        		
	        	}
			}else{
				return exp;
			}	
		}
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
			Object rv = exp.getExpr().accept(this);
			valueClass vc  = state.popVStack();
			TypeArray ta = (TypeArray) exp.getType();
			ExprConstInt ie =  (ExprConstInt) ta.getLength();
			List<valueClass> vcl = vc.getVectValue();
			state.pushVStack(new valueClass(vcl.subList(0, ie.getVal())));
			return rv;
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
	    	int t = 1;
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
	    			if( curv == 1 ) val += t; 
	    			t = t*2;
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
		
		if( !isComplete ){
			if( ! state.knowsVar(vname) ){
				state.pushVStack( new valueClass(vname) );
				return exp;
			}
		}
		
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
			String prefix = vname + "_idx_";
			for(int i=0; i<sz; ++i){
				String lnm = prefix + i;
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
				return new ExprVar(exp.getContext(), state.transName(exp.getName()));
			}
		}else{
			state.pushVStack(intValue);
			if(this.isReplacer && intValue.hasValue()){
				return new ExprConstInt(intValue.getIntValue());
			}else{
				return new ExprVar(exp.getContext(), state.transName(exp.getName()));
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
	    boolean isV = vchild.isVect();
	    assert !isV || (exp.getOp()== ExprUnary.UNOP_NOT ||exp.getOp()== ExprUnary.UNOP_BNOT) : "Vector unary currently only supported for not";
	    switch(exp.getOp())
	    {
	    case ExprUnary.UNOP_NOT: 
	    	if( isV ){
	    		List<valueClass> lst0 = vchild.getVectValue();
	    		List <valueClass> lst = new ArrayList<valueClass>();
	    		for(Iterator<valueClass> it = lst0.iterator(); it.hasNext(); ){
	    			valueClass vc = it.next();
	    			if( vc.hasValue() ){
	    				lst.add( new valueClass(1-vc.getIntValue()) );
	    			}else{
	    				lst.add(new valueClass("!" + vc) );
	    			}
	    		}
	    		state.pushVStack( new valueClass(lst) );
	    		if(this.isReplacer && childExp != exp.getExpr()) returnVal = new ExprUnary(exp.getContext(), exp.getOp(), childExp );
	    		return returnVal;
	    	}else{
	    		if( hv ){ 
		    		state.pushVStack(new valueClass(1-vchild.getIntValue()));
		    		if(this.isReplacer) returnVal = new ExprConstInt(1-vchild.getIntValue());
		    	}else{
		    		state.pushVStack( new valueClass("!" + child) );
		    		if(this.isReplacer && childExp != exp.getExpr()) returnVal = new ExprUnary(exp.getContext(), exp.getOp(), childExp );
		    	}	
	    	}
	    return returnVal;
	    case ExprUnary.UNOP_BNOT: 
	    	if( hv ){ 
	    		if(vchild.isVect()){
	    			List<valueClass> lst = vchild.getVectValue();
	    			
	    			List<valueClass> lst2 = new ArrayList<valueClass>();
	    			for(Iterator<valueClass> it = lst.iterator(); it.hasNext();  ){
	    				valueClass vc = it.next();
	    				if( vc.hasValue()){
	    					lst2.add(new valueClass(1-vc.getIntValue()));
	    				}else{
	    					lst2.add(new valueClass("~" + vc));
	    				}
	    			}
	    			state.pushVStack(new valueClass(lst2));
	    		}else{
	    			state.pushVStack(new valueClass(1-vchild.getIntValue()));
	    			if(this.isReplacer) returnVal = new ExprConstInt(1-vchild.getIntValue());
	    		}
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
	    assert false : "This should not happen";
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
        
        case ExprBinary.BINOP_RSHIFT:{
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
        
        case ExprBinary.BINOP_LSHIFT:{
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
        
        case ExprBinary.BINOP_AND:{
        	if(hasv){        
        		newv = boolToInt( intToBool(lhs.getIntValue()) && intToBool(rhs.getIntValue()));
        	}else{
        		if( lhs.hasValue() && !intToBool(lhs.getIntValue())){
        			hasv = true;
        			newv = 0;
        		}
        		if( rhs.hasValue() && !intToBool(rhs.getIntValue())){
        			hasv = true;
        			newv = 0;
        		}
        	}
        	break;
        }
        case ExprBinary.BINOP_OR:{   
        	if(hasv){
	        	newv = boolToInt( intToBool(lhs.getIntValue()) || intToBool(rhs.getIntValue())); 
	        }else{
	    		if( lhs.hasValue() && intToBool(lhs.getIntValue())){
	    			hasv = true;
	    			newv = 1;
	    		}
	    		if( rhs.hasValue() && intToBool(rhs.getIntValue())){
	    			hasv = true;
	    			newv = 1;
	    		}
	    	}
        	break;
        }
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
        	result += lhs + " " + op + " " + rhs + ")";            
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
    
    private void addParamCopyDecl(Parameter param, Expression expr)
    {
		//if(expr instanceof ExprVar && param.getName().equals(((ExprVar)expr).getName()))
		//	return;		
    	Statement varDecl=new StmtVarDecl(expr.getContext(),param.getType(),state.transName(param.getName()),expr);
    	addStatement(varDecl);
    }
    private void addParamCopyDecl(Parameter param)
    {
    	Statement varDecl=new StmtVarDecl(null,param.getType(),state.transName(param.getName()),null);
    	addStatement(varDecl);
    }
    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    /////////////////////////////////////////////////////
    String inParameterSetter(Iterator formalParamIterator, Iterator actualParamIterator, boolean checkError){    	
    	List<Expression> actualsList = new ArrayList<Expression>();
    	List<valueClass> actualsValList = new ArrayList<valueClass>();
    	while(actualParamIterator.hasNext()){
    		Expression actualParam = (Expression)actualParamIterator.next();
    		actualParam = (Expression) actualParam.accept(this);
        	valueClass actualParamValue = state.popVStack();
        	actualsList.add(actualParam);
        	actualsValList.add(actualParamValue);
    	}
    	
    	state.pushLevel();
    	
    	StringBuffer result = new StringBuffer();
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
	    		result.ensureCapacity( result.length() +  lst.size()*10 );
	    		while( actualParamValueIt.hasNext() ){
	    			valueClass currentVal =  actualParamValueIt.next();
	    			String currentName = formalParamName + "_idx_" + idx;
	    			state.varDeclare(currentName);
		    		lhsname = state.varGetLHSName(currentName);			    		
		    		if( !formalParam.isParameterOutput() ){
			    		if(!currentVal.hasValue()){
			    			result.append(lhsname + " = " + currentVal + ";\n");
			    		}else{
			    			state.setVarValue(currentName, currentVal.getIntValue());
			    		}
		    		}
	    			++idx;
	    		}
	    		if(this.isReplacer && !formalParam.isParameterOutput()) {
	    			addParamCopyDecl(formalParam,actualParam);
	    		}else if(this.isReplacer && formalParam.isParameterOutput()) {
	    			addParamCopyDecl(formalParam);
	    		}
	    	}else{
	    		String formalParamName = formalParam.getName();
	        	state.varDeclare(formalParamName);
	    		String lhsname = state.varGetLHSName(formalParamName);		    		
	    		Assert(actualParamValue.hasValue() || !checkError, "I must be able to determine the values of the parameters at compile time.");
	    		if( !formalParam.isParameterOutput() ){
		    		if(!actualParamValue.hasValue()){
		    			result.append(lhsname + " = " + actualParamValue + ";\n");
		    			if(isReplacer){
		    				addParamCopyDecl(formalParam,actualParam);
		    			}
		    		}else{
		    			int value = actualParamValue.getIntValue();
		    			state.setVarValue(formalParamName, value);
		    			if(isReplacer){
		    				addParamCopyDecl(formalParam,new ExprConstInt(actualParam.getContext(),value));
		    			}
		    		}
	    		}else{
	    			if(isReplacer){
	    				addParamCopyDecl(formalParam);
	    			}
	    		}
	    	}
        }
        return result.toString();
    }
    
    
    String outParameterSetter(Iterator formalParamIterator, Iterator actualParamIterator, boolean checkError){
    	String result = "";
    	FEContext context = null;
    	List<valueClass> formalList = new ArrayList<valueClass>();
    	List<String> formalTransNames = new ArrayList<String>();
    	while(formalParamIterator.hasNext()){
    		Parameter formalParam = (Parameter) formalParamIterator.next();
    		if( formalParam.isParameterOutput() ){
    			String formalParamName = formalParam.getName();    			    			
    			formalTransNames.add(state.transName(formalParamName));
    			
	        	int sz = state.checkArray(formalParamName);
	        	if( sz > 0 ){
	        		List<valueClass> tmplst = new ArrayList<valueClass>();
	        		for(int i=0; i<sz; ++i){
	        			String formalName = formalParamName + "_idx_" + i;		        			
        				if(state.varHasValue(formalName)){
        					valueClass vv = new valueClass(state.varValue(formalName));
        					tmplst.add(vv);
        				}else{
        					String rhsname = state.varGetRHSName(formalName);
        					valueClass vv = new valueClass(rhsname);
        					tmplst.add(vv);
        				}				    		
	        		}
	        		valueClass vv = new valueClass(tmplst);
	        		formalList.add(vv);
	        	}else{
	        		if(state.varHasValue(formalParamName)){
	    				int val = state.varValue(formalParamName);
	    				valueClass vv = new valueClass(val);
		        		formalList.add(vv);
    				}else{
    					String rhsname = state.varGetRHSName(formalParamName);
    					valueClass vv = new valueClass(rhsname);
    					formalList.add(vv);
    				}
	        	}
    		}else{
    			formalList.add(null);
    			formalTransNames.add(null);
    		}
    	}
    	
    	state.popLevel();
    	Iterator<valueClass> vcIt = formalList.iterator();
    	Iterator<String> fTransNamesIt = formalTransNames.iterator(); 
        while(actualParamIterator.hasNext()){	        	
        	Expression actualParam = (Expression)actualParamIterator.next();			        	        	
        	valueClass formal = vcIt.next();
        	String fTransName = fTransNamesIt.next();
        	if( formal != null ){
        		LHSvisitor lhsvisit =  new LHSvisitor();
            	String apnm = (String) actualParam.accept(lhsvisit);	        			        	
	        	
	        	if( formal.isVect() ){
	        		List<valueClass> elems = formal.getVectValue();
	        		int sz = elems.size();
	        		
	        		for(int i=0; i<sz; ++i){
	        			valueClass elemValue = elems.get(i);
        				if(elemValue.hasValue()){
        					state.setVarValue(apnm+"_idx_"+i, elemValue.getIntValue());
        				}else{        					
			    			result += state.varGetLHSName(apnm+"_idx_"+i) + " = " + elemValue.toString() + ";\n";
        				}				    		
	        		}
	        		if(this.isReplacer){
	        			addStatement(new StmtAssign(context, lhsvisit.lhsExp, new ExprVar(context, fTransName) ));
	        		}
		    	}else{
	    			if(formal.hasValue()){
	    				int val = formal.getIntValue();
    					state.setVarValue(apnm, val);
    					if(this.isReplacer){
    	        			addStatement(new StmtAssign(context, lhsvisit.lhsExp, new ExprConstInt(context, val) ));
    	        		}
    				}else{    					
		    			result += state.varGetLHSName(apnm) + " = " + formal.toString() + ";\n";
		    			if(this.isReplacer){
		        			addStatement(new StmtAssign(context, lhsvisit.lhsExp, new ExprVar(context, fTransName) ));
		        		}
    				}
		    	}
        	}
        }
        return result;
    }
    
    /* Set output parameters to arbitrary values (i.e. 0).
     * This is needed when we don't really care for the computation done
     * inside a function, but do want to preserve the dependences between
     * inputs and output in a way (e.g. when doing bounded function inlining).
     */
    String outParameterSetterArbitrary (Iterator formalParamIterator,
                                        Iterator actualParamIterator,
                                        boolean checkError)
    {
        String result = "";
        while(actualParamIterator.hasNext()){	        	
            Expression actualParam = (Expression)actualParamIterator.next();			        	
            Parameter formalParam = (Parameter) formalParamIterator.next();

            LHSvisitor lhsvisit =  new LHSvisitor();
            String apnm = (String) actualParam.accept(lhsvisit);	        	
            if( formalParam.isParameterOutput() ){
                String formalParamName = formalParam.getName();
                int sz = state.checkArray(formalParamName);
                if( sz > 0 ){
                    for(int i=0; i<sz; ++i){
                        state.setVarValue(apnm+"_idx_"+i, 0);
                    }
                    if(this.isReplacer){
                        addStatement(new StmtAssign(actualParam.getContext(), lhsvisit.lhsExp, new ExprConstInt(actualParam.getContext(), 0) ));
                    }
                }else{
                    state.setVarValue(apnm, 0);
                    if(this.isReplacer){
                        addStatement(new StmtAssign(actualParam.getContext(), lhsvisit.lhsExp, new ExprConstInt(actualParam.getContext(), 0) ));
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
	        			String lname = "_p_"+nnm;
	        			state.varDeclare(lname);
	            		String tmplhsn2 = state.varGetLHSName(lname);
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
