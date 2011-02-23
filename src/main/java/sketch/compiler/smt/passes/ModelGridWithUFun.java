package sketch.compiler.smt.passes;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Function.FuncType;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.Program;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtIfThen;
import sketch.compiler.ast.core.typs.TypePrimitive;

/**
 * This class is used with StencilSmtSketchMain class to abstract the input array with a
 * SKETCH function
 * 
 * in(idx, !r)
 * 
 * is turned into in(idx, g0, v0, v1, !r) <code>
 * 
 * int in(int idx, int g0, int v0, int v1, int r) {
 * 
 * 		if (idx == g0) 
 * 			return v0; 
 * 		else 
 * 			return v1; 
 * }
 * 
 * </code>
 * 
 * In the case of multi dimensional input array: in(idx1, idx2, !r)
 * 
 * Suppose there are three accesses to input array:
 * 
 * int in(int idx1, int idx2, int r) {
 * 
 * if (idx1 == g0 && idx2 == g1) return v0; else if (idx1 == g2 && idx2 == g3)
 * return v1; else return v2; }
 * 
 * Invariants: 1) the number of branches = # v-values = # accesses to input
 * array 2) the number of g-value in the condition is the dimension of the input
 * array
 * 
 * 
 * @author Lexin Shan
 * @email lshan@eecs.berkeley.edu
 * 
 */
public class ModelGridWithUFun extends FEReplacer {

	// from outside
	protected TempVarGen mTmpVarGen;
	protected Map<String, Integer> mNumGridAccesses;

	public ModelGridWithUFun(Map<String, Integer> numGridAccesses,
			TempVarGen tmpVarGen) {
		super();
		mTmpVarGen = tmpVarGen;
		mNumGridAccesses = numGridAccesses;
	}

	@Override
	public Object visitProgram(Program prog) {

		for (String ufunName : mNumGridAccesses.keySet()) {
			AddUFunModeling visitor = new AddUFunModeling(ufunName,
					mNumGridAccesses.get(ufunName), mTmpVarGen);
			prog = (Program) prog.accept(visitor);
		}

		return prog;
	}

	public static class AddUFunModeling extends FEReplacer {

		// from outside
		protected TempVarGen mTmpVarGen;
		int mNumCases;
		String mUfunToReplace;

		// from inside
		protected Function mModelingFunc;

		protected List<Parameter> mNewParamsToAdd;
		protected List<Expression> mNewArgs;

		protected Set<String> mTaintedSet;
		protected Set<String> mNewTaintedSet; // funs to process at each
		// iteration
		protected Set<String> mProcessedFuns;

		protected Function mCurrFunc;

		protected boolean mIsFirstPass = true;

		/**
		 * Constructor
		 * 
		 * @param numAccess
		 *            number of distinct accesses the the input array
		 */
		public AddUFunModeling(String ufunName, int numCases,
				TempVarGen tmpVarGen) {
			mUfunToReplace = ufunName;
			mNumCases = numCases;
			mTmpVarGen = tmpVarGen;

			mTaintedSet = new HashSet<String>();
			mTaintedSet.add(mUfunToReplace);

			mNewParamsToAdd = new LinkedList<Parameter>();
			mNewArgs = new LinkedList<Expression>();

			mProcessedFuns = new HashSet<String>();
		}

		@Override
		public Object visitFunction(Function func) {
			if (mIsFirstPass) {

				if (func.isUninterp() && func.getName().equals(mUfunToReplace)) {
					return createUFun(func, func.getName());
				}
				return func;

			} else {

				mCurrFunc = func;
				Function ret = (Function) super.visitFunction(func);

				if (mNewTaintedSet.contains(func.getName())) {
					// if yourself is tainted, you need to modify yourself

					List<Parameter> newParams = new LinkedList<Parameter>(
							mNewParamsToAdd);
					newParams.addAll(ret.getParams());
					ret = new Function(ret, ret.getCls(), ret.getName(), ret
							.getReturnType(), newParams,
							ret.getSpecification(), ret.getBody());

				}

				return ret;
			}
		}

		/**
		 * The general strategy of this class is this: 1) visit all functions
		 * once to create the concrete modeling function to replace the ufun.
		 * because the modeling func has more parameters than the ufunc, all
		 * callers of the original ufunc need to call the modeling func and thus
		 * need to pass extra arguments. Since the new arguments should be
		 * treated as inputs, they are propagated to the calling function and
		 * its calling functions and so on.
		 * 
		 * 2) use a worklist algorithm: 1) if the worklist is not empty, pop a
		 * function out of it 2) visit the function, if the function calls any
		 * function in the mTaintedSet and the calling function is not in
		 * mProcessedFuns set, then the calling function is also tainted. Each
		 * tainted function will need to add the mNewParamsToAdd list of
		 * parameters to its original param list. That way the new params
		 * introduced in the modeling function are propagated to all of its
		 * callers and will eventually treated as an input to the sketching
		 * problem
		 */
		@Override
		public Object visitStreamSpec(StreamSpec spec) {

			StreamSpec ret = (StreamSpec) super.visitStreamSpec(spec);

			mIsFirstPass = false;
			while (!mTaintedSet.isEmpty()) {
				mNewTaintedSet = new HashSet<String>();
				ret = (StreamSpec) super.visitStreamSpec(ret);

				mTaintedSet = mNewTaintedSet;
			}
			return ret;
		}

