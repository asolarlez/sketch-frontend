package streamit.frontend.experimental;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Map.Entry;

abstract public class varState {	
	private int maxSize = -1;
	private TreeMap<Integer, abstractValue> arrElems=null;
	private abstractValue absVal = null;
	private varState parent = null;	
	
	
	public varState rootParent(){
		if( parent == null){
			return this;
		}else{
			return parent.rootParent();
		}
	}
	
	final public abstractValue state(abstractValueType vtype){
		if( arrElems != null){
			int sz = this.maxKey();
			List<abstractValue> avl = new ArrayList<abstractValue>(sz);
			for(int i=0; i<sz; ++i){
				if( this.hasKey(i)  ){
					avl.add(this.state(i));
				}else{
					avl.add(newLHSvalue(i));
				}
			}			
			return vtype.ARR(avl);
		}else{
			return absVal;	
		}
	}
	
	
	protected void init(abstractValue absVal){
		this.absVal = absVal;
	}
	protected void init(int size){
		maxSize = size;
		arrElems = new TreeMap<Integer, abstractValue>();
	}
	public varState(){
		
	}
	public varState(abstractValue absVal){
		init(absVal);
	}
	public varState(int size){
		init(size);
	}
	
	final public int maxKey(){
		if(maxSize >= 0) return maxSize;
		int lmax = arrElems.lastKey();
		if( parent == null)
			return lmax;
		else
			return Math.max(parent.maxKey(), lmax);
	}
	
	final public boolean hasKey(int idx){
		if(arrElems.containsKey(idx)){
			return true;
		}else{
			if( parent != null){
				return parent.hasKey(idx);
			}
		}
		return false;
	}
	
	final public abstractValue state(int i){ 
		if(arrElems.containsKey(i)){
			return arrElems.get(i);
		}else{
			if( parent != null){
				return parent.state(i);
			}
		}
		abstractValue newVal = newLHSvalue(i);
		arrElems.put(i, newVal);
		return newVal;
	}
	
	
	public void update(abstractValue val, abstractValueType vtype){
		if( arrElems != null){
			if(  val.isVect() ){
				int lv = this.maxKey();
				List<abstractValue> vlist = val.getVectValue();
				for(int i=0; i<lv ; ++i){
					abstractValue cv;
					if( i < vlist.size() ){
						cv = vlist.get(i);	
					}else{
						cv = vtype.CONST(0);
					}
					update(vtype.CONST(i), cv, vtype);
				}
			}else{
				int lv = this.maxKey();
				update(vtype.CONST(0), val, vtype);
				for(int i=1; i<lv ; ++i){
					abstractValue cv;
					cv = vtype.CONST(0);
					update(vtype.CONST(i), cv, vtype);
				}
			}
		}else{
			absVal.update(val);
		}
	}
	
	public void update(abstractValue idx, abstractValue val, abstractValueType vtype){
		assert arrElems != null;
		if( idx.hasIntVal() ){
			int iidx = idx.getIntVal();
			if( arrElems.containsKey(iidx) ){
				arrElems.get(iidx).update(val);
			}else{
				abstractValue newVal = newLHSvalue(iidx);
				arrElems.put(iidx, newVal);
				newVal.update(val);
			}
		}else{
			abstractValue bottom = vtype.BOTTOM();
			int lv = this.maxKey();
			for(int i=0; i<lv ; ++i){
				// update(vtype.CONST(i), bottom, vtype);
				if( arrElems.containsKey(i) ){
					arrElems.get(i).update(bottom); // This could be more precise by doing a condjoin between the current value and prevvalue on cond (idx == i).
				}else{
					abstractValue newVal = newLHSvalue(i);
					arrElems.put(i, newVal);
					newVal.update(bottom);
				}
			}
			/*
			for(Iterator<Entry<Integer, abstractValue>> it = arrElems.entrySet().iterator(); it.hasNext(); ){
				Entry<Integer, abstractValue> entry = it.next();
				entry.getValue().update( bottom );
			}
			*/
		}
	}
	
	abstract public varState getDeltaClone(abstractValueType vt);
	abstract public abstractValue newLHSvalue();
	abstract public abstractValue newLHSvalue(int i);
	
	protected void helperDeltaClone(varState parent, abstractValueType vt){
		this.maxSize = parent.maxSize;
		this.parent = parent;
		if( parent.arrElems != null ){
			arrElems = new TreeMap<Integer, abstractValue>();
		}else{
			absVal = parent.absVal.clone();
		}
	}
		
