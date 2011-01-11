package sketch.compiler.smt;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstBoolean;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.stmts.StmtWhile;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.dataflow.MethodState.Level;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.smt.partialeval.BitVectUtil;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.NodeToSmtVtype;
import sketch.compiler.smt.partialeval.TypedPartialEvaluator;
import sketch.compiler.smt.partialeval.TypedVtype;
import sketch.compiler.smt.passes.AddWrapper;

public class ProduceSMTCode extends TypedPartialEvaluator {

	protected NodeToSmtVtype vtype;
	
	boolean tracing = false;
	PrintStream traceStream = System.out;
	boolean mUseTOA;
	boolean mUseBV;
	int currObserIdx;


	/*
	 * Getters & Setters
	 */
	

	/*
	 * Constructors
	 */
	public ProduceSMTCode(
			TypedVtype vtype,
			TempVarGen varGen,
			boolean useTheoryOfArray,
			boolean useBV,
			int maxUnroll,
			RecursionControl rcontrol, 
			boolean tracing) {
		// super(vtype, varGen, isReplacer, maxUnroll, rcontrol);
		super(vtype, 
				varGen, false,
				maxUnroll, rcontrol);
		
		this.tracing = tracing;
		mUseTOA = useTheoryOfArray;
		mUseBV = useBV;
		this.vtype = (NodeToSmtVtype) super.vtype;
		this.vtype.setMethodState(super.state);
		
	}
	
	@Override
	protected List<Function> functionsToAnalyze(StreamSpec spec) {
		List<Function> l = new LinkedList<Function>();
		for (Function f : spec.getFuncs()) {
			if (f.getName().equals(AddWrapper._MAIN) && 
			        f.isGenerator())
				l.add(f);
		}
		return l;
	}

	/**
	 * Generate the formula for the "_main" function
	 * 
	 * 1) initialize function parameters to values from observatino or a free variable
	 * 2) generate formula for function body
	 * 
	 * @param func
	 * @return
	 */
	@Override
	public Object visitFunction(Function func) {
		// int sp(int v) {
	    // }
	    // Translates into:
	    // v : INT;          // v is a global input variable
	    // v_0 = v;    // v_0 is a local variable
	    
	    // int sp(int[] a) {
        // }
	    // Translates into:
        // a : ARRAY BITVECTOR(32) OF BITVECTOR(32);     // a is a global input variable
        // a_0 = a;    // a_0 is a local variable
		for (Parameter param : func.getParams()) {
		    if(param.isParameterOutput()){
		        state.outVarDeclare(param.getName(), param.getType());
		    }else{
		        state.varDeclare(param.getName(), param.getType());
		    }
			if (param.isParameterInput()) {

				NodeToSmtValue inputVal;
				Type paramType = param.getType();
				inputVal = (NodeToSmtValue) state.varValue(param.getName());
				
				if (mUseTOA) {
				    NodeToSmtValue paramVal = vtype.newParam(param, param.getType());
                    state.setVarValue(param.getName(), paramVal);
				    
				} else {
				    if (BitVectUtil.isPrimitive(paramType)) {
		                
	                    NodeToSmtValue paramVal = vtype.newParam(param, param.getType());
	                    state.setVarValue(param.getName(), paramVal);
	                    
	                } else {
	                    List<abstractValue> vectValue = inputVal.getVectValue();
	                    
	                    int i = 0;
	                    for (abstractValue arrEle : vectValue) {
	                        NodeToSmtValue ntsvEle = (NodeToSmtValue) arrEle;
	                        
	                        NodeToSmtValue paramVal = vtype.newParam(param, i, ntsvEle.getType());
	                        
	                        state.setVarValue(param.getName(), vtype.CONST(i),
	                                paramVal);
	                        i++;
	                    }
	                }
				}
				
			}
		}

		Level lvl3 = state.beginFunction(func.getName());
		func.getBody().accept(this);
		state.endFunction(lvl3);

		return func;
	}	

	public int ufunAccCount = 0;
	public boolean forReal = false;
	
	public int getUFunAccCount() { return ufunAccCount; }

