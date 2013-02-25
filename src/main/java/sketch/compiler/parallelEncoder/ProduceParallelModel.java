package sketch.compiler.parallelEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.DummyFENode;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FcnType;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.controlflow.CFG;
import sketch.compiler.controlflow.CFGNode;
import sketch.compiler.controlflow.CFGNode.EdgePair;
import sketch.compiler.passes.printers.SimpleCodePrinter;

/**
 * This class produces a parallel model of a sketch.
 * It takes a sketch containing parallel loops, and replaces it with a sketch
 * that has as input a schedule that defines how the threads are going to interleave.
 *
 *
 * Preconditions:
 * Variable names should be unique (this is accomplished by the preprocessing pass).
 * Function should have only a single ploop.
 * Ploop can not be inside any other loop.
 * All functions have bee inlined.
 * The ploop can not have return statements.
 *
 * */

public class ProduceParallelModel extends FEReplacer {

	TempVarGen varGen;
	Expression SchedLen;
	Expression LockLen;
	Function restFunction = null;

	///The following are names that will be used in the generated function.
	static final String PCbase = "PC";
	static final String DONE = "_done";
	static final String SCHEDULE = "_schedule";
	static final String STEP = "_step";
	static final String FUN_NAME_BASE = "rest";
	static final String INV_SCHEDULE = "_invalidSchedule";
	static final String locksName = "_locks";
	static final String COUNTER = "_counts";
	static final ExprVar locksVar = new ExprVar((FEContext) null, locksName);

	public ProduceParallelModel(TempVarGen varGen, int schedLen, int locklen){
		super();
		this.varGen = varGen;
		this.SchedLen = new ExprConstInt(schedLen);
		this.LockLen =  new ExprConstInt(locklen);
	}



	public static class FindLocals extends FEReplacer{
		Set<StmtVarDecl> local = new HashSet<StmtVarDecl>();
		Expression nthreads;
		public FindLocals(Expression nthreads){
			this.nthreads = nthreads;
		}

		public Object visitStmtVarDecl(StmtVarDecl decl){
			List<Type> types  = new ArrayList<Type>(decl.getNumVars());
			List<Expression> inits  = new ArrayList<Expression>(decl.getNumVars());
			for(int i=0; i<decl.getNumVars(); ++i){
				types.add( new TypeArray(decl.getType(i), nthreads));
				inits.add(null);
			}

			local.add(new StmtVarDecl(decl, types, decl.getNames(), inits));
			return decl;
		}
		Set<StmtVarDecl> getLocals(Statement stmt){
			local.clear();
			stmt.accept(this);
			return local;
		}
	}


	/**
	 *
	 * Performs the following replacements:
	 * lock(i) becomes:
	 * if(locks[i] == 0 || locks[i] == threadID+1){
	 * 		locks[i] = threadID+1;
	 * }else{
	 * 		//lock is taken by someone else. This thread shouldn't have been scheduled.
	 * 		invalidSchedule[threadID] = 1;
	 * }
	 *
	 * A statement of the form unlock(i) will translate into:
	 *
	 * assert locks[i] == threadID+1; //no unlocking locks you don't hold.
	 * locks[i] = 0;
	 *
	 *
	 *
	 * @author asolar
	 *
	 */

	public static class EliminateLockUnlock extends FEReplacer{


		public Expression loopVar = null;
		public Expression lockLen = null;
		EliminateLockUnlock(Expression loopVar, Expression lockLen){
			this.loopVar = loopVar;
			this.lockLen = lockLen;
		}

		 public Object visitStmtExpr(StmtExpr stmt)
		    {
			 	if(stmt.getExpression() instanceof ExprFunCall){
			 		Object o = stmt.getExpression().accept(this);
			 		if(o instanceof Expression){
			 			if( o == null) return null;
				        if (o == stmt.getExpression()) return stmt;
				        return new StmtExpr(stmt, (Expression)o);
			 		}else{
			 			assert o instanceof Statement;
			 			return o;
			 		}
			 	}

		        Expression newExpr = doExpression(stmt.getExpression());
		        if( newExpr == null) return null;
		        if (newExpr == stmt.getExpression()) return stmt;
		        return new StmtExpr(stmt, newExpr);
		    }


