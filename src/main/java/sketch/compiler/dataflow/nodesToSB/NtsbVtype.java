package streamit.frontend.experimental.nodesToSB;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.abstractValueType;
import streamit.frontend.experimental.varState;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprStar;
import streamit.frontend.nodes.ExprTypeCast;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.tosbit.ValueOracle;
import streamit.frontend.tosbit.valueClass;

public class NtsbVtype extends abstractValueType {
	public PrintStream out;
	private ValueOracle oracle;	
	public NtsbVtype(ValueOracle oracle, PrintStream out){
		this.oracle = oracle;
		this.out = out;		
	}
	
	public abstractValue STAR(ExprStar star){
		String cvar = oracle.addBinding(star);
		String isFixed = star.isFixed()? " *" : "";
		String rval;
		if(star.getSize() > 1)
			rval =  "<" + cvar + "  " + star.getSize() + isFixed+ ">";
		else
			rval =  "<" + cvar +  ">";		
		return new NtsbValue(rval);
	}
	
	public abstractValue BOTTOM(){
		return new NtsbValue();
	}
	public abstractValue BOTTOM(Type t){
		if( t instanceof TypePrimitive ){
			return BOTTOM();
		}
		assert false;
		return null;
	}
	public abstractValue CONST(int v){
		return new NtsbValue(v); 
	}
	
	public abstractValue ARR(List<abstractValue> vals){
		return new NtsbValue(vals);		
	}
	
	
	private String opStr(abstractValue v1, abstractValue v2, String op){
		return "(" + v1.toString() + " " + op + " " + v2.toString() + ")";
	}
	
	public abstractValue plus(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( v1.getIntVal() + v2.getIntVal() ); 
		}else{
			return new NtsbValue( opStr(v1, v2, "+") );
		}
	}
	public abstractValue minus(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( v1.getIntVal() - v2.getIntVal() ); 
		}else{
			return new NtsbValue( opStr(v1, v2, "-") );
		}
	}
	public abstractValue times(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( v1.getIntVal() * v2.getIntVal() ); 
		}else{
			return new NtsbValue( opStr(v1, v2, "*") );
		}
	}
	public abstractValue over(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( v1.getIntVal() / v2.getIntVal() ); 
		}else{
			return new NtsbValue( opStr(v1, v2, "/") );
		}
	}
	public abstractValue mod(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( v1.getIntVal() % v2.getIntVal() ); 
		}else{
			return new NtsbValue( opStr(v1, v2, "%") );
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
	
	public abstractValue and(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( boolToInt(intToBool(v1.getIntVal()) && intToBool(v2.getIntVal())) ); 
		}else{
			return new NtsbValue( opStr(v1, v2, "&") );
		}
	}
	public abstractValue or(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( boolToInt(intToBool(v1.getIntVal()) || intToBool(v2.getIntVal())) ); 
		}else{
			return new NtsbValue( opStr(v1, v2, "|") );
		}
	}
	public abstractValue xor(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( boolToInt(intToBool(v1.getIntVal()) ^ intToBool(v2.getIntVal())) ); 
		}else{
			return new NtsbValue( opStr(v1, v2, "^") );
		}
	}
	public abstractValue gt(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( v1.getIntVal() > v2.getIntVal() ); 
		}else{
			return new NtsbValue( opStr(v1, v2, ">") );
		}
	}
	public abstractValue lt(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( v1.getIntVal() < v2.getIntVal() ); 
		}else{
			return new NtsbValue( opStr(v1, v2, "<") );
		}
	}
	public abstractValue ge(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( v1.getIntVal() >= v2.getIntVal() ); 
		}else{
			return new NtsbValue( opStr(v1, v2, ">=") );
		}
	}
	
	public abstractValue le(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( v1.getIntVal() <= v2.getIntVal() ); 
		}else{
			return new NtsbValue( opStr(v1, v2, "<=") );
		}
	}
	
	public abstractValue eq(abstractValue v1, abstractValue v2){
		if( v1.hasIntVal() && v2.hasIntVal() ){
			return new NtsbValue( v1.getIntVal() == v2.getIntVal() ); 
		}else{
			return new NtsbValue( opStr(v1, v2, "==") );
		}
	}

	public abstractValue arracc(abstractValue arr, abstractValue idx){
		assert false; return null;
	}
	public abstractValue arracc(abstractValue arr, abstractValue idx, abstractValue len){
		if(len != null) assert len.hasIntVal() && len.getIntVal() == 1 : "NYI";
		
		if( idx.hasIntVal() ){
			int iidx = idx.getIntVal() ;
			int size = arr.getVectValue().size();
			if( iidx < 0 || iidx >= size  )
				throw new ArrayIndexOutOfBoundsException("ARRAY OUT OF BOUNDS !(0<=" + iidx + " < " + size);
			return arr.getVectValue().get(idx.getIntVal());
		}else{
			return new NtsbValue( "(" + arr + "[" + idx + "])" );
		}		
	}
	
	public abstractValue cast(abstractValue v1, Type type){
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
				return new NtsbValue(val);
	    	}else{
	    		return new NtsbValue(result);
	    	}
		}
		assert false : "Can't cast " + v1 + " into " + type;
		return null;
	}
	
	public abstractValue not(abstractValue v1){
		if( v1.hasIntVal() ){
			return new NtsbValue( 1-v1.getIntVal()  ); 
		}else{
			return new NtsbValue( "( ! " + v1 + ")" );
		}
	}
	public abstractValue neg(abstractValue v1){
		if( v1.hasIntVal() ){
			return new NtsbValue( -v1.getIntVal()  ); 
		}else{
			return new NtsbValue( "( -" + v1 + ")" );
		}
	}
	
	public abstractValue join(abstractValue v1, abstractValue v2){
		if( v1.equals(v2) ){
			return v1;
		}else{
			return this.BOTTOM();
		}
	}
	public abstractValue condjoin(abstractValue cond, abstractValue vtrue, abstractValue vfalse){
		if( cond.hasIntVal() ){
			if( cond.getIntVal() != 0){
				return vtrue;
			}else{
				return vfalse;
			}
		}else{
			return new NtsbValue( "(" + cond + "?" + vtrue + ":" + vfalse + ")" ); 
		}
	}
	
	public void Assert(abstractValue val){
		 out.print ("assert (" + val + ");\n");
	}
	public abstractValue funcall(String name, List<abstractValue> params , List<String> outputs){
		assert false; return null;
	}
	
	public varState cleanState(String var, Type t){
		return new NtsbState(var, t, this);
	}
}
