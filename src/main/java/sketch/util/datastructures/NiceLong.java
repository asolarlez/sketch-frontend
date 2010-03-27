package sketch.util.datastructures;

/**
 * NOTE -- don't use DefaultNiceObj here, since we don't want to double the storage with
 * the $hash field.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class NiceLong implements NiceObj<NiceLong> {
    protected final long value;

    public NiceLong(final long value) {
        this.value = value;
    }

    public int baseCompare(final NiceLong o) {
        assert this.value != o.value : "hash collision";
        if (this.value < o.value) {
            return -1;
        }
        return 1;
    }

    public int compareTo(final NiceLong o) {
        if (this.value == o.value) {
            return 0;
        }
        if (this.value < o.value) {
            return -1;
        }
        return 1;
    }

    public long longHash() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return (int) this.longHash();
    }

    @Override
    public boolean equals(final Object obj) {
        if ((obj == null) || (!(obj instanceof NiceLong))) {
            return false;
        }
        NiceLong other = (NiceLong) obj;
        return this.value == other.value;
    }
}
