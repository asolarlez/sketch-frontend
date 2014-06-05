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
        if (getType(exp.getLeft()).isStruct()) {
            // report(exp, "ExprVar must be of type struct");
        
        StructDef ts = getStructDef((TypeStructRef) getType(exp.getLeft()));
        List<Expression> matchedFields = new ArrayList<Expression>();
      
            for (StructFieldEnt e : ts.getFieldEntriesInOrder()) {
              
                if (e.getType().equals(t)) {
                matchedFields.add(new ExprField(exp.getLeft(), exp.getLeft(),
                        e.getName(), false));
            }
        }
        return new ExprArrayInit(exp.getContext(), matchedFields);
        }else{
            throw new ExceptionAtNode("ExprLeft must be of type struct", exp);

        }
        
    }

}
