package streamit.frontend.experimental.nodesToSB;

import java.io.PrintStream;
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

public class NtsbVtype extends IntVtype {
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
	
	public abstractValue BOTTOM(String label){
		return new NtsbValue(label);
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
	
	public abstractValue CONST(boolean v){
		return new NtsbValue(v); 
	}
	
	public abstractValue ARR(List<abstractValue> vals){
		return new NtsbValue(vals);		
	}
	
	
	public void Assert(abstractValue val){
		 out.print ("assert (" + val + ");\n");
	}
	
	public varState cleanState(String var, Type t){
		return new NtsbState(var, t, this);
	}
}
