package sketch.util.fcns;

import java.util.Vector;

/**
 * map values from one iterator to another.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class VectorMap {
    public static <INTYPE, OUTTYPE> Vector<OUTTYPE> vecmap(Iterable<INTYPE> inarr,
            VectorMapFcn<INTYPE, OUTTYPE> map)
    {
        Vector<OUTTYPE> result = new Vector<OUTTYPE>();
        for (INTYPE elt : inarr) {
            result.add(map.map(elt));
        }
        return result;
    }

    public static <INTYPE, OUTTYPE> Vector<OUTTYPE> vecmap_nonnull(
            Iterable<INTYPE> inarr, VectorMapFcn<INTYPE, OUTTYPE> map)
    {
        Vector<OUTTYPE> result = new Vector<OUTTYPE>();
        for (INTYPE elt : inarr) {
            final OUTTYPE next = map.map(elt);
            if (next != null) {
                result.add(next);
            }
        }
        return result;
    }

    public static abstract class VectorMapFcn<INTYPE, OUTTYPE> {
        public abstract OUTTYPE map(INTYPE arg);
    }
}
