/**
 *
 */
package sketch.compiler.passes.lowering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStructRef;

/**
 * Get the struct names used by each function. Assumes that all globals are turned into
 * parameters. don't need to consider ExprFunCall and call graph, because if f calls g,
 * and g uses struct S, then f must use S (at least as a struct_ref) when calling g
 * 
 * @author Zhilei
 */
public class GetUsedStructs extends FEReplacer {
    private final Map<String, Set<String>> usedStructs;
    private final Map<String, Set<String>> createdStructs;

    public GetUsedStructs(NameResolver nres) {
        setNres(nres);
        usedStructs = new HashMap<String, Set<String>>();
        createdStructs = new HashMap<String, Set<String>>();
    }

    public Map<String, Set<String>> get() {
        return usedStructs;
    }

    public Map<String, Set<String>> getCreated() {
        return createdStructs;
    }

    class Transitivity extends FEReplacer {

        public boolean changed = false;
        Set<String> currentUsedStructs;
        Set<String> currentCreatedStructs;

        Transitivity(NameResolver nres) {
            setNres(nres);
        }

        public Object visitProgram(Program prog) {
            assert prog != null : "FEReplacer.visitProgram: argument null!";
            List<Package> newStreams = new ArrayList<Package>();
            for (Package ssOrig : prog.getPackages()) {
                newStreams.add((Package) ssOrig.accept(this));
            }
            return prog.creator().streams(newStreams).create();
        }

        public Object visitExprFunCall(ExprFunCall efc) {
            String name = nres.getFunName(efc.getName());
            {
                Set<String> callee = usedStructs.get(name);
                for (String cs : callee) {
                    if (!currentUsedStructs.contains(cs)) {
                        currentUsedStructs.add(cs);
                        changed = true;
                    }
                }
            }
            {
                Set<String> callee = createdStructs.get(name);
                for (String cs : callee) {
                    if (!currentCreatedStructs.contains(cs)) {
                        currentCreatedStructs.add(cs);
                        changed = true;
                    }
                }
            }
            return efc;
        }

        public Object visitFunction(Function func) {
            String name = nres.getFunName(func.getName());
            currentUsedStructs = usedStructs.get(name);
            currentCreatedStructs = createdStructs.get(name);
            super.visitFunction(func);
            currentUsedStructs = null;
            currentCreatedStructs = null;
            return func;
        }

    };

    void computeTransitivity(Program prog) {
        Transitivity tt = new Transitivity(nres);
        do {
            tt.changed = false;
            tt.visitProgram(prog);
        } while (tt.changed);
    }

    public Object visitProgram(Program prog) {
        assert prog != null : "FEReplacer.visitProgram: argument null!";

        for (Package ssOrig : prog.getPackages()) {
            ssOrig.accept(this);
        }
        computeTransitivity(prog);

        return prog;
    }

    public Object visitFunction(Function func) {
        String name = nres.getFunName(func.getName());
        Set<String> set;
        Set<String> createdset;
        if (usedStructs.containsKey(name)) {
            set = usedStructs.get(name);
        } else {
            set = new HashSet<String>();
            usedStructs.put(name, set);
        }

        if (createdStructs.containsKey(name)) {
            createdset = createdStructs.get(name);
        } else {
            createdset = new HashSet<String>();
            createdStructs.put(name, createdset);
        }

        CollectStructs collector = new CollectStructs(set, createdset, nres);
        collector.visitFunction(func);

        return func;
    }

    private static class CollectStructs extends SymbolTableVisitor {
        private Set<String> currentSet;
        private Set<String> createdSet;

        public CollectStructs(Set<String> set, Set<String> createdSet, NameResolver nres)
        {
            super(null);
            currentSet = set;
            this.createdSet = createdSet;
            setNres(nres);
        }

        public Object visitExprField(ExprField ef) {
            Type t = getType(ef.getLeft());
            t.accept(this);
            return super.visitExprField(ef);
        }

        public Object visitExprNew(ExprNew en) {
            Type nt = en.getTypeToConstruct();
            StructDef ts = nres.getStruct(nt.toString());
            createdSet.add(ts.getFullName());
            return super.visitExprNew(en);
        }

        @Override
        public Object visitStructDef(StructDef ts) {
            currentSet.add(ts.getFullName());
            return super.visitStructDef(ts);
        }

        @Override
        public Object visitTypeStructRef(TypeStructRef t) {
            StructDef ts = nres.getStruct(t.getName());
            currentSet.add(ts.getFullName());
            return super.visitTypeStructRef(t);
        }
    }

}

