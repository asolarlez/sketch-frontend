package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFieldsListMacro;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.ExceptionAtNode;

/**
 * This class eliminates the list of fields macro of the form e.{T}
 */
public class EliminateListOfFieldsMacro extends SymbolTableVisitor{

    public EliminateListOfFieldsMacro() {
        super(null);
    }


    @Override
    public Object visitExprFieldsListMacro(ExprFieldsListMacro exp) {
        Type t = exp.getType();
        if (t.isStruct())
            if (nres.isTemplate(((TypeStructRef) t).getName()))
                return exp;

        if (getType(exp.getLeft()).isStruct()) {
            StructDef ts = getStructDef((TypeStructRef) getType(exp.getLeft()));
            List<Expression> matchedFields = new ArrayList<Expression>();

            for (StructFieldEnt e : ts.getFieldEntriesInOrder()) {
				if (t.isArray() && !e.getType().isArray())
					continue;
                if (e.getType().promotesTo(t, nres)) {
                    matchedFields.add(new ExprField(exp.getLeft(), exp.getLeft(),
                            e.getName(), false));
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
