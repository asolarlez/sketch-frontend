package streamit.frontend.passes;

import java.util.*;

import streamit.frontend.nodes.*;

/**
 * Converts function that return something to functions that take
 * extra output parameters. Also fixes all function calls.
 *  
 * @author liviu
 */
public class FunctionParamExtension extends SymbolTableVisitor 
{

	private class ParameterCopyResolver extends SymbolTableVisitor 
	{
		private HashMap<String,Parameter> unmodifiedParams;

		public ParameterCopyResolver()
		{
			super(null);
		}
		
		private Function addVarCopy(Function func, Parameter param, String newName)
		{
			StmtBlock body=(StmtBlock) func.getBody();
			StmtVarDecl decl=new StmtVarDecl(func.getContext(),param.getType(),param.getName(),
					new ExprVar(func.getContext(),newName));
			List stmts=new ArrayList(body.getStmts().size()+2);
			stmts.add(decl);
			stmts.addAll(body.getStmts());
			return new Function(func.getContext(),func.getCls(),func.getName(),func.getReturnType(),
				func.getParams(),func.getSpecification(),
				new StmtBlock(body.getContext(),stmts));
		}
		@Override
		public Object visitFunction(Function func)
		{
			unmodifiedParams=new HashMap<String,Parameter>();
			for(Iterator<Parameter> iter=func.getParams().iterator();iter.hasNext();) {
				Parameter param=iter.next();
				if(param.isParameterOutput()) continue;
				unmodifiedParams.put(param.getName(),param);
			}
			Function ret=(Function) super.visitFunction(func);
			List<Parameter> parameters=func.getParams(); //assume it's mutable; for the current Function implementation that is true
			for(int i=0;i<parameters.size();i++) {
				Parameter param=parameters.get(i);
				if(param.isParameterOutput()) continue;
				if(!unmodifiedParams.containsValue(param)) {
					String newName=getNewInCpID(param.getName());
					Parameter newPar=new Parameter(param.getType(),newName,param.isParameterOutput());
					parameters.set(i,newPar);
					ret=addVarCopy(ret,param,newName);
				}
			}
			return ret;
		}
		public Object visitStmtAssign(StmtAssign stmt)
		{
			Expression lhs=(Expression) stmt.getLHS().accept(this);
			while (lhs instanceof ExprArrayRange) lhs=((ExprArrayRange)lhs).getBase();
			assert lhs instanceof ExprVar;
			String lhsName=((ExprVar)lhs).getName();
			unmodifiedParams.remove(lhsName);
			return super.visitStmtAssign(stmt);
		}
		public Object visitStmtVarDecl(StmtVarDecl stmt)
		{
			int n=stmt.getNumVars();
			for(int i=0;i<n;i++) {
				unmodifiedParams.remove(stmt.getName(i));
			}
			return super.visitStmtVarDecl(stmt);
		}
	}
	
	private int inCpCounter;
	private int outCounter;
	private Function currentFunction;
	private ParameterCopyResolver paramCopyRes;
	
	public FunctionParamExtension() {
		this(null);
	}

	public FunctionParamExtension(SymbolTable symtab) {
		super(symtab);
		paramCopyRes=new ParameterCopyResolver();
	}

	private String getParamName(int x) { 
		return "_out_"+x; 
	}
	
	private String getNewOutID() {
		return "_ret_"+(outCounter++); 
	}

	private String getReturnFlag() { 
		return "_has_out_";
	}
	
