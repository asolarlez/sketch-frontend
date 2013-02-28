/**
 *
 */
package sketch.compiler.passes.lowering;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.typs.TypeArray;

/**
 * Nullify the "dims" property in any array type
 * 
 * @author Chris Jones
 */
public class EliminateArrayDims extends FEReplacer {
    @Override
    public Object visitTypeArray(TypeArray ta) {
        return ta.nullifyDims();
    }
}