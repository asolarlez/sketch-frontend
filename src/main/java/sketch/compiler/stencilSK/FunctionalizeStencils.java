package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

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
import streamit.frontend.nodes.Program;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
import streamit.frontend.passes.FunctionParamExtension;
import streamit.frontend.stencilSK.ParamTree.treeNode.PathIterator;
import streamit.frontend.tosbit.SelectFunctionsToAnalyze;






class scopeHist{
	/**
	 * funs tracks all the arrays that defined at this scope.
	 * Note that if there are nested loops, arrays modified at deeper
	 * levels of nesting will not be registered in this funs, but in the one corresponding
	 * to the nested scopes.
	 */	
	List<ArrFunction> funs;
	scopeHist(){
		funs = new ArrayList<ArrFunction>();
	}
	
}



/**
 * @author asolar
 * This class tracks information about each loop encountered.
 */
class loopHist{
	/**
	 * This is the name of the main induction variable in the loop. 
	 */
	String var;
	/**
	 * low and high describe the upper and lower bounds of the loop in
	 * the form of expressions. So for a loop that goes from a to b,
	 * low would equal (i>=0) and high would equal (i<=b);
	 * highPlusOne would equal b+1.
	 */
	Expression low;
	Expression high;
	Expression highPlusOne;
	/**
	 * This variable counts statements in the loop body.
	 */
	int stage;	
	
	
	void computeHighPlusOne(){		
		if(false){
			assert high instanceof ExprBinary;
			ExprBinary bhigh = (ExprBinary) high;
			assert bhigh.getLeft() instanceof ExprVar || bhigh.getRight() instanceof ExprVar;
			if( bhigh.getLeft() instanceof ExprVar && ((ExprVar)bhigh.getLeft()).getName().equals(var) ){
				if( bhigh.getOp() == ExprBinary.BINOP_LE ){
				highPlusOne = new ExprBinary(null, ExprBinary.BINOP_ADD, bhigh.getRight(), new ExprConstInt(1));
				}else{
					assert bhigh.getOp() == ExprBinary.BINOP_LT;
					highPlusOne = bhigh.getRight();
				}
			}else{
				if( bhigh.getOp() == ExprBinary.BINOP_LE ){
					highPlusOne = new ExprBinary(null, ExprBinary.BINOP_ADD, bhigh.getLeft(), new ExprConstInt(1));
					}else{
						assert bhigh.getOp() == ExprBinary.BINOP_LT;
						highPlusOne = bhigh.getLeft();
					}
			}
		}else{
			highPlusOne = ExprConstInt.zero;
		}
	}
	loopHist(String var, Expression low, Expression high){
		this.var = var;
		this.low = low;
		this.high = high;
		stage = 1;		
		if(high != null)
			computeHighPlusOne();
	}
	
	public StmtVarDecl newVD(){
	    	return new StmtVarDecl(null, TypePrimitive.inttype,  var, highPlusOne);	    	
	}
}



class condition{
	Expression expr;
	ParamTree.treeNode tn;
	condition(Expression expr, ParamTree.treeNode tn ){
		this.expr = expr;
		this.tn = tn;
	}
}


/**
 * This class keeps track of all the functions 
 * that represent a given array.
 * 
 * @author asolar
 *
 */
class arrInfo{
	/**
	 * This variable registers the position in the stack of loop histories where
	 * the array was created.
	 */
	int stack_beg;
	ArrFunction fun;
	Stack<ArrFunction> sfun = new Stack<ArrFunction>();
}

/**
 * This function directs the process of reducing the stencils down to finite functions.
 * All the actual work is done by {@link ProcessStencil}. 
 * 
 * To use it, first visit the functions that you want to process. 
 * If you visit the functions one at a time, instead of the whole program, {@link superParams}
 * won't be populated.
 * 
 * Visiting the program produces {@link ArrFunction} objects for each variable, 
 * but then you have to call processFuns to actually make function objects out of these.
 * 
 * 
 * @author asolar
 *
 */
public class FunctionalizeStencils extends FEReplacer {	
	/**
	 * This map stores the {@link ArrFunction} for all the variables in all the
	 * functions we've analyzed. The indices have all been quantified with the
	 * name of their respective function already.
	 */
	private Map<String, ArrFunction> funmap;
	/**
	 * These correspond to the global parameters which need to be passed
	 * as inputs to all functions. Part of the job of {@link FunctionalizeStencils}
	 * is to populate this array before visiting the functions.
	 */
	private Map<String, Type> superParams;
	/**
	 * This list contains all the functions that we have reduced.
	 */
	private List<Function> userFuns;
	private StreamSpec ss;
	
	/**
	 * Maps a function name to a map of input grid names to abstract grids. 
	 */	
	private Map<String, Map<String, Function> > globalInVars;
	