	private String getNewInCpID(String oldName) {
		return oldName+"_"+(inCpCounter++); 
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
			Type retType=fun.getReturnType();
			if(!(retType instanceof TypeCompound)) continue;
			TypeCompound rt=(TypeCompound) retType;
			List types=rt.getTypes();
			for(int i=0;i<types.size();i++) {
				fun.getParams().add(new Parameter((Type) types.get(i),getParamName(i),true));
			}
		}
		return super.visitStreamSpec(spec);
	}

	public Object visitFunction(Function func) {
		if(func.getReturnType()==TypePrimitive.voidtype) return func;
		currentFunction=func;
			outCounter=0;
			inCpCounter=0;
			func=(Function) super.visitFunction(func);
		currentFunction=null;
		func=(Function)func.accept(paramCopyRes);
		List stmts=new ArrayList(((StmtBlock)func.getBody()).getStmts());
		//add a declaration for the "return flag"
		stmts.add(0,new StmtVarDecl(func.getBody().getContext(),TypePrimitive.bittype,getReturnFlag(),new ExprConstInt(null,0)));
		func=new Function(func.getContext(),func.getCls(),func.getName(),
				TypePrimitive.voidtype, func.getParams(),
				func.getSpecification(), new StmtBlock(func.getContext(),stmts));
		return func;
	}

	public Object visitExprFunCall(ExprFunCall exp) {
		// first let the superclass process the parameters (which may be function calls)
		exp=(ExprFunCall) super.visitExprFunCall(exp);

		// resolve the function being called
		Function fun=symtab.lookupFn(exp.getName());
		
		// now we create a temp (or several?) to store the result
		List outParams=getOutputParams(fun);
		String tempNames[]=new String[outParams.size()];
		for(int i=0;i<outParams.size();i++) {
			Parameter param=(Parameter) outParams.get(i);
			tempNames[i]=getNewOutID();
			Statement decl=new StmtVarDecl(exp.getContext(),
					Collections.singletonList(param.getType()),
					Collections.singletonList(tempNames[i]),
					Collections.singletonList(null)
				);
			addStatement(decl);
		}
		// modify the function call and re-issue it as a statement
		List args=new ArrayList(fun.getParams().size());
		List existingArgs=exp.getParams();
		for(int i=0;i<existingArgs.size();i++) {
			Expression oldArg=(Expression) existingArgs.get(i);
			if(oldArg instanceof ExprVar)
				args.add(oldArg);
			else {
				Parameter param=(Parameter) fun.getParams().get(i);
				String tempVar=getNewOutID();
				Statement decl=new StmtVarDecl(exp.getContext(),
						Collections.singletonList(param.getType()),
						Collections.singletonList(tempVar),
						Collections.singletonList(oldArg)
					);
				addStatement(decl);
				args.add(new ExprVar(exp.getContext(),tempVar));
			}
		}
		for(int i=0;i<outParams.size();i++)
			args.add(new ExprVar(exp.getContext(),tempNames[i]));
		ExprFunCall newcall=new ExprFunCall(exp.getContext(),exp.getName(),args);
		addStatement(new StmtExpr(newcall));
		
		// replace the original function call with an instance of the temp variable
		// (which stores the return value)
		assert tempNames.length==1; //TODO handle the case when it's >1
		return new ExprVar(exp.getContext(),tempNames[0]);
	}

	public Object visitStmtReturn(StmtReturn stmt) {
		FEContext cx=stmt.getContext();
		stmt=(StmtReturn) super.visitStmtReturn(stmt);
		List stmts=new ArrayList();
		List params=getOutputParams(currentFunction);
		for(int i=0;i<params.size();i++) {
			Parameter param=(Parameter) params.get(i);
			String name=param.getName(); 
			Statement assignRet=new StmtAssign(cx, new ExprVar(cx, name), stmt.getValue(), 0);
			stmts.add(assignRet);
		}
		stmts.add(new StmtAssign(cx, new ExprVar(cx, getReturnFlag()), new ExprConstInt(cx, 1), 0));
		Statement ret=new StmtIfThen(cx,
			new ExprBinary(cx, ExprBinary.BINOP_EQ, 
				new ExprVar(cx, getReturnFlag()), 
				new ExprConstInt(cx, 0)),
			new StmtBlock(cx,stmts),
			null);
		return ret;
	}

}
