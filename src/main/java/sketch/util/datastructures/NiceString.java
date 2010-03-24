package sketch.util.datastructures;

/**
 * string with a precomputed hash code.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class NiceString extends DefaultNiceObj<NiceString> {
    protected final String value;

    public NiceString(final String value) {
        super(NiceString.betterStringHash(value), 10761305050607L);
        this.value = value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

    /**
     * the normal Java string hash has collisions with simple words like slqkkm and
     * kibbxott. See revision 2d88c9b0d08a / WordHashTest.
     */
    public static long betterStringHash(final String value) {
        long result = 18833637047881L * (value.length() + 1);
        for (int a = 0; a < value.length(); a++) {
            result = result * 12797199422779L + value.charAt(a);
        }
        return result;
    }

    @Override
    public int baseCompare(final NiceString o) {
        return this.getValue().compareTo(o.getValue());
    }

    public String getValue() {
        return this.value;
    }
}
