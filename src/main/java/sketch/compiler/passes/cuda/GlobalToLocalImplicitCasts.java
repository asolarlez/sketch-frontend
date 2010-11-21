package sketch.compiler.passes.cuda;

import java.util.Iterator;
import java.util.Vector;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.compiler.main.seq.SequentialSketchOptions;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.cuda.CudaThreadBlockDim;
import sketch.util.datastructures.TypedHashSet;

@CompilerPassDeps(runsBefore = {}, runsAfter = { GenerateAllOrSomeThreadsFunctions.class }, debug = true)
public class GlobalToLocalImplicitCasts extends SymbolTableVisitor {
    protected StreamSpec spec;
    protected CudaThreadBlockDim cudaBlockDim;

    public GlobalToLocalImplicitCasts(SequentialSketchOptions opts) {
        super(null);
        this.cudaBlockDim = opts.getCudaBlockDim();
    }

    @Override
    public Object visitStreamSpec(StreamSpec spec) {
        this.spec = spec;
        super.visitStreamSpec(spec);

        final CallReplacer cr = new CallReplacer(symtab);
        // FIXME -- return this when everything works
        StreamSpec spec1 = (StreamSpec) cr.visitStreamSpec(spec);
        Vector<Function> fcns = new Vector<Function>();
        for (Function fcn : spec1.getFuncs()) {
            // FIXME -- HACK!!!!!!!!
            if (!fcn.getName().startsWith("somethreads")) {
                fcns.add(fcn);
            }
            // if (cr.visitedFcns.contains(fcn.getName()) ||
            // fcn.getSpecification() != null ||
            // fcn.getName().equals("generated_nospec"))
            // {
            // fcns.add(fcn);
            // }
        }
        return spec1.newFromFcns(fcns);
    }

    protected class CallReplacer extends SymbolTableVisitor {
        protected TypedHashSet<String> visitedFcns = new TypedHashSet<String>();

        public CallReplacer(SymbolTable symtab) {
            super(symtab);
        }

        @Override
        public Object visitExprFunCall(ExprFunCall exp) {
            this.visitedFcns.add(exp.getName());
            // for local expressions that aren't already vectors, copy them many times
            Vector<Expression> nextParams = new Vector<Expression>();
            Function funcSigParams = spec.getFuncNamed(exp.getName());
            Iterator<Parameter> iter = funcSigParams.getParams().iterator();
            for (Expression e : exp.getParams()) {
                Parameter param = iter.next();
                final CudaMemoryType mt = getType(e).getCudaMemType();
                if (mt != CudaMemoryType.LOCAL_TARR &&
                        param.getType().getCudaMemType() == CudaMemoryType.LOCAL_TARR)
                {
                    Vector<Expression> e_dupl = new Vector<Expression>();
                    for (int a = 0; a < cudaBlockDim.all(); a++) {
                        e_dupl.add(e);
                    }
                    nextParams.add(new ExprArrayInit(exp, e_dupl));
                } else {
                    nextParams.add(e);
                }
            }
            assert nextParams.size() == exp.getParams().size();

            return new ExprFunCall(exp, exp.getName(), nextParams);
        }
    }
}
