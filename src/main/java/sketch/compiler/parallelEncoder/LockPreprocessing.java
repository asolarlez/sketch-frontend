package streamit.frontend.parallelEncoder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprTypeCast;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.passes.SymbolTableVisitor;

public class LockPreprocessing extends SymbolTableVisitor {
	public LockPreprocessing(){
		super(null);
	}
	static final ExprVar NTYPES = new ExprVar(null,"NTYPES"); 
	
	Map<String, ExprConstInt> lockedTypes = new HashMap<String, ExprConstInt>();
	
	public Object visitExprFunCall(ExprFunCall exp)
    {
		if(exp.getName().equals("lock") || exp.getName().equals("unlock") ){
			List<Expression>  pars = exp.getParams();
			assert pars.size() == 1 : "Lock and unlock should have exactly one argument";
			Expression par = pars.get(0); 
			par = new ExprTypeCast(par.getCx(), TypePrimitive.inttype, par);
			Type t = getType(par);
			String tname = t.toString();
			if(!lockedTypes.containsKey(tname)){
				lockedTypes.put(tname, new ExprConstInt(lockedTypes.size()));
			}
			ExprConstInt offset = lockedTypes.get(tname);
			ExprBinary newPar = new ExprBinary(new ExprBinary(par, "*", NTYPES ), "+" , offset );
			return new ExprFunCall(exp.getCx(), exp.getName(), newPar);
		}
		return exp;
    }
	
	public Object visitStreamSpec(StreamSpec spec)
    {
		StreamSpec sspec = (StreamSpec)super.visitStreamSpec(spec);
		
		sspec.getVars().add(new FieldDecl(spec.getCx(), TypePrimitive.inttype, NTYPES.getName(), new ExprConstInt(lockedTypes.size())));
		sspec.getFuncs().add(Function.newUninterp("lock", TypePrimitive.voidtype, Collections.singletonList(new Parameter(TypePrimitive.inttype, "mem"))));
		sspec.getFuncs().add(Function.newUninterp("unlock", TypePrimitive.voidtype, Collections.singletonList(new Parameter(TypePrimitive.inttype, "mem"))));
		
		return sspec;
    }
	
}
