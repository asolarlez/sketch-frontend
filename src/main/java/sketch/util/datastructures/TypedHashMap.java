package sketch.util.datastructures;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * avoid annoying untyped lookups.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class TypedHashMap<K, V> implements Cloneable {
    private final HashMap<K, V> base;

    public TypedHashMap() {
        base = new HashMap<K, V>();
    }

    public TypedHashMap(int initialCapacity) {
        base = new HashMap<K, V>(initialCapacity);
    }

    protected TypedHashMap(HashMap<K, V> base) {
        this.base = base;
    }

    /** WARNING -- not a clone constructor. Created since base is private. */
    protected TypedHashMap(TypedHashMap<K, V> prev) {
        this.base = prev.base;
    }

    @Override
    public String toString() {
        String result = super.toString() + " {\n";
        for (Entry<K, V> ent : this.entrySet()) {
            result += "    " + ent.getKey() + ": " + ent.getValue() + ",\n"; 
        }
        return result + " }";
    }

    public void clear() { base.clear(); }
    @SuppressWarnings("unchecked")
    protected HashMap<K, V> baseClone() { return (HashMap<K, V>) base.clone(); }
    /** recreate this method using baseClone() for any extending classes */
    public TypedHashMap<K, V> clone() { return new TypedHashMap<K, V>(baseClone()); }
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

    /** NOTE -- uses asserts only, run with -ea if you need it! */
    public void addAssertDiscrete(final TypedHashMap<K, V> other) {
        for (Entry<K, V> ent : other.entrySet()) {
            assert !this.containsKey(ent.getKey());
            this.put(ent.getKey(), ent.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    public TypedHashMap<K, V> immutable() {
        if (this instanceof ImmutableTypedHashMap) {
            return this;
        } else {
            return new ImmutableTypedHashMap<K, V>(this.base);
        }
    }
}