		@Override
		public Object visitExprFunCall(ExprFunCall exp) {
			if (mTaintedSet.contains(exp.getName())) {
				Function callee = sspec.getFuncNamed(exp.getName());

				if (!mProcessedFuns.contains(mCurrFunc.getName())) {
					mNewTaintedSet.add(mCurrFunc.getName());
					mProcessedFuns.add(mCurrFunc.getName());
				}

				// modify the call to include new args

				LinkedList<Expression> newArgList = new LinkedList<Expression>(
						mNewArgs);

				if (callee.getName().equals(mModelingFunc.getName())) {
					// if the original call has N arguments, add the first N-1
					// to the front of the newArgList, add the last one to
					// end of newArgList
//					for (int i = 0; i < exp.getParams().size() - 1; i++) {
//						newArgList.addFirst(exp.getParams().get(i));
//					}
					
					newArgList.addAll(exp.getParams());

//					newArgList.add(exp.getParams().get(
//							exp.getParams().size() - 1));
					return new ExprFunCall(exp, mModelingFunc.getName(),
							newArgList);
				} else {
					newArgList.addAll(exp.getParams());
					return new ExprFunCall(exp, exp.getName(), newArgList);
				}

			}
			return super.visitExprFunCall(exp);
		}

		Expression getRetVar(ExprFunCall exp) {
			Function func = sspec.getFuncNamed(exp.getName());

			int pos = 0;
			for (Parameter p : func.getParams()) {
				if (p.isParameterOutput()) {
					break;
				}
				pos++;
			}

			return exp.getParams().get(pos);
		}

		protected Function createUFun(Function ufun, String name) {
			int inputArrDim = ufun.getParams().size() - 1;

			// parameters for the new func
			List<Parameter> paramList = new LinkedList<Parameter>();

			// get the index variables from the ufun
			List<ExprVar> idxVarList = new LinkedList<ExprVar>();
			for (int i = 0; i < inputArrDim; i++) {
				String idxVarName = ufun.getParams().get(i).getName();
				idxVarList.add(new ExprVar(ufun, idxVarName));

				// add the index vars as parameters to new fun
				paramList.add(new Parameter(TypePrimitive.inttype, idxVarName));
			}

			ExprVar r = new ExprVar(ufun, "r");

			// create the if-branches which constitutes the main body of the
			// func
			StmtIfThen stmtIf = (StmtIfThen) newIf(inputArrDim, idxVarList, r,
					mNumCases);

			StmtBlock funBody = new StmtBlock(ufun, stmtIf);

			paramList.addAll(mNewParamsToAdd);
			paramList.add(new Parameter(ufun.getReturnType(), "r",
					Parameter.OUT));

			mModelingFunc = new Function(ufun, FuncType.FUNC_BUILTIN_HELPER,
					name, TypePrimitive.voidtype, paramList, funBody);

			return mModelingFunc;
		}

		/**
		 * 
		 * Modifies mNewArgs to accumulate the necessary arguments to pass to
		 * this ufun.
		 * 
		 * Modifies mNewParamsToAdd to accumulate the necessary parameter to add
		 * to this ufun.
		 * 
		 * @param inputArrDim
		 * @param idxVarList
		 * @param retVar
		 * @param numRemaining
		 * @return
		 */
		protected Statement newIf(int inputArrDim, List<ExprVar> idxVarList,
				ExprVar retVar, int numRemaining) {
			FENode ctx = retVar;
			ExprVar vVar = new ExprVar(ctx, mTmpVarGen.nextVar("__v"));
			mNewArgs.add(vVar);
			mNewParamsToAdd.add(new Parameter(TypePrimitive.inttype, vVar
					.getName()));

			if (numRemaining > 0) {

				// construct the condition
				ExprVar gVar = new ExprVar(ctx, mTmpVarGen.nextVar("__g"));
				mNewArgs.add(gVar);
				mNewParamsToAdd.add(new Parameter(TypePrimitive.inttype, gVar
						.getName()));
				ExprVar idxVar = idxVarList.get(0);
				Expression condExpr = new ExprBinary(ctx, idxVar, "==", gVar);

				for (int i = 1; i < inputArrDim; i++) {
					// get the var for the i-th dimension
					idxVar = idxVarList.get(i);
					// new g-value
					gVar = new ExprVar(ctx, mTmpVarGen.nextVar("__g"));
					mNewArgs.add(gVar);
					mNewParamsToAdd.add(new Parameter(TypePrimitive.inttype,
							gVar.getName()));

					Expression newEqExpr = new ExprBinary(ctx, idxVar, "==",
							gVar);
					condExpr = new ExprBinary(retVar, condExpr, "&&", newEqExpr);
				}
				StmtAssign assign1 = new StmtAssign(ctx, retVar, vVar);
				StmtIfThen ifThen = new StmtIfThen(
						ctx,
						condExpr,
						assign1,
						newIf(inputArrDim, idxVarList, retVar, numRemaining - 1));
				return ifThen;
			} else {
				return new StmtAssign(ctx, retVar, vVar);
			}
		}
	}
}
