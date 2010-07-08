package sketch.compiler.passes.structure;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.util.datastructures.TypedHashMap;

/**
 * stupid visitor to get backwards links for "implements"
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class GetImplementedFcns extends FEReplacer {
    public TypedHashMap<String, String> sketchToSpec = new TypedHashMap<String, String>();

    @Override
    public Object visitFunction(Function fcn) {
        if (fcn.getSpecification() != null) {
            this.sketchToSpec.put(fcn.getName(), fcn.getSpecification());
        }
        return fcn;
    }
}