	@Override
	public Object visitExprFunCall(ExprFunCall exp) {
		String name = exp.getName();
		// Local function?
		Function fun = super.ss.getFuncNamed(name);
		
		if (fun != null) {
			assert !fun.isUninterp() : "fun calls to uninterpreted functions are not supported yet.";
			// normal function call
			if (rcontrol.testCall(exp)) {
				
				ArrayList<Expression> inArgs = new ArrayList<Expression>();
				LinkedList<ExprVar> outArgs = new LinkedList<ExprVar>();
				LinkedList<Expression> refArgs = new LinkedList<Expression>();
				separateInputOutputRef(fun.getParams(), exp.getParams(), inArgs, outArgs, refArgs);
				
				
				NodeToSmtValue[] inArgVals = new NodeToSmtValue[inArgs.size()];
				int i = 0;
				for (Expression inArg : inArgs) {
					inArgVals[i] = (NodeToSmtValue) inArg.accept(this);
					i++;
				}
				
				NodeToSmtValue funccall = vtype.FUNCCALL(fun.getReturnType(), fun.getName(), inArgVals);
				// check cache first
				List<NodeToSmtValue> outlets = vtype.getHashedFuncCall(funccall);
				if (outlets == null) {
					// partial eval the fun
					inlineFunction(exp, fun, funccall);
					
				} else {
					// if the function call was cached, use the cached outlet values
					setOutletToOutArgs(outArgs, outlets);
				}
				
			} else {
				// don't inline
				if (rcontrol.leaveCallsBehind()) {
					funcsToAnalyze.add(fun);
					return super.visitExprFunCall(exp);
				} else {
					StmtAssert sas = new StmtAssert(exp, ExprConstInt.zero, false);
					sas.setMsg((exp != null ? exp.getCx().toString() : "")
							+ exp.getName());
					sas.accept(this);
				}
			}
			exprRV = exp;
//				return vtype.FUNC("FUN_RET_VAL", fun.getReturnType());
			return null;
		}
		
		exprRV = null;
		return vtype.BOTTOM("FUN_RET_VAL", fun.getReturnType());
	}

	protected void setOutletToOutArgs(LinkedList<ExprVar> outArgs,
			List<NodeToSmtValue> outlets) {
		Iterator<NodeToSmtValue> outletIter = outlets.iterator();
		
		for (Expression argExp : outArgs) {
			abstractValue outletNode = outletIter.next();
			
			if( outletNode != null ){
				String lhsName = null;
		        abstractValue lhsIdx = null;

		        lhsVisitor lhsv = new lhsVisitor();
		        argExp.accept(lhsv);
		        lhsName = lhsv.lhsName;
		        lhsIdx = lhsv.lhsIdx;
		        assert lhsv.rlen == -1 : "Violates invariant";
				state.setVarValue(lhsName, lhsIdx, outletNode);
			}
		}
	}

	protected void inlineFunction(ExprFunCall exp, Function fun,
			NodeToSmtValue funccall) throws AssertionError {
		
		/* Increment inline counter. */
		rcontrol.pushFunCall(exp, fun);

//		String[] comment = { "Inlining :" + funccall };
//		vtype.addBlockComment(comment);
				
		Level lvl = state.pushLevel("producesmtcode inlinefunction");
		List<Statement> oldNewStatements = newStatements;
		newStatements = new ArrayList<Statement>();
		try {
		    Level lvl2;
			{
				Iterator<Expression> actualParams = exp.getParams()
						.iterator();
				Iterator<Parameter> formalParams = fun.getParams()
						.iterator();
				lvl2 = inParameterSetter(exp, formalParams, actualParams,
						false);
			}
			Statement body = null;
			try {

				body = (Statement) fun.getBody().accept(this);
			} catch (RuntimeException ex) {
				state.popLevel(lvl); // This is to compensate for a
				// pushLevel in inParamSetter.
				// Under normal circumstances, this gets offset by a
				// popLevel in outParamSetter, but not in the
				// pressence of exceptions.
				throw ex;
			} catch (AssertionError e) {
				state.popLevel(lvl); // This is to compensate for a
				// pushLevel in inParamSetter.
				// Under normal circumstances, this gets offset by a
				// popLevel in outParamSetter, but not in the
				// pressence of exceptions.
				throw e;
			}
			addStatement(body);
			{
				Iterator<Expression> actualParams = exp.getParams()
						.iterator();
				Iterator<Parameter> formalParams = fun.getParams()
						.iterator();
				
				
				List<NodeToSmtValue> outletNodes = new LinkedList<NodeToSmtValue>();
				for (Parameter param : fun.getParams()) {
					if (param.isParameterOutput()) {
						NodeToSmtValue outletNode = (NodeToSmtValue) state.varValue(param.getName());
						outletNodes.add(outletNode);
					}
				}
				vtype.putHashedFuncCall(funccall, outletNodes);
				
				outParameterSetter(formalParams, actualParams,
						false, lvl2);
			}
		} finally {

//			String[] comment2 = { "Done inlining :" + funccall };
//			vtype.addBlockComment(comment2);

			state.popLevel(lvl);
			newStatements = oldNewStatements;
			rcontrol.popFunCall(exp);
		}
	}

	
	
