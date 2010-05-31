package sketch.util.wrapper;

import static sketch.util.fcns.ZipWithIndex.zipwithindex;

import java.util.Collection;

import sketch.util.fcns.ZipIdxEnt;

/**
 * A few string functions that Python has and Java doesn't.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ScRichString {
    public String str;

    public ScRichString(String base) {
        str = base;
    }

    public String join(String[] arr) {
        if (arr == null) {
            return "<null list>";
        }
        StringBuilder b = new StringBuilder();
        for (String item : arr) {
            if (b.length() != 0) {
                b.append(str);
            }
            b.append(item);
        }
        return b.toString();
    }

    public String join(Object[] arr) {
        if (arr == null) {
            return "<null list>";
        }
        String[] as_string = new String[arr.length];
        for (int a = 0; a < arr.length; a++) {
            if (arr[a] == null) {
                as_string[a] = "<null>";
            } else {
                try {
                    as_string[a] = arr[a].toString();
                } catch (IllegalStateException e) {
                    if (!e.getMessage().contains("please enable asserts")) {
                        System.err.println("re-throwing exception");
                        throw e;
                    } else {
                        as_string[a] =
                                "<couldn't get string for " +
                                        arr[a].getClass().getName() + ">";
                    }
                }
            }
        }
        return join(as_string);
    }

    // java's string class is kinda thin
    public String rtrim(String trim) {
        while (true) {
            int start_idx = str.length() - trim.length();
            if (start_idx >= str.length() || !str.substring(start_idx).equals(trim)) {
                break;
            }
            str = str.substring(0, start_idx);
        }
        return str;
    }

    public String lpad(int nspaces) {
        StringBuilder result = new StringBuilder();
        for (int a = 0; a < nspaces - str.length(); a++) {
            result.append(' ');
        }
        result.append(str);
        return result.toString();
    }

    public String join(Collection<String> strList) {
        if (strList == null) {
            return "<null list>";
        }
        String result = "";
        for (ZipIdxEnt<String> elt : zipwithindex(strList)) {
            result += elt.entry + (elt.isLast ? "" : ", ");
        }
        return result;
    }
}