	public FunctionalizeStencils() {
		super();
		superParams = new TreeMap<String, Type>();
		funmap = new HashMap<String, ArrFunction>();
		globalInVars = new HashMap<String, Map<String, Function> >();
		userFuns = new ArrayList<Function>();
	}
	
	
	private static final List<Parameter> makeParams(List<StmtVarDecl> ls) {
		return makeParams(ls.iterator());
	}
	private static final List<Parameter> makeParams(Iterator<StmtVarDecl> it) {
		List<Parameter> ret=new ArrayList<Parameter>();
		while(it.hasNext()) {
			StmtVarDecl var=it.next();
			for(int i=0;i<var.getNumVars();i++)
				ret.add(new Parameter(var.getType(i),var.getName(i)));
		}
		return ret;
	}
	private static final List<ExprVar> makeRefs(List<StmtVarDecl> ls) {
		return makeRefs(ls.iterator());
	}
	private static final List<ExprVar> makeRefs(Iterator<StmtVarDecl> it) {
		List<ExprVar> ret=new ArrayList<ExprVar>();
		while(it.hasNext()) {
			StmtVarDecl var=it.next();
			for(int i=0;i<var.getNumVars();i++)
				ret.add(new ExprVar(null,var.getName(i)));
		}
		return ret;
	}
	private static final List<Expression> extractInits(Iterator<StmtVarDecl> it) {
		List<Expression> ret=new ArrayList<Expression>();
		while(it.hasNext()) {
			StmtVarDecl var=it.next();
			for(int i=0;i<var.getNumVars();i++)
				ret.add(var.getInit(i));
		}
		return ret;
	}
	
	public Program processFuns(Program prog){
		StreamSpec strs=(StreamSpec)prog.getStreams().get(0);		
		strs.getVars().clear();
		List<Function> functions=strs.getFuncs();		
		for(Iterator<Function> it = functions.iterator(); it.hasNext(); ){
			Function fun = it.next();
			if(!fun.isUninterp()){
				it.remove();
			}
		}
		
		
		//add the functions generated from ArrFunction objects to the program 
		for(Iterator<ArrFunction> it = funmap.values().iterator(); it.hasNext(); ){
			ArrFunction af = it.next();			
			//System.out.println(af.toString());
			af.processMax();
			functions.add(af.toAST());
		}
		//collect all unique AbstractArray objects 
		Set<Function> arrys=new HashSet<Function>();
		for(Iterator<Entry<String, Map<String, Function>>> it = globalInVars.entrySet().iterator(); it.hasNext(); ){
			Map<String, Function> af = it.next().getValue();
			arrys.addAll(af.values());
		}
		//add the functions generated from AbstractArray objects to the program 
		for(Iterator<Function> it = arrys.iterator(); it.hasNext(); ){
			Function aa = it.next();
			functions.add(aa);
		}
		
		// prog.accept(new SimpleCodePrinter());
		
		//convert all functions to procedures, translating calls and returns appropriately 
		prog = (Program) prog.accept(new FunctionParamExtension(true));
		strs=(StreamSpec)prog.getStreams().get(0);
		functions=strs.getFuncs();
		
		//generate the "driver" functions for spec and sketch
		for(Iterator<Function> it=userFuns.iterator();it.hasNext();) {
			Function f=it.next();
			Parameter outp=(Parameter) f.getParams().get(f.getParams().size()-1);
			Type outpType = outp.getType();
			
			while(outpType instanceof TypeArray){
				outpType = ((TypeArray) outpType).getBase();
			}
			
			assert outp.isParameterOutput();
			String outfname=outp.getName()+"_"+f.getName();
			ArrFunction outf=funmap.get(outfname);
			assert outf!=null;
			
			List<Parameter> driverParams=new ArrayList<Parameter>();
			driverParams.addAll(makeParams(outf.idxParams));
			driverParams.addAll(makeParams(outf.othParams));
			driverParams.addAll(makeParams(outf.inputParams));
			driverParams.add(new Parameter(outpType,outp.getName(),Parameter.OUT));
			
			List<Expression> callArgs=new ArrayList<Expression>();
			callArgs.addAll(makeRefs(outf.idxParams));
			callArgs.addAll(extractInits(outf.iterParams.iterator()));
			callArgs.addAll(makeRefs(outf.othParams));
			callArgs.addAll(makeRefs(outf.inputParams));
			callArgs.add(new ExprVar(null,outp.getName()));
			assert(callArgs.size()==strs.getFuncNamed(outfname).getParams().size());
			
			Statement fcall=new StmtExpr(new ExprFunCall(null,outfname,callArgs));
			
			Function fun=Function.newHelper(null,f.getName(),TypePrimitive.voidtype,
				driverParams, f.getSpecification(), 
				new StmtBlock(null, Collections.singletonList(fcall)));
			functions.add(fun);
		}
		return prog;
	}
	
	
	public void printFuns(){
		for(Iterator<Entry<String, ArrFunction>> it = funmap.entrySet().iterator(); it.hasNext(); ){
			ArrFunction af = it.next().getValue();
			System.out.println(af);
		}
		
		for(Iterator<Entry<String, Map<String, Function>>> it = globalInVars.entrySet().iterator(); it.hasNext(); ){
			Map<String, Function> af = it.next().getValue();
			
			for(Iterator<Entry<String, Function>> aait = af.entrySet().iterator(); aait.hasNext(); ){
				Function aa = aait.next().getValue();
				System.out.println(aa);
			}
		}
	}
	
	
	
	
	 public Object visitStreamSpec(StreamSpec spec)
	    {
	        
	        for (Iterator iter = spec.getVars().iterator(); iter.hasNext(); )
	        {
	            FieldDecl oldVar = (FieldDecl)iter.next();
	            oldVar.accept(this);
	        }
	        
	        StreamSpec oldSS = ss;
	        ss = spec;
	        
	        SelectFunctionsToAnalyze funSelector = new SelectFunctionsToAnalyze();
		    List<Function> funcs = funSelector.selectFunctions(spec);
	        
	        for (Iterator<Function> iter = funcs.iterator(); iter.hasNext(); ){
	        	Function f = iter.next();
	        	f.accept(this);
	        }
	        
	        ss = oldSS;
	        return spec;	        
	    }


