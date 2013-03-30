package sketch.compiler.main.passes;

import java.util.HashSet;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.main.cmdline.SketchOptions;

public class TruncateVarArray extends FEReplacer {
    private int arrSize;

    public TruncateVarArray(SketchOptions options) {
        this.arrSize = options.bndOpts.arrSize;
    }

    @Override
    public Object visitStreamSpec(Package p) {
        HashSet<String> tochange = new HashSet<String>();
        List<Function> funcs = p.getFuncs();
        for (int i = 0; i < funcs.size(); ++i) {
            Function f = funcs.get(i);
            String spec = f.getSpecification();
            if (spec != null) {
                tochange.add(spec);
            }
        }

        for (int i = 0; i < funcs.size(); ++i) {
            Function f = funcs.get(i);
            String name = f.getName();
            if (tochange.contains(name) || f.getFcnType() == Function.FcnType.Harness) {
                f = addAssumes(f);
                funcs.set(i, f);
            }
        }

        return p;
    }

    private Function addAssumes(Function f) {
        // TODO Auto-generated method stub
        return null;
    }
}
