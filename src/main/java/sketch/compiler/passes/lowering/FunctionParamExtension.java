package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.*;
import sketch.compiler.ast.core.Function.FunctionCreator;
import sketch.compiler.ast.core.Package;
import sketch.compiler.ast.core.exprs.ExprArrayRange;
import sketch.compiler.ast.core.exprs.ExprBinary;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprField;
import sketch.compiler.ast.core.exprs.ExprFunCall;
import sketch.compiler.ast.core.exprs.ExprTernary;
import sketch.compiler.ast.core.exprs.ExprUnary;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.*;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.core.typs.TypeStructRef;
import sketch.compiler.passes.annotations.CompilerPassDeps;
import sketch.compiler.stencilSK.VarReplacer;
import sketch.util.exceptions.UnrecognizedVariableException;

import static sketch.util.DebugOut.assertFalse;


/**
 * Converts function that return something to functions that take
 * extra output parameters. Also fixes all function calls.
 *
 * @author liviu
 */
@CompilerPassDeps(runsBefore = {}, runsAfter = {})
public class FunctionParamExtension extends SymbolTableVisitor
{

	/**
	 *
	 *
	 *
	 * @author asolar
	 *
	 */
	private class ParameterCopyResolver extends SymbolTableVisitor
	{
		private HashMap<String,Parameter> unmodifiedParams;

		public ParameterCopyResolver()
		{
			super(null);
		}

		private Function addVarCopy(Function func, Parameter param, String newName)
		{
			StmtBlock body=(StmtBlock) func.getBody();
			StmtVarDecl decl=new StmtVarDecl(func,param.getType(),param.getName(),
					new ExprVar(func,newName));
			List<Statement> stmts = new ArrayList<Statement> (body.getStmts().size()+2);
			stmts.add(decl);
			stmts.addAll(body.getStmts());
            return func.creator().body(new StmtBlock(body, stmts)).create();
		}
		@Override
		public Object visitFunction(Function func)
		{
			unmodifiedParams=new HashMap<String,Parameter>();
			for(Iterator<Parameter> iter=func.getParams().iterator();iter.hasNext();) {
				Parameter param=iter.next();
				if(param.isParameterOutput()) continue;
				unmodifiedParams.put(param.getName(),param);
			}
			func=(Function) super.visitFunction(func);
			List<Parameter> parameters=new ArrayList<Parameter>(func.getParams());
			for(int i=0;i<parameters.size();i++) {
				Parameter param=parameters.get(i);
				if(param.isParameterOutput()) continue;
				if(!unmodifiedParams.containsValue(param)) {
					String newName=getNewInCpID(param.getName());
					Parameter newPar=new Parameter(param.getType(),newName,param.getPtype());
					parameters.set(i,newPar);
					func=addVarCopy(func,param,newName);
				}
			}
			return func.creator().params(parameters).create();
		}
		public Object visitStmtAssign(StmtAssign stmt)
		{
			Expression lhs=(Expression) stmt.getLHS().accept(this);
			while (lhs instanceof ExprArrayRange) lhs=((ExprArrayRange)lhs).getBase();
			assert lhs instanceof ExprVar  || lhs instanceof ExprField;
			if( lhs instanceof ExprVar ){
				String lhsName=((ExprVar)lhs).getName();
				unmodifiedParams.remove(lhsName);
			}
			return super.visitStmtAssign(stmt);
		}
		public Object visitStmtVarDecl(StmtVarDecl stmt)
		{
			int n=stmt.getNumVars();
			for(int i=0;i<n;i++) {
				unmodifiedParams.remove(stmt.getName(i));
			}
			return super.visitStmtVarDecl(stmt);
		}
	}

	private int inCpCounter;
    // private int outCounter;
	private Function currentFunction;
	private ParameterCopyResolver paramCopyRes;
	public boolean initOutputs=false;
    TempVarGen varGen;
	
	public FunctionParamExtension(boolean io, TempVarGen vargen) {
        this(null, vargen);
        initOutputs = io;
	}

    protected boolean hasFunCall(Expression exp) {
        class checker extends FEReplacer {
            public boolean found = false;

            public Object visitExprFunCall(ExprFunCall ear) {
                found = true;
                return ear;
            }
        }
        ;
        checker ck = new checker();
        exp.accept(ck);
        return ck.found;
    }

