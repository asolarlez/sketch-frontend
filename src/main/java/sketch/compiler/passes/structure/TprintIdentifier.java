package sketch.compiler.passes.structure;

import java.util.List;
import java.util.Vector;

/**
 * Gets the string keys for a tprint call
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class TprintIdentifier extends Vector<String> {
    private static final long serialVersionUID = 7858856422699462372L;

    public String id() {
        return get(0);
    }

    public List<String> fields() {
        return subList(1, this.size());
    }
}
