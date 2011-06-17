package sketch.compiler.main.passes;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.util.exceptions.LastGoodProgram;
import sketch.util.exceptions.SketchException;

/**
 * A meta-stage of compilation
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public abstract class MetaStage extends FEReplacer {
    protected final TempVarGen varGen;
    protected final SketchOptions options;

    public MetaStage(TempVarGen varGen, SketchOptions options) {
        this.varGen = varGen;
        this.options = options;
    }

    public final Program visitProgram(Program prog) {
        try {
            return visitProgramInner(prog);
        } catch (SketchException e) {
            e.setLastGoodProgram(new LastGoodProgram(this.getClass().getSimpleName(),
                    prog));
            throw e;
        }
    }

    public abstract Program visitProgramInner(Program prog);
}
