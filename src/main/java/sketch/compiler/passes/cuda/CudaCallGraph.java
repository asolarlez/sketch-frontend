package sketch.compiler.passes.cuda;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.cuda.stmts.CudaSyncthreads;
import sketch.compiler.passes.structure.CallGraph;
import sketch.util.datastructures.TypedHashSet;

/**
 * compute additional information for cuda threading model transformations
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class CudaCallGraph extends CallGraph {
    protected final TypedHashSet<Function> fcnsCallingFcnsWithSyncthreads =
            new TypedHashSet<Function>();

    public CudaCallGraph(Program prog) {
        super.init(prog);
    }

    public boolean callsFcnWithSyncthreads(Function f) {
        return fcnsCallingFcnsWithSyncthreads.contains(f);
    }

    public boolean callsFcnWithSyncthreads(ExprFunCall exp) {
        return callsFcnWithSyncthreads(getTarget(exp));
    }

    @Override
    public Object visitCudaSyncthreads(CudaSyncthreads cudaSyncthreads) {
        assert enclosing != null;
        fcnsCallingFcnsWithSyncthreads.add(enclosing);
        return super.visitCudaSyncthreads(cudaSyncthreads);
    }

    @Override
    protected void buildEdges() {
        super.buildEdges();

        for (Function f : fcnsCallingFcnsWithSyncthreads) {
            for (Function e : closureEdges.callersTo(f)) {
                fcnsCallingFcnsWithSyncthreads.add(e);
            }
        }
    }
}
