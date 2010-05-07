package sketch.util.datastructures;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * get rid of the very annoying untyped queries from Java hash sets
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class TypedHashSet<K> implements Iterable<K> {
    private final HashSet<K> base = new HashSet<K>();

    @Override
    public String toString() {
        String result = super.toString() + " {\n";
        for (K ent : this) {
            result += "    " + ent + ",\n";
        }
        return result + " }";
    }

    public boolean add(K e) { return base.add(e); }
    public boolean addAll(Collection<? extends K> c) { return base.addAll(c); }
    public void clear() { base.clear(); }
    public boolean contains(K v) { return base.contains(v); }
    public boolean remove(K v) { return base.remove(v); }
    public boolean isEmpty() { return base.isEmpty(); }
    public int size() { return base.size(); }
    public Iterator<K> iterator() { return base.iterator(); }
}
