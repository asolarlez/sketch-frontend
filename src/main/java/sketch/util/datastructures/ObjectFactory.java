package sketch.util.datastructures;

/**
 * stupid hack to avoid deficiencies in the Java language (can't have abstract
 * static methods)
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public abstract class ObjectFactory<T> {
    public abstract T create();
}
