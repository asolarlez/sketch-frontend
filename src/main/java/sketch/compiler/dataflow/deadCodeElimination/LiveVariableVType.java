package streamit.frontend.experimental.deadCodeElimination;

import java.util.Iterator;
import java.util.List;

import streamit.frontend.experimental.MethodState;
import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.abstractValueType;
import streamit.frontend.experimental.varState;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Type;

public class LiveVariableVType extends abstractValueType {

	static public LiveVariableVType vtype = new LiveVariableVType();
	
	@Override
	public abstractValue ARR(List<abstractValue> vals) {
		LVSet lv = new LVSet();
		Iterator<abstractValue> it = vals.iterator();
		while(it.hasNext()){
			abstractValue av = it.next();
			if( av instanceof LVSet){
				lv.set.addAll(((LVSet)av).set );
				continue;
			}
			if( av instanceof LiveVariableAV){
				lv.set.add( (LiveVariableAV) av );
				continue;
			}
			assert false;			
		}		
		return null;
	}

	@Override
	public void Assert(abstractValue val) {
		if( val instanceof LVSet){					
			((LVSet)val).enliven();
		}
		if( val instanceof LiveVariableAV){
			LiveVariableAV lv = (LiveVariableAV) val;
			if(lv.mstate != null  ){
				lv.mstate.setVarValue(lv.mstate.untransName(lv.name), new joinAV( LiveVariableAV.LIVE));
			}			
		}

	}

	@Override
	public abstractValue BOTTOM() {		
		return new LiveVariableAV("BOTTOM", null);
	}

	@Override
	public abstractValue BOTTOM(Type t) {
		return BOTTOM();
	}

	@Override
	public abstractValue CONST(int v) {
		return BOTTOM();
	}

	@Override
	public abstractValue STAR(FENode star) {
		return BOTTOM();
	}

	@Override
	public abstractValue and(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue arracc(abstractValue arr, abstractValue idx) {
		LVSet lv = new LVSet();
		if( arr instanceof LVSet){
			lv.set.addAll(((LVSet)arr).set );
		}
		if( arr instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) arr );
		}
		if( idx instanceof LVSet){
			lv.set.addAll(((LVSet)idx).set );
		}
		if( idx instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) idx );
		}
		return lv;
	}

	@Override
	public abstractValue arracc(abstractValue arr, abstractValue idx,
			abstractValue len, boolean isUnchecked) {
		LVSet lv = new LVSet();
		if( arr instanceof LVSet){
			lv.set.addAll(((LVSet)arr).set );
		}
		if( arr instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) arr );
		}
		if( idx instanceof LVSet){
			lv.set.addAll(((LVSet)idx).set );
		}
		if( idx instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) idx );
		}
		if( len instanceof LVSet){
			lv.set.addAll(((LVSet)len).set );
		}
		if( len instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) len );
		}
		return lv;
		
	}

	@Override
	public abstractValue cast(abstractValue v1, Type t) {		
		return v1;
	}

	@Override
	public varState cleanState(String var, Type t, MethodState mstate) {
		return new LiveVariableVarState(var,t, mstate);
	}

	@Override
	public abstractValue condjoin(abstractValue cond, abstractValue vtrue,
			abstractValue vfalse) {
		if(cond != null ){
			if( cond instanceof LVSet){
				((LVSet)cond).enliven();
			}
			if( cond instanceof LiveVariableAV){
				LiveVariableAV lv = (LiveVariableAV) cond;
				lv.mstate.setVarValue(lv.mstate.untransName(lv.name), new joinAV( LiveVariableAV.LIVE));		
			}
		}
		
		assert vtrue instanceof LiveVariableAV && vfalse instanceof LiveVariableAV ;
		
		if( ((LiveVariableAV)vtrue).getLiveness() == LiveVariableAV.DEAD && 
			 ((LiveVariableAV)vfalse).getLiveness() == LiveVariableAV.DEAD){
			return new joinAV(LiveVariableAV.DEAD);
		}else{
			return new joinAV(LiveVariableAV.LIVE);
		}
	}

	@Override
	public abstractValue eq(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;

	}

	@Override
	public void funcall(Function fun, List<abstractValue> avlist,
			List<abstractValue> outSlist) {
		
		
		
		Iterator<Parameter> formalParams = fun.getParams().iterator();		
		LVSet lv = new LVSet();
		Iterator<abstractValue> it = avlist.iterator();
		while(it.hasNext()){
			abstractValue av = it.next();
			if( av instanceof LVSet){
				lv.set.addAll(((LVSet)av).set );
				continue;
			}
			if( av instanceof LiveVariableAV){
				lv.set.add( (LiveVariableAV) av );
				continue;
			}
			assert false;			
		}
		
		boolean hasOP = false;
    	while(formalParams.hasNext()){
    		Parameter param = formalParams.next();    	
    		if( param.isParameterOutput()){
    			outSlist.add(lv);
    			hasOP = true;
    		}
    	}
    	assert hasOP : "This is dangerous.";
	}

	@Override
	public abstractValue ge(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue gt(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue le(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue lt(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue minus(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue mod(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue neg(abstractValue v1) {
		return v1;
	}

	@Override
	public abstractValue not(abstractValue v1) {
		return v1;
	}

	@Override
	public abstractValue or(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue over(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue plus(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue shl(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue shr(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue times(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

	@Override
	public abstractValue xor(abstractValue v1, abstractValue v2) {
		LVSet lv = new LVSet();
		if( v1 instanceof LVSet){
			lv.set.addAll(((LVSet)v1).set );
		}
		if( v1 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v1 );
		}
		if( v2 instanceof LVSet){
			lv.set.addAll(((LVSet)v2).set );
		}
		if( v2 instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) v2 );
		}
		return lv;
	}

}
