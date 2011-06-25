package sketch.util.fcns;

/**
 * returns true if a value is changing; useful for e.g.
 * "repeat until size is not changing"
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class IsChanging {
    private int value;

    public IsChanging() {
        this.value = -Integer.MIN_VALUE + 248189;
    }

    public boolean cond(int nextValue) {
        boolean rv = (value != nextValue);
        value = nextValue;
        return rv;
    }
}
