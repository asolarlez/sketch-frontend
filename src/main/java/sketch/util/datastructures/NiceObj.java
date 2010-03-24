package sketch.util.datastructures;

/**
 * comparable and long hash. unfortunately, there's no way to enforce implementation of
 * equals() and hashCode().
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public interface NiceObj<T> extends Comparable<T>, LongHashObject {}
