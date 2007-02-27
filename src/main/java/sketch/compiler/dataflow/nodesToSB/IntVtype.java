package streamit.frontend.experimental.nodesToSB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.abstractValueType;
import streamit.frontend.experimental.varState;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;

public class IntVtype extends abstractValueType {

	

	public abstractValue STAR(FENode star){
		return BOTTOM("??");
	}
	
	public abstractValue BOTTOM(Type t){
		if( t instanceof TypePrimitive ){
			return BOTTOM();
		}
		assert false;
		return null;
	}
	

	public abstractValue ARR(List<abstractValue> vals){
		return new IntAbsValue(vals);		
	}

	public abstractValue BOTTOM(){
		return new IntAbsValue();
	}
	
	public abstractValue BOTTOM(String label){
		return new IntAbsValue(label);
	}

	public abstractValue CONST(int v){
		return new IntAbsValue(v); 
	}
	
	public abstractValue CONST(boolean v){
		return new IntAbsValue(v); 
	}


	public void Assert(abstractValue val){
		 if( val.hasIntVal() ){
			 assert val.getIntVal() != 0 : "Assertion failure";
		 }
	}
	

	public varState cleanState(String var, Type t){
		return new IntState(t, this);
	}
	
	private String opStr(abstractValue v1, abstractValue v2, String op) {
		return "(" + v1.toString() + " " + op + " " + v2.toString() + ")";
	}

