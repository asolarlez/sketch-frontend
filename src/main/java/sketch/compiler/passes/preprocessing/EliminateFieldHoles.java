package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.regens.ExprAlt;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect;
import sketch.compiler.ast.core.exprs.regens.ExprChoiceSelect.SelectField;
import sketch.compiler.ast.core.exprs.regens.ExprRegen;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class EliminateFieldHoles extends SymbolTableVisitor {

    public EliminateFieldHoles() {
        super(null);
        // TODO Auto-generated constructor stub
    }

    @Override
    public Object visitExprField(ExprField exp) {
        if (exp.isHole()) {
            Type t = exp.getTypeOfHole();
            StructDef ts = getStructDef((TypeStructRef) getType(exp.getLeft()));
            List<String> matchedFields = new ArrayList<String>();
            for (StructFieldEnt e : ts.getFieldEntries()) {
                if (e.getType().promotesTo(t, nres)) {
                    matchedFields.add(e.getName());
                }
            }
            if (matchedFields.isEmpty()) {
                return new ExprNullPtr();
            }
 else if (matchedFields.size() == 1) {
                return new ExprField(exp.getLeft(), matchedFields.get(0), false);
            } else {
                ExprAlt prev =
                        new ExprAlt(new ExprChoiceSelect(exp.getLeft(), new SelectField(
                                matchedFields.get(0))), new ExprChoiceSelect(
                                exp.getLeft(), new SelectField(matchedFields.get(1))));
                for (int i = 2; i < matchedFields.size(); i++) {
                    prev =
                            new ExprAlt(new ExprChoiceSelect(exp.getLeft(),
                                    new SelectField(matchedFields.get(i))), prev);
                }
                return new ExprRegen(prev, prev);

            }

            // return exp;
        } else {
            return exp;
        }
    }

}
