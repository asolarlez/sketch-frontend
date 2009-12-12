package sketch.util;

/**
 * generic version of cloneable (avoids casts)
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public interface ScCloneable<T> {
    public T clone();
}