		/**
		 *
		 * @param param
		 * @return If param is a grid, it returns the dimension of the grid.
		 * Otherwise, it returns -1
		 */
		
		public static int checkIfGrid(Parameter param){
			Type type = param.getType();
			//TODO I am assuming that all arrays are unbounded. 
			//This could be refined to make it identify bounded grids,
			//since bounded grids don't have to be abstracted and 
			//can be treated as regular inputs.
			if( type instanceof TypeArray ){
				int tt = 0;
				while( type instanceof TypeArray ){
					++tt;
					TypeArray ta = (TypeArray)type;
					type = ta.getBase();				
				}
				return tt;			
			}else{
				return -1;
			}
		}
	
	 
	 protected Map<String, Function> getInGrids(Function func){
		 
		if( globalInVars.containsKey(func.getName()) ){
			return globalInVars.get(func.getName());
		}else{
			//It may be that the abstract functions haven't been defined for this function,
			//but if they have been defined for it's spec, then we reuse those. 
			//Remember spec and sketch must share the same abstract grids.
			String spec = func.getSpecification();
			if( spec != null ){
				if( globalInVars.containsKey(spec) ){
					Map<String, Function> inVars = globalInVars.get(spec);
					this.globalInVars.put(func.getName(), inVars);
					return globalInVars.get(spec);
				}
			}
			
			Map<String, Function> inVars = new HashMap<String, Function>();
			
			boolean onlyOneOutput = true;
			List params = func.getParams();
			for(Iterator it = params.iterator(); it.hasNext();  ){
				Parameter param = (Parameter) it.next();
				if( !param.isParameterOutput() ){    		
					int dim = checkIfGrid( param );
					if( dim > 0){   				
						Type ptype = param.getType();
						while( ptype instanceof TypeArray){
							ptype = ((TypeArray)ptype).getBase();
						}												
						List<Parameter> fparams = new ArrayList<Parameter>();
						for(int i=0; i<dim; ++i){
							fparams.add(new Parameter(TypePrimitive.inttype, "idx_" + i));
						}						
						
						Function ufun = Function.newUninterp(param.getName(), ptype, fparams);
						
						inVars.put(param.getName(), ufun);
						/////////////////
					}
				}else{
					assert onlyOneOutput : "The function can have only one output!! ";
					onlyOneOutput = false;					
				}
			}
			
			this.globalInVars.put(func.getName(), inVars);
			if( spec != null){
				this.globalInVars.put(spec, inVars);
			}
			return inVars;
		}
	}
	
	 public Object visitFunction(Function func){
		Map<String, Function> funInGrids = getInGrids(func);
		 	
		 
		ProcessStencil ps = new ProcessStencil(func.getName(), superParams, funInGrids);
		
		func.accept(ps);
		funmap.putAll(ps.getAllFunctions());
		userFuns.add(func);

		return func;
	 }


	public Object visitFieldDecl(FieldDecl field)
    {
        List<Expression> newInits = new ArrayList<Expression>();
        for (int i = 0; i < field.getNumFields(); i++)
        {
            Expression init = field.getInit(i);
            if (init != null){
                init = (Expression)init.accept(this);
            }else{
            	superParams.put(field.getName(i), field.getType(i));
            }
            newInits.add(init);
        }
        return new FieldDecl(field.getContext(), field.getTypes(),
                             field.getNames(), newInits);
    }
}

class ProcessStencil extends FEReplacer {
	private FEContext ccontext = null; 
	/*
	 * Includes the global variables and the scalar parameters of the function.
	 */
	private final Map<String, Type> superParams;

	private Stack<Expression> conds;
	private ParamTree.treeNode currentTN;
	private Stack<scopeHist> scopeStack;
	private Map<String, arrInfo> smap;
	private final Map<String, Function> inVars;
	private Set<String> outVar;
	private final List<StmtVarDecl> inArrParams;
	private ParamTree ptree;
	private final String suffix; 
		
	
	public Map<String, ArrFunction> getAllFunctions(){
		Map<String, ArrFunction> nmap = new TreeMap<String, ArrFunction>();
		for(Iterator<Entry<String, arrInfo>> it = smap.entrySet().iterator(); it.hasNext(); ){
			Entry<String, arrInfo> tmp = it.next();
			assert tmp.getValue().sfun.size() == 1;
			ArrFunction fun = tmp.getValue().sfun.peek();
			nmap.put(fun.getFullName(), fun);
		}
		return nmap;
	}
	
	class SetupParamTree extends FEReplacer{
		ParamTree ptree;
		public ParamTree producePtree(Function func)
	    {
			ptree = new ParamTree();
			visitFunction(func);
	        return ptree;
	    }
		 
