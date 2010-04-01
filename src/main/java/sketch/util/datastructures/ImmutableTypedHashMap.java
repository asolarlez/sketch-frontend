package sketch.util.datastructures;

import java.util.HashMap;

public class ImmutableTypedHashMap<K, V> extends TypedHashMap<K, V> {
    @SuppressWarnings("unchecked")
    protected ImmutableTypedHashMap(HashMap<K, V> base) {
        super((HashMap<K, V>) base.clone());
    }
}
