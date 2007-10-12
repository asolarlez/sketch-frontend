package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprArrayRange;
import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtPloop;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.ExprArrayRange.Range;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;


/**
 *
 * Assumptions. For this pass, we are assuming that all variable
 * names in the program are unique. This is enforced by the preprocessing pass.
 *
 *
 * @author asolar
 *
 */


class IdentifyModifiedVars extends FEReplacer {
	boolean isLeft = false;
	public HashSet<String> lhsVars = new HashSet<String>();
	public HashSet<String> rhsVars = new HashSet<String>();
	public HashSet<String> locals = new HashSet<String>();

	public Object visitStmtAssign(StmtAssign stmt)
    {
	 	boolean tmpLeft = isLeft;
	 	isLeft = true;
        Expression newLHS = doExpression(stmt.getLHS());
        isLeft = tmpLeft;
        Expression newRHS = doExpression(stmt.getRHS());
        return stmt;
    }


	public Object visitExprArrayRange(ExprArrayRange exp) {
		doExpression(exp.getBase());
		final List l=exp.getMembers();
		for(int i=0;i<l.size();i++) {
			Object obj=l.get(i);
			if(obj instanceof Range) {
				Range range=(Range) obj;
				boolean tmpLeft = isLeft;
			 	isLeft = false;
				doExpression(range.start());
				doExpression(range.end());
				isLeft = tmpLeft;
			}
			else if(obj instanceof RangeLen) {
				RangeLen range=(RangeLen) obj;
				boolean tmpLeft = isLeft;
			 	isLeft = false;
				doExpression(range.start());
				isLeft = tmpLeft;
			}
		}
		return exp;
	}

	public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            Expression init = stmt.getInit(i);
            if (init != null)
                init = doExpression(init);
            Type t = (Type) stmt.getType(i).accept(this);
            locals.add(stmt.getName(i));
        }
        return stmt;
    }

	public Object visitExprVar(ExprVar exp) {
		if(locals.contains(exp.getName()))
			return exp;
		if(isLeft){
			lhsVars.add(exp.getName());
		}else{
			rhsVars.add(exp.getName());
		}
		 return exp;
	}
}

class FindModifiedVarsInPloops extends FEReplacer {
	public HashSet<String> lhsVars = new HashSet<String>();
	public HashSet<String> rhsVars = new HashSet<String>();
	public HashMap<String, Type> varTypes = new HashMap<String, Type>();
	public HashMap<StmtPloop, HashSet<String> > varsPerLoop = new HashMap<StmtPloop, HashSet<String> >();

	public Object visitParameter(Parameter par){
    	Type t = (Type) par.getType().accept(this);
    	varTypes.put(par.getName(), t);
    	return par;
    }

	public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            Expression init = stmt.getInit(i);
            if (init != null)
                init = doExpression(init);
            Type t = (Type) stmt.getType(i).accept(this);
            varTypes.put(stmt.getName(i), t);
        }
        return stmt;
    }

	public Object visitStmtPloop(StmtPloop loop){
    	IdentifyModifiedVars imv = new IdentifyModifiedVars();
    	loop.getLoopVarDecl().accept(this);
    	loop.getBody().accept(imv);
    	lhsVars.addAll(imv.lhsVars);
    	rhsVars.addAll(imv.rhsVars);
    	rhsVars.removeAll(lhsVars);
    	varsPerLoop.put(loop, rhsVars);
    	// loop.getBody().accept(this); There should not be nested ploops.
    	return loop;
    }
}


public class SpinPreprocessor extends FEReplacer {
	public HashSet<String> lhsVars;
	public HashSet<String> rhsVars;
	public HashMap<StmtPloop, HashSet<String> > varsPerLoop;
	public HashMap<String, Type> varTypes;
	List<Function> generatedFuncs = new ArrayList<Function>();
	private TempVarGen varGen;

	public SpinPreprocessor(TempVarGen varGen) {
		this.varGen = varGen;
	}

	public Object visitExprUnary (ExprUnary eu) {
		if (ExprUnary.UNOP_PREDEC == eu.getOp ()
			|| ExprUnary.UNOP_PREINC == eu.getOp ()) {
			eu.report ("WARNING: converting prefix incr/decr to postfix");
			return new ExprUnary (eu.getCx (),
					ExprUnary.UNOP_PREDEC == eu.getOp () ? ExprUnary.UNOP_POSTDEC
														 : ExprUnary.UNOP_POSTINC,
								  (Expression) eu.getExpr ().accept (this));
		} else {
			return super.visitExprUnary (eu);
		}
	}

