package sketch.compiler.dataflow.deadCodeElimination;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.Parameter;
import sketch.compiler.ast.core.TempVarGen;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.ExprNew;
import sketch.compiler.ast.core.exprs.ExprTypeCast;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssert;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtAtomicBlock;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtEmpty;
import sketch.compiler.ast.core.stmts.StmtExpr;
import sketch.compiler.ast.core.stmts.StmtMinimize;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;
import sketch.compiler.ast.promela.stmts.StmtFork;
import sketch.compiler.dataflow.MethodState.Level;
import sketch.compiler.dataflow.abstractValue;
import sketch.compiler.dataflow.recursionCtrl.BaseRControl;

public class EliminateDeadCode extends BackwardDataflow {
	final private boolean keepAsserts;

    public EliminateDeadCode(TempVarGen varGen, boolean keepAsserts) {
        super(LiveVariableVType.vtype, varGen, true, -1, (new BaseRControl(10)));
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

    public Object visitExprTypeCast(ExprTypeCast exp) {
        abstractValue childExp = (abstractValue) exp.getExpr().accept(this);
        Expression narg = exprRV;
        Type tt = exp.getType();
        while (tt instanceof TypeArray) {
            TypeArray ta = (TypeArray) tt;
            abstractValue tab = (abstractValue) ta.getLength().accept(this);
            childExp = vtype.plus(childExp, tab);
            tt = ta.getBase();
        }
        Type t = (Type) exp.getType().accept(this);
        if (isReplacer)
            exprRV = new ExprTypeCast(exp, t, narg);
        return vtype.cast(childExp, t);
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

	protected List<Function> functionsToAnalyze(Package spec){
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
            if (stmt.getRHS() instanceof ExprVar) {
                if (stmt.getLHS().equals(stmt.getRHS())) {
                    return null;
                }
            }

		}
		return super.visitStmtAssign(stmt);
	}

	@Override
    protected Object assignmentToField(String lhsName, StmtAssign stmt,
            abstractValue idx, abstractValue rhs, Expression nlhs, Expression nrhs)
    {
		abstractValue lhsv = state.varValue(lhsName);
		lhsv = vtype.plus(rhs, lhsv);
        lhsv = vtype.plus(idx, lhsv);
		state.setVarValue(lhsName, lhsv);
		return isReplacer?  new StmtAssign(stmt, nlhs, nrhs, stmt.getOp())  : stmt;
	}

    @Override
    public Object visitStmtMinimize(StmtMinimize stmtMinimize) {
        if (!keepAsserts) {
            return null;
        }
        Expression e = stmtMinimize.getMinimizeExpr();
        if (e instanceof ExprConstInt) {
            return null;
        }
        abstractValue vcond = (abstractValue) stmtMinimize.getMinimizeExpr().accept(this);
        vtype.Assert(vcond, null);
        return isReplacer ? new StmtMinimize(exprRV, stmtMinimize.userGenerated)
                : stmtMinimize;
    }

	public Object visitExprField(ExprField exp) {
		abstractValue leftav = (abstractValue)exp.getLeft().accept(this);
		Expression left = exprRV;
		if(isReplacer) exprRV = new ExprField(exp, left, exp.getName());
		return leftav;
	}

	public Object visitFunction(Function func)
	{
	    
	    
	    Level lvl = state.beginFunction(func.getName());
	    
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

		state.endFunction(lvl);
		if(newBody == null) newBody = new StmtEmpty(func);
		return isReplacer? func.creator().params(nparams).body(newBody).create() : null;

		//state.pushVStack(new valueClass((String)null) );
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
            return svdBis(new StmtVarDecl(stmt, types, names, inits));
		}else{
			return null;
		}
	}

    abstractValue typeAV = null;

    public Object visitTypeArray(TypeArray t) {
        Type nbase = (Type) t.getBase().accept(this);
        abstractValue avlen = (abstractValue) t.getLength().accept(this);
        if (typeAV == null) {
            typeAV = avlen;
        } else {
            typeAV = this.vtype.plus(typeAV, avlen);
        }
        Expression nlen = exprRV;
        if (nbase == t.getBase() && t.getLength() == nlen)
            return t;
        return isReplacer ? new TypeArray(nbase, nlen) : t;
    }

	protected Statement svdBis(StmtVarDecl stmt) {
        List<Type> types = isReplacer ? new ArrayList<Type>() : null;
        List<String> names = isReplacer ? new ArrayList<String>() : null;
        List<Expression> inits = isReplacer ? new ArrayList<Expression>() : null;
        for (int i = 0; i < stmt.getNumVars(); i++) {
            String nm = stmt.getName(i);
            typeAV = null;
            Type vt = (Type) stmt.getType(i).accept(this);
            // Variable declaration not needed.
            Expression ninit = null;
            abstractValue init = typeAV;
            if (stmt.getInit(i) != null) {
                abstractValue tt = (abstractValue) stmt.getInit(i).accept(this);
                if (init == null) {
                    init = tt;
                } else {
                    init = vtype.plus(init, tt);
                }
                ninit = exprRV;
            }
            if (init != null) {
                state.setVarValue(nm, init);
            }
            if (isReplacer) {
                types.add(vt);
                names.add(transName(nm));
                inits.add(ninit);
            }
        }
        return isReplacer ? new StmtVarDecl(stmt, types, names, inits) : stmt;
    }

	@Override
    public Object visitExprNew(ExprNew expNew) {
        Type nt = (Type) expNew.getTypeToConstruct().accept(this);
        boolean changed = false;
        List<ExprNamedParam> enl =
                new ArrayList<ExprNamedParam>(expNew.getParams().size());

        abstractValue av = vtype.CONST(0);
        for (ExprNamedParam epar : expNew.getParams()) {
            Expression old = epar.getExpr();
            av = vtype.plus(av, (abstractValue) epar.getExpr().accept(this));
            if (old != exprRV) {
                enl.add(new ExprNamedParam(epar, epar.getName(), exprRV));
                changed = true;
            } else {
                enl.add(epar);
            }
        }

        if (isReplacer) {
            if (nt != expNew.getTypeToConstruct() || changed) {
                if (!changed) {
                    enl = expNew.getParams();
                }
                exprRV = new ExprNew(expNew, nt, enl);
            } else {
                exprRV = expNew;
            }
        }

        return av;
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
