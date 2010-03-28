package sketch.util.datastructures;

import static sketch.util.Misc.nonnull;

/**
 * Calls subobject equals() and hashCode() methods. Arguments are checked to be non-null
 * (with asserts only). Subclasses should create named accessors for the $left and $right
 * fields.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public abstract class ObjPairBase<L, R> {
    protected final L left;
    protected final R right;

    public ObjPairBase(L left, R right) {
        this.left = nonnull(left);
        this.right = nonnull(right);
    }

    @Override
    public int hashCode() {
        return left.hashCode() * 391847 + right.hashCode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ObjPairBase)) {
            return false;
        }
        ObjPairBase<L, R> other = (ObjPairBase<L, R>) obj;
        return this.left.equals(other.left) && this.right.equals(other.right);
    }
}