		 public Object visitExprFunCall(ExprFunCall exp)
		    {


			 if(exp.getName().equals("lock")){
				 assert exp.getParams().size() == 1;
				 Expression p = exp.getParams().get(0);
				 List<Statement> bodyL = new ArrayList<Statement>();
/** This is the code we are producing here.
  	 * if(locks[i] == 0 || locks[i] == threadID+1){
	 * 		locks[i] = threadID+1;
	 * }else{
	 * 		//lock is taken by someone else. This thread shouldn't have been scheduled.
	 * 		invalidSchedule[threadID] = 1;
	 * }
 */
				 bodyL.add(new StmtAssert(exp, new ExprBinary(p, "<", lockLen), "The lock expression is out of bounds.", false));
				 StmtAssign getLock = new StmtAssign(new ExprArrayRange(locksVar, p),  new ExprBinary(loopVar, "+", ExprConstInt.one));

				 Statement sleep = new StmtAssign(new ExprArrayRange(new ExprVar(exp, INV_SCHEDULE), loopVar), ExprConstInt.one);

				 Expression cond = new ExprBinary( new ExprBinary(new ExprArrayRange(locksVar, p), "==", ExprConstInt.zero), "||",
						 			new ExprBinary(new ExprArrayRange(locksVar, p), "==", new ExprBinary(loopVar, "+", ExprConstInt.one) ));
				 bodyL.add(new StmtIfThen(exp, cond, getLock, sleep));
				 return new StmtBlock(exp, bodyL);
			 }else  if(exp.getName().equals("unlock")){
				 assert exp.getParams().size() == 1;
				 Expression p = exp.getParams().get(0);
				 List<Statement> bodyL = new ArrayList<Statement>();
				 bodyL.add(new StmtAssert(exp, new ExprBinary(p, "<", lockLen), "The lock expression is out of bounds.", false));
				 bodyL.add(new StmtAssert(exp, new ExprBinary(new ExprArrayRange(locksVar, p), "==", new ExprBinary(loopVar, "+", ExprConstInt.one) ), "You can't release a lock you don't own", false));
				 bodyL.add(new StmtAssign(new ExprArrayRange(locksVar, p), ExprConstInt.zero ));
				 return new StmtBlock(exp, bodyL);

			 }
			 return exp;
		    }

	}



