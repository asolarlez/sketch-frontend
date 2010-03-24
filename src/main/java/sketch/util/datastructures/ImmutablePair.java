package sketch.util.datastructures;

/**
 * pair of objects, when the objects have good hash codes
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@SuppressWarnings("unchecked")
public class ImmutablePair<L extends NiceObj, R extends NiceObj> extends
        DefaultNiceObj<ImmutablePair<L, R>>
{
    protected final L left;
    protected final R right;

    /** for ephemeral types; for type decorations, use method below */
    public ImmutablePair(final L left, final R right) {
        super(left.longHash() * 9269472386663L + right.longHash(), 6466004128513L);
        this.left = left;
        this.right = right;
    }

    public ImmutablePair(final L left, final R right, final long classId) {
        super(left.longHash() * 9269472386663L + right.longHash(), classId);
        this.left = left;
        this.right = right;
    }

    @Override
    public int baseCompare(final ImmutablePair<L, R> o) {
        int leftResult = this.left.compareTo(o.left);
        return (leftResult == 0) ? this.right.compareTo(o.right) : leftResult;
    }
}