		public Object visitStmtFor(StmtFor stmt)
		{
			FEContext context = stmt.getContext();
			assert stmt.getInit() instanceof StmtVarDecl;
			StmtVarDecl init = (StmtVarDecl) stmt.getInit();
			assert init.getNumVars() == 1;
			String indVar = init.getName(0);
			Expression exprStart = init.getInit(0);
			Expression exprStartPred = new ExprBinary(context, ExprBinary.BINOP_GE, new ExprVar(context, indVar), exprStart);
			Expression exprEndPred = stmt.getCond();
			Statement body = stmt.getBody();
			
			loopHist lh = new loopHist(indVar, exprStartPred, exprEndPred);
			ptree.beginLevel(lh, stmt);
			body.accept(this);
			ptree.endLevel();	
			
			return stmt;
		}
	}
	
	 
	
	
	void closeOutOfScopeVars(scopeHist sc2){
		for(Iterator<ArrFunction> it = sc2.funs.iterator(); it.hasNext();  ){
			 ArrFunction t = it.next();
			 t.close();
			 smap.get(t.arrName).fun=null;
		 }
	}
	
	
	 public void processForLoop(StmtFor floop, String indVar, Expression exprStartPred, Expression exprEndPred, Statement body, boolean direction){
		 currentTN = ptree.getTNode(floop);
		 //loopHist lh = currentTN.lh;
		 scopeHist sc = new scopeHist();
		 scopeStack.push( sc );
		 conds.push(exprStartPred);
		 conds.push(exprEndPred);
		 
		 body.accept(this);
		 		 
		 Expression e1 = conds.pop();
		 assert e1 == exprEndPred;
		 Expression e2 = conds.pop();
		 assert e2 == exprStartPred;		 
		 
		 scopeHist sc2 = scopeStack.pop();
		 assert sc2 == sc;
		 currentTN = currentTN.getFather();
		 currentTN.incrStage();
		 closeOutOfScopeVars(sc2);
	 }
	 
	 
	 public Object visitStmtBlock(StmtBlock stmt)
	    {
		 scopeHist sc = new scopeHist();
		 scopeStack.push( sc );
		 super.visitStmtBlock(stmt);
		 scopeHist sc2 = scopeStack.pop();
		 assert sc2 == sc;
		 closeOutOfScopeVars(sc2);
		 return stmt;
	    }
	 
	 
	 
	    public Object visitStmtIfThen(StmtIfThen stmt)
	    {
	    	ccontext = stmt.getContext();
	        Expression cond = stmt.getCond();
	        conds.push(cond);
	        stmt.getCons().accept(this);
	        cond = conds.pop();
	        if( stmt.getAlt() != null){
	        	cond = new ExprUnary(stmt.getContext(), ExprUnary.UNOP_NOT, cond);
	        	conds.push(cond);
	        	stmt.getAlt().accept(this);
	        	conds.pop();
	        }	 
	        currentTN.incrStage();	        
	        return stmt;	        
	    }
	    
	    /*
	    void addIterParams( loopHist lh ){
	    	for(Iterator<scopeHist> it = scopeStack.iterator(); it.hasNext(); ){
	    		scopeHist sc = it.next();
	    		for(Iterator<arrFunction> ait = sc.funs.iterator(); ait.hasNext(); ){
	    			arrFunction fun = ait.next();
	    			fun.iterParams.add( newVD(lh.var, lh.highPlusOne) );
	    			fun.posIterParams.add( newVD("st" + fun.posIterParams.size(), new ExprConstInt(lh.stage)) );
	    		}
	    	}	    	
	    }*/
	    
	    void populateArrInfo(arrInfo ainf, String var, Type type, int dim){	    	
	    	assert ainf.sfun.size()==0; //added by LT after removing this from the constructor to ArrFunction
   	 		ainf.fun = new ArrFunction(var, type, suffix, ptree, currentTN, conds.size());
   	 		for(int i=0; i<dim; ++i) ainf.fun.idxParams.add( newVD(ArrFunction.IPARAM+i, null) );
   	 		
   	 		for(Iterator<Entry<String, Type>> pIt = superParams.entrySet().iterator(); pIt.hasNext(); ){
   	 			Entry<String, Type> par = pIt.next();
   	 			ainf.fun.othParams.add(new StmtVarDecl(ccontext, par.getValue(), par.getKey(), null));
   	 		}
   	 		
   	 		ainf.fun.inputParams = this.inArrParams;
   	 		
   	 		int sz = ainf.sfun.size();
   	 		ainf.fun.addRetStmt( nullMaxIf(sz>0 ? ainf.sfun.peek():null));
   	 		ainf.sfun.add(ainf.fun);
   	 		scopeStack.peek().funs.add(ainf.fun);   	 		
	    }
	    
	    public Object visitStmtVarDecl(StmtVarDecl stmt)
	    {	        
	    	ccontext = stmt.getContext();
	        for (int i = 0; i < stmt.getNumVars(); i++)
	        {
	        	if( stmt.getType(i) instanceof TypeArray ){
	        		Type ta = stmt.getType(i);
	        		String var = stmt.getName(i);
	        		declareNewArray(ta, var);	        		
	        	}else{
	        		String var = stmt.getName(i);
	        		arrInfo ainf = null;
    	    		assert !smap.containsKey(var);
    	    		ainf = new arrInfo();
    	    		smap.put(var, ainf);
    	    		populateArrInfo(ainf, var, stmt.getType(i), 0);
    	    		if( stmt.getInit(i) != null ){
    	    			List<Expression> indices = new ArrayList<Expression>(0);
    	    			processArrAssign(var, indices, stmt.getInit(i));
    	    		}
	        	}
	        }
	        return stmt;
	    }
	    
	    
	    public StmtVarDecl newVD(String name, Expression init){
	    	return new StmtVarDecl(ccontext, TypePrimitive.inttype,  name, init);	    	
	    }
	    public Statement nullMaxIf(ArrFunction prevFun){	    	
	    	// cond = IND_VAR = 0;
	    	Expression cond = new ExprVar(ccontext, ArrFunction.IND_VAR); 
	    	Statement rval;
	    	if(prevFun != null){
	    		List<Expression>  params = new ArrayList<Expression>();
	    		for(Iterator<StmtVarDecl> it = prevFun.idxParams.iterator(); it.hasNext(); ){
	    			StmtVarDecl par = it.next();
	    			params.add(new ExprVar(ccontext, par.getName(0)));
	    		}
	    		for(Iterator<StmtVarDecl> it = ptree.iterator(); it.hasNext(); ){
	    			StmtVarDecl par = it.next();
	    			params.add( par.getInit(0));
	    		}
	    		for(Iterator<StmtVarDecl> it = prevFun.othParams.iterator(); it.hasNext(); ){
	    			StmtVarDecl par = it.next();
	    			params.add( new ExprVar(ccontext, par.getName(0)));
	    		}	    		
	    		rval = new StmtReturn(ccontext, new ExprFunCall(null, prevFun.getFullName() , params) );
	    	}else{
	    		rval = new StmtReturn(ccontext, ExprConstInt.zero );
	    	}	    	
	    	return new StmtIfThen(ccontext, cond, rval, null); 
	    	//   "if( max_idx == null ) return prevFun;" 
	    }
	    