	/**
	 *
	 * This method produces a function with the following skeleton:
	 * T restFun(globals, [NT]locals, int[NSTEPS] sched, int t){
	 *    forall i<NT : globals'[i] = globals;
	 *    forall i<NT : locals'[i] = locals;
	 *    bool done = true;
	 *    bit[NTHREADS] invalidSchedule;
	 *    for(int i=0; i<NT; ++i){
	 *    	if(locals[i].pc == 0){
	 *    		cfg[0] ( globals -> globals'[i], locals -> locals'[i][i])
	 *    	}
	 *      ...
	 *      if(locals[i].pc == ENDPC-1){
	 *      	cfg[ENDPC-1] ( globals -> globals'[i], locals -> locals'[i][i])
	 *      }
	 *      if(locals[i].pc != ENDPC){
	 *      	done = false;
	 *      }else{
	 *      	invalidSchedule[i] = 1;
	 *      }
	 *    }
	 *
	 *    if(done){
	 *    	return postpar;
	 *    }else{
	 *
	 *      assert t < sched.length : "You need to inline more";
	 *      T[NT] out;
	 *      bit noDeadlock = 0;
	 *      for(int i=0; i<NT; ++i){
	 *          if(invalidSchedule[i]){
	 *          	out[i] = 1;
	 *          }else{
	 *      		out[i] = rest(globals'[i], locals'[i], sched, t+1);
	 *      		noDeadlock = 1;
	 *      	}
	 *      }
	 *
	 *      assert noDeadlock : "Your program has deadlock";
	 *    	return out[sched[t]];
	 *
	 *    }
	 * }
	 *
	 * T Fun(inputs, sched){
	 * 	 declare globals;
	 *   prepar;
	 *
	 *   return restFun(globals, defaultLocals, sched, 0);
	 *
	 * }
	 *
	 * bit[NTHREADS] invalidSchedule := This variable is local to rest. It indicates whether it was legal to schedule thread i,
	 * or whether it should have scheduled another thread because this one is either finished or stuck on a lock.
	 *
	 * The global variables will include a global variable called
	 * int[NLOCKS] locks := for each lock, we have either threadID of the thread holding the lock or -1 if no thread is holding it.
	 *
	 * Thus, a statement of the form lock(i) will translate into:
	 *
	 * if(locks[i] == 0 || locks[i] == threadID+1){
	 * 		locks[i] = threadID+1;
	 * }else{
	 * 		//lock is taken by someone else. This thread shouldn't have been scheduled.
	 * 		invalidSchedule[i] = 1;
	 * }
	 *
	 * A statement of the form unlock(i) will translate into:
	 *
	 * assert locks[i] == threadID+1; //no unlocking locks you don't hold.
	 * locks[i] = 0;
	 *
	 *
	 *
	 *
	 */
	public Function constructRestFunction(CFG parcfg, Statement postpar, Statement prepar, StmtFork ploop,
										   Set<StmtVarDecl> globals, Set<StmtVarDecl> locals, Function fun){

		String funName = fun.getName() + FUN_NAME_BASE;

		List<Statement> bodyL = new ArrayList<Statement>();

		Expression nthreads = ploop.getIter();

		Set<StmtVarDecl> oriGlobals = new HashSet<StmtVarDecl>(globals);
		Parameter outputParam = null;
		//All the input parameters of the function must be added as globals.
		for(Iterator<Parameter> it = fun.getParams().iterator(); it.hasNext(); ){
			Parameter p = it.next();
			if(p.isParameterOutput()){
				assert !p.isParameterInput() : "Reference parameters not allowed in interface functions: " + funName ;
				assert outputParam == null;
				outputParam = p;
			}else{
				globals.add( new StmtVarDecl(p, p.getType(), p.getName(), null) );
			}
		}

		bodyL.add(new StmtAssign(new ExprVar(outputParam, outputParam.getName()), ExprConstInt.zero));

		globals.add( new StmtVarDecl(fun, new TypeArray(TypePrimitive.inttype, LockLen), this.locksName, null) );


		Type sType; ///This is the type of the schedule. If only two threads, schedule is bit. Otherwise it's int.
		Integer nT = nthreads.getIValue();
		if(nT != null && nT <= 2){
			sType = TypePrimitive.bittype;
		}else{
			sType = TypePrimitive.inttype;
		}


		//In addition to the current locals, we need a local to correspond to the PC for each thread.
		String pcname = varGen.nextVar(PCbase);
		locals.add(new StmtVarDecl(fun, new TypeArray(TypePrimitive.inttype, nthreads), pcname, ExprConstInt.zero));
		ExprVar pcVar = new ExprVar(fun, pcname);
		{
/**/		List<StmtVarDecl> localpDecl = new ArrayList<StmtVarDecl>();
/**/		List<StmtVarDecl> globalpDecl = new ArrayList<StmtVarDecl>();
			//First, we declare the global' and local' variables.
			declArrFromScalars(globals.iterator(), globalpDecl, nthreads);
			declArrFromScalars(locals.iterator(), localpDecl, nthreads);

			bodyL.addAll(localpDecl);
			bodyL.addAll(globalpDecl);
		}
		//Then, we assign to them the original locals and globals.
		{
			List<Statement> assignments = new ArrayList<Statement>();
			String idxNm = varGen.nextVar();
			Expression idx = new ExprVar(fun, idxNm);
			indexedAssignments(locals.iterator(), idx,  assignments);
			indexedAssignments(globals.iterator(), idx, assignments);
			StmtVarDecl idxDecl = new StmtVarDecl(fun, TypePrimitive.inttype, idxNm, ExprConstInt.zero);
			StmtAssign idxIncr = new StmtAssign(idx, new ExprBinary(idx, "+", ExprConstInt.one));
			Expression comp = new ExprBinary(null, ExprBinary.BINOP_LT, idx, nthreads);
/**/		StmtFor assign = new StmtFor(idxDecl, idxDecl, comp, idxIncr, new StmtBlock((FEContext) null, assignments));

			bodyL.add(assign);
		}

		{// Declaration of rest-local variables.
/**/		StmtVarDecl doneVar = new StmtVarDecl(fun, TypePrimitive.bittype, DONE, ExprConstInt.one);
/**/		StmtVarDecl invalidScheduleVar = new StmtVarDecl(fun, new TypeArray(TypePrimitive.bittype, nthreads), INV_SCHEDULE, ExprConstInt.zero);

			bodyL.add(doneVar);
			bodyL.add(invalidScheduleVar);
		}

		//Loop to execute all the different basic blocks.
		{
/**/ 		List<Statement> conditCF = new ArrayList<Statement>();

			String idxNmb = varGen.nextVar();
			Expression idxb = new ExprVar(fun, idxNmb);


			EliminateLockUnlock luelim = new EliminateLockUnlock(idxb, this.LockLen);
			VarSetReplacer vrepl = new VarSetReplacer();
			populateVarReplacerLocal(locals.iterator(), idxb, vrepl);
			populateVarReplacer(globals.iterator(), idxb, vrepl);
			parcfg.setNodeIDs();
			Statement tmp = new StmtAssign(pcVar, new ExprConstInt(parcfg.getExit().getId()));
			tmp = (Statement)tmp.accept(vrepl);
			conditCF.add(tmp);
			for(Iterator<CFGNode> itnode = parcfg.getNodes().iterator(); itnode.hasNext(); ){
				CFGNode node = itnode.next();
				condForNode(parcfg, node, idxb, pcVar, vrepl, luelim, conditCF);
			}
			Statement idxbDecl = new StmtVarDecl(fun, TypePrimitive.inttype, idxNmb, ExprConstInt.zero);
			Expression idxbCond = new ExprBinary(idxb, "<", nthreads);
			Statement idxbIncr = new StmtAssign(idxb, new ExprBinary(idxb, "+", ExprConstInt.one));
			bodyL.add(new StmtFor(idxbDecl, idxbDecl, idxbCond, idxbIncr, new StmtBlock((FEContext) null, conditCF)));
		}
		///Now, before we do the recursive call, we will build the parameter list, so we know what parameters to pass.
		///The parameters include both the globals and the locals.
		///Note that the globals should include the input parameters of the original function.

		List<Parameter> parList = new ArrayList<Parameter>();

		buildParsFromDecls(globals.iterator(), parList);
		buildParsFromDecls(locals.iterator(), parList);
        parList.add(new Parameter(fun, new TypeArray(sType, SchedLen), SCHEDULE));
        parList.add(new Parameter(fun, TypePrimitive.inttype, STEP));
        parList.add(new Parameter(fun, new TypeArray(TypePrimitive.inttype, nthreads),
                COUNTER));
		parList.add(outputParam);


		Statement recCalls = produceRecursiveCalls(nthreads, parList, funName);

		StmtIfThen finalIf = new StmtIfThen((FEContext) null, new ExprVar((FEContext) null, DONE), postpar, recCalls );


		bodyL.add(finalIf);

		restFunction = Function.creator(ploop, funName, FcnType.Generator).params(parList).body(new StmtBlock(bodyL)).create();

		//So now we have build the rest function, but while we are at it, we are also going to build the main function.

		assert parcfg.getEntry().getId() == 0: "This is an invariant";
		return buildMainFunction(oriGlobals, locals, ploop.getLoopVarDecl(), nthreads ,prepar, funName, fun.getName(),  parList, fun.getParams(),sType, pcname, fun.getSpecification());

	}



