package sketch.compiler.dataflow.cflowChecks;

import java.util.Iterator;
import java.util.List;

import sketch.compiler.dataflow.abstractValue;

public class CfcValue extends abstractValue {

	public static final int BOTTOM = 0;
	public static final int INT = 1;
	public static final int LIST = 2;
	
	static abstract class STATE{ abstract STATE join(STATE s);  };
	static class NOINIT extends STATE{ 
		private NOINIT(){}  
		public String toString(){ return "NOINIT";}
		STATE join(STATE s){ if(s instanceof NOINIT){ return this; }else{
			return someinit;			
		} }
	}
	static class ALLINIT extends STATE{ 
		private ALLINIT(){}
		public String toString(){ return "ALLINIT";}
		STATE join(STATE s){ if(s instanceof ALLINIT){ return this; }else{
			return someinit;			
		} }
	};	
	static class SOMEINIT extends STATE{ 
		private SOMEINIT(){}
		public String toString(){ return "SOMEINIT";}
		STATE join(STATE s){ return someinit; }
	};
	
	
	public static final NOINIT noinit = new NOINIT();
	public static final SOMEINIT someinit = new SOMEINIT();
	public static final ALLINIT allinit = new ALLINIT();
	
	
	protected int type;
	protected List obj;
	protected STATE state = noinit;


	public boolean isallinit(){
		switch(type){
		case INT:
			return state == allinit;
		case LIST:
			Iterator<abstractValue> it1 = getVectValue().iterator();
			while(it1.hasNext()){
				if(! ((CfcValue)it1.next()).isallinit()) return false;
			}
			return true;
		default:
				return false;
		}		
	}
	
	
	public boolean equals(Object obj){
		if( !(obj instanceof CfcValue) ) return false;
		CfcValue v2 = (CfcValue) obj;
		switch(type){
		case INT: 
			if(v2.type != INT  ) return false;
			return v2.state == state;			
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
		return new CfcValue(this);
	}
	
	public CfcValue(CfcValue n){
		this.obj = n.obj;
		this.state = n.state;
		this.type = n.type;
		this.isVolatile = n.isVolatile;
	}
		
	
	public CfcValue(){
		this.obj = null;
		this.type = BOTTOM;
	}
	
	public CfcValue(List<abstractValue> obj){
		this.obj = obj;
		this.type = LIST;
	}
			
	public CfcValue(STATE s){
		this.obj = null;
		this.state = s;
		this.type = INT;
	}
	
	public boolean hasValue() {
		return false;
	}

	public boolean isVect() {
		return type == LIST;
	}

	public int getIntVal() {
		assert false: "no int val";
		return 0;
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
		return false;
	}

	@Override
	public void makeVolatile(){
		super.makeVolatile();
		this.obj = null;
		this.type = BOTTOM;
	}
	
	public void update(abstractValue v){
		if(isVolatile){ return; }// If the variable is volatile, the update has no effect.
		assert v instanceof CfcValue;
		CfcValue ntsv = ((CfcValue)v);
		assert ntsv.type == type  || ntsv.type == BOTTOM || type == BOTTOM : "Updating with incompatible values " +  v + " <> " + this;
		{
			obj = ntsv.obj;
			type = ntsv.type;	
			state = ntsv.state;
		}
	}
	public String toString(){
		switch(type){
		case INT: return state.toString()+ (isVolatile ? "_v" : "");
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
