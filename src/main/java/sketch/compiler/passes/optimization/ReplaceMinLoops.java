package sketch.compiler.passes.optimization;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.stmts.StmtMinLoop;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.GlobalsToParams;
import sketch.compiler.passes.preprocessing.MainMethodCreateNospec;

/**
 * replace special constructs
 * 
 * <pre>
 * minloop {
 *     body
 * }
 * </pre>
 * 
 * with (a bit messy, due to some bugs with globals)
 * 
 * <pre>
 * int globalTmpVar;
 * 
 * int localTmpVar = ??;
 * assert(globalTmpVar == localTmpVar)
 * repeat(localTmpVar) {
 *     body
 * }
 * 
 * void setLoopBounds() {
 *     globalTmpVar = ??;
 *     minimize (globalTmpVar + ...)
 * }
 * 
 * void myfunction implements ... {
 *     setLoopBounds();
 * }
 * </pre>
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
@CompilerPassDeps(runsAfter = { MainMethodCreateNospec.class }, runsBefore = { GlobalsToParams.class })
public class ReplaceMinLoops extends FEReplacer {
    protected final TempVarGen varGen;

    public ReplaceMinLoops(TempVarGen varGen) {
        this.varGen = varGen;
    }

    @Override
    public Object visitStmtMinLoop(StmtMinLoop stmtMinLoop) {
        // TODO Auto-generated method stub
        return super.visitStmtMinLoop(stmtMinLoop);
    }
}
