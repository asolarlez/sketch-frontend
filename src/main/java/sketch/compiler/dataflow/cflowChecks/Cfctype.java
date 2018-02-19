package sketch.compiler.dataflow.cflowChecks;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssume;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.dataflow.MethodState;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.abstractValueType;
import sketch.compiler.dataflow.varState;
import sketch.util.exceptions.TypeErrorException;

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

    public abstractValue TUPLE(List<abstractValue> vals, String name) {
        return allinit;
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

    public abstractValue RCONST(double v) {
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


    public void Assert(abstractValue val, StmtAssert stmt) {
        if(val.equals(noinit)){
            throw new RuntimeException("Asserting an uninitialized variable " +
                    stmt.getMsg());
        }       
    }

    @Override
    public void Assume(abstractValue val, StmtAssume stmt) {
        if (val.equals(noinit)) {
            throw new RuntimeException("Assuming an uninitialized variable " +
                    stmt.getMsg());
        }
    }

    public varState cleanState(String var, Type t, MethodState mstate){
        return new CfcState(t, this);
    }

    public abstractValue plus(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue minus(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue times(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue over(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue mod(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }



    public abstractValue shr(abstractValue v1, abstractValue v2){
        return mix(v1, v2);
    }

    public abstractValue shl(abstractValue v1, abstractValue v2){
        return mix(v1, v2);
    }


    public abstractValue and(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue or(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue xor(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue gt(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue lt(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue ge(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue le(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue eq(abstractValue v1, abstractValue v2) {
        return mix(v1, v2);
    }

    public abstractValue tupleacc(abstractValue tuple, abstractValue idx) {
        return mix(tuple, idx);
    }
    public abstractValue arracc(abstractValue arr, abstractValue idx) {
        return mix(arr, idx);
    }
    
    
    public abstractValue outOfBounds(){
        return BOTTOM("OUT OF BOUNDS");
    }

    public abstractValue arracc(abstractValue arr, abstractValue idx, abstractValue len, boolean isUnchecked) {
        return mix(arr, idx);
    }


    protected abstractValue rawArracc(abstractValue arr, abstractValue idx){
        return mix(arr, idx);
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

    public abstractValue mix(abstractValue v1, abstractValue v2) {
        CfcValue cv1 = (CfcValue) v1;
        CfcValue cv2 = (CfcValue) v2;        
        if(cv1.maybeinit() && cv2.maybeinit()){
            return someinit;
        }else{
            return noinit;
        }       
    }
    
    public abstractValue join(abstractValue v1, abstractValue v2) {
        CfcValue cv1 = (CfcValue) v1;
        CfcValue cv2 = (CfcValue) v2;
        if(cv1.maybeinit() || cv2.maybeinit()){
            return someinit;
        }else{
            return noinit;
        }       
    }


    public abstractValue ternary(abstractValue cond, abstractValue vtrue, abstractValue vfalse) {
        return mix(cond, join(vtrue, vfalse));
    }

    public abstractValue condjoin(abstractValue cond, abstractValue vtrue, abstractValue vfalse) {
        return join(cond, join(vtrue, vfalse));
    }

    public void funcall(Function fun, List<abstractValue> avlist,
            List<abstractValue> outSlist, abstractValue patchCond, MethodState state,
            int clusterId)
    {
        Iterator<Parameter> formalParams = fun.getParams().iterator();
        int idx = 0;
        while(formalParams.hasNext()){
            Parameter param = formalParams.next();
            if( param.isParameterOutput()){
                outSlist.add(CONST(1));
            }

            if (param.isParameterInput()) {
                if (!param.isParameterOutput()) {
                    if (!((CfcValue) avlist.get(idx)).maybeinit()) {
                        throw new TypeErrorException("Parameter " + param.getName() +
                                " to " + fun.getName() +
                                " is used without first being initialized.", param);
                    }
                }
                ++idx;
            }           
        }

    }

    @Override
    public abstractValue ADTnode(Map<String, Map<String, abstractValue>> cases) {
        // TODO xzl should we refine this?
        return BOTTOM();
    }

}