    protected Expression doLogicalExpr(ExprBinary eb) {
        Expression left = eb.getLeft(), right = eb.getRight();

        if (!(hasFunCall(left) || hasFunCall(right)))
            return (Expression) super.visitExprBinary(eb);

        boolean isAnd = eb.getOpString().equals("&&") || eb.getOpString().equals("&");

        String resName = varGen.nextVar("_pac_sc");

        addStatement(new StmtVarDecl(eb, TypePrimitive.bittype, resName, null));
        symtab.registerVar(resName, TypePrimitive.bittype, eb, SymbolTable.KIND_LOCAL);

        ExprVar res = new ExprVar(eb, resName);
        Expression cond = isAnd ? res : new ExprUnary("!", res);
        List<Statement> blist = new ArrayList<Statement>();
        blist.add(new StmtAssign(res, right));
        StmtBlock nb =
                new StmtBlock(new StmtAssign(res, left), new StmtIfThen(eb, cond,
                        new StmtBlock(blist), null));

        doStatement(nb);

        return res;
    }

    public Object visitExprTernary(ExprTernary exp) {
        Expression a = exp.getA().doExpr(this);
        Expression b = exp.getB();
        Expression c = exp.getC();

        if (!(hasFunCall(b) || hasFunCall(c))) {
            return super.visitExprTernary(exp);
        }

        String resName = varGen.nextVar("_pac_sc");
        Type t = getTypeReal(exp);
        if (t == TypePrimitive.nulltype) {
            t = TypePrimitive.inttype;
        }
        addStatement(new StmtVarDecl(exp, t, resName, null));
        symtab.registerVar(resName, t, exp, SymbolTable.KIND_LOCAL);
        ExprVar res = new ExprVar(exp, resName);

        StmtBlock thenBlock = null;
        {
            List<Statement> oldStatements = newStatements;
            newStatements = new ArrayList<Statement>();

            b = doExpression(b);
            newStatements.add(new StmtAssign(res, b));

            thenBlock = new StmtBlock(exp, newStatements);
            newStatements = oldStatements;
        }

        StmtBlock elseBlock = null;
        if (c != null) {
            List<Statement> oldStatements = newStatements;
            newStatements = new ArrayList<Statement>();

            c = doExpression(c);
            newStatements.add(new StmtAssign(res, c));

            elseBlock = new StmtBlock(exp, newStatements);
            newStatements = oldStatements;
        }

        addStatement(new StmtIfThen(exp, a, thenBlock, elseBlock));

        return res;
    }

    /** We have to conditionally protect function calls. */
    public Object visitExprBinary(ExprBinary eb) {
        String op = eb.getOpString();
        if (op.equals("&&") || op.equals("||"))
            return doLogicalExpr(eb);
        else
            return super.visitExprBinary(eb);
    }

    public FunctionParamExtension(TempVarGen varGen) {
        this(null, varGen);
	}

    public FunctionParamExtension(SymbolTable symtab, TempVarGen varGen) {
		super(symtab);
		paramCopyRes=new ParameterCopyResolver();
        this.varGen = varGen;
	}

	private String getOutParamName() {
		return "_out";
	}

	String rvname = null;
	private String getNewOutID(String nm) {
        varGen.nextVar(rvname);
	    if(rvname != null && nm.contains("_out")){
            return varGen.nextVar(rvname);// rvname + "_r" + (outCounter++);
	    }else{
            return varGen.nextVar(nm);// nm + "_r" + (outCounter++);
	    }
	}

    Expression callLHS = null;
	public Object visitStmtAssign(StmtAssign sa){
        if (sa.getLHS() instanceof ExprVar) {
            rvname = sa.getLHS().toString();
        }
        if (sa.getRHS() instanceof ExprFunCall) {
            Expression lhs = doExpression(sa.getLHS());
            callLHS = lhs;
            Expression rhs = doExpression(sa.getRHS());
            rvname = null;
            callLHS = null;
            if (rhs == null) {
                return null;
            } else {
                if (lhs == sa.getLHS() && rhs == sa.getRHS())
                    return sa;
                return new StmtAssign(sa, lhs, rhs, sa.getOp());
            }
        } else {
            callLHS = null;
            Object o = super.visitStmtAssign(sa);
            rvname = null;
            return o;
        }
	    
	}
	

