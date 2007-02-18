package streamit.frontend.experimental.nodesToSB;

import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import streamit.frontend.experimental.*;
import streamit.frontend.tosbit.valueClass;




public class NtsbValue extends abstractValue {
	public static final int BOTTOM=0;	
	public static final int INT=1;
	public static final int LIST=2;
	public boolean isLHS=false;
	private String name = null; // name is only used when it is LHS.
	private int type;	
	private Object obj;
	private int lhsIdx;
	
	public abstractValue clone(){	
		return new NtsbValue(this);
	}
	
	public int getlhsIdx(){
		return lhsIdx;
	}
	
	public int getrhsIdx(){
		return lhsIdx-1;
	}
	
	public NtsbValue(String name, boolean isLHS){
		this.lhsIdx = 1;
		this.obj = name;
		if( isLHS  ){
			this.name = name;
		}
		this.type = BOTTOM;
		assert name != null : "This should never happen!!!! Name should never be null.";
		this.isLHS = isLHS;
	}
	
	public NtsbValue(NtsbValue n){
		this.lhsIdx = n.lhsIdx;
		this.obj = n.obj;
		this.type = n.type;
		this.isLHS = n.isLHS;
		this.name = n.name;
	}
	
	public NtsbValue(String name){
		this.lhsIdx = 1;
		this.obj = name;
		this.type = BOTTOM;
		assert name != null : "This should never happen!!!! Name should never be null.";
	}
	
	public NtsbValue(){
		this.lhsIdx = 1;
		this.obj = null;
		this.type = BOTTOM;
	}
	public NtsbValue(Object obj, int type){
		this.lhsIdx = 1;
		this.obj = obj;
		this.type = type;
	}
	
	public NtsbValue(List<abstractValue> obj){
		this.lhsIdx = 1;
		this.obj = obj;
		this.type = LIST;
	}
	public NtsbValue(boolean obj){
		this.lhsIdx = 1;
		this.obj = obj? new Integer(1) : new Integer(0);
		this.type = INT;
	}
	public NtsbValue(int obj){
		this.lhsIdx = 1;
		this.obj = obj;
		this.type = INT;
	}
	public boolean hasValue(){
		return type != BOTTOM;
	}
	public boolean isVect(){
		return type == LIST;
	}
	public int getIntVal(){
		assert type == INT : "Incorrect value type. Asking for int from " + this;
		return ((Integer)this.obj).intValue();
	}
	
	@SuppressWarnings("unchecked")
	public List<abstractValue> getVectValue(){
		assert type == LIST : "Incorrect value type";
		return (List<abstractValue>)obj;
	}
	public String toString(){
		switch(type){
		case INT: return obj.toString();
		case LIST: {
			String rval = "$ ";
			for(Iterator<abstractValue> it = getVectValue().iterator(); it.hasNext(); ){
				rval += it.next().toString() + " ";
			}
			rval += "$";
			return rval;
		}
		case BOTTOM:{ 
			if( isLHS ){
				return  name + "_" +  this.getrhsIdx();
			}else{
				if( obj != null ){
					return obj.toString();
				}	
			}
		}
		}
		return "NULL";
	}
	public boolean isBottom(){
		return !hasValue();
	}	
	public boolean hasIntVal(){
		return type == INT;
	}
	public void update(abstractValue v){
		lhsIdx ++;
		assert v instanceof NtsbValue;
		assert ((NtsbValue)v).type == type  || ((NtsbValue)v).type == BOTTOM || type == BOTTOM : "Updating with incompatible values " +  v + " <> " + this;
		{
			obj = ((NtsbValue)v).obj;
			type = ((NtsbValue)v).type;	
		}
	}
}
