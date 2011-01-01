package sketch.compiler.passes.structure;

import java.util.List;
import java.util.Vector;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.PrintFcnType;

public class GetPrintFcns extends FEReplacer {
    Vector<Function> funcs = new Vector<Function>();

    public static List<Function> run(FENode n) {
        GetPrintFcns gpf = new GetPrintFcns();
        n.accept(gpf);
        return gpf.funcs;
    }
    
    @Override
    public Object visitFunction(Function func) {
        if (func.getInfo().printType == PrintFcnType.Printfcn) {
            funcs.add(func);
        }
        return func;
    }
}