	private String getNewInCpID(String oldName) {
		return oldName+"_"+(inCpCounter++);
	}

	private List getOutputParams(Function f) {
		List params=f.getParams();
		for(int i=0;i<params.size();i++)
			if(((Parameter)params.get(i)).getPtype() == Parameter.OUT)
				return params.subList(i,params.size());
		return Collections.EMPTY_LIST;
	}

    public Object visitProgram(Program prog) {
        SymbolTable oldSymTab = symtab;
        symtab = new SymbolTable(symtab);
        nres = new NameResolver();

        List<Package> oldStreams = new ArrayList<Package>();
        for (Package spec : prog.getPackages()) {
            List<Function> funs = new ArrayList<Function>();
            for (Function fun : (List<Function>) spec.getFuncs()) {
                Type retType = fun.getReturnType();
                List<Parameter> params = new ArrayList<Parameter>(fun.getParams());
                if (!retType.equals(TypePrimitive.voidtype)) {
                    params.add(new Parameter(retType, getOutParamName(), Parameter.OUT));
                }
                FunctionCreator fc = fun.creator().params(params);
                if (fun.isUninterp()) {
                    fc.returnType(TypePrimitive.voidtype);
                }
                funs.add(fc.create());
            }

            Package tpkg =
                    new Package(spec, spec.getName(), spec.getStructs(),
                            spec.getVars(), funs);
            nres.populate(tpkg);
            oldStreams.add(tpkg);

        }

        List<Package> newStreams = new ArrayList<Package>();
        for (Package ssOrig : oldStreams) {
            newStreams.add((Package) ssOrig.accept(this));
        }

        Program o = prog.creator().streams(newStreams).create();

        symtab = oldSymTab;
        return o;
    }



	Set<String> currentRefParams = new HashSet<String>();

    String retVar = null;

    public Object visitExprVar(ExprVar ev) {
        if (ev.getName().equals(retVar)) {
            return new ExprVar(ev, getOutParamName());
        }
        return ev;
    }

    public Object visitStmtVarDecl(StmtVarDecl stmt) {
        List<Expression> newInits = new ArrayList<Expression>();
        List<Type> newTypes = new ArrayList<Type>();
        List<String> newNames = new ArrayList<String>();
        boolean changed = false;
        for (int i = 0; i < stmt.getNumVars(); i++) {
            if (stmt.getName(i).equals(retVar)) {
                changed = true;
                if (stmt.getInit(i) != null) {
                    addStatement(new StmtAssign(new ExprVar(stmt, getOutParamName()),
                            doExpression(stmt.getInit(i))));
                }
                continue;
            }
            symtab.registerVar(stmt.getName(i), actualType(stmt.getType(i)), stmt,
                    SymbolTable.KIND_LOCAL);
            Expression oinit = stmt.getInit(i);
            Expression init = null;
            if (oinit != null)
                init = doExpression(oinit);
            Type ot = stmt.getType(i);
            Type t = (Type) ot.accept(this);
            if (ot != t || oinit != init) {
                changed = true;
            }
            newInits.add(init);
            newTypes.add(t);
            newNames.add(stmt.getName(i));
        }
        if (!changed) {
            return stmt;
        }
        if (newNames.size() == 0) {
            return null;
        }
        return new StmtVarDecl(stmt, newTypes, newNames, newInits);
    }

