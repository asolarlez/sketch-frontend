package sketch.compiler.passes.structure;

import java.util.Vector;

import sketch.compiler.ast.core.exprs.ExprTprint;
import sketch.util.datastructures.TprintTuple;

public class GetTprintIdentifiers extends ASTObjQuery<Vector<TprintIdentifier>> {
    public GetTprintIdentifiers() {
        super(new Vector<TprintIdentifier>());
    }

    @Override
    public Object visitExprTprint(ExprTprint exprTprint) {
        TprintIdentifier id = new TprintIdentifier();
        for (TprintTuple t : exprTprint.expressions) {
            id.add(t.getFirst());
        }
        if (!this.result.contains(id)) {
            result.add(id);
        }
        return super.visitExprTprint(exprTprint);
    }
}
