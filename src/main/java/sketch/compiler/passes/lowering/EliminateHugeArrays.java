package sketch.compiler.passes.lowering;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;

public class EliminateHugeArrays extends FEReplacer {

    @Override
    public Object visitTypeArray(TypeArray t) {
        Type base = (Type) t.getBase().accept(this);
        Expression len = t.getLength();
        if (len != null) {
            Integer lv = len.getIValue();
			if (lv != null && lv > 1000) {
                len = null;
            }
        }
        if (len == t.getLength() && base == t.getBase()) {
            return t;
        } else {
            return new TypeArray(base, len);
        }
    }
}
