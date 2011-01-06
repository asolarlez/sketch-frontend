package sketch.util.datastructures;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * multimap to hashset. consider multimap to lists also.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class HashmapSet<K, V> extends TypedHashMap<K, HashSet<V>> {
    private static final long serialVersionUID = 1L;

    public boolean add(final K key, final V value) {
        HashSet<V> values = this.get(key);
        if (values == null) {
            values = new HashSet<V>();
            this.put(key, values);
        }
        return values.add(value);
    }

    @SuppressWarnings("unchecked")
    public Set<V> getOrEmpty(final K key) {
        Set<V> values = this.get(key);
        return ((values == null) ? (Set<V>)Collections.emptySet() : values);
    }
}