	/**
	 * T Fun(inputs){
	 * 	 declare globals;
	 *   prepar;
	 *   return restFun(globals, defaultLocals, sched, 0);
	 *
	 * }
	 * @param oriGlobals
	 * @param locals
	 * @param prepar
	 * @param funName
	 * @param parList
	 * @param oldPar
	 */

	public Function buildMainFunction(Set<StmtVarDecl> oriGlobals, Set<StmtVarDecl> locals, StmtVarDecl loopIdx,
										Expression nthreads, Statement prepar, String funName, String oriName,
										List<Parameter> parList, List<Parameter> oldPar, Type sType, String pcname, String specName){
		List<Statement> bodyL = new ArrayList<Statement>();
		for(Iterator<StmtVarDecl> it = oriGlobals.iterator(); it.hasNext(); ){
			StmtVarDecl svd = it.next();
			bodyL.add(svd);
		}
		bodyL.add(prepar);

		String itIdx = varGen.nextVar();
		StmtVarDecl idx = new StmtVarDecl(loopIdx, new TypeArray(loopIdx.getType(0), nthreads), loopIdx.getName(0), null);
		ExprVar loopIdxVar = new ExprVar((FEContext) null, loopIdx.getName(0));
		StmtAssign idxBody = new StmtAssign(new ExprArrayRange(loopIdxVar,new ExprVar((FEContext) null, itIdx) ), new ExprVar((FEContext) null, itIdx));
		bodyL.add(idx);
		bodyL.add(new StmtFor(itIdx, nthreads, idxBody));

		Map<String, Expression> locNames = new HashMap<String, Expression>();
		for(Iterator<StmtVarDecl> it = locals.iterator(); it.hasNext(); ){
			StmtVarDecl svd = it.next();
			for(int i=0; i<svd.getNumVars(); ++i){
				if(!loopIdxVar.getName().equals(svd.getName(i))){
					locNames.put(svd.getName(i), svd.getInit(i));
				}
			}
		}
		List<Expression> actuals = new ArrayList<Expression>();
		for(Iterator<Parameter> it = parList.iterator(); it.hasNext(); ){
			Parameter p = it.next();
			String name = p.getName();
			if(locNames.containsKey(name)){
				Expression e = locNames.get(name);
				if(e != null){
					actuals.add(e);
				}else{
					actuals.add(ExprConstInt.zero);
				}
			}else{
				if(pcname.equals(name)){
					actuals.add(ExprConstInt.zero);
				}else{
					if(name.equals(STEP) || name.equals(locksName) || name.equals(COUNTER)){
						actuals.add(ExprConstInt.zero);
					}else{
						actuals.add(new ExprVar((FEContext) null, name));
					}
				}
			}
		}
		ExprFunCall efc = new ExprFunCall((FEContext) null, funName, actuals);
		bodyL.add(new StmtExpr(efc));
		List<Parameter> formals = new ArrayList<Parameter>();
		Parameter outPar = null;
		for(Iterator<Parameter> it = oldPar.iterator(); it.hasNext(); ){
			Parameter p = it.next();
			if(p.isParameterOutput()){
				assert !p.isParameterInput(): "Reference parameters not allowed in interface functions: " + funName;
				assert outPar == null;
				outPar = p;
			}else{
				formals.add(p);
			}
		}
        formals.add(new Parameter(outPar, new TypeArray(sType, SchedLen), SCHEDULE));
		formals.add(outPar);
		bodyL.add(0, new StmtAssign(new ExprVar((FEContext) null, outPar.getName()), ExprConstInt.zero));
        return Function.creator((FEContext) null, oriName, FcnType.Generator).params(formals).spec(
                specName).body(new StmtBlock(bodyL)).create();
	}




