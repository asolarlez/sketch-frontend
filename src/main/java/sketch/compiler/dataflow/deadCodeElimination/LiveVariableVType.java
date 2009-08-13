package sketch.compiler.dataflow.deadCodeElimination;

import java.util.Iterator;
import java.util.List;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.MethodState;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.abstractValueType;
import sketch.compiler.dataflow.varState;

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
	public void Assert(abstractValue val, String msg) {
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
	public abstractValue BOTTOM(String label) {
		return BOTTOM();
	}

	@Override
	public abstractValue CONST(int v) {
		return BOTTOM();
	}
	public abstractValue NULL() {
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

	
	
	public abstractValue ternary(abstractValue cond, abstractValue vtrue,
			abstractValue vfalse) {
		LVSet lv = new LVSet();
		if( cond instanceof LVSet){
			lv.set.addAll(((LVSet)cond).set );
		}
		if( cond instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) cond );
		}
		if( vtrue instanceof LVSet){
			lv.set.addAll(((LVSet)vtrue).set );
		}
		if( vtrue instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) vtrue );
		}
		
		if( vfalse instanceof LVSet){
			lv.set.addAll(((LVSet)vfalse).set );
		}
		if( vfalse instanceof LiveVariableAV){
			lv.set.add( (LiveVariableAV) vfalse );
		}
		return lv;
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
				if(lv.mstate != null){
					lv.mstate.setVarValue(lv.mstate.untransName(lv.name), new joinAV( LiveVariableAV.LIVE));
				}
			}
		}
		
		assert vtrue instanceof LiveVariableAV && vfalse instanceof LiveVariableAV ;
		LiveVariableAV vt = ((LiveVariableAV)vtrue);
		LiveVariableAV vf = ((LiveVariableAV)vfalse);
		if( vt.getLiveness() == LiveVariableAV.DEAD && 
			 vf.getLiveness() == LiveVariableAV.DEAD){
			if(vt.hasBeenLive() || vf.hasBeenLive()){
				return new joinAV(LiveVariableAV.HBLDEAD);	
			}
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
			List<abstractValue> outSlist, abstractValue pathCond) {
		
		
		
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
    	lv.enliven();
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
