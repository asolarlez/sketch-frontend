package sketch.util.datastructures;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * multimap to hashset. consider multimap to lists also.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class TreemapSet<K, V> extends TypedTreeMap<K, TreeSet<V>> {
    private static final long serialVersionUID = 1L;

    public boolean add(final K key, final V value) {
        TreeSet<V> values = this.get(key);
        if (values == null) {
            values = new TreeSet<V>();
            this.put(key, values);
        }
        return values.add(value);
    }

    @SuppressWarnings("unchecked")
    public Set<V> getOrEmpty(final K key) {
        Set<V> values = this.get(key);
        return ((values == null) ? (Set<V>) Collections.emptySet() : values);
    }
}
