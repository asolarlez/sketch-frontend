package sketch.compiler.smt.passes;

import java.util.ArrayList;
import java.util.List;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;

public class ReplaceStructTypeWithInt extends FEReplacer {

	
	@Override
	public Object visitTypeStruct(TypeStruct ts) {
		return TypePrimitive.inttype;
	}
	
	@Override
	public Object visitTypeStructRef(TypeStructRef tsr) {
		return TypePrimitive.inttype;
	}
	
	@Override
	public Object visitExprNullPtr(ExprNullPtr nptr) {
		return new ExprConstInt(nptr, -1);
	}
	
	@Override
	public Object visitExprTypeCast(ExprTypeCast exp)
    {
	    Type newType = (Type) exp.getType().accept(this);
        Expression expr = doExpression(exp.getExpr());
        if (expr == exp.getExpr() && newType == exp.getType())
            return exp;
        else
            return new ExprTypeCast(exp, exp.getType(), expr);
    }
	
	public Object visitFieldDecl(FieldDecl field)
    {
        List<Type> newTypes = new ArrayList<Type>();
        for (int i = 0; i < field.getTypes().size(); i++)
        {
            Type t = (Type) field.getType(i).accept(this);
            newTypes.add(t);
        }
        return new FieldDecl(field, newTypes,
                             field.getNames(), field.getInits());
    }
}
