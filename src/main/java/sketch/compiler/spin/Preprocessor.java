package streamit.frontend.spin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.SJRoundRobin;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtJoin;
import streamit.frontend.nodes.StmtFork;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.StreamType;
import streamit.frontend.nodes.TempVarGen;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypePrimitive;


class FindModifiedVarsInPloops extends FEReplacer {
	public HashSet<String> lhsVars = new HashSet<String>();
	public HashSet<String> rhsVars = new HashSet<String>();
	public HashMap<String, Type> varTypes = new HashMap<String, Type>();
	public HashMap<String, Expression> varInits = new HashMap<String, Expression> ();
	public HashMap<StmtFork, HashSet<String> > varsPerLoop = new HashMap<StmtFork, HashSet<String> >();

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
            varInits.put (stmt.getName (i), init);
        }
        return stmt;
    }

	public Object visitStmtFork(StmtFork loop){
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


public class Preprocessor extends FEReplacer {
	public static final String PROC_PFX = "_fork_thread_";

	public HashSet<String> lhsVars;
	public HashSet<String> rhsVars;
	public HashMap<StmtFork, HashSet<String> > varsPerLoop;
	public HashMap<String, Type> varTypes;
	List<Function> generatedFuncs = new ArrayList<Function>();
	private TempVarGen varGen;

	public Preprocessor(TempVarGen varGen) {
		this.varGen = varGen;
	}

	/**
	 * Converts unary prefix increment/decrement expressions to postfix
	 * increments/decrements.  This is to work around a quirk of SPIN.
	 *
	 * WARNING: this change is not safe.  It can seriously break semantics.
	 */
	public Object visitExprUnary (ExprUnary eu) {
		if (ExprUnary.UNOP_PREDEC == eu.getOp ()
			|| ExprUnary.UNOP_PREINC == eu.getOp ()) {
			eu.report ("WARNING: converting prefix incr/decr to postfix");
			return new ExprUnary (eu,
					ExprUnary.UNOP_PREDEC == eu.getOp () ? ExprUnary.UNOP_POSTDEC
														 : ExprUnary.UNOP_POSTINC,
								  (Expression) eu.getExpr ().accept (this));
		} else {
			return super.visitExprUnary (eu);
		}
	}

    public Object visitFunction(Function func) {
    	List<Parameter> newParam = new ArrayList<Parameter>();
    	Iterator<Parameter> it = func.getParams().iterator();
    	boolean samePars = true;
    	while(it.hasNext()){
    		Parameter par = it.next();
    		Parameter newPar = (Parameter) par.accept(this) ;
    		boolean isLHS = lhsVars.contains (newPar.getName ());

    		if(par != newPar || isLHS)
    			samePars = false;
    		if (!isLHS)
    			newParam.add( newPar );
    	}

    	Type rtype = (Type)func.getReturnType().accept(this);

    	if( func.getBody() == null  ){
    		assert func.isUninterp() : "Only uninterpreted functions are allowed to have null bodies.";
    		return func;
    	}
        Statement newBody = (Statement)func.getBody().accept(this);
        if (newBody == func.getBody() && samePars && rtype == func.getReturnType()) return func;
        return new Function(func, func.getCls(),
                            func.getName(), rtype,
                            newParam, func.getSpecification(), newBody);
    }

    public Object visitStmtFork(StmtFork loop){
    	HashSet<String> vars = varsPerLoop.get(loop);
    	StmtVarDecl decl = (StmtVarDecl)loop.getLoopVarDecl().accept(this);

    	List<Parameter> pars = new ArrayList<Parameter>(vars.size());
    	List<Expression> actuals = new ArrayList<Expression>(vars.size());
    	FENode cx = loop;
    	for (String pname : vars) {
    		Parameter par = new Parameter(varTypes.get(pname), pname);
    		pars.add(par);
    		actuals.add(new ExprVar(cx, pname));
    	}
    	Statement body = (Statement) loop.getBody().accept(this);
    	String fname = varGen.nextVar (PROC_PFX);
    	Function fun = new Function(cx, Function.FUNC_ASYNC, fname ,TypePrimitive.voidtype, pars, body);

    	generatedFuncs.add(fun);

    	ExprFunCall fcall = new ExprFunCall(cx, fname, actuals);
    	Expression niter = (Expression) loop.getIter().accept(this);
    	assert decl.getNumVars() == 1;
    	assert decl.getType(0).equals(TypePrimitive.inttype);
    	String ivname = decl.getName(0);
    	StmtVarDecl ndecl = new StmtVarDecl(cx, decl.getType(0), ivname, ExprConstInt.zero);
    	ExprVar ivar = new ExprVar (cx, ivname);
    	Expression cmp = new ExprBinary(ivar, "<", niter);
    	Statement incr = new StmtAssign(ivar, new ExprBinary(ivar, "+", new ExprConstInt(1)));
    	List<Statement> forBody = new ArrayList<Statement> ();
    	forBody.add (new StmtExpr (fcall));
    	StmtFor spawnLoop = new StmtFor(cx, ndecl, cmp, incr, new StmtBlock (fcall, forBody));

    	// Create loop to join spawned threads
    	niter = (Expression) loop.getIter().accept(this);
    	ivname = decl.getName(0) + "_2_";	// cheap!
    	ndecl = new StmtVarDecl(cx, decl.getType(0), ivname, ExprConstInt.zero);
    	ivar = new ExprVar(cx, ivname);
    	cmp = new ExprBinary(ivar, "<", niter);
    	incr = new StmtAssign(ivar, new ExprBinary(ivar, "+", new ExprConstInt(1)));
    	List<Statement> joinBody =
    		Collections.singletonList ((Statement) new StmtJoin (cx, new SJRoundRobin (cx, new ExprVar (cx, ivname))));
    	StmtFor joinLoop = new StmtFor (cx, ndecl, cmp, incr, new StmtBlock (cx, joinBody));

    	addStatement (spawnLoop);

    	return joinLoop;
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
            } else {

            }
        }
        if(newInits.size() > 0)
        	return new StmtVarDecl(stmt, newTypes,
        			newNames, newInits);
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
        	FieldDecl newVar = new FieldDecl(spec, viploops.varTypes.get(vtd), vtd, viploops.varInits.get (vtd));
        	newVars.add(newVar);
        	changed = true;
        }

        for (Iterator iter = spec.getFuncs().iterator(); iter.hasNext(); )
        {
            Function oldFunc = (Function)iter.next();
            Function newFunc = (Function)oldFunc.accept(this);
            if (oldFunc != newFunc) changed = true;
            if(newFunc!=null) newFuncs.add(newFunc);
        }
        changed |= generatedFuncs.size () > 0;
        newFuncs.addAll(generatedFuncs);
        sspec = oldSS;
        if (!changed && newST == spec.getStreamType()) return spec;
        return new StreamSpec(spec, spec.getType(),
                              newST, spec.getName(), spec.getParams(),
                              newVars, newFuncs);

    }




}
