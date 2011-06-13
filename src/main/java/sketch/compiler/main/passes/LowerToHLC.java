package sketch.compiler.main.passes;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.main.seq.SequentialSketchOptions;
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
    public LowerToHLC(TempVarGen varGen, SequentialSketchOptions options) {
        super(varGen, options);
    }

    public Program visitProgram(Program prog) {
        prog = (Program) prog.accept(new EliminateBitSelector(varGen));
        prog = (Program) prog.accept(new EliminateArrayRange(varGen));
        return prog;
    }
}
