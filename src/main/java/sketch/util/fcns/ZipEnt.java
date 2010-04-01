package sketch.util.fcns;

/**
 * zip with index entry
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ZipEnt<L, R> {
    protected final ZipEnt<L, R> prev;
    public final L left;
    public final R right;

    public final int idx;
    public final boolean isLast;

    public ZipEnt(ZipEnt<L, R> prev, L left, R right, int idx, boolean isLast) {
        this.prev = prev;
        this.left = left;
        this.right = right;
        this.idx = idx;
        this.isLast = isLast;
    }
}
