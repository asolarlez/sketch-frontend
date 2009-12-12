package sketch.util.fcns;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import sketch.util.ScCloneable;

/**
 * extend arrays
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class ScArrayUtil {
    public static int[] extend_arr(int[] arr, int sz) {
        int[] next = new int[sz];
        System.arraycopy(arr, 0, next, 0, arr.length);
        return next;
    }

    public static long[] extend_arr(long[] arr, int sz) {
        long[] next = new long[sz];
        System.arraycopy(arr, 0, next, 0, arr.length);
        return next;
    }

    public static boolean[] extend_arr(boolean[] arr, int sz) {
        boolean[] next = new boolean[sz];
        System.arraycopy(arr, 0, next, 0, arr.length);
        return next;
    }

    public static float[] extend_arr(float[] arr, int sz) {
        float[] next = new float[sz];
        System.arraycopy(arr, 0, next, 0, arr.length);
        return next;
    }

    /**
     * @param clone
     *            <ul>
     *            <li>2 to deep clone (clone all elements within all vectors).
     *            Elements must extend ScCloneable.</li>
     *            <li>1 to clone vectors themselves</li>
     *            <li>0 to copy vector references</li>
     *            </ul>
     * @param empty_not_null_sz
     *            -1 to create null elements, $n$ to create empty vectors with
     *            $n$ capacity.
     */
    @SuppressWarnings("unchecked")
    public static <T> Vector<T>[] extend_arr(Vector<T>[] arr, int sz,
            int empty_not_null_sz, int clone)
    {
        Vector<T>[] next = new Vector[sz];
        for (int i = 0; i < arr.length; i++) {
            if (clone == 2) {
                next[i] =
                        (Vector<T>) deep_clone((Vector<ScCloneable<?>>) arr[i]);
            } else if (clone == 1) {
                next[i] = (Vector<T>) arr[i].clone();
            } else {
                next[i] = arr[i];
            }
        }
        if (empty_not_null_sz >= 0) {
            for (int a = arr.length; a < sz; a++) {
                next[a] = new Vector(empty_not_null_sz);
            }
        }
        return next;
    }

    public static int[] extend_arr(int[] arr, int sz, int defaultv) {
        int[] next = new int[sz];
        System.arraycopy(arr, 0, next, 0, arr.length);
        for (int a = arr.length; a < sz; a++) {
            next[a] = defaultv;
        }
        return next;
    }

    public static long[] extend_arr(long[] arr, int sz, long defaultv) {
        long[] next = new long[sz];
        System.arraycopy(arr, 0, next, 0, arr.length);
        for (int a = arr.length; a < sz; a++) {
            next[a] = defaultv;
        }
        return next;
    }

    public static boolean[] extend_arr(boolean[] arr, int sz, boolean defaultv)
    {
        boolean[] next = new boolean[sz];
        System.arraycopy(arr, 0, next, 0, arr.length);
        for (int a = arr.length; a < sz; a++) {
            next[a] = defaultv;
        }
        return next;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ScCloneable> Vector<T> deep_clone(Vector<T> arr) {
        Vector result = new Vector(arr.size());
        for (ScCloneable elt : arr) {
            result.add(elt.clone());
        }
        return result;
    }

    public static <T> List<T> arrayToList(T[] array) {
        ArrayList<T> as_list = new ArrayList<T>();
        for (T elt : array) {
            as_list.add(elt);
        }
        return Collections.unmodifiableList(as_list);
    }
}