	/**
	 * This function produces the following code:
	 * 	{
	 *      T[NT] out;
	 *      bit noDeadlock = 0;
	 *      for(int i=0; i<NT; ++i){
	 *          if(invalidSchedule[i]){
	 *          	out[i] = 1;
	 *          }else{
	 *      		out[i] = rest(globals'[i], locals'[i], sched, t+1);
	 *      		noDeadlock = 1;
	 *      	}
	 *      }
	 *      assert noDeadlock : "There is a possible deadlock in the code.";
	 *      assert t < NSTEPS
	 *    	return out[sched[t]];
	 * 	}
	 *
	 * @param nthreads
	 * @param parList
	 * @param funName
	 * @return
	 */
	public Statement produceRecursiveCalls(Expression nthreads, List<Parameter> parList, String funName){
		Type rtype = null;
		String rName = null;
		String tmpOut = varGen.nextVar();
		String fullOut = varGen.nextVar();
		String noDeadlock = varGen.nextVar("noDL");

		List<Statement> bodyL = new ArrayList<Statement>();

		List<Expression> actuals = new ArrayList<Expression>();
		ExprVar idx = new ExprVar((FEContext) null, varGen.nextVar());
		for(Iterator<Parameter> it = parList.iterator(); it.hasNext(); ){
			Parameter par = it.next();
			String name = par.getName();
			Type t = par.getType();
			if(name.equals(SCHEDULE)){
				actuals.add(new ExprVar((FEContext) null, name));
			}else if(name.equals(STEP)){
				Expression e = new ExprVar((FEContext) null, name);
				e = new ExprBinary(null, ExprBinary.BINOP_ADD, e, ExprConstInt.one);
				actuals.add(e);
			}else if(par.isParameterOutput()){
				assert !par.isParameterInput() : "Reference parameters not yet allowed in interface functions.";
				rtype = t;
				rName = name;
				actuals.add(new ExprVar((FEContext) null, tmpOut));
			}else if(name.equals(COUNTER)){
				actuals.add(new ExprVar((FEContext) null, COUNTER+ "_tmp"));
			}else{
				Expression e = new ExprVar((FEContext) null, name + "_p");
				e = new ExprArrayRange(e, idx);
				actuals.add(e);
			}
		}

		assert rName != null;
		Expression rExpr = new ExprVar((FEContext) null, rName);

		ExprFunCall fc = new ExprFunCall((FEContext) null, funName, actuals);
		List<Statement> ctrL = new ArrayList<Statement>();

		ctrL.add(new StmtVarDecl((FEContext) null, new TypeArray(TypePrimitive.inttype, nthreads), COUNTER + "_tmp", new ExprVar((FEContext) null,  COUNTER) ));
		ctrL.add(new StmtAssign(new ExprArrayRange(new ExprVar((FEContext) null, COUNTER+ "_tmp"), idx),  new ExprBinary(new ExprArrayRange(new ExprVar((FEContext) null, COUNTER+ "_tmp"), idx)  , "+", ExprConstInt.one ) ));
		Expression localDone =  new ExprArrayRange(new ExprVar((FEContext) null,INV_SCHEDULE), idx);
		Expression execCond = new ExprBinary(new ExprArrayRange(new ExprVar((FEContext) null, SCHEDULE), new ExprVar((FEContext) null, STEP)  ), "==", idx);

		Statement s = new StmtIfThen((FEContext) null, execCond, new StmtExpr(fc), null);
		Statement bbb = new StmtBlock(s, new StmtAssign(new ExprVar((FEContext) null, noDeadlock), ExprConstInt.one));
		s = new StmtIfThen((FEContext) null, new ExprUnary(localDone, ExprUnary.UNOP_NOT, localDone), bbb, null);
		ctrL.add(s);
		StmtBlock callToRest = new StmtBlock((FEContext) null, ctrL) ;


		bodyL.add( new StmtVarDecl((FEContext) null, rtype, tmpOut, ExprConstInt.one) );
		bodyL.add( new StmtVarDecl((FEContext) null, TypePrimitive.bittype, noDeadlock, ExprConstInt.zero) );


		List<Statement> forBody = new ArrayList<Statement>();
		forBody.add(callToRest);

		Statement forInit = new StmtVarDecl((FEContext) null, TypePrimitive.inttype, idx.getName(), ExprConstInt.zero );
		Statement forIncr = new StmtAssign(idx, new ExprBinary(idx, "+", ExprConstInt.one));
		Expression forCond = new ExprBinary(null,  ExprBinary.BINOP_LT, idx, nthreads);
		bodyL.add(new StmtFor(forInit, forInit, forCond, forIncr, new StmtBlock((FEContext) null, forBody)));
		bodyL.add(new StmtAssert(new ExprVar((FEContext) null, noDeadlock), "There is a possible deadlock in the code.", false));
		bodyL.add(new StmtAssert(new ExprBinary(new ExprVar((FEContext) null, STEP), "<", SchedLen) , "The schedule is too short. Not all threads had time to terminate.", false));
		bodyL.add(new StmtAssign(rExpr, new ExprVar((FEContext) null, tmpOut ) ));

		return new StmtBlock((FEContext) null, bodyL);
	}


