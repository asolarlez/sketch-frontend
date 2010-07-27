package sketch.util.datastructures;

import sketch.compiler.ast.core.exprs.Expression;
import sketch.util.Pair;

public class TprintTuple extends Pair<String, Expression> {
    public TprintTuple(String first, Expression second) {
        super(first, second);
    }
}
