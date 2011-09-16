package sketch.util.datastructures;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import static sketch.util.Misc.nonnull;

/**
 * get rid of the very annoying untyped queries from Java hash sets
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class TypedHashSet<K> implements Iterable<K>, Cloneable {
    private final HashSet<K> base;

    public TypedHashSet() {
        base = new HashSet<K>();
    }
    
    protected TypedHashSet(HashSet<K> base) {
        this.base = nonnull(base);
    }
    
    @Override
    public String toString() {
        String result = clsname() + "{\n";
        for (K ent : this) {
            result += "    " + ent + ",\n";
        }
        return result + " }";
    }

    protected String clsname() {
        return super.toString() + " ";
    }

    public boolean add(K e) { return base.add(e); }
    public boolean addAll(Collection<? extends K> c) { return base.addAll(c); }
    public void clear() { base.clear(); }
    @SuppressWarnings("unchecked")
    public TypedHashSet<K> clone() { return new TypedHashSet<K>((HashSet<K>) base.clone()); }
    public boolean contains(K v) { return base.contains(v); }
    public boolean remove(K v) { return base.remove(v); }
    public boolean removeAll(Collection<? extends K> c) { return base.removeAll(c); }
    public boolean isEmpty() { return base.isEmpty(); }
    public int size() { return base.size(); }
    public Iterator<K> iterator() { return base.iterator(); }
    public Collection<K> asCollection() { return base; }
    public HashSet<K> asHashSet() { return base; }

    public TypedHashSet<K> intersect(Collection<? extends K> c) {
        TypedHashSet<K> rv = clone();
        rv.base.retainAll(c);
        return rv;
    }

    public TypedHashSet<K> intersect(TypedHashSet<? extends K> c) {
        return intersect(c.asCollection());
    }

    public TypedHashSet<K> union(Collection<? extends K> c) {
        TypedHashSet<K> rv = clone();
        rv.addAll(c);
        return rv;
    }

    public TypedHashSet<K> subtract(Collection<? extends K> c) {
        TypedHashSet<K> rv = clone();
        rv.removeAll(c);
        return rv;
    }
}