	    public Expression buildSecondaryConstr(Iterator<StmtVarDecl> iterIt, String newVar, int jj){
	    	assert iterIt.hasNext();
	    	StmtVarDecl iterPar = iterIt.next();
    		ExprArrayRange ear = new ExprArrayRange(ccontext, new ExprVar(ccontext, newVar), new ExprConstInt(jj));
    		Expression tmp = new ExprBinary(ccontext,ExprBinary.BINOP_LT, ear, new ExprVar(ccontext, iterPar.getName(0)));
    		//tmp = idx[2*jj+1] < par_jj
    		Expression eq =  new ExprBinary(ccontext,ExprBinary.BINOP_EQ, ear, new ExprVar(ccontext, iterPar.getName(0)));
    		Expression out;
    		if( iterIt.hasNext()){
    			Expression andExp = new ExprBinary(ccontext, ExprBinary.BINOP_AND, eq, buildSecondaryConstr(iterIt, newVar, jj+1));
    			out = new ExprBinary(ccontext, ExprBinary.BINOP_OR, tmp, andExp);
    		// out = tmp || (eq &&  buildSecondaryConstr(iterIt))
    		}else{
    			out = tmp;
    		// out = tmp;
    		}
    		return out;
	    }
	    
	    
	    public StmtMax newStmtMax(int i, List<Expression> indices, ArrFunction fun){	
	    	assert indices.size() == fun.idxParams.size();
	    	String newVar = ArrFunction.IDX_VAR + i;
	    	ExprVar idxi = new ExprVar(ccontext, newVar);
	    	//"idx_i := max{expr1==t, idx < in_idx, conds }; "
	    	int ii=0;
	    	StmtMax smax = new StmtMax(currentTN.getLevel()*2+1, newVar, ArrFunction.GUARD_VAR + i);
	    	// First we add the primary constraints.
	    	// indices[k][fun.iterParam[j] -> idx_i[2*j+1]]==fun.idxParams[k]
	    	// idx_i[2*j] == pos_j 
	    	for(Iterator<StmtVarDecl> idxIt = fun.idxParams.iterator(); idxIt.hasNext(); ii++){
	    		StmtVarDecl idxPar = idxIt.next();	   
	    		Expression cindex = indices.get(ii);
	    		int jj=0;
	    		for(Iterator<StmtVarDecl> iterIt = currentTN.limitedPathIter(); iterIt.hasNext(); jj++){
	    			StmtVarDecl iterPar = iterIt.next();
	    			ExprArrayRange ear = new ExprArrayRange(ccontext, new ExprVar(ccontext, newVar), new ExprConstInt(2*jj+1));
	    			cindex = (Expression) cindex.accept(new VarReplacer(iterPar.getName(0), ear ));
	    		}
	    		cindex = new ExprBinary(ccontext, ExprBinary.BINOP_EQ, cindex, new ExprVar(ccontext, idxPar.getName(0)));
	    		cindex = (Expression)cindex.accept(new ArrReplacer(idxi));
	    		smax.primC.add(cindex);
	    	}
	    	//Put additional primary constraints for the loop variables for loops outside the declaration site.
	    	
	    	if( fun.declarationSite != this.ptree.getRoot()  ){
	    		boolean found = false;
	    		int jj=0;
		    	for(PathIterator iterIt = currentTN.limitedPathIter(); iterIt.hasNext(); jj++){
		    		ParamTree.treeNode iterPar = iterIt.tnNext();
		    		ExprArrayRange ear = new ExprArrayRange(ccontext, new ExprVar(null, newVar), new ExprConstInt(2*jj+1));
		    		Expression cindex = new ExprBinary(ccontext, ExprBinary.BINOP_EQ, ear, new ExprVar(ccontext, iterPar.vdecl.getName(0)));		    		
		    		smax.primC.add(cindex);
		    		if( iterPar == fun.declarationSite ){ found = true; break;}
		    	}
		    	assert found;
	    	}
	    	
	    	
	    	
	    	{		    	
		    	{
		    		int stage0 = this.ptree.getRoot().getStage(); 		    			
		    		ExprConstInt val = new ExprConstInt( stage0);
		    		ExprArrayRange ear = new ExprArrayRange(ccontext, new ExprVar(ccontext, newVar), ExprConstInt.zero);
		    		Expression cindex = new ExprBinary(ccontext, ExprBinary.BINOP_EQ, ear, val);
		    		cindex = (Expression)cindex.accept(new ArrReplacer(idxi));
		    		smax.primC.add(cindex);
		    	}		    	
		    	int jj=0;
		    	for(ParamTree.treeNode.PathIterator iterIt = currentTN.limitedPathIter(); iterIt.hasNext();jj++){
		    		loopHist lh = iterIt.lhNext();
		    		ExprConstInt val = new ExprConstInt( lh.stage);
		    		ExprArrayRange ear = new ExprArrayRange(ccontext, new ExprVar(null, newVar), new ExprConstInt(2*jj+2));
		    		Expression cindex = new ExprBinary(ccontext, ExprBinary.BINOP_EQ, ear, val);
		    		cindex = (Expression)cindex.accept(new ArrReplacer(idxi));
		    		smax.primC.add(cindex);
		    	}
		    	//assert jj == indices.size();
	    	}
	    	
	    	
	    	//Now we add the secondary constraints.
	    	int tt = 0;
	    	PathIterator pIt = currentTN.limitedPathIter();
	    	if( fun.declarationSite != this.ptree.getRoot()  ){
	    		for(; pIt.hasNext(); ){
		    		ParamTree.treeNode iterPar = pIt.tnNext();	
		    		tt+= 2;
		    		if( iterPar == fun.declarationSite ){break;}
		    	}
	    		
	    	}
	    	pIt.makeUnlimited();
	    	if(pIt.hasNext()){
		    	Expression binexp = buildSecondaryConstr(pIt, newVar, tt);
		    	smax.secC.add(binexp);
	    	}
	    	
	    	//Finally, we add the tertiary constraints.
	    	//In these constraints, we again replace
	    	//   fun.iterParam[j] -> idx_i[2*j+1]
	    	//We must also replace all accesses to arrays with calls to the array functions.	
	    	int ll = 0;
	    	for(Iterator<Expression> condIt = conds.iterator(); condIt.hasNext(); ++ll ){
	    		Expression cond = condIt.next();
	    		if( ll >= fun.condsPos   ){
		    		int jj=0;
		    		for(Iterator<StmtVarDecl> iterIt = currentTN.limitedPathIter(); iterIt.hasNext(); jj++){
		    			StmtVarDecl iterPar = iterIt.next();
		    			ExprArrayRange ear = new ExprArrayRange(ccontext, new ExprVar(ccontext, newVar), new ExprConstInt(2*jj+1));
		    			cond = (Expression) cond.accept(new VarReplacer(iterPar.getName(0), ear ));
		    		}
		    		
			    	cond = (Expression)cond.accept(new ArrReplacer(idxi));
		    		smax.terC.add(cond);
	    		}
	    	}
	    	return smax;
	    }
	    
		
		public Expression comp(int pos, int dim, ExprVar v1, ExprVar v2){
			ExprArrayRange ear1 = new ExprArrayRange(ccontext, v1, new ExprConstInt(pos));
			ExprArrayRange ear2 = new ExprArrayRange(ccontext, v2, new ExprConstInt(pos));
			Expression tmp = new ExprBinary(ccontext,ExprBinary.BINOP_LT, ear1, ear2);
			Expression eq =  new ExprBinary(ccontext,ExprBinary.BINOP_EQ, ear1, ear2);
			Expression out;
			if(pos<dim-1){
				Expression andExp = new ExprBinary(ccontext, ExprBinary.BINOP_AND, eq, comp(pos+1, dim,  v1, v2));
				out = new ExprBinary(ccontext, ExprBinary.BINOP_OR, tmp, andExp);
			// out = tmp || (eq &&  comp(iterIt))
			}else{
				out = tmp;
			// out = tmp;
			}
			return out;		
		}
		
		
		public Statement processMax(int dim, ExprVar v1, ExprVar v2, String gv2, int id){
			Expression cond1 = new ExprVar(ccontext, gv2);
			Expression cond2 = comp(0, dim, v1, v2);
			
			
			StmtAssign as2 = new StmtAssign(ccontext, v1, v2);
			List<Statement> lst = new ArrayList<Statement>(3+id);
			StmtAssign as0 = new StmtAssign(ccontext, new ExprVar(null, ArrFunction.IND_VAR), ExprConstInt.zero);
			lst.add(as0);
			for(int i=0; i<id; ++i){
				StmtAssign as = new StmtAssign(ccontext, new ExprVar(null, ArrFunction.IND_VAR+i), ExprConstInt.zero);
				lst.add(as);
			}
			StmtAssign as1 = new StmtAssign(ccontext, new ExprVar(null, ArrFunction.IND_VAR+id), new ExprConstInt(1));
			lst.add(as1);
			lst.add(as2);
			
			StmtIfThen if2 = new StmtIfThen(ccontext, cond2,  new StmtBlock(null, lst), null);
			StmtIfThen if1 = new StmtIfThen(ccontext, cond1,  if2, null);		
			return if1;
		}
	    
	    
	    
	    
	    public Statement pickLargest(int i, int dim){
	    	//"max_idx = max(idx_i|{(stack-smap[x].stack_beg).stage}, max_idx);" 
	    	ExprVar mvar = new ExprVar(ccontext, ArrFunction.MAX_VAR);
	    	ExprVar idxvar = new ExprVar(ccontext, ArrFunction.IDX_VAR + i);
    		return processMax(dim, mvar, idxvar, ArrFunction.GUARD_VAR + i , i);
	    }
	    
