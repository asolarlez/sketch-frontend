package sketch.compiler.dataflow.nodesToSB;

import java.util.Iterator;
import java.util.List;

import sketch.compiler.dataflow.abstractValue;

public class IntAbsValue extends abstractValue {

	public static final int BOTTOM = 0;
	public static final int INT = 1;
	public static final int LIST = 2;
	protected int type;
	protected Object obj;


	
	public boolean equals(Object obj){
		if( !(obj instanceof IntAbsValue) ) return false;
		IntAbsValue v2 = (IntAbsValue) obj;
		switch(type){
		case INT: 
			if(v2.type != INT  ) return false;
			return v2.obj.equals(obj);			
		case LIST: {
			if(v2.type != LIST) return false;			
			Iterator<abstractValue> it1 = getVectValue().iterator();
			Iterator<abstractValue> it2 = v2.getVectValue().iterator();
			while( it2.hasNext()){
				if(!it1.hasNext()) return false;
				if( !it1.next().equals(it2.next()) ) return false;
			}
			return true;
		}
		case BOTTOM:{ 
			return (v2.type == BOTTOM );
		}
		}
		return false;
	}
	
	
	public abstractValue clone(){
		return new IntAbsValue(this);
	}
	
	public IntAbsValue(IntAbsValue n){
		this.obj = n.obj;
		this.type = n.type;
		this.isVolatile = n.isVolatile;
	}
	
	public IntAbsValue(String label){
		this.obj = label;
		this.type = BOTTOM;
		assert label != null : "This should never happen!!!! Name should never be null.";
	}
	
	public IntAbsValue(){
		this.obj = null;
		this.type = BOTTOM;
	}
	
	public IntAbsValue(List<abstractValue> obj){
		this.obj = obj;
		this.type = LIST;
	}
	
	public IntAbsValue(boolean obj){
		this.obj = obj? new Integer(1) : new Integer(0);
		this.type = INT;
	}
	
	public IntAbsValue(int obj){
		this.obj = obj;
		this.type = INT;
	}
	
	public boolean hasValue() {
		return type != BOTTOM;
	}

	public boolean isVect() {
		return type == LIST;
	}

	public int getIntVal() {
		assert type == INT : "Incorrect value type. Asking for int from " + this;
		return ((Integer)this.obj).intValue();
	}

	@SuppressWarnings("unchecked")
	public List<abstractValue> getVectValue() {
		assert type == LIST : "Incorrect value type";
		return (List<abstractValue>)obj;
	}

	public boolean isBottom() {
		return !hasValue();
	}

	public boolean hasIntVal() {
		return type == INT;
	}

	@Override
	public void makeVolatile(){
		super.makeVolatile();
		this.obj = null;
		this.type = BOTTOM;
	}
	
	public void update(abstractValue v){
		if(isVolatile){ return; }// If the variable is volatile, the update has no effect.
		assert v instanceof IntAbsValue;
		IntAbsValue ntsv = ((IntAbsValue)v);
		assert ntsv.type == type  || ntsv.type == BOTTOM || type == BOTTOM : "Updating with incompatible values " +  v + " <> " + this;
		{
			obj = ntsv.obj;
			type = ntsv.type;	
		}
	}
	public String toString(){
		switch(type){
		case INT: return obj.toString()+ (isVolatile ? "_v" : "");
		case LIST: {
			String rval = "$ ";
			int i=0;
			for(Iterator<abstractValue> it = getVectValue().iterator(); it.hasNext(); ){
				rval += it.next().toString() + " ";
				if(++i>10){ rval += "..." ; break; }
			}
			rval += "$";
			return rval;
		}
		case BOTTOM:{ 
			if( obj != null ){
				if(obj.toString().length() > 10){
					return "BOTTOM" + (isVolatile ? "_v" : "");
				}
				return obj.toString() + (isVolatile ? "_v" : "");
			}else{
				return "BOTTOM"+ (isVolatile ? "_v" : "");
			}
		}
		}
		return "NULL";
	}

}
