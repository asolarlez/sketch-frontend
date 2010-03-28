package sketch.util.fcns;

/**
 * zip with index entry
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class ZipIdxEnt<T> {
    public final T prev;
    public final int idx;
    public final T entry;
    public final boolean isLast;
    /** zero-based */
    public final int idxFromEnd;

    public ZipIdxEnt(T prev, int idx, int sz, T entry, boolean isLast) {
        this.prev = prev;
        this.idx = idx;
        this.idxFromEnd = sz - (idx + 1);
        this.entry = entry;
        this.isLast = isLast;
    }
}