    public Object visitStmtPloop(StmtPloop loop){
    	HashSet<String> vars = varsPerLoop.get(loop);
    	StmtVarDecl decl = (StmtVarDecl)loop.getLoopVarDecl().accept(this);

    	List<Parameter> pars = new ArrayList<Parameter>(vars.size());
    	List<Expression> actuals = new ArrayList<Expression>(vars.size());
    	FEContext cx = loop.getCx();
    	for(Iterator<String> it = vars.iterator(); it.hasNext(); ){
    		String pname = it.next();
    		Parameter par = new Parameter(varTypes.get(pname), pname);
    		pars.add(par);
    		actuals.add(new ExprVar(cx, pname));
    	}
    	Statement body = (Statement) loop.getBody().accept(this);
    	String fname = varGen.nextVar();
    	Function fun = new Function(cx, Function.FUNC_ASYNC, fname ,TypePrimitive.voidtype, pars, body);

    	generatedFuncs.add(fun);

    	ExprFunCall fcall = new ExprFunCall(cx, fname, actuals);
    	Expression niter = (Expression) loop.getIter().accept(this);
    	assert decl.getNumVars() == 1;
    	assert decl.getType(0).equals(TypePrimitive.inttype);
    	String ivname = decl.getName(0);
    	StmtVarDecl ndecl = new StmtVarDecl(cx, decl.getType(0), ivname, ExprConstInt.zero);
    	ExprVar ivar = new ExprVar(cx, ivname);
    	Expression cmp = new ExprBinary(cx, ExprBinary.BINOP_LT, ivar, niter);
    	Statement incr = new StmtAssign(cx, ivar, new ExprBinary(cx, ExprBinary.BINOP_ADD, ivar, new ExprConstInt(1)));
    	return new StmtFor(cx, ndecl, cmp, incr, new StmtExpr(fcall));
    }


    public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        List<Expression> newInits = new ArrayList<Expression>();
        List<Type> newTypes = new ArrayList<Type>();
        List<String> newNames = new ArrayList<String>();


        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            Expression init = stmt.getInit(i);
            if (init != null)
                init = doExpression(init);
            Type t = (Type) stmt.getType(i).accept(this);
            String name = stmt.getName(i);
            if(!lhsVars.contains(name)){
	            newInits.add(init);
	            newTypes.add(t);
	            newNames.add(name);
            }
        }
        if(newInits.size() > 0)
        	return new StmtVarDecl(stmt.getContext(), newTypes,
        			stmt.getNames(), newInits);
        else
        	return null;
    }



	public Object visitStreamSpec(StreamSpec spec){
        // Oof, there's a lot here.  At least half of it doesn't get
        // visited...
        StreamType newST = null;
        StreamSpec oldSS = sspec;
        sspec = spec;
        if (spec.getStreamType() != null)
            newST = (StreamType)spec.getStreamType().accept(this);
        List<FieldDecl> newVars = new ArrayList<FieldDecl>();
        List<Function> newFuncs = new ArrayList<Function>();
        boolean changed = false;
        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
        {
            FieldDecl oldVar = (FieldDecl)iter.next();
            FieldDecl newVar = (FieldDecl)oldVar.accept(this);
            if (oldVar != newVar) changed = true;
            if(newVar!=null) newVars.add(newVar);
        }

        FindModifiedVarsInPloops viploops = new FindModifiedVarsInPloops();
        spec.accept(viploops);
        lhsVars = viploops.lhsVars;
        rhsVars = viploops.rhsVars;
        varsPerLoop = viploops.varsPerLoop;
        varTypes = viploops.varTypes;

        for(Iterator<String> it = lhsVars.iterator(); it.hasNext(); ){
        	String vtd = it.next();
        	FieldDecl newVar = new FieldDecl(spec.getCx(), viploops.varTypes.get(vtd), vtd, null);
        	newVars.add(newVar);
        }


        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
            Function oldFunc = (Function)iter.next();
            Function newFunc = (Function)oldFunc.accept(this);
            if (oldFunc != newFunc) changed = true;
            if(newFunc!=null) newFuncs.add(newFunc);
        }
        newFuncs.addAll(generatedFuncs);
        sspec = oldSS;
        if (!changed && newST == spec.getStreamType()) return spec;
        return new StreamSpec(spec.getContext(), spec.getType(),
                              newST, spec.getName(), spec.getParams(),
                              newVars, newFuncs);

    }




}
