package sketch.compiler.dataflow.nodesToSB;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.dataflow.MethodState;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.varState;
import sketch.compiler.solvers.constructs.AbstractValueOracle;

public class NtsbVtype extends IntVtype {
    public PrintStream out;
    protected AbstractValueOracle oracle;   
    
    
    public NtsbVtype(AbstractValueOracle oracle, PrintStream out){
        this.oracle = oracle;
        this.out = out;     
    }
    
    
    public abstractValue outOfBounds(){
        return CONST(0);
    }
    
    protected Map<FENode, NtsbValue> memoizedValues = new HashMap<FENode, NtsbValue>();
    
    public abstractValue STAR(FENode node){     
        if(oracle.allowMemoization()){ 
            if(memoizedValues.containsKey(node)){ 
                abstractValue val = memoizedValues.get(node);   
                return val; 
            }
        }
        if(node instanceof ExprStar){
            ExprStar star = (ExprStar) node;
            
            Type t = star.getType();
            int ssz = 1;
            List<abstractValue> avlist = null;
            if(t instanceof TypeArray){
                Integer iv = ((TypeArray)t).getLength().getIValue();
                assert iv != null;
                ssz = iv;
                avlist = new ArrayList<abstractValue>(ssz);
            }
            String isFixed = star.isFixed()? " *" : "";
            NtsbValue nv = null;
            for(int i=0; i<ssz; ++i){               
                String cvar = oracle.addBinding(star.getDepObject(i));
                String rval = "";
                if(star.getSize() > 1)
                    rval =  "<" + cvar + "  " + star.getSize() + isFixed+ "> ";
                else
                    rval =  "<" + cvar +  "> ";
                nv = new NtsbValue(rval, true);
                if(avlist != null) avlist.add(nv);
            }
            if(avlist != null) nv = new NtsbValue(avlist);
            if(oracle.allowMemoization()){ memoizedValues.put(node, nv); }
            return nv;
        }
        String cvar = oracle.addBinding(node);
        NtsbValue nv =new NtsbValue("<" + cvar +  ">", true);
        if(oracle.allowMemoization()){ memoizedValues.put(node, nv); }
        return nv;
    }
    
    public abstractValue BOTTOM(){
        return new NtsbValue();
    }

    public abstractValue BOTTOM(Type t){
        if( t instanceof TypePrimitive ){
            return BOTTOM();
        }
        assert false;
        return null;
    }

    @Override
    public abstractValue BOTTOM(String label, boolean knownGeqZero) {
        return new NtsbValue(label, knownGeqZero);
    }

    public abstractValue CONST(int v){
        return new NtsbValue(v); 
    }
    
    public abstractValue NULL(){
        return CONST(-1); 
    }
    
    public abstractValue CONST(boolean v){
        return new NtsbValue(v); 
    }
    
    public abstractValue ARR(List<abstractValue> vals){
        return new NtsbValue(vals);     
    }
    
    
    public void Assert(abstractValue val, String msg){
         if( val.hasIntVal() ){
             if(val.getIntVal() == 0){
                 throw new RuntimeException( "Assertion failure: " + msg);
             }
             if(val.getIntVal() == 1){
                 return;
             }
         }
         out.print ("assert (" + val + ") : \"" + msg + "\" ;\n");
    }
    
    public varState cleanState(String var, Type t, MethodState mstate){
        return new NtsbState(var, t, this);
    }
    
