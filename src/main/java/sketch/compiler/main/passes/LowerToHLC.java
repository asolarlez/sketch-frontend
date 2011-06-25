package sketch.compiler.main.passes;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.lowering.EliminateArrayRange;
import sketch.compiler.passes.lowering.EliminateBitSelector;

/**
 * Lower to high-level C code -- i.e. don't lower models like SPMD or stencils
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class LowerToHLC extends MetaStage {
    public LowerToHLC(TempVarGen varGen, SketchOptions options) {
        super(varGen, options);
    }

    public Program visitProgramInner(Program prog) {
        prog = (Program) prog.accept(new EliminateBitSelector(varGen));
        prog = (Program) prog.accept(new EliminateArrayRange(varGen));
        return prog;
    }
}
