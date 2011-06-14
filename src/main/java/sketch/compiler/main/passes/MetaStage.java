package sketch.compiler.main.passes;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.main.seq.SequentialSketchOptions;

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
    protected final SequentialSketchOptions options;

    public MetaStage(TempVarGen varGen, SequentialSketchOptions options) {
        this.varGen = varGen;
        this.options = options;
    }

    // TODO -- insert debugging code here
    public final Program visitProgram(Program prog) {
        return visitProgramInner(prog);
    }

    public abstract Program visitProgramInner(Program prog);
}
