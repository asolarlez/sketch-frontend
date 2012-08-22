package sketch.compiler.stencilSK.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstFloat;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

import static sketch.util.DebugOut.printDebug;

public class ReplaceFloatsWithBits extends SymbolTableVisitor{
	
	public static final Type FLOAT = TypePrimitive.floattype;
	
	TempVarGen varGen;
	
	Map<Float, Function> floatConstants = new HashMap<Float, Function>();		
	
	public ReplaceFloatsWithBits(TempVarGen varGen){
		super(null);
		this.varGen = varGen;
	}

    Function newFloatFunction(String flName) {
        printDebug("newFloatFunction", flName);
        List<Parameter> pl = new ArrayList<Parameter>(1);
        pl.add(new Parameter(TypePrimitive.bittype, "_out", Parameter.OUT));
        return Function.creator((FEContext) null, flName, FcnType.Uninterp).returnType(
                TypePrimitive.bittype).params(pl).pkg(nres.curPkg().getName()).create();
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
		List<Expression> pl = new ArrayList<Expression>(1);
		ExprVar ev = new ExprVar(fexp, varGen.nextVar());
		pl.add(ev);
		addStatement(new StmtVarDecl(fexp, TypePrimitive.bittype, ev.getName(), null));
		addStatement(new StmtExpr( new ExprFunCall(fexp, name, pl ) ));
		return ev;
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
                case ExprBinary.BINOP_EQ: newOp = ExprBinary.BINOP_EQ; break;
        	default:
        		assert false : "You can't apply this floating point operation if you are doing floating-point to boolean replacement." + exp + " " + exp.getOp();
        }        
        if (left == exp.getLeft() && right == exp.getRight() && newOp == exp.getOp())
            return exp;
        else
            return new ExprBinary(exp, newOp, left, right, exp.getAlias());
    }
	
	public Object visitStreamSpec(StreamSpec spec)
    {
        spec = (StreamSpec) super.visitStreamSpec(spec);
        if (floatConstants.size() == 0) {
            return spec;
        } else {
            List<Function> lf = new ArrayList<Function>(spec.getFuncs());
            lf.addAll(floatConstants.values());
            return new StreamSpec(spec, spec.getName(), spec.getStructs(),
                    spec.getVars(), lf);
        }
    }
	
	
	
	
	
	
	
	
}
