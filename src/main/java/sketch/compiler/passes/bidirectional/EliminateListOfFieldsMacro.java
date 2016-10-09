package sketch.compiler.passes.bidirectional;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.NameResolver;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFieldsListMacro;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.NotYetComputedType;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * This class eliminates the list of fields macro of the form e.{T}
 */
public class EliminateListOfFieldsMacro extends BidirectionalPass {

    @Override
    public Object visitExprFieldsListMacro(ExprFieldsListMacro exp) {
		NameResolver nres = nres();
        Type t = exp.getType();
        if (t.isStruct())
            if (nres.isTemplate(((TypeStructRef) t).getName()))
                return exp;
		if (t instanceof NotYetComputedType) {
			return new ExprArrayInit(exp.getContext(),
					new ArrayList<Expression>());
		}
		final Expression left = exp.getLeft();
		if (driver.getType(left).isStruct()) {
			StructDef ts = driver.getStructDef((TypeStructRef) driver
					.getType(exp.getLeft()));
            List<Expression> matchedFields = new ArrayList<Expression>();

            for (StructFieldEnt e : ts.getFieldEntriesInOrder()) {
				if (t.isArray() && !e.getType().isArray())
					continue;
                if (e.getType().promotesTo(t, nres)) {
					ExprField field = new ExprField(left,
							exp.getLeft(), e.getName(), false);
					if (t.isArray()) {
						// TODO: Is this casting necessary? There are also other
						// places where I do this casting.
						Expression len = ((TypeArray) t).getLength();
						if (len == null) {
							len = ((TypeArray) e.getType()).getLength();
							FEReplacer repVars = new FEReplacer() {
								public Object visitExprVar(ExprVar ev) {
									return new ExprField(left, ev.getName());
								}
							};
							len = (Expression) len.accept(repVars);
						}
						matchedFields.add(new ExprArrayRange(exp, field,
								new ExprArrayRange.RangeLen(ExprConstInt.zero,
										len)));
					} else {
						matchedFields.add(field);
					}
                }
            }
			if (t.isArray() && matchedFields.isEmpty()) {
				matchedFields.add(new ExprArrayInit(exp.getContext(),
						new ArrayList<Expression>()));
			}
            return new ExprArrayInit(exp.getContext(), matchedFields);
        }else{
            throw new ExceptionAtNode("ExprLeft must be of type struct", exp);

        }

    }

}
