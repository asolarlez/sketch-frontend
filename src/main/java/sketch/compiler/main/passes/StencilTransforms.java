package sketch.compiler.main.passes;

import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.compiler.passes.lowering.EliminateNestedArrAcc;
import sketch.compiler.stencilSK.FunctionalizeStencils;
import sketch.compiler.stencilSK.MatchParamNames;

/**
 * Stencil transformations
 * 
 * @author Armando Solar-Lezama
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class StencilTransforms extends MetaStage {
    public StencilTransforms(TempVarGen varGen, SketchOptions options) {
        super("sten", "Stencil transformations", varGen, options);
    }

    @Override
    public Program visitProgramInner(Program p) {
        p = (Program) p.accept(new MatchParamNames());
        p = (Program) p.accept(new EliminateNestedArrAcc(true));

        // dump(p, "BEFORE Stencilification");
        FunctionalizeStencils fs =
                new FunctionalizeStencils(varGen, options.bndOpts.arrSize);
        p = (Program) p.accept(fs); // convert Function's to ArrFunction's
        p = fs.processFuns(p, varGen); // process the ArrFunction's and create new
                                       // Function's
        // p.debugDump("After stencilification");
        return p;
    }
}
