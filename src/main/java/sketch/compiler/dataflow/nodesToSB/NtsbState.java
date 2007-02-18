package streamit.frontend.experimental.nodesToSB;

import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.abstractValueType;
import streamit.frontend.experimental.varState;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;

public class NtsbState extends varState {
	private final String name;
	private final Type t;
	NtsbVtype vtype;
	
	public abstractValue newLHSvalue(){
		return new NtsbValue(name, true);
	}
	
	public abstractValue newLHSvalue(int i){
		return new NtsbValue(name + "_idx_" + i, true);
	}
	
	NtsbState(String name, Type t, NtsbVtype vtype){		
		this.name = name;
		this.t = t;
		this.vtype = vtype;
		if( t instanceof  TypePrimitive){
			init( newLHSvalue() );
		}
		
		if( t instanceof TypeArray  ){
			TypeArray tarr = (TypeArray) t;
			abstractValue av = (abstractValue) tarr.getLength().accept( vtype.eval );
			if( av.hasIntVal() ){
				init( av.getIntVal() );
			}else{
				init( -1 );
			}
		}
	}
	
	NtsbValue val(){
		return (NtsbValue)this.state(vtype);
	}
	NtsbValue val(int i){
		return (NtsbValue)this.state(i);
	}
	
	public varState getDeltaClone(){
		NtsbState st  = new NtsbState(name, t, vtype);
		st.helperDeltaClone(this, vtype);
		return st;
	}
	
	public void update(abstractValue val, abstractValueType vt){
		if(! this.isArr() ){
			if(! val.hasIntVal() )
				vtype.out.println( name + "_" +  val().getlhsIdx() + " = " + val + ";");	
		}
		super.update(val, vtype);
	}
	
	public void update(abstractValue idx, abstractValue val, abstractValueType vt){
		if( idx.hasIntVal() ){
			int iidx = idx.getIntVal();
			NtsbValue lhsval = val(iidx);
			String result  =  name + "_idx_" + iidx + "_" +  lhsval.getlhsIdx() + " = " + val + ";";
			vtype.out.println( result );
		}else{
			int lk = maxKey();
			vtype.out.print("$ ");
			for(int i=0; i<lk; ++i){
				vtype.out.print(" " + name + "_idx_" + i + "_" +  val(i).getlhsIdx() );
			}
			vtype.out.print("$$ ");
			for(int i=0; i<lk; ++i){
				if( this.hasKey(i) ){
					vtype.out.print(" " +  this.val(i));
				}else{
					vtype.out.print(" " + 0 );
				}
			}
			vtype.out.print("$[ " + idx + "]=" +  val +";");
		}		
		super.update(idx, val, vtype);
	}
}
