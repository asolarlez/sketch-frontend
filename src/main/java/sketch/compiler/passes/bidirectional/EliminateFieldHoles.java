package sketch.compiler.passes.bidirectional;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * Expand new ??(...)
 */
public class EliminateFieldHoles extends BidirectionalPass {

    @Override
    public Object visitExprField(ExprField exp) {
        if (exp.isHole()) {
			Type t = driver.tdstate.getExpected();
            if (t.isStruct())
				if (nres().isTemplate(((TypeStructRef) t).getName()))
                    return exp;
			if (driver.getType(exp.getLeft()).isStruct()) {
                List<Expression> matchedFields = new ArrayList<Expression>();

				String cur = ((TypeStructRef) driver.getType(exp.getLeft()))
						.getName();
				while (cur != null) {
					StructDef ts = nres().getStruct(cur);
					for (StructFieldEnt e : ts.getFieldEntriesInOrder()) {
						if (t.isArray() && !e.getType().isArray())
							continue;
						if (e.getType().promotesTo(t, nres())) {
							ExprField field = new ExprField(exp.getLeft(), exp
									.getLeft(),
	                                e.getName(), false);
							if (t.isArray()) {
								matchedFields.add(new ExprArrayRange(exp, field,
				                        new ExprArrayRange.RangeLen(ExprConstInt.zero, ((TypeArray) t).getLength())));
							} else {
								matchedFields.add(field);
							}
						}
					}
					cur = nres().getStructParentName(cur);
                }
                if (matchedFields.isEmpty()) {
					if (t.isArray())
						return new ExprArrayInit(exp,
								new ArrayList<Expression>());
                    return t.defaultValue();
                } else if (matchedFields.size() == 1) {
                    return matchedFields.get(0);
                } else {
                    ExprStar expStar = new ExprStar(exp, 5, TypePrimitive.int32type);
                    return new ExprArrayRange(exp, new ExprArrayInit(exp.getContext(),
                            matchedFields), expStar);
                }
            } else {
                throw new ExceptionAtNode("ExprLeft must be of type struct", exp);

            }
        }
        return exp;
    }

}
