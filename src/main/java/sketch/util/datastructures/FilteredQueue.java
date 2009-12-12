package sketch.util.datastructures;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * filter element with a filter function. threadsafe.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public abstract class FilteredQueue<T> {
    ConcurrentLinkedQueue<T> base = new ConcurrentLinkedQueue<T>();

    public void add(T elt) {
        base.add(elt);
    }

    public T get() {
        while (!base.isEmpty()) {
            T next = base.peek();
            if (!apply_filter(next)) {
                if (base.remove(next)) {
                    remove_action(next);
                }
            } else {
                return next;
            }
        }
        return null;
    }

    public abstract boolean apply_filter(T elt);

    public void remove_action(T elt) {
    }
}
