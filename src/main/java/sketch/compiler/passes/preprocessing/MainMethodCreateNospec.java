package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.typs.TypePrimitive;

/**
 * create an artificial nospec() function if there is a main function present
 * (that doesn't have a specification).
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class MainMethodCreateNospec extends FEReplacer {
    public Function mainFunction;
    public String mainName;
    public String nospecName;

    public MainMethodCreateNospec(String mainName) {
        this.mainName = mainName;
        nospecName = "nospec" + Math.abs((new Random()).nextInt());
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public Object visitStreamSpec(StreamSpec spec) {
        // see super for how to create a new one
        spec = (StreamSpec) super.visitStreamSpec(spec);
        if (mainFunction != null) {
            ArrayList<Function> newFcns = new ArrayList<Function>();
            newFcns.addAll(spec.getFuncs());
            newFcns.add(Function.newStatic(FEContext.artificalFrom("nospec",
                    mainFunction), nospecName, TypePrimitive.voidtype,
                    mainFunction.getParams(), null, new StmtBlock(Collections.EMPTY_LIST)));
            return new StreamSpec(spec, spec.getType(), spec.getStreamType(),
                    spec.getName(), spec.getParams(), spec.getVars(),
                    Collections.unmodifiableList(newFcns));
        } else {
            return spec;
        }
    }

    @Override
    public Object visitFunction(Function func) {
        if (func.getName().equals(mainName) && func.getSpecification() == null
                && func.getReturnType().equals(TypePrimitive.voidtype))
        {
            assert mainFunction == null : "two main functions?";
            mainFunction =
                    new Function(func, func.getCls(), func.getName(), func
                            .getReturnType(), func.getParams(), nospecName, func
                            .getBody());
            return mainFunction;
        } else {
            return super.visitFunction(func);
        }
    }
}
