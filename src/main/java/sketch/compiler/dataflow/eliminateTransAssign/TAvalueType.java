package streamit.frontend.experimental.eliminateTransAssign;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import streamit.frontend.experimental.MethodState;
import streamit.frontend.experimental.abstractValue;
import streamit.frontend.experimental.abstractValueType;
import streamit.frontend.experimental.varState;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Type;

public class TAvalueType extends abstractValueType {
	public static TAvalueType  vtype = new TAvalueType();
	public static TAsupperBottom bottom = new TAsupperBottom();
	
	@Override
	public abstractValue ARR(List<abstractValue> vals) {
		return bottom;
	}

	@Override
	public void Assert(abstractValue val) {
	}

	@Override
	public abstractValue BOTTOM() {		
		return bottom;
	}

	@Override
	public abstractValue BOTTOM(Type t) {
		return bottom;
	}

	@Override
	public abstractValue CONST(int v) {
		return bottom;
	}

	@Override
	public abstractValue STAR(FENode star) {
		return bottom;
	}

	@Override
	public abstractValue and(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue arracc(abstractValue arr, abstractValue idx) { 
		return bottom;
	}

	@Override
	public abstractValue arracc(abstractValue arr, abstractValue idx,
			abstractValue len, boolean isUnchecked) {
		return bottom;
	}

	@Override
	public abstractValue cast(abstractValue v1, Type t) {
		return bottom;
	}

	@Override
	public varState cleanState(String var, Type t, MethodState mstate) {
		return new TAvarState(var, t, mstate);
	}

	@Override
	public abstractValue condjoin(abstractValue cond, abstractValue vtrue,
			abstractValue vfalse) {
		
		
		transAssignAbsValue taTrue = (transAssignAbsValue) vtrue;
		transAssignAbsValue taFalse = (transAssignAbsValue) vfalse;		
		assert taTrue.me.equals(taFalse.me) : "Their me should be equal";				
		transAssignAbsValue tmerged = new transAssignAbsValue(taTrue.me, taTrue.ms);		
		
		if(taTrue.varIamEqualTo != null && taTrue.varIamEqualTo.equals(taFalse.varIamEqualTo)  ){
			tmerged.varIamEqualTo =  taTrue.varIamEqualTo;
		}else{
			tmerged.varIamEqualTo = null;
		}
		Set<String> varsEqToMe = tmerged.varsEqToMe;
		Iterator<String> titer = taTrue.varsEqToMe.iterator();
		while(titer.hasNext()){
			String tnm = titer.next();			
			if( taFalse.varsEqToMe.contains(tnm)  ){
				varsEqToMe.add(tnm);
			}
		}
		taUpdater tau = new taUpdater(taUpdater.OVERWRITE, tmerged);
		return tau;
	}

	@Override
	public abstractValue eq(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public void funcall(Function fun, List<abstractValue> avlist,
			List<abstractValue> outSlist) {
		Iterator<Parameter> formalParams = fun.getParams().iterator();
    	while(formalParams.hasNext()){
    		Parameter param = formalParams.next();    	
    		if( param.isParameterOutput()){
    			outSlist.add(BOTTOM());
    		}
    	}
	}

	@Override
	public abstractValue ge(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue gt(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue le(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue lt(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue minus(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue mod(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue neg(abstractValue v1) {
		return bottom;
	}

	@Override
	public abstractValue not(abstractValue v1) {
		return bottom;
	}

	@Override
	public abstractValue or(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue over(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue plus(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue shl(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue shr(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue times(abstractValue v1, abstractValue v2) {
		return bottom;
	}

	@Override
	public abstractValue xor(abstractValue v1, abstractValue v2) {
		return bottom;
	}

}