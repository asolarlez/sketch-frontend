package sketch.util.datastructures;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * avoid annoying untyped lookups.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class TypedHashMap<K, V> {
    private final HashMap<K, V> base;

    public TypedHashMap() {
        base = new HashMap<K, V>();
    }

    public TypedHashMap(int initialCapacity) {
        base = new HashMap<K, V>(initialCapacity);
    }

    public void clear() { base.clear(); }
    public boolean containsKey(K key) { return base.containsKey(key); }
    public boolean containsValue(V value) { return base.containsValue(value); }
    public Set<java.util.Map.Entry<K, V>> entrySet() { return base.entrySet(); }
    public V get(K key) { return base.get(key); }
    public boolean isEmpty() { return base.isEmpty(); }
    public Set<K> keySet() { return base.keySet(); }
    public V put(K key, V value) { return base.put(key, value); }
    public void putAll(Map<? extends K, ? extends V> m) { base.putAll(m); }
    public V remove(K key) { return base.remove(key); }
    public int size() { return base.size(); }
    public Collection<V> values() { return base.values(); }

    public V getCreate(K key) {
        V value = base.get(key);
        if (value == null) {
            value = createValue();
            this.put(key, value);
        }
        return value;
    }

    public V createValue() {
        throw new RuntimeException("override TypedHashMap to provide createValue()");
    }

    public void addZipped(Collection<? extends K> keys, Collection<? extends V> values) {
        Iterator<? extends K> keyIter = keys.iterator();
        Iterator<? extends V> valueIter = values.iterator();
        while (keyIter.hasNext()) {
            put(keyIter.next(), valueIter.next());
        }
        assert !valueIter.hasNext();
    }
}
