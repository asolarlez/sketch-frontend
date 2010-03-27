package sketch.util.datastructures;

import java.util.Vector;

/**
 * multimap to lists. consider multimap to sets also.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class HashmapList<K, V> extends TypedHashMap<K, Vector<V>> {
    private static final long serialVersionUID = 1L;

    public void append(final K key, final V value) {
        Vector<V> values = this.get(key);
        if (values == null) {
            values = new Vector<V>();
            this.put(key, values);
        }
        values.add(value);
    }

    public Vector<V> getOrEmpty(final K key) {
        Vector<V> values = this.get(key);
        return (values == null) ? new Vector<V>() : values;
    }
}