	/**
	 * If the condition is true, then the resulting value will be val. If it's false, the value is left as it is.
	 * If it is unknown, then we just get a join of the two things.
	 * @param cond condition.
	 * @param val varState we want when the condition is true.
	 * @param vt abstractValueType.
	 * @return
	 */
	final public varState condjoin(abstractValue cond, varState val, abstractValueType vt){
		//WARNING: This call poisons val.
		//After a call to this function, val should not be used at all
		varState rv = getDeltaClone(vt);		
		if( rv.isArr() ){
			assert val.isArr() : "NYS";
			for(Iterator<Entry<Integer, abstractValue>>  thIt = arrElems.entrySet().iterator(); thIt.hasNext(); ){
				Entry<Integer, abstractValue> toUd = thIt.next();
				int idx = toUd.getKey();
				if( val.arrElems.containsKey( idx ) ){
					//update(vt.CONST( idx ), vt.condjoin(cond, toUd.getValue() , val.arrElems.get(idx) ), vt );
					rv.arrElems.put(idx,  vt.condjoin(cond, val.arrElems.get(idx), toUd.getValue() ));
					// toUd.getValue().update( vt.condjoin(cond, toUd.getValue() , val.arrElems.get(idx) ) );
					val.arrElems.remove(idx);
				}else{
					rv.arrElems.put(idx,  vt.condjoin(cond, val.state(idx), toUd.getValue() ));
					//Nothing to do here, since we would be updating to the same value.
				}
			}
			for(Iterator<Entry<Integer, abstractValue>>  thIt = val.arrElems.entrySet().iterator(); thIt.hasNext(); ){
				Entry<Integer, abstractValue> toUd = thIt.next();
				int idx = toUd.getKey();
				rv.arrElems.put(idx,  vt.condjoin(cond, toUd.getValue(), state(idx) ));
			}
		}else{
			assert !val.isArr() : " Can't assign an array into a non-array";			
			//update( vt.condjoin(cond, state() , val.state()) );
			rv.absVal = vt.condjoin(cond, val.state(vt) , state(vt)) ;
		}
		val.arrElems = null;
		val.absVal = null;
		return rv;
	}
	
	/**
	 * returns true if vs equals this. 
	 * @param vs
	 * @return
	 */
	public boolean compare(varState val, abstractValueType vt){
		//WARNING: This call poisons val.
		//After a call to this function, val should not be used at all
		if( this.isArr() ){
			if(!val.isArr()) return false;
			for(Iterator<Entry<Integer, abstractValue>>  thIt = arrElems.entrySet().iterator(); thIt.hasNext(); ){
				Entry<Integer, abstractValue> toUd = thIt.next();
				int idx = toUd.getKey();
				if( val.arrElems.containsKey( idx ) ){
					if( !val.arrElems.get(idx).equals(toUd.getValue()) ) return false;
					val.arrElems.remove(idx);
				}else{
					if( !val.state(idx).equals(toUd.getValue()) ) return false;					
					//Nothing to do here, since we would be updating to the same value.
				}
			}
			for(Iterator<Entry<Integer, abstractValue>>  thIt = val.arrElems.entrySet().iterator(); thIt.hasNext(); ){
				Entry<Integer, abstractValue> toUd = thIt.next();
				int idx = toUd.getKey();
				if(!arrElems.containsKey(idx)){
					if( !toUd.getValue().equals( state(idx) ) ) return false;
				}
			}
		}else{
			if(val.isArr()) return false;
			if( !val.state(vt).equals( state(vt) ) ) return false;
		}
		return true;
	}
	
	final public boolean isArr(){		
		return arrElems != null;
	}
	
	final public Iterator<Entry<Integer, abstractValue>> iterator(){
		return arrElems.entrySet().iterator();
	}
	
	public String toString(){
		if( arrElems != null ){
			String fu = "{";
			for(Iterator<Entry<Integer, abstractValue>>  thIt = arrElems.entrySet().iterator(); thIt.hasNext(); ){
				Entry<Integer, abstractValue> toUd = thIt.next();
				fu += toUd.getKey() + ":" + toUd.getValue().toString() + ", "; 
			}
			fu += "}";
			return fu;
		}else{
			if(absVal != null)
				return absVal.toString();
			if(parent != null )
				return "son of:" + parent.toString();
		}
		return "NULL";
	}
	
}
