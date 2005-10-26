package streamit.frontend.passes;

import java.util.*;

import streamit.frontend.nodes.*;

/**
 * Converts function that return something to functions that take
 * extra output parameters. Also fixes all function calls.
 *  
 * @author liviu
 */
public class FunctionParamExtension extends SymbolTableVisitor {

	private int tempCounter;
	private Function currentFunction;
	
	public FunctionParamExtension() {
		this(null);
	}

	public FunctionParamExtension(SymbolTable symtab) {
		super(symtab);
	}

	private String getParamName(int x) { return "_out_"+x; }

	public Object visitStreamSpec(StreamSpec spec) {
		// before we continue, we must add parameters to all the functions
		for(Iterator itf=spec.getFuncs().iterator();itf.hasNext();) {
			Function fun=(Function) itf.next();
			TypeCompound rt=(TypeCompound) fun.getReturnType();
			List types=rt.getTypes();
			for(int i=0;i<types.size();i++) {
				fun.getParams().add(new Parameter((Type) types.get(i),getParamName(i),true));
			}
		}
		return super.visitStreamSpec(spec);
	}

	public Object visitFunction(Function func) {
		final Object ret;
		currentFunction=func;
			tempCounter=0;
			ret=super.visitFunction(func);
		currentFunction=null;
		return ret;
	}

	public Object visitExprFunCall(ExprFunCall exp) {
		// TODO Auto-generated method stub
		return super.visitExprFunCall(exp);
	}

	public Object visitStmtReturn(StmtReturn stmt) {
		List params=currentFunction.getParams();
		for(int i=0;i<params.size();i++) {
			if(((Parameter)params.get(i)).isParameterOutput()) {
				params=params.subList(i,params.size());
				break;
			}
		}
		for(int i=0;i<params.size();i++) {
			Parameter param=(Parameter) params.get(i);
			String name=param.getName(); 
			Statement assignRet=new StmtAssign(stmt.getContext(), new ExprVar(stmt.getContext(), name), stmt.getValue(), 0);
			addStatement(assignRet);
		}
		return null;
	}

}
