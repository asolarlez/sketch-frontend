package sketch.compiler.smt;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.smt.partialeval.NodeToSmtValue;
import sketch.compiler.smt.partialeval.TypedVtype;
import sketch.compiler.smt.partialeval.VarNode;

public class ProduceSMTStencilCode extends ProduceSMTCode {

	Map<String, Integer> mNumGridAccesses;
	Map<String, NodeToSmtValue[]> mVMap;
	Map<String, NodeToSmtValue[]> mGMap;
	
	
	public ProduceSMTStencilCode(TypedVtype vtype,
			Map<String, Integer> numGridAccesses, 
			TempVarGen varGen, 
			boolean useTheoryOfArray,
			boolean useBV,
			int maxUnroll,
			RecursionControl rcontrol, boolean tracing) {
		super(vtype, varGen, useTheoryOfArray,
		        useBV,
		        maxUnroll, 
		        rcontrol, tracing);
		mNumGridAccesses = numGridAccesses;
		
		mVMap = new HashMap<String, NodeToSmtValue[]>();
		mGMap = new HashMap<String, NodeToSmtValue[]>();
	}
	
	@Override
	public Object visitExprFunCall(ExprFunCall exp) {
		
		Function fun = super.ss.getFuncNamed(exp.getName());
		String funName = fun.getName();
		if (fun.isUninterp()) {

			// Floating point number modeling
			if (fun.getName().startsWith("FL")) {
				
				state.varDeclare(funName, fun.getReturnType());
				state.varValue(fun.getName());
				NodeToSmtValue floatVal = (NodeToSmtValue) state.varValue(funName);
				ExprVar lhs = (ExprVar) exp.getParams().get(0);
				state.setVarValue(lhs.getName(), floatVal);
				return null;
				
			} else {
				// ufun is for input array
				
				ArrayList<Expression> inArgs = new ArrayList<Expression>();
				LinkedList<ExprVar> outArgs = new LinkedList<ExprVar>();
				LinkedList<Expression> refArgs = new LinkedList<Expression>();
				separateInputOutputRef(fun.getParams(), exp.getParams(), inArgs, outArgs, refArgs);
				int inputArrDim = inArgs.size();
				
				// partial eval the arguments
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
				
					NodeToSmtValue returnVal = constructModelingFuncFormula(
							fun, funName, inputArrDim, inArgVals);
					
					outlets = new LinkedList<NodeToSmtValue>();
					outlets.add(returnVal);
					vtype.putHashedFuncCall(funccall, outlets);
					
					assert outArgs.size() == 1 : "More than 1 out param is found";
					state.setVarValue(outArgs.get(0).getName(), returnVal);
					
				} else {
					// if the function call was cached, use the cached outlet values
					setOutletToOutArgs(outArgs, outlets);
				}
			}

			return vtype.BOTTOM("FUN_RET_VAL", fun.getReturnType());
		}
		return super.visitExprFunCall(exp);
	}

	private NodeToSmtValue constructModelingFuncFormula(Function fun,
			String funName, int inputArrDim, NodeToSmtValue[] inArgVals) {
		int i;
		// construction the formula for the modeling function
		Integer numCases = mNumGridAccesses.get(funName);
		NodeToSmtValue[] vVars = mVMap.get(funName);
		NodeToSmtValue[] gVars = mGMap.get(funName);
		
		if (vVars == null) {
			gVars = new NodeToSmtValue[numCases*inputArrDim];
			vVars = new NodeToSmtValue[numCases];
			
			// init the gVars, the number of gVars is numCases * inputArrDim
			for (i = 0; i < numCases*inputArrDim; i++) {
				String gName = funName + "_g" + i;	
				Type paramType = fun.getParams().get(0).getType();
				gVars[i] = NodeToSmtValue.newParam(gName, paramType, 
						vtype.getNumBitsForType(paramType));
				vtype.declareInput((VarNode) gVars[i]);
			}
			
			// init the vVars
			for (i = 0; i < numCases; i++) {
				String vName = funName + "_v" + i;
				
				vVars[i] = NodeToSmtValue.newParam(vName, fun.getReturnType(), 
						vtype.getNumBitsForType(fun.getReturnType()));
				vtype.declareInput((VarNode) vVars[i]);
			}
			
			mVMap.put(funName, vVars);
			mGMap.put(funName, gVars);
		}
		
		NodeToSmtValue returnVal = recursive(inArgVals, vVars, gVars);
		return returnVal;
	}
	

	protected NodeToSmtValue recursive(NodeToSmtValue[] inArgVals, 
			NodeToSmtValue[] vVars, NodeToSmtValue[] gVars) {
		
		int numCases = vVars.length;
		int gIdx = gVars.length - 1;
		int vIdx = numCases - 1;
		NodeToSmtValue opnd = gVars[vIdx];
		for (vIdx = numCases-2 ; vIdx >= 0; vIdx--) {

			int i = 0;
			NodeToSmtValue cond = vtype.eq(inArgVals[i], gVars[gIdx]);
			gIdx--;
			for (i = 1; i < inArgVals.length; i++) {
				cond = vtype.and(cond, vtype.eq(inArgVals[i], gVars[gIdx]));
				gIdx--;
			}
			
			opnd = vtype.condjoin(cond, vVars[vIdx], opnd);
		} 
		assert vIdx == -1;
		return opnd;
	}

}
