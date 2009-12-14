package sketch.util.annot;

/**
 * Designation that code has been generated with a bash-compatible command line
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public @interface ScGenerateWith {
    public String cwd();

    public String value();
}