	    /**
	     * This class replaces arrays with their corresponding function representations.
	     * expr2[x[l]->x_fun(l, idx_i)]
	     * 
	     * The idx_i parameter is the base for the loop index parameters
	     * @author asolar
	     *
	     */
	    class ArrReplacer extends FEReplacer{
	    	ExprVar idxi;	    	
	    	public ArrReplacer( ExprVar idxi){
	    		this.idxi = idxi;
	    	}
	    	
	    	void setIterPosIterParams(ArrFunction callee, List<Expression> params){
	    		//TODO should also add position parameters. 
	    		Iterator<StmtVarDecl> globIter = ptree.iterator();
	    		Iterator<StmtVarDecl> locIter = currentTN.pathIter();
	    		
	    		StmtVarDecl loc = locIter.hasNext()? locIter.next() : null;
	    		int ii=0;
	    		while(globIter.hasNext()){
	    			StmtVarDecl par = globIter.next();
	    			if( par == loc){
	    				ExprArrayRange ear = new ExprArrayRange(ccontext,idxi, new ExprConstInt(ii));
	    				++ii;
	    				params.add(ear);
	    				loc = locIter.hasNext()? locIter.next() : null;
	    			}else{
	    				params.add( par.getInit(0) );
	    			}
	    		}
	    	}
	    	
