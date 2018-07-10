package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprFunCall;

public class MethodRename extends FEReplacer {
    Map<String, String> oldToNew;
    public MethodRename(Map<String, String> oldToNew){
        this.oldToNew = oldToNew;
    }
    
    public Object visitProgram(Program prog) {
        assert prog != null : "FEReplacer.visitProgram: argument null!";
        nres = new NameResolver(prog);
        List<Package> newStreams = new ArrayList<Package>();
        Set<String> fnames = new HashSet<String>();
        for (Package sp : prog.getPackages()) {
            for (Function f : sp.getFuncs()) {
                fnames.add(f.getName());
            }
        }
        for (Entry<String, String> en : oldToNew.entrySet()) {
            if (fnames.contains(en.getValue())) {
                String s = en.getValue();
                do {
                    s = "_" + s;
                } while (fnames.contains(s));
                oldToNew.put(en.getKey(), s);
                fnames.add(s);
            }
        }

        for (Package ssOrig : prog.getPackages()) {
            newStreams.add((Package) ssOrig.accept(this));
        }
        return prog.creator().streams(newStreams).create();
    }

    public String maybeSub(String s) {
        if (s == null) {
            return s;
        }
        if (oldToNew.containsKey(s)) {
            return oldToNew.get(s);
        } else {
            return s;
        }
    }

    @Override
    public Object visitFunction(Function func)
    {
        Function rv = (Function) super.visitFunction(func);
        String spec = rv.getSpecification();
        if (oldToNew.containsKey(rv.getName()) ||
                (spec != null && oldToNew.containsKey(spec)))
        {
            return rv.creator().name(maybeSub(rv.getName())).spec(maybeSub(spec)).create();
        }else{
            return rv;
        }        
    }
    
    public Object visitExprFunCall(ExprFunCall exp)
    {
        ExprFunCall efc = (ExprFunCall) super.visitExprFunCall(exp);
        if(oldToNew.containsKey(efc.getName())){
            return new ExprFunCall(efc, oldToNew.get(efc.getName()), efc.getParams(), efc.getTypeParams());
        }else{
            return efc;
        }
    }
    
}
