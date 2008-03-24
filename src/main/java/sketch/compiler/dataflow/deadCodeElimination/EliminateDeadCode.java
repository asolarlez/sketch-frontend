package streamit.frontend.experimental.deadCodeElimination;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import streamit.frontend.experimental.abstractValue;
import streamit.frontend.nodes.ExprField;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFork;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.Type;
import streamit.frontend.tosbit.recursionCtrl.BaseRControl;

public class EliminateDeadCode extends BackwardDataflow {
	final private boolean keepAsserts;
	public EliminateDeadCode(boolean keepAsserts){
		super(LiveVariableVType.vtype, null, true, -1,(new BaseRControl(10)));
		this.keepAsserts = keepAsserts;
	}


	public Object visitStmtIfThen(StmtIfThen stmt)
	{

		Object obj = super.visitStmtIfThen(stmt);
		return obj;
	}
	public Object visitStmtBlock(StmtBlock sb){
		Object obj = super.visitStmtBlock(sb);
		if(obj instanceof StmtBlock){
			if(((StmtBlock)obj).getStmts().size() == 0){
				return null;
			}
		}
		return obj;
	}

	public Object visitStmtAssert (StmtAssert stmt) {
		if(!keepAsserts){ return null; }
		Expression exp = stmt.getCond();
		Integer ival = exp.getIValue();
		if(ival != null){
			if(ival == 1){
				return null;
			}
		}
		return super.visitStmtAssert(stmt);
	}

	protected List<Function> functionsToAnalyze(StreamSpec spec){
		return new LinkedList<Function>(spec.getFuncs());
	}

	public Object visitStmtAssign(StmtAssign stmt)
	{
		if(stmt.getLHS() instanceof ExprVar  ){
			ExprVar v = (ExprVar) stmt.getLHS();
			LiveVariableAV av = (LiveVariableAV)state.varValue(v.getName());
			if(av.getLiveness() == LiveVariableAV.DEAD){
				return null;
			}

		}
		return super.visitStmtAssign(stmt);
	}

	@Override
	protected Object assignmentToField(String lhsName, StmtAssign stmt, abstractValue rhs, Expression nlhs, Expression nrhs){
		abstractValue lhsv = state.varValue(lhsName);
		lhsv = vtype.plus(rhs, lhsv);
		state.setVarValue(lhsName, lhsv);
		return isReplacer?  new StmtAssign(stmt, nlhs, nrhs, stmt.getOp())  : stmt;
	}

	public Object visitExprField(ExprField exp) {
		abstractValue leftav = (abstractValue)exp.getLeft().accept(this);
		Expression left = exprRV;
		if(isReplacer) exprRV = new ExprField(exp, left, exp.getName());
		return leftav;
	}

	public Object visitFunction(Function func)
	{
		List<Parameter> params = func.getParams();
		List<Parameter> nparams = isReplacer ? new ArrayList<Parameter>() : null;
		for(Iterator<Parameter> it = params.iterator(); it.hasNext(); ){
			Parameter param = it.next();
			state.varDeclare(param.getName() , param.getType());
			if( isReplacer){
				Type ntype = (Type)param.getType().accept(this);
				nparams.add( new Parameter(ntype, transName(param.getName()), param.getPtype()));
			}
			if(param.isParameterOutput()){
				state.setVarValue(param.getName(), new joinAV(LiveVariableAV.LIVE));
			}
		}


		state.beginFunction(func.getName());

		Statement newBody = (Statement)func.getBody().accept(this);

		state.endFunction();

		return isReplacer? new Function(func, func.getCls(),
				func.getName(), func.getReturnType(),
				nparams, func.getSpecification(), newBody) : null;

		//state.pushVStack(new valueClass((String)null) );
	}



	public Object visitStmtVarDecl(StmtVarDecl stmt)
	{
		List<Type> types =  new ArrayList<Type>();
		List<String> names = new ArrayList<String>();
		List<Expression> inits = new ArrayList<Expression>();
		for (int i = 0; i < stmt.getNumVars(); i++)
		{
			String nm = stmt.getName(i);

			LiveVariableAV av = (LiveVariableAV)state.varValue(nm);
			if(av.hasBeenLive()){
				types.add(stmt.getType(i));
				names.add(nm);
				inits.add(stmt.getInit(i));
			}
		}
		if(types.size() > 0){
			return super.visitStmtVarDecl(new StmtVarDecl(stmt, types, names, inits));
		}else{
			return null;
		}
	}
	@Override
	public Object visitExprFunCall(ExprFunCall exp){
		Iterator actualParams = exp.getParams().iterator();
		while(actualParams.hasNext()){
			Expression actual = (Expression) actualParams.next();
			if(actual instanceof ExprVar){
				String name = ((ExprVar)actual).getName();
				state.setVarValue(name, new joinAV( LiveVariableAV.LIVE));
			}
		}
		Object obj = super.visitExprFunCall(exp);
		return obj;
	}

	public Object visitStmtFork (StmtFork loop) {
		StmtFork sf = (StmtFork) super.visitStmtFork (loop);
    	if (null == sf.getBody ()) {
    		return null;
    	} else if (sf.getBody () == loop.getBody ()
    			   && sf.getLoopVarDecl () == loop.getLoopVarDecl ()
    			   && sf.getIter () == loop.getIter ()) {
    		return loop;
    	} else {
    		return sf;
    	}
	}

}