	    	public Object visitExprVar(ExprVar evar){
	    		String bname = evar.getName();
	    		if(smap.containsKey(bname)){
	    			List<Expression> tmp = new ArrayList<Expression>();
	    			return doReplacement(bname, tmp);
	    		}else{
	    			return evar;
	    		}
	    	}

	    	public ExprFunCall doReplacement(String bname, List<Expression> mem){
//	    		First, we get the function representation of the array.
	    		arrInfo ainf = smap.get(bname);
	    		ArrFunction arFun = ainf.sfun.peek();
	    		//Now we build a function call to replace the array access.
	    		List<Expression> params = new ArrayList<Expression>();	    		
	    		assert arFun.idxParams.size() == mem.size();
	    		for(int i=0; i<mem.size(); ++i){	    			
	    			Expression obj=mem.get(i);
	    			//assert obj instanceof RangeLen;
	    			Expression newPar = (Expression)obj.accept(this);
	    			params.add(newPar);
	    		}
	    		// Now we have to set the other parameters, which is a little trickier.	    			    		
	    		setIterPosIterParams(arFun, params);
	    		//Then, we must set the other parameters.
	    		for(Iterator<Entry<String, Type>> pIt = superParams.entrySet().iterator(); pIt.hasNext(); ){		    			
	   	 			Entry<String, Type> par = pIt.next();
	   	 			params.add(new ExprVar(ccontext, par.getKey()));
	   	 		}
	    		
	    		//Finally, we must set the parameters that are passed through to the input arrays.
	    		
	    		for(Iterator<StmtVarDecl> it = inArrParams.iterator(); it.hasNext();  ){
	    			StmtVarDecl svd = it.next();
	    			ExprVar ev = new ExprVar(ccontext, svd.getName(0));
	    			params.add(ev);
	    		}
	    			
	    		return new ExprFunCall(ccontext, arFun.getFullName(), params);
	    	}
	    	
	    	
	    	public ExprFunCall doInputReplacement(String bname, List<Expression> mem){
	    		Function inArr = inVars.get(bname);
//	    		Now we build a function call to replace the array access.
	    		List<Expression> params = new ArrayList<Expression>();	
	    		
	    		//First, we add the index parameters.
	    		for(int i=0; i<mem.size(); ++i){	    			
	    			Expression obj=mem.get(i);
	    			Expression newPar = (Expression)obj.accept(this);
	    			params.add(newPar);
	    		}
	    		
	    		return new ExprFunCall(ccontext, inArr.getName(), params);
	    	}
	    	
	    	
	    	
	    	public Object visitExprArrayRange(ExprArrayRange exp) {
	    		final ExprVar newBase= exp.getAbsoluteBase();	    		
	    		String bname = newBase.getName();
	    		if(smap.containsKey(bname)){	    			
	    			List<Expression> mem = getArrayIndices(exp);
		    		return doReplacement(bname, mem);
	    		}else{
	    			//Now, we must check whether it is an input array.
	    			if( inVars.containsKey(bname)){
	    				List<Expression> mem = getArrayIndices(exp);
			    		return doInputReplacement(bname, mem);
	    			}
	    			return exp;
	    		}
	    	}
	    }
	    
	    class RHSReplacer extends VarReplacer{
	    	final List<Expression> indices;
	    	final List<StmtVarDecl> formals;
	    	RHSReplacer(String oldName, Expression newName, List<Expression> indices, List<StmtVarDecl> formals){
	    		super(oldName, newName);
	    		this.indices = indices;
	    		this.formals = formals;
	    	}
	    	
