package sketch.compiler.passes.cleanup;

import java.util.Vector;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.stmts.StmtMinimize;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.util.fcns.VectorMap.VectorMapFcn;
import static sketch.util.fcns.VectorMap.vecmap_nonnull;

/**
 * remove minimize functions from final code printout
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = {}, runsBefore = {})
public class CleanupRemoveMinFcns extends FEReplacer {
    protected static boolean isMinFcn;

    @Override
    public Object visitPackage(Package spec) {
        Vector<Function> newFcns =
                vecmap_nonnull(spec.getFuncs(), new VectorMapFcn<Function, Function>() {
                    public Function map(Function fcn) {
                        isMinFcn = false;
                        Object result = fcn.accept(CleanupRemoveMinFcns.this);
                        return (Function) ((isMinFcn) ? null : result);
                    }
                });
        return new Package(spec, spec.getName(),
                spec.getStructs(), spec.getVars(), newFcns);
    }

    @Override
    public Object visitStmtMinimize(StmtMinimize stmtMinimize) {
        if (!stmtMinimize.userGenerated) {
            isMinFcn = true;
        }
        return stmtMinimize;
    }
}
