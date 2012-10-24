package sketch.compiler.stencilSK.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstFloat;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.SymbolTableVisitor;
import sketch.util.exceptions.ExceptionAtNode;

import static sketch.util.DebugOut.printDebug;

public class ReplaceFloatsWithBits extends SymbolTableVisitor{
	
	public static final Type FLOAT = TypePrimitive.floattype;
    public static final Type DOUBLE = TypePrimitive.doubletype;
	
    public static final double epsilon = 1e-10;

	TempVarGen varGen;
	
	Map<Float, Function> floatConstants = new HashMap<Float, Function>();		
	
	public ReplaceFloatsWithBits(TempVarGen varGen){
		super(null);
		this.varGen = varGen;
	}

    Function newFloatFunction(String flName) {
        printDebug("newFloatFunction", flName);
        List<Parameter> pl = new ArrayList<Parameter>(1);
        pl.add(new Parameter(replType(), "_out", Parameter.OUT));
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
        double flc = fexp.getVal();
        if (flc <= epsilon && flc >= -epsilon) {
            return ExprConstInt.zero;
        }

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
        addStatement(new StmtVarDecl(fexp, replType(), ev.getName(), null));
		addStatement(new StmtExpr( new ExprFunCall(fexp, name, pl ) ));
		return ev;
	}

    public Object visitExprTypeCast(ExprTypeCast exp) {
        Expression expr = doExpression(exp.getExpr());
        Type told = getType(exp.getExpr());
        Type tnew = exp.getType();
        if (told.equals(TypePrimitive.inttype) && isFloat(tnew)) {
            throw new ExceptionAtNode(
                    "You can't cast from ints to doubles/floats if you are using --fe-fencoding TO_BIT." +
                            exp, exp);
        }
        if (tnew.equals(TypePrimitive.inttype) && isFloat(told)) {
            throw new ExceptionAtNode(
                    "You can't cast from doubles/floats to int if you are using --fe-fencoding TO_BIT." +
                            exp, exp);
        }
        if (expr == exp.getExpr() && tnew == exp.getType())
            return exp;
        else
            return new ExprTypeCast(exp, tnew, expr);
    }
	
	
    public Type replType() {
        return TypePrimitive.bittype;
    }
	
	
	
	public Object visitTypePrimitive(TypePrimitive t) {
        if (isFloat(t)) {
            return replType().withMemType(t.getCudaMemType());
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
	
	
    boolean isFloat(Type t) {
        return t.equals(FLOAT) || t.equals(DOUBLE);
    }

	public Object visitExprBinary(ExprBinary exp)
    {
		Type ltype = getType(exp.getLeft());
		Type rtype = getType(exp.getRight());
		
        if (!isFloat(ltype) && !isFloat(rtype)) {
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
                throw new ExceptionAtNode(
                        "You can't apply this floating point operation if you are using --fe-fencoding TO_BIT. " +
                                exp + " " + exp.getOp(), exp);
        }        
        if (left == exp.getLeft() && right == exp.getRight() && newOp == exp.getOp())
            return exp;
        else
            return new ExprBinary(exp, newOp, left, right, exp.getAlias());
    }
	
	public Object visitStreamSpec(Package spec)
    {
        spec = (Package) super.visitStreamSpec(spec);
        if (floatConstants.size() == 0) {
            return spec;
        } else {
            List<Function> lf = new ArrayList<Function>(spec.getFuncs());
            lf.addAll(floatConstants.values());
            return new Package(spec, spec.getName(), spec.getStructs(),
                    spec.getVars(), lf, spec.getAssumptions());
        }
    }
	
	
	
	
	
	
	
	
}
