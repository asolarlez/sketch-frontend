package sketch.util.datastructures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class OrderedHashSet<E> implements Set<E> {
    HashSet<E> inner = new HashSet<E>();
    List<E> order = new ArrayList<E>();

    public int size() {
        return inner.size();
    }

    public boolean isEmpty() {
        return inner.isEmpty();
    }

    public boolean contains(Object o) {
        return inner.contains(o);
    }

    public Iterator<E> iterator() {
        return order.iterator();
    }

    public Object[] toArray() {
        return order.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return order.toArray(a);
    }

    public boolean add(E e) {
        if (!inner.contains(e)) {
            inner.add(e);
            order.add(e);
            return true;
        }
        return false;
    }

    public boolean remove(Object o) {
        throw new RuntimeException("NYI");
    }

    public boolean containsAll(Collection<?> c) {
        inner.containsAll(c);
        return false;
    }

    public boolean addAll(Collection<? extends E> c) {
        boolean rv = true;
        for (E x : c) {
            rv = rv && add(x);
        }
        return rv;
    }

    public boolean retainAll(Collection<?> c) {
        throw new RuntimeException("NYI");
    }

    public boolean removeAll(Collection<?> c) {
        throw new RuntimeException("NYI");
    }

    public void clear() {
        inner.clear();
        order.clear();
    }

}
