package sketch.compiler.stencilSK.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstFloat;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class ReplaceFloatsWithBits extends SymbolTableVisitor{
	
	public static final Type FLOAT = TypePrimitive.floattype;
	
	
	Map<Float, Function> floatConstants = new HashMap<Float, Function>();		
	
	public ReplaceFloatsWithBits(){
		super(null);
	}
	
	
	Function newFloatFunction(String flName){
		return Function.newUninterp(flName, TypePrimitive.bittype, new ArrayList<Parameter>(0));
	}
	
	
	String fName(Float fl){
		String name = fl.toString();
		name  = name.replace('.', '_');
		return "FL_" + name;
	}
	
	public Object visitExprConstFloat(ExprConstFloat fexp){
		String name = null;
		
		Float fl = new Float(fexp.getVal());
		if(floatConstants.containsKey(fl)){
			name = floatConstants.get(fl).getName();
		}else{
			name = fName(fl);			
			floatConstants.put(fl, newFloatFunction(name));			
		}
		return new ExprFunCall(fexp, name, new ArrayList<Expression>(0) );
	}
	
	
	
	
	
	public Object visitTypePrimitive(TypePrimitive t) {
		if(t.equals(FLOAT)){
			return TypePrimitive.bittype;
		}
    	return t; 
    }
	
	

	@Override
	public Object visitParameter(Parameter par){
		Type ot = par.getType();
		Type t = (Type) par.getType().accept(this);

		symtab.registerVar(par.getName(),
                actualType(ot), // We want to remember the old type even if the type changed.
                par,
                SymbolTable.KIND_FUNC_PARAM);

		if( t == par.getType()){
    		return par;
    	}else{
    		return new Parameter(t, par.getName(), par.getPtype() );
    	}
	}
	
	
	public Object visitExprBinary(ExprBinary exp)
    {
		Type ltype = getType(exp.getLeft());
		Type rtype = getType(exp.getRight());
		
		if(!ltype.equals(FLOAT) && !rtype.equals(FLOAT)){
			return super.visitExprBinary(exp);
		}		 
        Expression left = doExpression(exp.getLeft());
        Expression right = doExpression(exp.getRight());
        int newOp = exp.getOp();
        switch(exp.getOp()){
        	case ExprBinary.BINOP_ADD: newOp = ExprBinary.BINOP_BXOR; break;
        	case ExprBinary.BINOP_SUB: newOp = ExprBinary.BINOP_BXOR; break;
        	case ExprBinary.BINOP_MUL: newOp = ExprBinary.BINOP_BAND; break;
        	default:
        		assert false : "You can't apply this floating point operation if you are doing floating-point to boolean replacement." + exp;
        }        
        if (left == exp.getLeft() && right == exp.getRight() && newOp == exp.getOp())
            return exp;
        else
            return new ExprBinary(exp, newOp, left, right, exp.getAlias());
    }
	
	public Object visitStreamSpec(StreamSpec spec)
    {
		spec = (StreamSpec)super.visitStreamSpec(spec);		
		spec.getFuncs().addAll(floatConstants.values());
		return spec;
    }
	
	
	
	
	
	
	
	
}
