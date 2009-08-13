package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.SymbolTable;
import sketch.compiler.ast.core.UnrecognizedVariableException;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNullPtr;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStruct;
import sketch.compiler.ast.core.typs.TypeStructRef;

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
			StmtVarDecl decl=new StmtVarDecl(func,param.getType(),param.getName(),
					new ExprVar(func,newName));
			List<Statement> stmts = new ArrayList<Statement> (body.getStmts().size()+2);
			stmts.add(decl);
			stmts.addAll(body.getStmts());
			return new Function(func,func.getCls(),func.getName(),func.getReturnType(),
				func.getParams(),func.getSpecification(),
				new StmtBlock(body,stmts));
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
			return new Function(func, func.getCls(), func.getName(),
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
	private boolean inRetStmt = false;
	
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
			funs.add(new Function(fun, fun.getCls(), fun.getName(), fun.getReturnType(),
				params, fun.getSpecification(), fun.getBody()));
		}
		spec=new StreamSpec(spec, spec.getType(), spec.getStreamType(), spec.getName(), spec.getParams(), spec.getVars(), funs);
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

		func=(Function)func.accept(paramCopyRes);




		List<Statement> stmts=new ArrayList<Statement>(((StmtBlock)func.getBody()).getStmts());
		//add a declaration for the "return flag"
		stmts.add(0,new StmtVarDecl(func.getBody(),TypePrimitive.bittype,getReturnFlag(),new ExprConstInt(0)));
		if(initOutputs){

			List<Parameter> lp = func.getParams();
			for(Iterator<Parameter> it = lp.iterator(); it.hasNext(); ){
				Parameter p = it.next();
				if(p.getPtype() == Parameter.OUT){
					Parameter outParam = p;
					String outParamName  = outParam.getName();
					assert outParam.isParameterOutput();

					Expression defaultValue = getDefaultValue(func.getReturnType());
					
					stmts.add(0, new StmtAssign(new ExprVar(func, outParamName), defaultValue));
				}
			}
		}
		func=new Function(func,func.getCls(),func.getName(),
				TypePrimitive.voidtype, func.getParams(),
				func.getSpecification(), new StmtBlock(func,stmts));
		return func;
	}

	@Override
	public Object visitStmtIfThen(StmtIfThen stmt)
	{
		Statement cons=stmt.getCons();
		Statement alt=stmt.getAlt();
		if(cons!=null && !(cons instanceof StmtBlock))
			cons=new StmtBlock(stmt,Collections.singletonList(cons));
		if(alt!=null && !(alt instanceof StmtBlock))
			alt=new StmtBlock(stmt,Collections.singletonList(alt));
		if(cons!=stmt.getCons() || alt!=stmt.getAlt())
			stmt=new StmtIfThen(stmt,stmt.getCond(),cons,alt);
		if( globalEffects(stmt) ){
			return conditionWrap( (Statement)
			super.visitStmtIfThen(stmt) );
		}else{
			return super.visitStmtIfThen(stmt);
		}
	}

	
	protected boolean hasRet(FENode n){		
		class ReturnFinder extends FEReplacer{
			public boolean hasRet = false;
			public Object visitStmtReturn(StmtReturn stmt){
				hasRet  = true;
				return stmt;
			}			
		};
		
		ReturnFinder hf = new ReturnFinder();
		n.accept(hf);
		return hf.hasRet;
	}
	
	
	@Override
	public Object visitStmtWhile(StmtWhile stmt)
	{
		Statement body=stmt.getBody();
		Expression cond = stmt.getCond();
		if(body!=null && !(body instanceof StmtBlock))
			body=new StmtBlock(stmt,Collections.singletonList(body));
		
		if(hasRet(body)){
			cond = new ExprBinary(cond, "&&", new ExprBinary(
					new ExprVar(cond, getReturnFlag()), "==",
					getFalseLiteral()) );
		}
		
		if(body!=stmt.getBody() || cond != stmt.getCond())
			stmt=new StmtWhile(stmt,cond, body);		
		return super.visitStmtWhile(stmt);
	}
	
	@Override
	public Object visitStmtDoWhile(StmtDoWhile stmt)
	{
		Statement body=stmt.getBody();
		Expression cond = stmt.getCond();
		if(body!=null && !(body instanceof StmtBlock))
			body=new StmtBlock(stmt,Collections.singletonList(body));
		
		if(hasRet(body)){
			cond = new ExprBinary(cond, "&&", new ExprBinary(
					new ExprVar(cond, getReturnFlag()), "==",
					getFalseLiteral()) );
		}
		
		if(body!=stmt.getBody() || cond != stmt.getCond())
			stmt=new StmtDoWhile(stmt,body,cond);
		return super.visitStmtDoWhile(stmt);
	}

	@Override
	public Object visitStmtFor(StmtFor stmt)
	{
		Statement body=stmt.getBody();
		if(body!=null && !(body instanceof StmtBlock))
			body=new StmtBlock(stmt,Collections.singletonList(body));
		
		Expression cond = stmt.getCond();
		
		if(SimpleLoopUnroller.decideForLoop(stmt)<0 &&  hasRet(body)){
			cond = new ExprBinary(cond, "&&", new ExprBinary(
					new ExprVar(cond, getReturnFlag()), "==",
					getFalseLiteral()) );
		}
		
		if(body!=stmt.getBody() || cond != stmt.getCond())
			stmt=new StmtFor(stmt,stmt.getInit(),cond,stmt.getIncr(),body);
		return super.visitStmtFor(stmt);
	}

	@Override
	public Object visitStmtLoop(StmtLoop stmt)
	{
		Statement body=stmt.getBody();
		if(body!=null && !(body instanceof StmtBlock))
			body=new StmtBlock(stmt,Collections.singletonList(body));
		if(body!=stmt.getBody())
			stmt=new StmtLoop(stmt,stmt.getIter(),body);
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
			throw new UnrecognizedVariableException(exp + ": Function name " + e.getMessage() + " not found"  );
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
			Expression oldArg= (p.getType() instanceof TypeStruct || p.getType() instanceof TypeStructRef) ? new ExprNullPtr() : getDefaultValue(p.getType()) ;
			if(ptype == Parameter.REF || ptype == Parameter.IN){
				oldArg=(Expression) existingArgs.get(psz);
				++psz;
			}
			if(oldArg != null && oldArg instanceof ExprVar || (oldArg instanceof ExprConstInt && !p.isParameterOutput())){
				args.add(oldArg);
			}else{
				String tempVar = getNewOutID();
				Statement decl = new StmtVarDecl(exp, p.getType(), tempVar, oldArg);
				ExprVar ev =new ExprVar(exp,tempVar);
				args.add(ev);
				addStatement(decl);
				if(ptype == Parameter.OUT){
					tempVars.add(ev);
				}
				if(ptype == Parameter.REF){
					refAssigns.add(new StmtAssign(oldArg, ev  ));
				}
			}
		}

		ExprFunCall newcall=new ExprFunCall(exp,exp.getName(),args);
		addStatement( conditionWrap(new StmtExpr(newcall)));
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
		Statement s = (Statement) super.visitStmtAssert(sa);
		return conditionWrap(s);
	}



	private Statement conditionWrap(Statement s){
		
		if(!inRetStmt){
			
			Statement ret=new StmtIfThen(s,
					new ExprBinary(s, ExprBinary.BINOP_EQ,
						new ExprVar(s, getReturnFlag()),
						ExprConstInt.zero),
					s,
					null);
			return ret;
		}else{
			return s;
		}
	}




	private boolean globalEffects(Statement s){
		if(s instanceof StmtAssert){
			return true;
		}else{
			
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

				@Override
				public Object visitExprFunCall(ExprFunCall exp){
					ge = true;
					return exp;
				}
			}
			findge f = new findge();
			s.accept(f);			
			return f.ge;
		}
	}


	@Override
	public Object visitStmtAssign(StmtAssign stmt){
		Statement s = (Statement) super.visitStmtAssign(stmt);
		if(globalEffects(s)){
			FENode cx=stmt;
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
		FENode cx=stmt;
		List<Statement> oldns = newStatements;
		boolean oldInrs = inRetStmt;
		inRetStmt = true;
		this.newStatements = new ArrayList<Statement> ();		
		stmt=(StmtReturn) super.visitStmtReturn(stmt);
		
		List params=getOutputParams(currentFunction);
		for(int i=0;i<params.size();i++) {
			Parameter param=(Parameter) params.get(i);
			String name=param.getName();
			Statement assignRet=new StmtAssign(cx, new ExprVar(cx, name), stmt.getValue(), 0);
			newStatements.add(assignRet);
		}
		newStatements.add(new StmtAssign(cx, new ExprVar(cx, getReturnFlag()), new ExprConstInt(cx, 1), 0));
		Statement ret=new StmtIfThen(cx,
			new ExprBinary(cx, ExprBinary.BINOP_EQ,
				new ExprVar(cx, getReturnFlag()),
				new ExprConstInt(cx, 0)),
			new StmtBlock(cx,newStatements),
			null);
		newStatements = oldns;
		inRetStmt = oldInrs;
		return ret;
	}
	
	protected Expression getFalseLiteral() {
		return ExprConstInt.zero;
	}
	
	protected Expression getDefaultValue(Type t) {
		Expression defaultValue = null;
		if(t.isStruct()){
			defaultValue = ExprNullPtr.nullPtr;
		} else {
			defaultValue = ExprConstInt.zero;
		}
		
		return defaultValue;
	}

}