	/**
	 * Creates one parameter for each variable declaration in the input list.
	 *
	 *
	 * @param vars an iterator to the input list.
	 * @param params output parameter.
	 */
	public void buildParsFromDecls(Iterator<StmtVarDecl> vars, List<Parameter>/*out*/ params){
		while(vars.hasNext()){
			StmtVarDecl svd = vars.next();
			FENode cx = svd;
			for(int i=0; i<svd.getNumVars(); ++i){
                params.add(new Parameter(svd, svd.getType(i), svd.getName(i)));
			}
		}
	}



	/**
	 * Populates the VarSetReplacer with replacement rules of the form:
	 * X -> X_p[idx][idx].
	 *
	 * @param vars
	 * @param idx
	 * @param vrepl
	 */
	public void populateVarReplacerLocal(Iterator<StmtVarDecl> vars, Expression idx, VarSetReplacer/*out*/ vrepl){
		while(vars.hasNext()){
			StmtVarDecl svd = vars.next();
			FENode cx = svd;
			for(int i=0; i<svd.getNumVars(); ++i){
				String oname = svd.getName(i);
				Expression arr = new ExprArrayRange(new ExprVar(cx, oname + "_p"), idx);
				arr = new ExprArrayRange(arr, idx);
				vrepl.addPair(oname, arr);
			}
		}
	}

	/**
	 * Populates the VarSetReplacer with replacement rules of the form:
	 * X -> X_p[idx].
	 *
	 * @param vars
	 * @param idx
	 * @param vrepl
	 */
	public void populateVarReplacer(Iterator<StmtVarDecl> vars, Expression idx, VarSetReplacer/*out*/ vrepl){
		while(vars.hasNext()){
			StmtVarDecl svd = vars.next();
			FENode cx = svd;
			for(int i=0; i<svd.getNumVars(); ++i){
				String oname = svd.getName(i);
				vrepl.addPair(oname, new ExprArrayRange(new ExprVar(cx, oname + "_p"), idx));
			}
		}
	}



	private Statement transformStmt(Statement newStmt, VarSetReplacer vrepl, EliminateLockUnlock luelim){
		newStmt = (Statement)newStmt.accept(luelim);
		newStmt = (Statement)newStmt.accept(vrepl);

		return newStmt;
	}

