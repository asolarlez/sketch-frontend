package sketch.util.exceptions;

import sketch.compiler.ast.core.Program;

/**
 * crash info -- last good program and the stage that was running
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class LastGoodProgram {
    public final String stageName;
    public final Program prog;

    public LastGoodProgram(String stageName, Program prog) {
        this.stageName = stageName;
        this.prog = prog;
    }
}
