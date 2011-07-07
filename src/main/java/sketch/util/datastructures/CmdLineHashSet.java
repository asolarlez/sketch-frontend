package sketch.util.datastructures;

import java.util.Arrays;
import java.util.HashSet;

/**
 * create a hash set from comma-separated command line values
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class CmdLineHashSet extends TypedHashSet<String> {
    public CmdLineHashSet() {}

    public CmdLineHashSet(String cmdline) {
        super(new HashSet<String>(Arrays.asList(cmdline.split(","))));
    }

    @Override
    protected String clsname() {
        return "";
    }
}