	public Object visitFunction(Function func) {
        retVar = null;
		if(func.isUninterp() ) return func;

		{
			currentRefParams.clear();
			List<Parameter> lp = func.getParams();
			for(Iterator<Parameter> it = lp.iterator(); it.hasNext(); ){
				Parameter p = it.next();
				if(p.isParameterOutput()){
					currentRefParams.add(p.getName());
				}
			}
		}
        final String bad = "BAD";
        FEReplacer retVarPop = new FEReplacer() {
            public Object visitStmtReturn(StmtReturn sr) {
                if (retVar == bad) {
                    return sr;
                }
                if (sr.getValue() instanceof ExprVar) {
                    String cname = ((ExprVar) sr.getValue()).getName();
                    if (retVar == null) {
                        retVar = cname;
                    } else {
                        if (!cname.equals(retVar)) {
                            retVar = bad;
                        }
                    }
                } else {
                    retVar = bad;
                }
                return sr;
            }
        };
        func.getBody().accept(retVarPop);
        if (retVar == bad) {
            retVar = null;
        }
        if (retVar != null) {
            FEReplacer checkProperDecl = new FEReplacer() {
                boolean hasDecl = false;

                public Object visitParameter(Parameter par) {
                    if (par.getName().equals(retVar)) {
                        retVar = null;
                    }
                    return par;
                }

                public Object visitStmtReturn(StmtReturn sr) {
                    if (!hasDecl) {
                        retVar = null;
                    }
                    return sr;
                }

                boolean isForDecl = false;

                public Object visitStmtFor(StmtFor stmt) {
                    Statement newInit = null;
                    boolean tmpisfd = isForDecl;
                    isForDecl = true;
                    if (stmt.getInit() != null) {
                        newInit = (Statement) stmt.getInit().accept(this);
                    }
                    isForDecl = tmpisfd;
                    Expression newCond = doExpression(stmt.getCond());
                    Statement newIncr = null;
                    if (stmt.getIncr() != null) {
                        newIncr = (Statement) stmt.getIncr().accept(this);
                    }
                    Statement tmp = stmt.getBody();
                    Statement newBody = StmtEmpty.EMPTY;
                    if (tmp != null) {
                        newBody = (Statement) tmp.accept(this);
                    }

                    if (newInit == stmt.getInit() && newCond == stmt.getCond() &&
                            newIncr == stmt.getIncr() && newBody == stmt.getBody())
                        return stmt;
                    return new StmtFor(stmt, newInit, newCond, newIncr, newBody);
                }

                public Object visitStmtVarDecl(StmtVarDecl svd) {
                    if (retVar == null) {
                        return svd;
                    }
                    for (String nm : svd.getNames()) {
                        if (nm.equals(retVar)) {
                            if (hasDecl || isForDecl) {
                                retVar = null;
                            }
                            hasDecl = true;
                        }
                    }
                    return svd;
                }

                public Object visitStmtBlock(StmtBlock sb) {
                    if (retVar == null) {
                        return sb;
                    }
                    boolean lhd = hasDecl;
                    Object o = super.visitStmtBlock(sb);
                    hasDecl = lhd;
                    return o;
                }
            };
            func.accept(checkProperDecl);
        }
		currentFunction=func;
        // outCounter=0;
			inCpCounter=0;
			func=(Function) super.visitFunction(func);
		currentFunction=null;

		//if(func.getReturnType()==TypePrimitive.voidtype) return func;
        paramCopyRes.setNres(nres);
		func=(Function)func.accept(paramCopyRes);




		List<Statement> stmts=new ArrayList<Statement>(((StmtBlock)func.getBody()).getStmts());
		
		if(initOutputs){

			List<Parameter> lp = func.getParams();
			for(Iterator<Parameter> it = lp.iterator(); it.hasNext(); ){
				Parameter p = it.next();
				if(p.getPtype() == Parameter.OUT){
					Parameter outParam = p;
					String outParamName  = outParam.getName();
					assert outParam.isParameterOutput();

                    Expression defaultValue = p.getType().defaultValue();
					assert defaultValue != null : "[FunctionParamExtension] default value null!";
					if (defaultValue == null) { assertFalse(); }
					
					stmts.add(0, new StmtAssign(new ExprVar(func, outParamName), defaultValue));
				}
			}
		}
        func =
                func.creator().returnType(TypePrimitive.voidtype).body(
                        new StmtBlock(func, stmts)).create();
		return func;
	}



	
	


	@Override
	public Object visitStmtLoop(StmtLoop stmt)
	{
		Statement body=stmt.getBody();
		if(body!=null && !(body instanceof StmtBlock))
			body=new StmtBlock(stmt,Collections.singletonList(body));
		if(body!=stmt.getBody())
			stmt=new StmtLoop(stmt,stmt.getIter(),body);
		return super.visitStmtLoop(stmt);
	}