	/**
	 * Produces an AST node of the form:
	 *		if(pc[idx] == 0){
	 *    		cfg[0] ( globals -> globals'[i], locals -> locals'[i])
	 *    		pc_p[idx][idx] = next;
	 *    	}
	 *    Or
	 *   	if(locals[i].pc != ENDPC){
	 *      	done = false;
	 *      }else{
	 *      	invalidSchedule[i] = 1;
	 *      }
	 * @param node
	 * @param nodeID
	 * @param pcVar
	 * @param vrepl
	 * @param conditCF
	 */
	public void condForNode(CFG cfg, CFGNode node, Expression idx, ExprVar pcVar, VarSetReplacer vrepl, EliminateLockUnlock luelim ,List<Statement>/*out*/ conditCF){
		DummyFENode cx = new DummyFENode (node.getStmt());

		Expression tmpPC = new ExprVar(cx, pcVar.getName() + "_p");
		tmpPC = new ExprArrayRange(tmpPC, idx);
		tmpPC = new ExprArrayRange(tmpPC, idx);
		final Expression lhsPC = tmpPC;

		Expression cond = new ExprBinary(cx, ExprBinary.BINOP_EQ, new ExprArrayRange(cx, pcVar, idx), new ExprConstInt(node.getId()) );

		if(node.isStmt()){
			Statement newStmt = transformStmt(node.getStmt(), vrepl, luelim);
			List<EdgePair> succ = cfg.getSuccessors(node);
			assert succ.size() == 1;
			assert succ.get(0).label == null;
			Expression rhs = new ExprConstInt( succ.get(0).node.getId() );
			Statement pcassign = new StmtAssign(lhsPC, rhs);

			List<Statement> bodyL = new ArrayList<Statement>(2);
			bodyL.add(newStmt);
			bodyL.add(pcassign);

			conditCF.add(new StmtIfThen(cx, cond, new StmtBlock(cx, bodyL), null));
		}

		if(node.isExpr() && ! node.isSpecial() ){
			Expression expr = (Expression)node.getExpr().accept(vrepl);
			List<EdgePair> succ = cfg.getSuccessors(node);
			assert succ.size() == 2;

			int tid;
			int fid;
			if(succ.get(0).label == 0){
				fid = succ.get(0).node.getId();
				assert succ.get(1).label == 1;
				tid = succ.get(1).node.getId();
			}else{
				assert succ.get(0).label == 1;
				tid = succ.get(0).node.getId();
				assert succ.get(1).label == 0;
				fid = succ.get(1).node.getId();

			}


			Expression rhsT = new ExprConstInt( tid );
			Statement pcassignT = new StmtAssign(lhsPC, rhsT);

			Expression rhsF = new ExprConstInt( fid );
			Statement pcassignF = new StmtAssign(lhsPC, rhsF);

			StmtIfThen condTransfer = new StmtIfThen(cx, expr, pcassignT, pcassignF);

			List<Statement> bodyL = new ArrayList<Statement>(2);

			if(node.getPreStmt() != null){
				bodyL.add(transformStmt(node.getPreStmt(), vrepl, luelim));
			}

			bodyL.add(condTransfer);


			conditCF.add(new StmtIfThen(cx, cond, new StmtBlock(cx, bodyL), null));
		}


		if(node.isExpr() && node.isSpecial() ){
			List<EdgePair> succ = cfg.getSuccessors(node);
			final int trans[] = new int[succ.size()];
			for(int i=0; i<trans.length; ++i){
				EdgePair ep = succ.get(i);
				trans[ep.label] = ep.node.getId();
			}
			Statement s =  transformStmt(node.getPreStmt(), vrepl, luelim);
			final ExprVar next = (ExprVar)node.getExpr();

			FEReplacer repl = new FEReplacer(){
				@Override
				public Object visitStmtAssign(StmtAssign stmt){
					if(stmt.getLHS() == next){
						assert stmt.getRHS() instanceof ExprConstInt;
						ExprConstInt id = (ExprConstInt) stmt.getRHS();
						return new StmtAssign(lhsPC, new ExprConstInt(trans[id.getVal()]) );
					}else{
						return stmt;
					}
				}
			};
			s = (Statement) s.accept(repl);
			conditCF.add(new StmtIfThen(cx, cond,s, null));
		}

		if(node.isEmpty()){
			List<EdgePair> succ = cfg.getSuccessors(node);

			if(succ.size() == 1){
				assert succ.get(0).label == null;
				Expression rhs = new ExprConstInt( succ.get(0).node.getId() );
				Statement pcassign = new StmtAssign(lhsPC, rhs);
				conditCF.add(new StmtIfThen(cx, cond, pcassign, null));
			}else{
				//In this case, we are in endpc.
				assert succ.size() == 0;
				Statement doneAss = new StmtAssign(new ExprVar((FEContext) null, "_done"), ExprConstInt.zero);
				Statement invSchedAss = new StmtAssign(new ExprArrayRange(new ExprVar((FEContext) null, INV_SCHEDULE), idx), ExprConstInt.one);
				cond = new ExprBinary(cx, ExprBinary.BINOP_NEQ, new ExprArrayRange(cx, pcVar, idx), new ExprConstInt(node.getId()) );
				conditCF.add(new StmtIfThen(cx, cond, doneAss, invSchedAss));
			}
		}
	}

