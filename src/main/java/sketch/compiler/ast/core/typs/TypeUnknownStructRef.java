package sketch.compiler.ast.core.typs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.NameResolver;

public class TypeUnknownStructRef extends Type {

    public Set<String> fields = new HashSet<String>();

    @Override
    public TypeComparisonResult compare(Type that) {
        // TODO Auto-generated method stub
        return null;
    }

    public TypeUnknownStructRef() {
        super(null);
    }

    public Type leastCommonPromotion(Type that, NameResolver nres) {
        if (this.promotesTo(that, nres))
            return that;
        if (that.promotesTo(this, nres))
            return this;
        throw new RuntimeException("NYI");
    }

    public boolean promotesTo(Type that, NameResolver nres) {
        if (super.promotesTo(that, nres))
            return true;
        if (that instanceof TypeStructRef) {
            StructDef sd = nres.getStruct(((TypeStructRef) that).getName());
            for (String f : fields) {
                if (!sd.hasField(f)) {
                    return false;
                }
            }
            return true;
        }
        if (that instanceof TypeUnknownStructRef) {
            throw new RuntimeException("NYI");
        }
        return false;
    }

    public Collection<Type> getBaseTypes() {
        return Collections.singletonList((Type) this);
    }

    public Map<String, Type> unify(Type t, Set<String> names) {
        return Collections.EMPTY_MAP;
    }

    public String cleanName() {
        throw new RuntimeException("This type is not known");
    }

}
