package streamit.frontend.passes;

import java.util.*;

import streamit.frontend.nodes.*;
import streamit.frontend.stencilSK.SimpleCodePrinter;

/**
 * Converts function that return something to functions that take
 * extra output parameters. Also fixes all function calls.
 *  
 * @author liviu
 */
public class FunctionParamExtension extends SymbolTableVisitor 
{

	/**
	 * 
	 * 
	 * 
	 * @author asolar
	 *
	 */
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
			StmtVarDecl decl=new StmtVarDecl(func.getCx(),param.getType(),param.getName(),
					new ExprVar(func.getCx(),newName));
			List stmts=new ArrayList(body.getStmts().size()+2);
			stmts.add(decl);
			stmts.addAll(body.getStmts());
			return new Function(func.getCx(),func.getCls(),func.getName(),func.getReturnType(),
				func.getParams(),func.getSpecification(),
				new StmtBlock(body.getCx(),stmts));
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
			func=(Function) super.visitFunction(func);
			List<Parameter> parameters=new ArrayList<Parameter>(func.getParams());
			for(int i=0;i<parameters.size();i++) {
				Parameter param=parameters.get(i);
				if(param.isParameterOutput()) continue;
				if(!unmodifiedParams.containsValue(param)) {
					String newName=getNewInCpID(param.getName());
					Parameter newPar=new Parameter(param.getType(),newName,param.getPtype());
					parameters.set(i,newPar);
					func=addVarCopy(func,param,newName);
				}
			}
			return new Function(func.getCx(), func.getCls(), func.getName(),
				func.getReturnType(), parameters, func.getSpecification(), func.getBody());
		}
		public Object visitStmtAssign(StmtAssign stmt)
		{
			Expression lhs=(Expression) stmt.getLHS().accept(this);
			while (lhs instanceof ExprArrayRange) lhs=((ExprArrayRange)lhs).getBase();
			assert lhs instanceof ExprVar  || lhs instanceof ExprField;
			if( lhs instanceof ExprVar ){
				String lhsName=((ExprVar)lhs).getName();
				unmodifiedParams.remove(lhsName);
			}
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
	public boolean initOutputs=false;
	
	public FunctionParamExtension(boolean io) {		
		this(null);
		initOutputs = io;
	}
	
	public FunctionParamExtension() {
		this(null);
	}

	public FunctionParamExtension(SymbolTable symtab) {
		super(symtab);
		paramCopyRes=new ParameterCopyResolver();
	}

	private String getOutParamName() { 
		return "_out"; 
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
			if(((Parameter)params.get(i)).getPtype() == Parameter.OUT)
				return params.subList(i,params.size());
		return Collections.EMPTY_LIST;
	}
	
	public Object visitStreamSpec(StreamSpec spec) {
		// before we continue, we must add parameters to all the functions
		List<Function> funs=new ArrayList<Function>();
		for(Function fun: (List<Function>)spec.getFuncs()) {
			Type retType=fun.getReturnType();
			List<Parameter> params=new ArrayList<Parameter>(fun.getParams());
			if(!retType.equals(TypePrimitive.voidtype)){
				params.add(new Parameter(retType,getOutParamName(),Parameter.OUT));
			}
			funs.add(new Function(fun.getCx(), fun.getCls(), fun.getName(), fun.getReturnType(),
				params, fun.getSpecification(), fun.getBody()));
		}
		spec=new StreamSpec(spec.getCx(), spec.getType(), spec.getStreamType(), spec.getName(), spec.getParams(), spec.getVars(), funs);
		return super.visitStreamSpec(spec);
	}

	
	Set<String> currentRefParams = new HashSet<String>();
	
	public Object visitFunction(Function func) {		
		if(func.isUninterp() ) return func;
		
		{
			currentRefParams.clear();
			List<Parameter> lp = func.getParams();
			for(Iterator<Parameter> it = lp.iterator(); it.hasNext(); ){
				Parameter p = it.next();
				if(p.isParameterOutput()){
					currentRefParams.add(p.getName());
				}
			}
		}
		
		
		currentFunction=func;
			outCounter=0;
			inCpCounter=0;
			func=(Function) super.visitFunction(func);
		currentFunction=null;
		
		//if(func.getReturnType()==TypePrimitive.voidtype) return func;
		func.accept(new SimpleCodePrinter());
		func=(Function)func.accept(paramCopyRes);
		func.accept(new SimpleCodePrinter());
		
		
		
		List stmts=new ArrayList(((StmtBlock)func.getBody()).getStmts());
		//add a declaration for the "return flag"
		stmts.add(0,new StmtVarDecl(func.getBody().getCx(),TypePrimitive.bittype,getReturnFlag(),new ExprConstInt(null,0)));
		if(initOutputs){
			
			List<Parameter> lp = func.getParams();
			for(Iterator<Parameter> it = lp.iterator(); it.hasNext(); ){
				Parameter p = it.next();
				if(p.getPtype() == Parameter.OUT){
					Parameter outParam = p;
					String outParamName  = outParam.getName();
					assert outParam.isParameterOutput();
					
					Expression defaultValue = null;
					
					if(func.getReturnType().isStruct()){			
						defaultValue = ExprNullPtr.nullPtr;
					}else{
						defaultValue = ExprConstInt.zero;
					}			
					stmts.add(0, new StmtAssign(null, new ExprVar(null, outParamName), defaultValue));
				}
			}
		}
		func=new Function(func.getCx(),func.getCls(),func.getName(),
				TypePrimitive.voidtype, func.getParams(),
				func.getSpecification(), new StmtBlock(func.getCx(),stmts));
		return func;
	}

	@Override
	public Object visitStmtIfThen(StmtIfThen stmt)
	{
		Statement cons=stmt.getCons();
		Statement alt=stmt.getAlt();
		if(cons!=null && !(cons instanceof StmtBlock))
			cons=new StmtBlock(stmt.getCx(),Collections.singletonList(cons));
		if(alt!=null && !(alt instanceof StmtBlock))
			alt=new StmtBlock(stmt.getCx(),Collections.singletonList(alt));
		if(cons!=stmt.getCons() || alt!=stmt.getAlt())
			stmt=new StmtIfThen(stmt.getCx(),stmt.getCond(),cons,alt);
		return super.visitStmtIfThen(stmt);
	}

	@Override
	public Object visitStmtDoWhile(StmtDoWhile stmt)
	{
		Statement body=stmt.getBody();
		if(body!=null && !(body instanceof StmtBlock))
			body=new StmtBlock(stmt.getCx(),Collections.singletonList(body));
		if(body!=stmt.getBody())
			stmt=new StmtDoWhile(stmt.getCx(),body,stmt.getCond());
		return super.visitStmtDoWhile(stmt);
	}

	@Override
	public Object visitStmtFor(StmtFor stmt)
	{
		Statement body=stmt.getBody();
		if(body!=null && !(body instanceof StmtBlock))
			body=new StmtBlock(stmt.getCx(),Collections.singletonList(body));
		if(body!=stmt.getBody())
			stmt=new StmtFor(stmt.getCx(),stmt.getInit(),stmt.getCond(),stmt.getIncr(),body);
		return super.visitStmtFor(stmt);
	}

	@Override
	public Object visitStmtLoop(StmtLoop stmt)
	{
		Statement body=stmt.getBody();
		if(body!=null && !(body instanceof StmtBlock))
			body=new StmtBlock(stmt.getCx(),Collections.singletonList(body));
		if(body!=stmt.getBody())
			stmt=new StmtLoop(stmt.getCx(),stmt.getIter(),body);
		return super.visitStmtLoop(stmt);
	}

	public Object visitExprFunCall(ExprFunCall exp) {
		// first let the superclass process the parameters (which may be function calls)
		exp=(ExprFunCall) super.visitExprFunCall(exp);

		// resolve the function being called
		Function fun;
		try{
		fun =symtab.lookupFn(exp.getName());
		}catch(UnrecognizedVariableException e){
			throw new UnrecognizedVariableException(exp.getCx() + ": Function name " + e.getMessage() + " not found"  );
		}
		// now we create a temp (or several?) to store the result
		
		
		List<Expression> args=new ArrayList<Expression>(fun.getParams().size());
		List<Expression> existingArgs=exp.getParams();
		
		List<Parameter> params=fun.getParams();
		
		List<Expression> tempVars = new ArrayList<Expression>();
		List<Statement> refAssigns = new ArrayList<Statement>();
		
		int psz = 0;
		for(int i=0;i<params.size();i++){
			Parameter p = params.get(i);
			int ptype = p.getPtype();
			Expression oldArg=null;
			if(ptype == Parameter.REF || ptype == Parameter.IN){
				oldArg=(Expression) existingArgs.get(psz);
				++psz;
			}
			if(oldArg != null && oldArg instanceof ExprVar || oldArg instanceof ExprConstInt){
				args.add(oldArg);
			}else{
				String tempVar = getNewOutID();
				Statement decl = new StmtVarDecl(exp.getCx(), p.getType(), tempVar, oldArg);
				ExprVar ev =new ExprVar(exp.getCx(),tempVar); 
				args.add(ev);
				addStatement(decl);
				if(ptype == Parameter.OUT){
					tempVars.add(ev);
				}
				if(ptype == Parameter.REF){
					refAssigns.add(new StmtAssign(exp.getCx(), oldArg, ev  ));
				}
			}
		}
		
		ExprFunCall newcall=new ExprFunCall(exp.getCx(),exp.getName(),args);
		addStatement(new StmtExpr(newcall));
		addStatements(refAssigns);
		
		// replace the original function call with an instance of the temp variable
		// (which stores the return value)
		if(tempVars.size() == 0){
			return null;
		}
		assert tempVars.size()==1; //TODO handle the case when it's >1
		return tempVars.get(0);
	}

	@Override
	public Object visitStmtAssert(StmtAssert sa){
		FEContext cx=sa.getCx();
		Statement s = (Statement) super.visitStmtAssert(sa);
		Statement ret=new StmtIfThen(cx,
				new ExprBinary(cx, ExprBinary.BINOP_EQ, 
					new ExprVar(cx, getReturnFlag()), 
					new ExprConstInt(cx, 0)),
				s,
				null);
		return ret;
	}
	
	
	private boolean globalEffects(Statement s){
		if(s instanceof StmtAssert){
			return true;
		}
		
		if(s instanceof StmtAssign){
			StmtAssign sa = (StmtAssign) s;
			
			Expression left = sa.getLHS();
			
			class findge extends FEReplacer{
				public boolean ge = false;
				public Object visitExprField(ExprField ef){
					ge = true;
					return ef;
				}
				@Override
				public Object visitExprArrayRange(ExprArrayRange exp){
					exp.getBase().accept(this);
					return exp;
				}
				
				@Override
				public Object visitExprVar(ExprVar ev){
					if(currentRefParams.contains(ev.getName())){
						ge = true;
					}
					return ev;
				}
			}
			findge f = new findge();
			left.accept(f);
			return f.ge;
		}
		
		return false;
	}
	
	
	@Override
	public Object visitStmtAssign(StmtAssign stmt){
		Statement s = (Statement) super.visitStmtAssign(stmt);
		if(globalEffects(s)){
			FEContext cx=stmt.getCx();
			Statement ret=new StmtIfThen(cx,
					new ExprBinary(cx, ExprBinary.BINOP_EQ, 
						new ExprVar(cx, getReturnFlag()), 
						new ExprConstInt(cx, 0)),
					s,
					null);
			return ret;
		}else{
			return s;
		}
	}
	
	
	
	@Override
	public Object visitStmtReturn(StmtReturn stmt) {
		FEContext cx=stmt.getCx();
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
