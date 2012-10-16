/**
 *
 */
package sketch.compiler.passes.lowering;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;

/**
 * Get the struct names used by each function. Assumes that all globals are turned into
 * parameters. don't need to consider ExprFunCall and call graph, because if f calls g,
 * and g uses struct S, then f must use S (at least as a struct_ref) when calling g
 * 
 * @author Zhilei
 */
public class GetUsedStructs extends SymbolTableVisitor {
    private Map<String, Set<String>> usedStructs;

    public GetUsedStructs(NameResolver nres) {
		super(null);
        setNres(nres);
        usedStructs = new HashMap<String, Set<String>>();
  	}

    public Map<String, Set<String>> get() {
        return usedStructs;
    }

    public Object visitFunction(Function func) {
        String name = nres.getFunName(func.getName());
        Set<String> set;
        if (usedStructs.containsKey(name)) {
            set = usedStructs.get(name);
        } else {
            set = new HashSet<String>();
            usedStructs.put(name, set);
        }

        CollectStructs collector = new CollectStructs(set, nres);
        collector.visitFunction(func);

        return func;
    }

    private static class CollectStructs extends FEReplacer {
        private Set<String> currentSet;

        public CollectStructs(Set<String> set, NameResolver nres) {
            currentSet = set;
            setNres(nres);
        }

        @Override
        public Object visitTypeStruct(TypeStruct ts) {
            currentSet.add(ts.getFullName());
            return super.visitTypeStruct(ts);
        }

        @Override
        public Object visitTypeStructRef(TypeStructRef t) {
            TypeStruct ts = nres.getStruct(t.getName());
            if (ts != null) {
                this.visitTypeStruct(ts);
            }
            return super.visitTypeStructRef(t);
        }
    }

}
