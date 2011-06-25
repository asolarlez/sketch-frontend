package sketch.util.datastructures;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import static sketch.util.DebugOut.assertFalse;

/**
 * allow locking / unmodifiable
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class LockableVector<E> extends Vector<E> {
    private static final long serialVersionUID = 4527559463109108823L;
    private boolean locked = false;

    public LockableVector(E... initial) {
        super(Arrays.asList(initial));
    }

    public LockableVector(Collection<E> initial) {
        super(initial);
    }

    @Override
    public synchronized String toString() {
        return this.getClass().getSimpleName() + " " + super.toString();
    }

    /** @returns this */
    protected LockableVector<E> lock() {
        this.locked = true;
        return this;
    }

    public LockableVector<E> clone() {
        LockableVector<E> result = new LockableVector<E>();
        result.addAll(this);
        return result;
    }

    public List<E> cloneToUnmodifiable() {
        return Collections.unmodifiableList(this.clone());
    }

    public synchronized boolean add(E e) {
        assertNotLocked();
        return super.add(e);
    }

    public synchronized boolean addAll(E... c) {
        return addAll(Arrays.asList(c));
    }

    @Override
    public synchronized boolean addAll(Collection<? extends E> c) {
        assertNotLocked();
        return super.addAll(c);
    }

    @Override
    public synchronized boolean addAll(int index, Collection<? extends E> c) {
        assertNotLocked();
        return super.addAll(index, c);
    }

    public synchronized void insertElementAt(E obj, int index) {
        assertNotLocked();
        super.insertElementAt(obj, index);
    };

    @Override
    public synchronized E remove(int index) {
        assertNotLocked();
        return super.remove(index);
    }

    @Override
    public boolean remove(Object o) {
        assertNotLocked();
        return super.remove(o);
    }

    @Override
    public synchronized boolean removeAll(Collection<?> c) {
        assertNotLocked();
        return super.removeAll(c);
    }

    @Override
    public void clear() {
        assertNotLocked();
        super.clear();
    }

    protected final void assertNotLocked() {
        if (locked) {
            assertFalse("[LockableVector] vector is locked, modification requested");
        }
    };
}