    public abstractValue plus(abstractValue v1, abstractValue v2) {
        NtsbValue rv = (NtsbValue) super.plus(v1, v2);
        if(rv.isBottom()){
            boolean c1 = v1.hasIntVal() && v2.knownGeqZero();
            boolean c2 = v1.knownGeqZero() && v2.hasIntVal();
            boolean c3 = v1.hasIntVal() && v2.hasIntVal();
            if (c1 || c2 || c3) {
                if(v2.hasIntVal()){
                    abstractValue tmp = v2;
                    v2 = v1;
                    v1 = tmp;
                }               
                assert v1.hasIntVal() && !v2.hasIntVal() : "This is an invariant";
                NtsbValue nv2 = (NtsbValue) v2;
                int A = 1;
                int B = 0;
                NtsbValue X = (NtsbValue)v2;
                if( nv2.isAXPB ){
                    A = nv2.A;
                    B = nv2.B;
                    X = nv2.X;
                }
                B = B + v1.getIntVal();
                rv.isAXPB = true;
                rv.A = A;
                rv.B = B;
                rv.X = X;
            }
        }
        return rv;
    }
    
    
    public abstractValue times(abstractValue v1, abstractValue v2) {
        NtsbValue rv = (NtsbValue) super.times(v1, v2);
        if(rv.isBottom()){
            if(v1.hasIntVal() || v2.hasIntVal()){
                if(v2.hasIntVal()){
                    abstractValue tmp = v2;
                    v2 = v1;
                    v1 = tmp;
                }
                assert v1.hasIntVal() && !v2.hasIntVal() : "This is an invariant";
                NtsbValue nv2 = (NtsbValue) v2;
                int A = 1;
                int B = 0;
                NtsbValue X = (NtsbValue)v2;
                if( nv2.isAXPB ){
                    A = nv2.A;
                    B = nv2.B;
                    X = nv2.X;
                }
                A = A * v1.getIntVal();
                B = B * v1.getIntVal();
                rv.isAXPB = true;
                rv.A = A;
                rv.B = B;
                rv.X = X;
            }
        }       
        return rv;
    }
    
    
    protected abstractValue rawArracc(abstractValue arr, abstractValue idx){
        NtsbValue nidx = (NtsbValue) idx;
        if(nidx.isAXPB){
            int i=nidx.B;
            List<abstractValue> vlist =arr.getVectValue(); 
            String rval = "($ ";
            int vsz = vlist.size();
            while(i < vsz ){
                rval += vlist.get(i).toString() + " ";
                i += nidx.A;
            }
            rval += "$[" + nidx.X + "])";
            return BOTTOM(rval);
        }else
            return BOTTOM( "(" + arr + "[" + idx + "])" );
    }
    
    int funid = 0;
    public void funcall(Function fun, List<abstractValue> avlist, List<abstractValue> outSlist, abstractValue pathCond){
        ++funid;
        Iterator<abstractValue> actualParams = avlist.iterator();
        String name = fun.getName();
        String plist = "";
        while( actualParams.hasNext() ){
            abstractValue param = actualParams.next();
            if(param.isVect()){
                List<abstractValue> lst = param.getVectValue();
                for(int tt = 0; tt<lst.size(); ++tt){
                    plist += lst.get(tt) + " ";
                }
            }else{
                plist += param;
            }
            plist += " ";           
        }
        
        Iterator<Parameter> formalParams = fun.getParams().iterator();
        boolean hasout = false;
        while(formalParams.hasNext()){
            Parameter param = formalParams.next();      
            if( param.isParameterOutput()){
                {
                    hasout = true;
                    if(param.getType().isArray()){
                        TypeArray ta = (TypeArray)param.getType();
                        int lnt = ta.getLength().getIValue();
                        List<abstractValue> ls = new ArrayList<abstractValue>(lnt);
                        for(int i=0; i< lnt; ++i){
                            ls.add(BOTTOM(name + "[" + printType(ta.getBase()) + "]( "+ plist +"  )(" + pathCond + ")[ _p_" + param.getName()+"_idx_" + i + "," + funid +"]"));
                            plist = "0";
                        }
                        outSlist.add(ARR(ls));
                    }else{
                        outSlist.add(BOTTOM(name + "[" + printType(param.getType()) + "]( "+ plist +"  )(" + pathCond + ")[ _p_" + param.getName() + "," + funid +"]"));
                    }
                }
            }
        }
        
        if(!hasout){
            String par = "___GaRbAgE" +  "=" + name + "[bit]( "+ plist +"  )(" + pathCond + ")[ NONE," + funid +"];";
            out.println(par);
        }
        
    }
    
    String printType(Type t){
        if(t.equals(TypePrimitive.bittype)){
            return "bit";
        }else{
            return "int";
        }
    }
}
