package sketch.util.fcns;

import static sketch.util.DebugOut.assertFalse;

import java.util.Iterator;
import java.util.List;

/**
 * allow copying of iterators (more java annoyance hacks)
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class CopyableIterator<T> implements Iterator<T>, Cloneable {
    protected List<T> vec;

    public CopyableIterator(List<T> elts) {
        this.vec = elts;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new CopyableIterator<T>(vec);
    }

    public boolean hasNext() {
        return !vec.isEmpty();
    }

    public T next() {
        T rv = vec.get(0);
        vec = vec.subList(1, vec.size());
        return rv;
    }

    public List<T> peekAllNext() {
        return vec;
    }

    public void remove() {
        assertFalse("remove for CopyableIterator not supported");
    }
}
