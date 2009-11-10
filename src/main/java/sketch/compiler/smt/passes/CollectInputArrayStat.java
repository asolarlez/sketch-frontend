package sketch.compiler.smt.passes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.dataflow.PartialEvaluator;
import sketch.compiler.dataflow.nodesToSB.IntVtype;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;

/**
 * Go through the stencil code to figure out which ufun is the input array and
 * count how many places that ufun is accessed.
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 * 
 */
public class CollectInputArrayStat extends PartialEvaluator {

	protected int mNumUFunAccess = 0;
	protected Function mFunTOModel;

	protected Function mCurrFunc;
	/**
	 * Maps form the ufun to the number of times the ufun is accessed
	 */
	private Map<String, Integer> mNumGridAccesses;

	protected Map<String, Map<String, Function>> mGlobalInVars;

	public int getNumOfAccess() {
		return mNumUFunAccess;
	}

	public Function getFuncToModel() {
		return mFunTOModel;
	}

	public CollectInputArrayStat(
			Map<String, Map<String, Function>> globalInVars, TempVarGen varGen,
			boolean isReplacer, int maxUnroll, RecursionControl rcontrol) {
		super(new IntVtype(), varGen, isReplacer, maxUnroll, rcontrol);

		mNumGridAccesses = new HashMap<String, Integer>();
		mGlobalInVars = globalInVars;
	}

	@Override
	public Object visitFunction(Function func) {
		mCurrFunc = func;
		return super.visitFunction(func);
	}

	public Object visitExprFunCall(ExprFunCall exp) {
		String name = exp.getName();
		// Local function?
		Function fun = ss.getFuncNamed(name);
		if (fun.getSpecification() != null) {
			assert false : "The substitution of sketches for their respective specs should have been done in a previous pass.";
		}
		if (fun != null) {

			if (fun.isUninterp()) {
				if (isFuncModelingInputGrid(fun)) {
					// if fun is one of the function that models the input grids
					int numAcc = 0;
					if (mNumGridAccesses.containsKey(fun.getName())) 
						numAcc = mNumGridAccesses.get(fun.getName());
					numAcc++;
					mNumGridAccesses.put(fun.getName(), numAcc);
					
//					System.out.println("accessing ufun:" + fun.getName());
					
				} else {
					// there could be other uninterpreted functions such
					// as those used to model floating point numbers
				}

				return super.visitExprFunCall(exp);
			} else {
				if (rcontrol.testCall(exp)) {
					
					/* Increment inline counter. */
					rcontrol.pushFunCall(exp, fun);

					List<Statement> oldNewStatements = newStatements;
					newStatements = new ArrayList<Statement>();
					state.pushLevel();
					try {
						{
							Iterator<Expression> actualParams = exp.getParams()
									.iterator();
							Iterator<Parameter> formalParams = fun.getParams()
									.iterator();
							inParameterSetter(exp, formalParams, actualParams,
									false);
						}
						Statement body = null;
						try {

							body = (Statement) fun.getBody().accept(this);
						} catch (RuntimeException ex) {
							state.popLevel(); // This is to compensate for a
												// pushLevel in inParamSetter.
							// Under normal circumstances, this gets offset by a
							// popLevel in outParamSetter, but not in the
							// pressence of exceptions.
							throw ex;
						}
						addStatement(body);
						{
							Iterator<Expression> actualParams = exp.getParams()
									.iterator();
							Iterator<Parameter> formalParams = fun.getParams()
									.iterator();
							outParameterSetter(formalParams, actualParams,
									false);
						}
					} finally {
						state.popLevel();
						newStatements = oldNewStatements;
					}
					rcontrol.popFunCall(exp);
				} else {
					
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
				return vtype.BOTTOM();
			}
		}
		exprRV = null;
		return vtype.BOTTOM();
	}

	protected boolean isFuncModelingInputGrid(Function fun) {
		String fName = fun.getName();
		for (Function f : mGlobalInVars.get(mCurrFunc.getName()).values()) {
			if (f.getName().equals(fName))
				return true;
		}
		return false;
	}

	public Map<String, Integer> getNumGridAccesses() {
		return mNumGridAccesses;
	}

}