	/**
	 * Get as input a collection of variables, and produce as et of assignemtns of the form:
	 *
	 * X_p[idx] = X; where X is a variable in the vars collection.
	 *
	 *
	 * @param vars
	 * @param idx
	 * @param pAssign
	 */
	public void indexedAssignments(Iterator<StmtVarDecl> vars, Expression idx, List<Statement>/*output*/pAssign){
		while(vars.hasNext()){
			StmtVarDecl svd = vars.next();
			for(int i=0; i<svd.getNumVars(); ++i){
				String oname = svd.getName(i);
				Expression lhs = new ExprVar(svd, oname+"_p");
				lhs = new ExprArrayRange(lhs, idx);
				Expression rhs = new ExprVar(svd, oname);
				pAssign.add(new StmtAssign(lhs, rhs));
			}
		}
	}


	/**
	 * The input collection contains variable declarations, and the output collection contains variable' declarations such that
	 * if T X; is a declaration in the original list, T[NTHREADS] X_p will be a declaration in the output list.
	 *
	 * @param original input list
	 * @param pDecl output list
	 * @param nthreads number of threads.
	 */
	public void declArrFromScalars(Iterator<StmtVarDecl> original,  List<StmtVarDecl>/*output*/ pDecl, Expression nthreads){
		while(original.hasNext()){
			StmtVarDecl svd = original.next();
			FENode cx = svd;
			for(int i=0; i<svd.getNumVars(); ++i){
				String oname = svd.getName(i);
				Type ot = svd.getType(i);
				//assert svd.getInit(i) == null : "At this stage, declarations shouldn't have initializers";
				String nname = oname + "_p";
				Type nt = new TypeArray(ot, nthreads);
				pDecl.add(new StmtVarDecl(cx, nt, nname, null));
			}
		}
	}


	public void vectorizeLocals(Set<StmtVarDecl> locals, Expression nthreads){
		List<StmtVarDecl> newLocals = new ArrayList<StmtVarDecl>(locals.size());
		for(Iterator<StmtVarDecl> it = locals.iterator(); it.hasNext(); ){
			StmtVarDecl svd = it.next();
			List<Type> newTypes = new ArrayList<Type>(svd.getNumVars());
			for(int i=0; i<svd.getNumVars(); ++i){
				newTypes.add(new TypeArray(svd.getType(i) , nthreads));
			}
			newLocals.add(new StmtVarDecl(svd, newTypes, svd.getNames(), svd.getInits()));
		}
		locals.clear();
		locals.addAll(newLocals);
	}

	public Function produceParallelModel(Function fun){
		/// First, we divide into pre and post parallelism sections.
		fun = (Function)fun.accept(new ExtractPreParallelSection());
		///Then, we build a CFG for the Ploop.

		fun.accept(new SimpleCodePrinter());

		BreakParallelFunction parts = new BreakParallelFunction();
		fun.accept(parts);

		if(parts.ploop != null){

			Set<StmtVarDecl> locals = new HashSet<StmtVarDecl>();
			StmtFork ploop = (StmtFork) parts.ploop.accept(new AtomizeConditionals(varGen));
			System.out.println("%%%%%%%%%%%%%%%%%%%%%%");
			ploop.accept(new SimpleCodePrinter());
			//ploop = (StmtPloop) ploop.accept(new MinimizeLocals());
			System.out.println("$$$$$$$$$$$$$$$$$$$$$$");
			ploop.accept(new SimpleCodePrinter());
			CFG cfg = CFGforPloop.buildCFG(ploop, locals);

			vectorizeLocals(locals, ploop.getIter());

			System.out.println(" globals = " + parts.globalDecls.size());
			System.out.println(" locals = " + locals.size());
			Function rest = constructRestFunction(cfg, parts.postpar, parts.prepar, ploop, parts.globalDecls, locals, fun);

			///Then, we build a function to represent the postParallelism section.
			if(restFunction != null)
				this.newFuncs.add(restFunction);
			restFunction = null;

			return rest;
		}else{
			return fun;
		}
	}

	public Object visitFunction(Function func)
    {
        return produceParallelModel(func);
    }



}
