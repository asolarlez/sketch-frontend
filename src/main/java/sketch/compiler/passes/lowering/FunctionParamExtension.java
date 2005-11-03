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

	private String getParamName(int x) { 
		return "_out_"+x; 
	}
	
	private String getNewTempID() { 
		return "_frv_"+(tempCounter++); 
	}
	
	private List getOutputParams(Function f) {
		List params=f.getParams();
		for(int i=0;i<params.size();i++)
			if(((Parameter)params.get(i)).isParameterOutput())
				return params.subList(i,params.size());
		return Collections.EMPTY_LIST;
	}
	
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
		final Function ret;
		currentFunction=func;
			tempCounter=0;
			ret=(Function) super.visitFunction(func);
		currentFunction=null;
		return new Function(ret.getContext(),ret.getCls(),ret.getName(),
				new TypePrimitive(TypePrimitive.TYPE_VOID), ret.getParams(),
				ret.getSpecification(), ret.getBody());
	}

	public Object visitExprFunCall(ExprFunCall exp) {
		// first let the superclass process the parameters (which may be function calls)
		ExprFunCall fcall=(ExprFunCall) super.visitExprFunCall(exp);

		// resolve the function being called
		Function fun=symtab.lookupFn(fcall.getName());
		
		// now we create a temp (or several?) to store the result
		List params=getOutputParams(fun);
		String tempNames[]=new String[params.size()];
		for(int i=0;i<params.size();i++) {
			Parameter param=(Parameter) params.get(i);
			tempNames[i]=getNewTempID();
			Statement decl=new StmtVarDecl(exp.getContext(),
					Collections.singletonList(param.getType()),
					Collections.singletonList(tempNames[i]),
					Collections.singletonList(null)
				);
			addStatement(decl);
		}
		// modify the function call and re-issue it as a statement
		List args=new ArrayList(fcall.getParams());
		for(int i=0;i<params.size();i++)
			args.add(new ExprVar(fcall.getContext(),tempNames[i]));
		ExprFunCall newcall=new ExprFunCall(fcall.getContext(),fcall.getName(),args);
		addStatement(new StmtExpr(newcall));
		
		// replace the original function call with an instance of the temp variable
		// (which stores the return value)
		assert tempNames.length==1; //TODO handle the case when it's >1
		return new ExprVar(exp.getContext(),tempNames[0]);
	}

	public Object visitStmtReturn(StmtReturn stmt) {
		stmt=(StmtReturn) super.visitStmtReturn(stmt);
		List params=getOutputParams(currentFunction);
		for(int i=0;i<params.size();i++) {
			Parameter param=(Parameter) params.get(i);
			String name=param.getName(); 
			Statement assignRet=new StmtAssign(stmt.getContext(), new ExprVar(stmt.getContext(), name), stmt.getValue(), 0);
			addStatement(assignRet);
		}
		return null;
	}

}
