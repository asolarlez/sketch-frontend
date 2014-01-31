package sketch.compiler.passes.preprocessing;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFieldMacro;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.StructDef;
import sketch.compiler.ast.core.typs.StructDef.StructFieldEnt;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.ExceptionAtNode;

public class EliminateMacros extends SymbolTableVisitor{
    
    public EliminateMacros() {
        super(null);
    }

    @Override
    public Object visitExprFieldMacro(ExprFieldMacro exp){
        Type t = exp.getType();
        Type ltype = getType(exp.getLeft());
        if (!(ltype instanceof TypeStructRef)) {
            throw new ExceptionAtNode("Left hand side is not a struct type", exp);
        }
        StructDef ts = getStructDef((TypeStructRef) ltype);
        List<Expression> matchedFields = new ArrayList<Expression>();
        for (StructFieldEnt e : ts.getFieldEntries()) {
            if (e.getType().promotesTo(t, nres)) {
                matchedFields.add(new ExprField(exp.getLeft(), exp.getLeft(),
                        e.getName(), false));
            }
        }
        return new ExprArrayInit(exp.getContext(), matchedFields);
        
    }

}
