package sketch.util.datastructures;

import java.util.HashMap;
import java.util.Map;

/**
 * disallow updates. constructor clones its argument.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ImmutableTypedHashMap<K, V> extends TypedHashMap<K, V> {
    @SuppressWarnings("unchecked")
    protected ImmutableTypedHashMap(HashMap<K, V> base) {
        super((HashMap<K, V>) base.clone());
    }

    public final V put(K key, V value) {
        throw new RuntimeException("can't add an element to an ImmutableHashMap!");
    }

    public final void putAll(Map<? extends K, ? extends V> m) {
        throw new RuntimeException("can't add an element to an ImmutableHashMap!");
    }
}