	public abstractValue plus(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() + v2.getIntVal() ); 
		}else{
			return BOTTOM( opStr(v1, v2, "+") );
		}
	}

	public abstractValue minus(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() - v2.getIntVal() ); 
		}else{
			return BOTTOM( opStr(v1, v2, "-") );
		}
	}

	public abstractValue times(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() * v2.getIntVal() ); 
		}else{
			return BOTTOM( opStr(v1, v2, "*") );
		}
	}

	public abstractValue over(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() / v2.getIntVal() ); 
		}else{
			return BOTTOM( opStr(v1, v2, "/") );
		}
	}

	public abstractValue mod(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() % v2.getIntVal() ); 
		}else{
			return BOTTOM( opStr(v1, v2, "%") );
		}
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
	
	
	public abstractValue shr(abstractValue v1, abstractValue v2){
		return BOTTOM( v1 + ">>" + v2);
	}
	
	public abstractValue shl(abstractValue v1, abstractValue v2){
		return BOTTOM( v1 + "<<" + v2);
	}


	public abstractValue and(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( boolToInt(intToBool(v1.getIntVal()) && intToBool(v2.getIntVal())) ); 
		}else{
			return BOTTOM( opStr(v1, v2, "&") );
		}
	}

	public abstractValue or(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( boolToInt(intToBool(v1.getIntVal()) || intToBool(v2.getIntVal())) ); 
		}else{
			return BOTTOM( opStr(v1, v2, "|") );
		}
	}

	public abstractValue xor(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( boolToInt(intToBool(v1.getIntVal()) ^ intToBool(v2.getIntVal())) ); 
		}else{
			return BOTTOM( opStr(v1, v2, "^") );
		}
	}

	public abstractValue gt(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() > v2.getIntVal() ); 
		}else{
			return BOTTOM( opStr(v1, v2, ">") );
		}
	}

	public abstractValue lt(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() < v2.getIntVal() ); 
		}else{
			return BOTTOM( opStr(v1, v2, "<") );
		}
	}

	public abstractValue ge(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() >= v2.getIntVal() ); 
		}else{
			return BOTTOM( opStr(v1, v2, ">=") );
		}
	}

	public abstractValue le(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() <= v2.getIntVal() ); 
		}else{
			return BOTTOM( opStr(v1, v2, "<=") );
		}
	}

	public abstractValue eq(abstractValue v1, abstractValue v2) {
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return CONST( v1.getIntVal() == v2.getIntVal() ); 
		}else{
			return BOTTOM( opStr(v1, v2, "==") );
		}
	}

	public abstractValue arracc(abstractValue arr, abstractValue idx) {
		assert false; return null;
	}

	public abstractValue arracc(abstractValue arr, abstractValue idx, abstractValue len, boolean isUnchecked) {
		if(len != null){
			assert len.hasIntVal() : "NYI";
			int ilen = len.getIntVal();
			if(ilen != 1){
				List<abstractValue> lst = new ArrayList<abstractValue>(ilen);
				for(int i=0; i<ilen; ++i){
					lst.add(  arracc(arr, plus(idx, CONST(i)), null, isUnchecked)  );
				}
				return ARR( lst );
			}
		}
		
		
		
		
		
		if( idx.hasIntVal() ){
			int iidx = idx.getIntVal() ;
			int size = arr.getVectValue().size();
			if( !isUnchecked && (iidx < 0 || iidx >= size)  )
				throw new ArrayIndexOutOfBoundsException("ARRAY OUT OF BOUNDS !(0<=" + iidx + " < " + size);
			if(iidx < 0 || iidx >= size)
				return CONST(0);
			return arr.getVectValue().get(idx.getIntVal());
		}else{
			return BOTTOM( "(" + arr + "[" + idx + "])" );
		}		
	}

	public abstractValue cast(abstractValue v1, Type type) {
		if(v1.isVect() && type.equals( TypePrimitive.inttype ) ){
			//Casting a bit-vector into an integer.
			List<abstractValue> lst = v1.getVectValue();
			String result =  "( $$";
			int i = 0;
	    	int val=0;
	    	int t = 1;
	    	boolean hasValue = true;
			for(Iterator<abstractValue> it = lst.iterator(); it.hasNext(); ){
				abstractValue o = it.next();
	    		if(!o.hasIntVal()){	        			
	    			result += " " + o;
	    			hasValue = false;
	    		}else{	        			
	    			int curv =  o.getIntVal();
	    			result += " " + curv;
	    			assert curv == 1 || curv == 0 : "Only boolean arrays please!!";
	    			if( curv == 1 ) val += t; 
	    			t = t*2;
	    		}
	    		++i;
			}
			result += " $$ )";
			if(hasValue){
				return CONST(val);
	    	}else{
	    		return BOTTOM(result);
	    	}
		}
		
		
		if(v1.isVect() && type instanceof TypeArray ){
			TypeArray t =  (TypeArray) type;
			Integer len = t.getLength().getIValue();
			if(len != null){
				int mlen = len;
				List<abstractValue> lst1 = v1.getVectValue();
				if( mlen >= lst1.size()  ) mlen = lst1.size();
				List<abstractValue> lst2 = lst1.subList(0, mlen);
				for(int j=mlen; j<len; ++j){
					lst2.add(CONST(0));
				}
				return ARR(lst2);
			}else{
				return v1;
			}
		}
		
		if(v1.isBottom() ){
			return v1;
		}
		
		assert false : "Can't cast " + v1 + " into " + type;
		return null;
	}

	public abstractValue not(abstractValue v1) {
		if( v1.hasIntVal() ){
			return CONST( 1-v1.getIntVal()  ); 
		}else{
			return BOTTOM( "( ! " + v1 + ")" );
		}
	}

	public abstractValue neg(abstractValue v1) {
		if( v1.hasIntVal() ){
			return CONST( -v1.getIntVal()  ); 
		}else{
			return BOTTOM( "( -" + v1 + ")" );
		}
	}

	public abstractValue join(abstractValue v1, abstractValue v2) {
		if( v1.equals(v2) ){
			return v1;
		}else{
			return this.BOTTOM();
		}
	}

	public abstractValue condjoin(abstractValue cond, abstractValue vtrue, abstractValue vfalse) {
		if(cond == null) return join(vtrue, vfalse);
		if( cond.hasIntVal() ){
			if( cond.getIntVal() != 0){
				return vtrue;
			}else{
				return vfalse;
			}
		}else{
			return BOTTOM( "(" + cond + "? (" + vtrue + ") : (" + vfalse + ") )" ); 
		}	
	}
	
	public void funcall(Function fun, List<abstractValue> avlist, List<abstractValue> outSlist){
		Iterator<Parameter> formalParams = fun.getParams().iterator();
    	while(formalParams.hasNext()){
    		Parameter param = formalParams.next();    	
    		if( param.isParameterOutput()){
    			outSlist.add(BOTTOM());
    		}
    	}
	}

}
