package sketch.util.datastructures;

import java.util.Iterator;

/**
 * lazily map values from one iterator to another.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public abstract class LazyMap<INTYPE, OUTTYPE> implements Iterable<OUTTYPE> {
    protected final Iterable<INTYPE> base;

    public LazyMap(Iterable<INTYPE> base) {
        this.base = base;
    }

    public Iterator<OUTTYPE> iterator() {
        return new LazyMapIterator(base.iterator());
    }

    public abstract OUTTYPE map(INTYPE value);

    public class LazyMapIterator implements Iterator<OUTTYPE> {
        protected final Iterator<INTYPE> baseIterator;

        public LazyMapIterator(Iterator<INTYPE> base) {
            baseIterator = base;
        }

        public boolean hasNext() {
            return baseIterator.hasNext();
        }

        public OUTTYPE next() {
            return map(baseIterator.next());
        }

        public void remove() {
            baseIterator.remove();
        }
    }
}
