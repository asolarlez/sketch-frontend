package sketch.compiler.ast.core.typs;

/**
 * a more informative comparison, allowing for unknown values
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public enum TypeComparisonResult {
    EQ, MAYBE, NEQ;

    public static TypeComparisonResult knownOrNeq(boolean value) {
        return value ? EQ : NEQ;
    }

    public static TypeComparisonResult knownOrMaybe(boolean value) {
        return value ? EQ : MAYBE;
    }
}
