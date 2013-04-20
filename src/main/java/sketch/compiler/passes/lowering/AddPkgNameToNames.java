package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStructRef;

public class AddPkgNameToNames extends FEReplacer {

    public static String GLOBALPKG = "GLOBAL";

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
                transFun(f.getSpecification())).pkg(GLOBALPKG).create();
    }

    public Object visitStructDef(StructDef ts) {
        ts = (StructDef) super.visitStructDef(ts);
        return ts.creator().name(transStruct(ts.getName())).pkg(GLOBALPKG).create();
        // return new TypeStruct(ts.getCudaMemType(), ts.getContext(),
        // transStruct(ts.getName()), ts.getFieldTypMap());
    }

    public Object visitTypeStructRef(TypeStructRef tsr) {
        tsr = (TypeStructRef) super.visitTypeStructRef(tsr);
        return new TypeStructRef(transStruct(tsr.getName()), tsr.isUnboxed());
    }

    @Override
    public Object visitType(Type t) {
        t = (Type) super.visitType(t);
        assert !(t instanceof TypeStructRef);
        return t;
    }

    public Object visitExprFunCall(ExprFunCall efc) {
        efc = (ExprFunCall) super.visitExprFunCall(efc);
        return new ExprFunCall(efc, transFun(efc.getName()), efc.getParams());
    }

    public Object visitProgram(Program prog) {
        if (prog.getPackages().size() > 1) {
        assert prog != null : "FEReplacer.visitProgram: argument null!";
        nres = new NameResolver(prog);
        List<Function> lf = new ArrayList<Function>();
        List<StructDef> ts = new ArrayList<StructDef>();
        for (Package ssOrig : prog.getPackages()) {
            Package pkg = (Package) ssOrig.accept(this);
            lf.addAll(pkg.getFuncs());
            ts.addAll(pkg.getStructs());
        }
            Package global = new Package(prog, GLOBALPKG, ts, new ArrayList(), lf);

        return prog.creator().streams(Collections.singletonList(global)).create();
        } else {
            return prog;
        }
    }

}