	    	 public Object visitExprBinary(ExprBinary exp)
    	    {
	    		 int i=0;
    		 for(Expression e : indices ){
    			 if( exp.equals(e)){
    				 StmtVarDecl sv = formals.get(i);
    				 System.out.println("FOUND ONE");
    				 return new ExprVar(sv.getCx(), sv.getName(0));
    			 }
    			 ++i;
    		 }
    		 
    		 return super.visitExprBinary(exp);
    	    }
	    } 
	    
	    
	    public Statement iMaxIf(int i, Expression rhs, ArrFunction fun, List<Expression> indices){
	    	//if(indvar == i+1){ return rhs; }
	    	ExprVar indvar = new ExprVar(ccontext, ArrFunction.IND_VAR+i);
	    	ExprVar idxi = new ExprVar(ccontext, ArrFunction.IDX_VAR + i);
	    	//ExprConstInt iiv = new ExprConstInt(i+1);	    	
	    	int ii=0;
	    	
	    	// rhs[ idx_param[ii] -> idx_i[2*ii+1] ]; 
	    	for(Iterator<StmtVarDecl> it = currentTN.limitedPathIter(); it.hasNext(); ++ii){
	    		StmtVarDecl par = it.next();	    		
	    		ExprArrayRange ear = new ExprArrayRange(ccontext,idxi, new ExprConstInt(2*ii+1));
	    		rhs = (Expression)rhs.accept(new RHSReplacer(par.getName(0), ear, indices, fun.idxParams));
	    	}
//	    	 rhs[ arr[i] -> arr_fun(i, idxi) ]; 
	    	Expression retV = (Expression)rhs.accept(new ArrReplacer(idxi));	    	
	    	return new StmtIfThen(rhs.getContext(), indvar, new StmtReturn(ccontext, retV), null); 
	    }
	   
	    
	    public void processArrAssign(String var, List<Expression> indices, Expression rhs){
	    	assert smap.containsKey(var);
	    	arrInfo ainf = smap.get(var);	    		    	
	   	 	assert ( ainf.fun != null);
	   	 	ArrFunction fun = ainf.fun;
	   	 	int i = fun.size();
	   	 	fun.addIdxAss( newStmtMax(i, indices, fun) );	   	 	
	   	 	fun.addMaxAss( pickLargest(i, currentTN.getLevel()*2+1) );
	   	 	fun.addRetStmt( iMaxIf(i, rhs, fun, indices)  );
	   	 	currentTN.incrStage();
	    }

	   
	    
	    private List<Expression> getArrayIndices(ExprArrayRange array) {
	        List<Expression> indices = new ArrayList<Expression>();
	        Expression base=array.getBase();
	        if(base instanceof ExprArrayRange) {
	        	indices.addAll(getArrayIndices((ExprArrayRange) base));
	        }
	        List memb=array.getMembers();
	        assert memb.size()==1: "In stencil mode, we permit only single-element indexing, i.e. no a[1,3,4]";
	        assert memb.get(0) instanceof RangeLen: "In stencil mode, array ranges (a[1:4]) are not allowed";
	        RangeLen rl=(RangeLen) memb.get(0);
	        assert rl.len()==1: "In stencil mode, array ranges (a[1::2]) are not allowed";
	        indices.add(rl.start());
	        return indices;
	    }
	    
	    public Object visitStmtAssign(StmtAssign stmt)
	    {
	    	ccontext = stmt.getContext();
	        Expression lhs = stmt.getLHS();
	        Expression rhs = stmt.getRHS();
	        if( lhs instanceof ExprArrayRange ){
		        assert lhs instanceof ExprArrayRange ;	        
		        ExprArrayRange nLHS = (ExprArrayRange) lhs;
		        String var = nLHS.getAbsoluteBase().getName();
		        List<Expression> indices = getArrayIndices(nLHS);
		        processArrAssign(var, indices, rhs);	
	        }else{
	        	assert lhs instanceof ExprVar;
	        	String var = ((ExprVar) lhs ).getName();
	        	List<Expression> indices = new ArrayList<Expression>(0);
	        	processArrAssign(var, indices, rhs);	        	
	        }
	        return stmt;	        
	    }
	    
	 
	 public Object visitStmtFor(StmtFor stmt)
	    {
		 	ccontext = stmt.getContext();
		 	FEContext context = stmt.getContext();
		 	assert stmt.getInit() instanceof StmtVarDecl;
		 	StmtVarDecl init = (StmtVarDecl) stmt.getInit();
		 	assert init.getNumVars() == 1;
	        String indVar = init.getName(0);
	        Expression exprStart = init.getInit(0);
	        Expression exprStartPred = new ExprBinary(context, ExprBinary.BINOP_GE, new ExprVar(context, indVar), exprStart);
	        Expression exprEndPred = stmt.getCond();
	        processForLoop(stmt, indVar, exprStartPred, exprEndPred, stmt.getBody(), true);	        
	        return stmt;
	    }
	
	
	public ProcessStencil(String suffix, Map<String, Type> sp, Map<String, Function> inVars) {
		super();
		conds = new Stack<Expression>();		
		scopeStack = new Stack<scopeHist>();
		smap = new HashMap<String, arrInfo>();		
		this.outVar = new TreeSet<String>();
		this.suffix = "_" + suffix;
		this.superParams = sp;
		this.inVars = inVars;
		this.inArrParams = new ArrayList<StmtVarDecl>();
	}
	

	private void declareNewArray(Type ta , String var){
		
		int tt = 0;
		while(ta instanceof TypeArray){
			ta = ((TypeArray) ta).getBase();
			++tt;
			assert tt < 100;
		}
				
		arrInfo ainf = null;
		assert !smap.containsKey(var);
		ainf = new arrInfo();
		smap.put(var, ainf);
		populateArrInfo(ainf, var, ta, tt);
	}
	
	
	
    public Object visitFunction(Function func)
    {
    	ptree = (new SetupParamTree()).producePtree(func);
    	currentTN = ptree.getRoot();
    	 scopeHist sc = new scopeHist();
		 scopeStack.push( sc );
		 
    	List params = func.getParams();
    	for(Iterator it = params.iterator(); it.hasNext();  ){
    		Parameter param = (Parameter) it.next();
    		if( param.isParameterOutput() ){    			
    			declareNewArray(param.getType(), param.getName());
    			outVar.add(param.getName());
    		}else{
    			
    			int dim = FunctionalizeStencils.checkIfGrid(param);
    			if( dim < 0){
    				//This means the parameter is a scalar parameter, and should
    				//be added to the SuperParams map.
    				superParams.put(param.getName(), param.getType());
    			}
    		}
    	}
    	Object tmp  = super.visitFunction(func);
    	scopeStack.pop();
        return tmp;
    }

}



