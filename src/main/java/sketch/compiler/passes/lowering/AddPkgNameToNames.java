package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;

public class AddPkgNameToNames extends FEReplacer {

    private String transFun(String name) {
        if (name != null) {
            String t = nres.getFunName(name);
            return t.replace('@', '_');
        }
        return null;
    }

    private String transStruct(String name) {
        String t = nres.getStructName(name);
        return t.replace('@', '_');
    }

    @Override
    public Object visitFunction(Function f) {
        f = (Function) super.visitFunction(f);
        return f.creator().name(transFun(f.getName())).spec(
                transFun(f.getSpecification())).create();
    }

    public Object visitTypeStruct(TypeStruct ts) {
        ts = (TypeStruct) super.visitTypeStruct(ts);
        return ts.creator().name(transStruct(ts.getName()));
        // return new TypeStruct(ts.getCudaMemType(), ts.getContext(),
        // transStruct(ts.getName()), ts.getFieldTypMap());
    }

    public Object visitTypeStructRef(TypeStructRef tsr) {
        tsr = (TypeStructRef) super.visitTypeStructRef(tsr);
        return new TypeStructRef(transStruct(tsr.getName()));
    }

    @Override
    public Object visitType(Type t) {
        t = (Type) super.visitType(t);
        assert !(t instanceof TypeStruct);
        assert !(t instanceof TypeStructRef);
        return t;
    }

    public Object visitExprFunCall(ExprFunCall efc) {
        efc = (ExprFunCall) super.visitExprFunCall(efc);
        return new ExprFunCall(efc, transFun(efc.getName()), efc.getParams());
    }

    public Object visitProgram(Program prog) {
        if (prog.getStreams().size() > 1) {
        assert prog != null : "FEReplacer.visitProgram: argument null!";
        nres = new NameResolver(prog);
        List<Function> lf = new ArrayList<Function>();
        List<TypeStruct> ts = new ArrayList<TypeStruct>();
        for (StreamSpec ssOrig : prog.getStreams()) {
            StreamSpec pkg = (StreamSpec) ssOrig.accept(this);
            lf.addAll(pkg.getFuncs());
            ts.addAll(pkg.getStructs());
        }
        StreamSpec global = new StreamSpec(prog, "GLOBAL", ts, new ArrayList(), lf);

        return prog.creator().streams(Collections.singletonList(global)).create();
        } else {
            return prog;
        }
    }

}
