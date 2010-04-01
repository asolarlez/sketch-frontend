package sketch.util.fcns;

import java.util.Iterator;

/**
 * python's enumerate() function
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class Zip<L, R> implements Iterable<ZipEnt<L, R>> {
    protected final Iterable<L> leftElements;
    protected final Iterable<R> rightElements;

    public Zip(Iterable<L> leftElements, Iterable<R> rightElements) {
        this.leftElements = leftElements;
        this.rightElements = rightElements;
    }

    public static <L, R> Zip<L, R> zip(Iterable<L> leftElements, Iterable<R> rightElements)
    {
        return new Zip<L, R>(leftElements, rightElements);
    }

    public Iterator<ZipEnt<L, R>> iterator() {
        return new ZipIterator(leftElements.iterator(), rightElements.iterator());
    }

    public class ZipIterator implements Iterator<ZipEnt<L, R>> {
        protected int idx = 0;
        protected final Iterator<L> left;
        protected final Iterator<R> right;
        protected ZipEnt<L, R> previous;

        public ZipIterator(Iterator<L> left, Iterator<R> right) {
            this.left = left;
            this.right = right;
        }

        public boolean hasNext() {
            assert left.hasNext() == right.hasNext();
            return left.hasNext();
        }

        public ZipEnt<L, R> next() {
            ZipEnt<L, R> result =
                    new ZipEnt<L, R>(previous, left.next(), right.next(), idx, !hasNext());
            idx += 1;
            previous = result;
            return result;
        }

        public void remove() {
            left.remove();
            right.remove();
        }
    }
}