	public Object visitExprFunCall(ExprFunCall exp) {
        Expression tempcallLHS = callLHS;
        callLHS = null;
		// first let the superclass process the parameters (which may be function calls)
		exp=(ExprFunCall) super.visitExprFunCall(exp);
        callLHS = tempcallLHS;
		// resolve the function being called
		Function fun;
		try{
            fun = nres.getFun(exp.getName(), exp);
		}catch(UnrecognizedVariableException e){
            // FIXME -- restore error noise
            throw e;
            // throw new UnrecognizedVariableException(exp + ": Function name " +
            // e.getMessage() + " not found" );
		}
		// now we create a temp (or several?) to store the result

		List<Expression> args=new ArrayList<Expression>(fun.getParams().size());
		List<Expression> existingArgs=exp.getParams();

		List<Parameter> params=fun.getParams();

		List<Expression> tempVars = new ArrayList<Expression>();
		List<Statement> refAssigns = new ArrayList<Statement>();

		Map<String, Expression> pmap = new HashMap<String, Expression>();
        VarReplacer vrep = new VarReplacer(pmap);
		
		int psz = 0;
		for(int i=0;i<params.size();i++){
			Parameter p = params.get(i);
			int ptype = p.getPtype();
			Expression oldArg=null; // (p.getType() instanceof TypeStruct || p.getType() instanceof TypeStructRef) ? new ExprNullPtr() : getDefaultValue(p.getType()) ;
			if(ptype == Parameter.REF || ptype == Parameter.IN){
				oldArg=(Expression) existingArgs.get(psz);
				++psz;
			}
            if (oldArg != null &&
                    !(oldArg instanceof ExprConstInt && p.isParameterOutput()))
            {
			 args.add(oldArg);
             pmap.put(p.getName(), oldArg);
            } else {
                Type tt = (Type) p.getType().accept(vrep);
                /*
                 * This looked like a nice optimization; the idea is that
                 * rather than creating a temporary for the output parameter,
                 * we reuse the same variable that was on the lhs of the assignment.
                 * The problem is that if there is aliasing between that variable
                 * and some of the parameters, there will be big problems.
                if (ptype == Parameter.OUT && callLHS != null) {
                    
                    Type t = getType(callLHS);                   
                    if (t instanceof TypeArray) {
                        if (t.equals(tt)) {
                            args.add(callLHS);
                            tempVars.add(null);
                            continue;
                        }
                    } else {
                        args.add(callLHS);
                        tempVars.add(null);
                        continue;
                    }
                }
                */
                {
                    String tempVar = getNewOutID(p.getName());
                    if (tt instanceof TypeStructRef) {
                        TypeStructRef tsf = (TypeStructRef) tt;
                        tt = tsf.addDefaultPkg(fun.getPkg(), nres);
                    }

                    Statement decl = (new StmtVarDecl(exp, tt, tempVar, oldArg));
                    symtab.registerVar(tempVar, tt, decl, SymbolTable.KIND_LOCAL);
                    ExprVar ev = new ExprVar(exp, tempVar);
                    args.add(ev);
                    pmap.put(p.getName(), ev);
                    addStatement(decl);
                    if (ptype == Parameter.OUT) {
                        tempVars.add(ev);
                    }
                    assert ptype != Parameter.REF;
                }
            }
		}

		ExprFunCall newcall=new ExprFunCall(exp,exp.getName(),args);
		addStatement( new StmtExpr(newcall));
		addStatements(refAssigns);

		// replace the original function call with an instance of the temp variable
		// (which stores the return value)
		if(tempVars.size() == 0){
			return null;
		}
		assert tempVars.size()==1; //TODO handle the case when it's >1
		return tempVars.get(0);
	}







	



	@Override
	public Object visitStmtReturn(StmtReturn stmt) {
		FENode cx=stmt;
		List<Statement> oldns = newStatements;
		
		if (stmt.getValue() == null) {
		    // NOTE -- ignore if already processed...
		    return stmt;
		}
		
        if (retVar != null) {
            return new StmtReturn(stmt, null);
        }

		this.newStatements = new ArrayList<Statement> ();		
		stmt=(StmtReturn) super.visitStmtReturn(stmt);
		
		List params=getOutputParams(currentFunction);
		for(int i=0;i<params.size();i++) {
			Parameter param=(Parameter) params.get(i);
			String name=param.getName();
			Statement assignRet=new StmtAssign(cx, new ExprVar(cx, name), stmt.getValue(), 0);
			newStatements.add(assignRet);
		}
		newStatements.add(new StmtReturn(stmt, null));
		 
		Statement ret=new StmtBlock(cx,newStatements);
		newStatements = oldns;
		
		return ret;
	}
	

	
	protected Expression getDefaultValue(Type t) {
        return t.defaultValue();
	}

}
