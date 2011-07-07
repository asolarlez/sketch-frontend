package sketch.compiler.main.passes;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.main.cmdline.SketchOptions;
import sketch.util.exceptions.LastGoodProgram;
import sketch.util.exceptions.SketchException;

import static sketch.util.DebugOut.printDebug;

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
    protected final String name;
    protected final String description;

    public MetaStage(String name, String description, TempVarGen varGen,
            SketchOptions options)
    {
        this.name = name;
        this.description = description;
        this.varGen = varGen;
        this.options = options;
    }

    public final Program visitProgram(Program prog) {
        try {
            if (options.debugOpts.printPasses) {
                printDebug("Running stage '" + this.name + "' -- " + this.description);
            }
            if (options.debugOpts.dumpBefore.contains(this.name)) {
                prog.debugDump("Before stage " + this.name);
            }
            Program result = visitProgramInner(prog);
            if (options.debugOpts.dumpAfter.contains(this.name)) {
                result.debugDump("After stage " + this.name);
            }
            return result;
        } catch (SketchException e) {
            if (prog != null) {
                e.setLastGoodProgram(new LastGoodProgram(this.getClass().getSimpleName(),
                        prog));
            }
            throw e;
        }
    }

    public abstract Program visitProgramInner(Program prog);
}
