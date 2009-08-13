package sketch.compiler.dataflow.preprocessor;

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
import sketch.compiler.ast.core.exprs.ExprStar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.dataflow.DataflowWithFixpoint;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.nodesToSB.IntVtype;
import sketch.compiler.dataflow.recursionCtrl.RecursionControl;
import sketch.compiler.spin.IdentifyModifiedVars;

/**
 *
 * The sketch preprocessor mainly does constant propagation and inlining of functions and unrolling of loops.
 * After this step, all the holes are now regarded as static holes.
 * @author asolar
 *
 */
public class PreprocessSketch extends DataflowWithFixpoint {

	public Map<String, Function> newFuns;

	private boolean inlineStatics = false;



	public Object visitExprStar(ExprStar star) {
		Object obj = super.visitExprStar(star);
		if(!inlineStatics){
			ExprStar old = (ExprStar)exprRV;
			ExprStar n = new ExprStar(old);
			n.extendName(rcontrol.callStack());
			exprRV = n;
		}
		return obj;
	}


	@Override
	public String transName(String name){
		return state.transName(name);
	}


	public PreprocessSketch(TempVarGen vargen, int maxUnroll, RecursionControl rcontrol, boolean uncheckedArrays, boolean inlineStatics){
		super(new IntVtype(), vargen,true, maxUnroll, rcontrol );
		newFuns = new HashMap<String, Function>();
		this.uncheckedArrays = uncheckedArrays;
		this.inlineStatics = inlineStatics;
	}

	public PreprocessSketch(TempVarGen vargen, int maxUnroll, RecursionControl rcontrol, boolean uncheckedArrays){
		super(new IntVtype(), vargen,true, maxUnroll, rcontrol );
		newFuns = new HashMap<String, Function>();
		this.uncheckedArrays = uncheckedArrays;
	}

	public PreprocessSketch(TempVarGen vargen, int maxUnroll, RecursionControl rcontrol){
		super(new IntVtype(), vargen,true, maxUnroll, rcontrol );
		newFuns = new HashMap<String, Function>();
	}



	 protected void startFork(StmtFork loop){
		 IdentifyModifiedVars imv = new IdentifyModifiedVars();
	    	loop.getBody().accept(imv);
	    	state.pushParallelSection(imv.lhsVars);
	    }

	@Override
	 public Object visitStmtAssert (StmtAssert stmt) {
	        /* Evaluate given assertion expression. */
	        Expression assertCond = stmt.getCond();
	        abstractValue vcond  = (abstractValue) assertCond.accept (this);
	        Expression ncond = exprRV;
	        String msg = null;
	        msg = stmt.getMsg();
	        state.Assert(vcond, msg, stmt.isSuper());
	        return isReplacer ?(
	        		(vcond.hasIntVal() && vcond.getIntVal()==1) ? null : new StmtAssert(stmt, ncond, stmt.getMsg(), stmt.isSuper())
	        		)
	        		: stmt
	        		;
	    }


	public Object visitExprFunCall(ExprFunCall exp)
	{
    	String name = exp.getName();


    	// Local function?
		Function fun = ss.getFuncNamed(name);
		if(fun.getSpecification()!= null){
			String specName = fun.getSpecification();
			if( newFuns.containsKey(specName)){
				fun = newFuns.get(specName);
			}else{
				Function newFun = ss.getFuncNamed(specName);
				state.pushLevel();
				fun = (Function)this.visitFunction(newFun);
				state.popLevel();
			}
		}
    	if (fun != null) {
    		if( fun.isUninterp()  || ( fun.isStatic() && !inlineStatics   ) ){
    			if(fun.isStatic()){
    				funcsToAnalyze.add(fun);
    			}
    			return super.visitExprFunCall(exp);
    		}else{
    			if(inlineStatics){
    				assert fun.isStatic() : " If you are in inlinestatics mode, you should only have statics or uninterpreted functions.";
    			}
	    		if (rcontrol.testCall(exp)) {
	                /* Increment inline counter. */
	            	rcontrol.pushFunCall(exp, fun);

					List<Statement>  oldNewStatements = newStatements;
					newStatements = new ArrayList<Statement> ();
					Statement result = null;
					int level = state.getLevel();
			    	int ctlevel = state.getCTlevel();
					state.pushLevel();
					try{
			    		{
			    			Iterator<Expression> actualParams = exp.getParams().iterator();
			    			Iterator<Parameter> formalParams = fun.getParams().iterator();
			    			inParameterSetter(exp, formalParams, actualParams, false);
			    		}
			    		try{
			    			Statement body = (Statement) fun.getBody().accept(this);
			    			addStatement(body);
			    		}finally{
			    			Iterator<Expression> actualParams = exp.getParams().iterator();
			    			Iterator<Parameter> formalParams = fun.getParams().iterator();
			    			outParameterSetter(formalParams, actualParams, false);
			    		}
			    		result = new StmtBlock(exp, newStatements);
		    		}finally{
		    			state.popLevel();
		    			assert level == state.getLevel() : "Somewhere we lost a level!!";
		        		assert ctlevel == state.getCTlevel() : "Somewhere we lost a ctlevel!!";
		    			newStatements = oldNewStatements;
		    		}
		            addStatement(result);

		    		rcontrol.popFunCall(exp);
	    		}else{
	    			StmtAssert sas = new StmtAssert(exp, ExprConstInt.zero, false);
	    			addStatement(sas);
	    		}
	    		exprRV = null;
	    		return vtype.BOTTOM();
    		}
    	}
    	exprRV = null;
    	return vtype.BOTTOM();
    }


	
	
	public Object visitFunction(Function func){
		if( newFuns.containsKey(func.getName()) ){
			return newFuns.get(func.getName());
		}
		Function obj = (Function)super.visitFunction(func);
		
		newFuns.put(obj.getName(), obj);
		return obj;
	}
}