	@Override
	public Object visitExprConstBoolean(ExprConstBoolean exp) {
		return ((NodeToSmtVtype) this.vtype).CONST(exp.getVal());
	}

	@Override
	public Object visitExprConstInt(ExprConstInt exp) {
		int v = exp.getVal();
		if (v == 0 || v == 1) {
			return ((NodeToSmtVtype) this.vtype).CONSTBIT(v);
		}
		return super.visitExprConstInt(exp);
	}
	
	@Override
	public Object visitStmtAssert(StmtAssert stmt) {
//		mFormula.addComment(stmt.toString());
		/* Evaluate given assertion expression. */
        Expression assertCond = stmt.getCond();
        abstractValue vcond  = (abstractValue) assertCond.accept (this);
        Expression ncond = exprRV;
        String msg = null;
        msg = stmt.getMsg();
        
        state.Assert(vcond, msg, stmt.isSuper());
        
        return isReplacer ?  new StmtAssert(stmt, ncond, stmt.getMsg(), stmt.isSuper())  : stmt;
	}
	
	@Override
	public Object visitExprStar(ExprStar star) {
		// define hole variable
	
			Type t = star.getType();
			int ssz = 1;
			List<abstractValue> avlist = null;
			

			if (BitVectUtil.isPrimitive(t)) {
				
				NodeToSmtValue holeValue = vtype.newHole(star, star.getType());
			
				return holeValue;
				
			} else {
				Integer iv = ((TypeArray) t).getLength().getIValue();
				assert iv != null;
				ssz = iv;
				avlist = new ArrayList<abstractValue>(ssz);
				Type baseType = ((TypeArray) t).getBase();
				
				NodeToSmtValue nv = null;

				for (int i = 0; i < ssz; ++i) {
					nv = vtype.newHole((ExprStar) star.getDepObject(i), baseType);
					
					if (avlist != null)
						avlist.add(nv);
				}
				if (avlist != null)
					nv = new NodeToSmtValue(avlist);
				
				return nv;
			}

	}
	
	/*
	 * 
	 * For debug breakpoing purpose
	 */
	@Override
	public Object visitStmtIfThen(StmtIfThen s) {
		return super.visitStmtIfThen(s);
	}

	@Override
	public Object visitStmtWhile(StmtWhile stmt) {
		return super.visitStmtWhile(stmt);
	}

	@Override
	public Object visitStmtVarDecl(StmtVarDecl s) {
//		mFormula.addComment(s.toString());
		return super.visitStmtVarDecl(s);
	}

	@Override
	public Object visitStmtAssign(StmtAssign s) {
//		vtype.addComment(s.toString());
		return super.visitStmtAssign(s);
	}
	
	@Override
	public Object visitExprTernary(ExprTernary exp) {
		return super.visitExprTernary(exp);
	}

	@Override
	protected void doStatement(Statement stmt) {
		super.doStatement(stmt);
	}

	@Override
	public Object visitStmtFor(StmtFor stmt) {
		return super.visitStmtFor(stmt);
	}


	/*
	 * Helpers
	 */
	
	private class GetUninterpretedFuncNames extends FEReplacer {

		private Set<Function> resultFunc = new HashSet<Function>();

		@Override
		public Object visitExprFunCall(ExprFunCall exp) {
			Function f = super.sspec.getFuncNamed(exp.getName());
			if (f.isUninterp() && !resultFunc.contains(f)) {
				resultFunc.add(f);
			}
			return super.visitExprFunCall(exp);
		}
	}
	
	/**
	 * 
	 * @param params
	 * @param args
	 * @param inArgs OUT PARAM
	 * @param outArgs OUT PARAM
	 * @param refArgs OUT PARAM
	 */
	protected void separateInputOutputRef(List<Parameter> params, List<Expression> args, List<Expression> inArgs, 
			List<ExprVar> outArgs, List<Expression> refArgs) {
		Iterator<Expression> argsIter = args.iterator();
		
		for (Parameter param: params) {
			Expression arg = argsIter.next();
			
			if (param.isParameterInput())
				inArgs.add(arg);
			else if (param.isParameterOutput())
				outArgs.add((ExprVar) arg);
			else if (param.isParameterReference())
				refArgs.add(arg);		
		}
	}

}
