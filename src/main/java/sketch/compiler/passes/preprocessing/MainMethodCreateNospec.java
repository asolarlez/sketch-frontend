package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

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
    public final Vector<Function> mainFcns = new Vector<Function>();
    public final HashSet<String> mainNames;
    public final Random rand = new Random();

    public MainMethodCreateNospec(String csvMainNames) {
        String[] mainNamesArray = csvMainNames.split(",");
        if (csvMainNames.equals("")) {
            mainNamesArray = new String[0];
        }
        mainNames = new HashSet<String>(mainNamesArray.length);
        for (String name : mainNamesArray) {
            assert !name.contains(";") && !name.contains(" ") : "illegal chars in main name, use commas to separate values";
            mainNames.add(name);
        }
    }

    @SuppressWarnings( { "deprecation", "unchecked" })
    @Override
    public Object visitStreamSpec(StreamSpec spec) {
        // see super for how to create a new one
        spec = (StreamSpec) super.visitStreamSpec(spec);
        if (!mainFcns.isEmpty()) {
            ArrayList<Function> newFcns = new ArrayList<Function>();
            newFcns.addAll(spec.getFuncs());
            for (Function mainFcn : mainFcns) {
                newFcns.add(Function.newStatic(FEContext.artificalFrom(
                        "nospec", mainFcn), mainFcn.getSpecification(),
                        TypePrimitive.voidtype, mainFcn.getParams(), null,
                        new StmtBlock(Collections.EMPTY_LIST)));
            }
            return new StreamSpec(spec, spec.getType(), spec.getStreamType(),
                    spec.getName(), spec.getParams(), spec.getVars(),
                    Collections.unmodifiableList(newFcns));
        } else {
            return spec;
        }
    }

    @Override
    public Object visitFunction(Function func) {
        boolean isMainName = mainNames.contains(func.getName());
        if (isMainName && func.getSpecification() == null
                && func.getReturnType().equals(TypePrimitive.voidtype))
        {
            return add_main_fcn(func);
        } else {
            return super.visitFunction(func);
        }
    }

    public Function add_main_fcn(Function func) {
        String nospecName = "nospec" + Math.abs((rand).nextInt());
        Function next =
                new Function(func, func.getCls(), func.getName(), func
                        .getReturnType(), func.getParams(), nospecName, func
                        .getBody());
        mainFcns.add(next);
        return next;
    }
}
