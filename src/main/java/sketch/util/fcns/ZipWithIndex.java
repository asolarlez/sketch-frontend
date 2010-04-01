package sketch.util.fcns;

import java.util.Collection;
import java.util.Iterator;

/**
 * python's enumerate() function
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ZipWithIndex<T> implements Iterable<ZipIdxEnt<T>> {
    protected final Collection<T> original;

    public ZipWithIndex(Collection<T> original) {
        this.original = original;
    }

    public static <T> ZipWithIndex<T> zipwithindex(Collection<T> original) {
        return new ZipWithIndex<T>(original);
    }

    public Iterator<ZipIdxEnt<T>> iterator() {
        return new ZipIterator(original.iterator(), original.size());
    }

    public class ZipIterator implements Iterator<ZipIdxEnt<T>> {
        protected final Iterator<T> iterator;
        protected T prev = null;
        protected int idx = 0;
        protected final int sz;

        public ZipIterator(Iterator<T> iterator, int sz) {
            this.iterator = iterator;
            this.sz = sz;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public ZipIdxEnt<T> next() {
            final ZipIdxEnt<T> result =
                    new ZipIdxEnt<T>(prev, idx, sz, iterator.next(), !iterator.hasNext());
            idx += 1;
            prev = result.entry;
            return result;
        }

        public void remove() {
            iterator.remove();
        }
    }
}
