package streamit.frontend.experimental.cflowChecks;

import java.util.ArrayList;
import java.util.Collections;
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
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;

public class Cfctype extends abstractValueType {

	public static final CfcValue allinit = new CfcValue(CfcValue.allinit);
	public static final CfcValue someinit = new CfcValue(CfcValue.someinit);
	public static final CfcValue noinit = new CfcValue(CfcValue.noinit);

	public abstractValue STAR(FENode star){
		return allinit;
	}

	public abstractValue BOTTOM(Type t){
		if( t instanceof TypePrimitive ){
			return BOTTOM();
		}
		assert false;
		return null;
	}


	public abstractValue ARR(List<abstractValue> vals){
		return allinit;
	}

	public abstractValue BOTTOM(){
		return allinit;
	}

	public abstractValue BOTTOM(String label){
		return allinit;
	}

	public abstractValue CONST(int v){
		return allinit;
	}

	public abstractValue NULL(){
		return allinit;
	}

	public abstractValue CONST(boolean v){
		return allinit;
	}


	public void Assert(abstractValue val, String msg){
		assert val.equals(allinit) : "Asserting an uninitialized variable";
	}


	public varState cleanState(String var, Type t, MethodState mstate){
		return new CfcState(t, this);
	}

	public abstractValue plus(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue minus(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue times(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue over(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue mod(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}



	public abstractValue shr(abstractValue v1, abstractValue v2){
		return join(v1, v2);
	}

	public abstractValue shl(abstractValue v1, abstractValue v2){
		return join(v1, v2);
	}


	public abstractValue and(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue or(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue xor(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue gt(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue lt(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue ge(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue le(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue eq(abstractValue v1, abstractValue v2) {
		return join(v1, v2);
	}

	public abstractValue arracc(abstractValue arr, abstractValue idx) {
		return join(arr, idx);
	}
	
	
	public abstractValue outOfBounds(){
		return BOTTOM("OUT OF BOUNDS");
	}

	public abstractValue arracc(abstractValue arr, abstractValue idx, abstractValue len, boolean isUnchecked) {
		return join(arr, idx);
	}


	protected abstractValue rawArracc(abstractValue arr, abstractValue idx){
		return join(arr, idx);
	}

	public abstractValue cast(abstractValue v1, Type type) {
		return v1;
	}

	public abstractValue not(abstractValue v1) {
		return v1;
	}

	public abstractValue neg(abstractValue v1) {
		return v1;
	}

	public abstractValue join(abstractValue v1, abstractValue v2) {
		CfcValue cv1 = (CfcValue) v1;
		CfcValue cv2 = (CfcValue) v2;
		if(cv1.isallinit()){
			if(cv2.isallinit()){
				return allinit;
			}else{
				return someinit;
			}
		}else{
			if(cv2.isallinit()){
				return someinit;
			}else{
				return noinit;				
			}
		}
	}


	public abstractValue ternary(abstractValue cond, abstractValue vtrue, abstractValue vfalse) {
		return join(cond, join(vtrue, vfalse));
	}

	public abstractValue condjoin(abstractValue cond, abstractValue vtrue, abstractValue vfalse) {
		return join(cond, join(vtrue, vfalse));
	}

	public void funcall(Function fun, List<abstractValue> avlist, List<abstractValue> outSlist, abstractValue patchCond){
		Iterator<Parameter> formalParams = fun.getParams().iterator();
		int idx = 0;
    	while(formalParams.hasNext()){
    		Parameter param = formalParams.next();
    		if( param.isParameterOutput()){
    			outSlist.add(CONST(1));
    		}else{
    			assert ((CfcValue)avlist.get(idx)).isallinit(): "One of the input parameters was not initialized";
    			++idx;
    		}    		
    	}
	}

}
