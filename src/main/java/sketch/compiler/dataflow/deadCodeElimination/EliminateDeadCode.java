package sketch.compiler.dataflow.deadCodeElimination;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.StreamSpec;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprTprint;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtEmpty;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.recursionCtrl.BaseRControl;
import sketch.util.datastructures.TprintTuple;

public class EliminateDeadCode extends BackwardDataflow {
	final private boolean keepAsserts;
	public EliminateDeadCode(boolean keepAsserts){
		super(LiveVariableVType.vtype, null, true, -1,(new BaseRControl(10)));
		this.keepAsserts = keepAsserts;
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

    public Object visitStmtAtomicBlock(StmtAtomicBlock stmt) {
        if (stmt.isCond()) {
            abstractValue val = (abstractValue) stmt.getCond().accept(this);
            enliven(val);
        }
        return super.visitStmtAtomicBlock(stmt);
    }

    public Object visitStmtExpr(StmtExpr stmt) {
        if (stmt.getExpression() != null) {
            Expression e = stmt.getExpression();
            if(e instanceof ExprConstInt || e instanceof ExprVar){
                return null;
            }            
            abstractValue val = (abstractValue) stmt.getExpression().accept(this);
            enliven(val);
        }
        return super.visitStmtExpr(stmt);
    }

    protected void enliven(abstractValue val) {
        if (val instanceof LVSet) {
            ((LVSet) val).enliven();
        }
        if (val instanceof LiveVariableAV) {
            LiveVariableAV lv = (LiveVariableAV) val;
            if (lv.mstate != null) {
                lv.mstate.setVarValueLight(lv.mstate.untransName(lv.name), new joinAV(
                        LiveVariableAV.LIVE));
            }
        }
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
	    
	    
	    state.beginFunction(func.getName());
	    
		List<Parameter> params = func.getParams();
		List<Parameter> nparams = isReplacer ? new ArrayList<Parameter>() : null;
		for(Iterator<Parameter> it = params.iterator(); it.hasNext(); ){
			Parameter param = it.next();
			state.outVarDeclare(param.getName() , param.getType());
			if( isReplacer){
				Type ntype = (Type)param.getType().accept(this);
				nparams.add( new Parameter(ntype, transName(param.getName()), param.getPtype()));
			}
			if(param.isParameterOutput()){
				state.setVarValue(param.getName(), new joinAV(LiveVariableAV.LIVE));
			}
		}


		

		Statement newBody = (Statement)func.getBody().accept(this);

		state.endFunction();
		if(newBody == null) newBody = new StmtEmpty(func);
		return isReplacer? func.creator().params(nparams).body(newBody).create() : null;

		//state.pushVStack(new valueClass((String)null) );
	}
	
	@Override
	public Object visitExprTprint(ExprTprint exprTprint) {
	    for (TprintTuple v : exprTprint.expressions) {
	        enliven((abstractValue) v.getSecond().accept(this));
	    }
	    return super.visitExprTprint(exprTprint);
	}

	public Object visitStmtEmpty (StmtEmpty stmt) {
		return null;
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
