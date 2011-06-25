package sketch.compiler.parallelEncoder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.passes.lowering.SymbolTableVisitor;

public class LockPreprocessing extends SymbolTableVisitor {
	public LockPreprocessing(){
		super(null);
	}
	static final ExprVar NTYPES = new ExprVar((FEContext) null,"NTYPES");

	Map<String, ExprConstInt> lockedTypes = new HashMap<String, ExprConstInt>();

	public Object visitExprFunCall(ExprFunCall exp)
    {
		if(exp.getName().equals("lock") || exp.getName().equals("unlock") ){
			List<Expression>  pars = exp.getParams();
			assert pars.size() == 1 : "Lock and unlock should have exactly one argument";
			Expression par = pars.get(0);
			par = new ExprTypeCast(par, TypePrimitive.inttype, par);
			Type t = getType(par);
			String tname = t.toString();
			if(!lockedTypes.containsKey(tname)){
				lockedTypes.put(tname, new ExprConstInt(lockedTypes.size()));
			}
			ExprConstInt offset = lockedTypes.get(tname);
			ExprBinary newPar = new ExprBinary(new ExprBinary(par, "*", NTYPES ), "+" , offset );
			return new ExprFunCall(exp, exp.getName(), newPar);
		}
		return exp;
    }

	public Object visitStreamSpec(StreamSpec spec)
    {
		StreamSpec sspec = (StreamSpec)super.visitStreamSpec(spec);

		sspec.getVars().add(new FieldDecl(spec, TypePrimitive.inttype, NTYPES.getName(), new ExprConstInt(lockedTypes.size())));
        sspec.getFuncs().add(
                Function.creator((FEContext) null, "lock", FcnType.Uninterp).params(
                        Collections.singletonList(new Parameter(TypePrimitive.inttype,
                                "mem"))).create());
        sspec.getFuncs().add(
                Function.creator((FEContext) null, "unlock", FcnType.Uninterp).params(
                        Collections.singletonList(new Parameter(TypePrimitive.inttype,
                                "mem"))).create());

		return sspec;
    }

}
