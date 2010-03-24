package sketch.util.datastructures;

/**
 * NOTE -- assign type parameter $T to the current class.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@SuppressWarnings("unchecked")
public abstract class DefaultNiceObj<T extends DefaultNiceObj> implements NiceObj<T> {
    protected final long hash;

    /**
     * @param classId
     *            constant class identifier
     */
    public DefaultNiceObj(final long hash, final long classId) {
        this.hash = hash * 20763880956251L + classId;
    }

    public int compareTo(final T o) {
        if (this.longHash() == o.longHash()) {
            return 0;
        }
        return this.baseCompare(o);
    }

    public abstract int baseCompare(final T o);

    @Override
    public final boolean equals(final Object obj) {
        if ((obj == null) || (!(obj instanceof DefaultNiceObj))) {
            return false;
        }
        return ((DefaultNiceObj) obj).longHash() == this.longHash();
    }

    public final long longHash() {
        return this.hash;
    }

    @Override
    public final int hashCode() {
        return (int) this.hash;
    }
}
