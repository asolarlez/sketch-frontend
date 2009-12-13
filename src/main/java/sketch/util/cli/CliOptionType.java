package sketch.util.cli;

import sketch.util.ScCloneable;

/**
 * support for option types other than float, long, etc. see gaoptions for
 * example usage.
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public interface CliOptionType<T> extends ScCloneable<T> {
    public T fromString(String value);
}
