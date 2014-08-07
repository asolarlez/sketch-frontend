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
import sketch.compiler.ast.core.exprs.ExprSpecialStar;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssume;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.dataflow.MethodState;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.varState;
import sketch.compiler.solvers.constructs.AbstractValueOracle;
import sketch.util.DebugOut;
import sketch.util.exceptions.ExceptionAtNode;

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


    public abstractValue STAR(FENode node) {
        // TODO: do we guarantee each node is visited just once?
        // if we do, why memoization?
        // TODO xzl TODO
        if (oracle.allowMemoization()) {
            if (memoizedValues.containsKey(node)) {
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
                if (iv == null) {
                    throw new ExceptionAtNode(
                            "If ?? is used as an array, the array must have constant length. ??:" +
                                    t, node);
                }
                ssz = iv;
                avlist = new ArrayList<abstractValue>(ssz);
            }
            String isFixed = star.isFixed()? " *" : "";
            NtsbValue nv = null;
            for(int i=0; i<ssz; ++i){               
                String cvar = oracle.addBinding(star.getDepObject(i));
                String rval = "";

                if (node instanceof ExprSpecialStar) {
                    rval += ((ExprSpecialStar) node).name;
                }

                String head = star.isAngelicMax() ? "<**" : "<";

                if (star.getSize() > 1 && !star.isCounter())
                    rval += head + cvar + "  " + star.getSize() + isFixed;
                else
                    rval = head + cvar;
                if (star.isCounter()) {
                    rval += " %";
                }
                rval += "> ";
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

    public abstractValue TUPLE(List<abstractValue> vals, String name) {
        return new NtsbValue(vals, name);
    }
    public abstractValue ARR(List<abstractValue> vals){
        return new NtsbValue(vals);     
    }

    public void Assert(abstractValue val, StmtAssert stmt) {
        String msg = stmt.getMsg();
        if (val.hasIntVal()) {
            if (val.getIntVal() == 0) {
                DebugOut.printWarning(stmt.getCx() +
                        "This assertion will fail unconditionally when you call this function: " +
                        msg);
            }
            if (val.getIntVal() == 1) {
                return;
            }
        }
        out.print(stmt.getAssertSymbol() + " (" + val + ") : \"" +
                msg + "\" ;\n");
    }

    public void Assume(abstractValue val, StmtAssume stmt) {
        String msg = stmt.getMsg();
        if (val.hasIntVal()) {
            if (val.getIntVal() == 0) {
                DebugOut.printWarning(stmt.getCx() +
                        "This assertion will fail unconditionally when you call this function: " +
                        msg);
            }
            if (val.getIntVal() == 1) {
                return;
            }
        }
        out.print("assume (" + val + ")" + (msg == null ? "" : (" : \"" + msg + "\"")) +
                ";\n");
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
            // boolean c_original = v1.hasIntVal() || v2.hasIntVal();
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
            // else if (c_original) {
            // printWarning("skipping ax+b optimization for nodes", v1, v2);
            // }
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
        if (nidx.isAXPB && arr.isVect()) {
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

    public void funcall(Function fun, List<abstractValue> avlist,
            List<abstractValue> outSlist, abstractValue pathCond, MethodState state)
    {
        ++funid;
        outputMap = new HashMap<Integer, Integer>();
        Iterator<abstractValue> actualParams = avlist.iterator();
        Iterator<Parameter> formalParams = fun.getParams().iterator();
        String name = fun.getName();
        String plist = "";
        while( actualParams.hasNext() ){
            abstractValue param = actualParams.next();
            Parameter formal = formalParams.next();
            while (!formal.isParameterInput() && formalParams.hasNext()) {
                formal = formalParams.next();
            }
            assert formal.isParameterInput();
            if(param.isVect()){
                TypeArray ta = (TypeArray) formal.getType();
                Expression eln = ta.getLength();
                Integer lntt = eln == null ? null : eln.getIValue();
                if (lntt == null) {
                    plist += "( {" + param + "} )";
                } else {
                    List<abstractValue> lst = param.getVectValue();
                    assert lntt == lst.size();
                    for (int tt = 0; tt < lst.size(); ++tt) {
                        plist += lst.get(tt) + " ";
                    }
                }
            }else{
                Type ftype = formal.getType();
                if (ftype instanceof TypeArray) {
                    TypeArray ta = (TypeArray) ftype;
                    Expression eln = ta.getLength();
                    Integer lntt = eln == null ? null : eln.getIValue();
                    if (lntt == null) {
                        plist += param;
                    } else {
                        int lsz = lntt;
                        plist += param + " ";
                        for (int tt = 1; tt < lsz; ++tt) {
                            plist += CONST(ta.getBase().defaultValue().getIValue()) + " ";
                        }
                    }
                } else {
                    plist += param;
                }
            }
            plist += " ";           
        }
        String oplist = plist;
        formalParams = fun.getParams().iterator();
        boolean hasout = false;
        while (formalParams.hasNext()) {
            Parameter param = formalParams.next();
            if (param.isParameterOutput()) {
                hasout = true;
            }
        }
        formalParams = fun.getParams().iterator();
        actualParams = avlist.iterator();

        int outIndex = 0;
        String outLhsName = "out_" + fun.getName() + "_" + fun.getPkg();
        if (hasout) {
            String pkg = fun.getPkg();
            if (pkg == null) {
                pkg = "ANNONYMOUS";
            }
        abstractValue outval =
                    BOTTOM(name + "[*" + fun.getName().toUpperCase() + "_" +
                            pkg.toUpperCase() + "]( " + plist + "  )(" +
                            pathCond + ")[ _p_out_" +
                        fun.getName() + "_" + fun.getPkg() + "," + funid + "]");
        state.varDeclare(outLhsName, new TypeStructRef("norec", false));
        abstractValue outLhsIdx = null;

        state.setVarValue(outLhsName, outval);
        }
        while (formalParams.hasNext()) {
            Parameter param = formalParams.next();
            abstractValue ov;
            if (param.isParameterOutput()) {
                if (param.getType().isArray()) {
                    TypeArray ta = (TypeArray) param.getType();
                    Expression el = ta.getLength();
                    Integer lntt = el != null ? el.getIValue() : null;
                    if (lntt != null) {
                        int lnt = lntt;
                        List<abstractValue> avList = new ArrayList<abstractValue>();
                        for (int p = 0; p < lnt; p++) {
                            abstractValue av =
                                    tupleacc(state.varValue(outLhsName), CONST(outIndex));
                            outIndex++;
                            avList.add(av);
                        }
                        ov = ARR(avList);
                        outSlist.add(ov);

                    } else {
                        ov = tupleacc(state.varValue(outLhsName), CONST(outIndex));
                        outIndex++;
                        String tmpName = "___tEmP" + (gbgid++);
                        state.varDeclare(tmpName, new TypeStructRef("norec", false));
                        state.setVarValue(tmpName, ov);
                        abstractValue rhs = state.varValue(tmpName);
                        outSlist.add(rhs);
                    }
                } else {
                    ov = tupleacc(state.varValue(outLhsName), CONST(outIndex));
                    outIndex++;
                    outSlist.add(ov);
                }

            }
        }
        if (!hasout) {
            String par = "___GaRbAgE" + (gbgid++) +  "=" + name + "[bit]( "+ plist +"  )(" + pathCond + ")[ NONE," + funid +"];";
            out.println(par);
        }        
    }
    int gbgid = 0; //This is a big hack!!

    String printType(Type t){
        if (t instanceof TypeStructRef) {
            TypeStructRef ts = (TypeStructRef) t;
            StructDef struct = nres.getStruct(ts.getName());
            if (struct.immutable()) {
                return "*" + struct.getName().toUpperCase() + "_" +
                        struct.getPkg().toUpperCase();
            }
        }
        if(t.equals(TypePrimitive.bittype)){
            return "bit";
        }else{
            if (t.equals(TypePrimitive.floattype) || t.equals(TypePrimitive.doubletype)) {
                return "float";
            } else {
                return "int";
            }
        }
    }
}
