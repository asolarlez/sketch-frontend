package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import streamit.frontend.nodes.ExprArray;
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
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.StreamSpec;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;
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
	}
	loopHist(String var, Expression low, Expression high){
		this.var = var;
		this.low = low;
		this.high = high;
		stage = 0;		
		if(high != null)
			computeHighPlusOne();
	}
	
	public StmtVarDecl newVD(){
	    	return new StmtVarDecl(null, TypePrimitive.inttype,  var, highPlusOne);	    	
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


class VarReplacer extends FEReplacer{
	String oldName;
	Expression newName;
	
	VarReplacer(String oldName, Expression newName){
		this.oldName = oldName;
		this.newName = newName;
	}
	
	public Object visitExprVar(ExprVar exp) {
		if( exp.getName().equals(oldName)){
			return newName;
		}else{
			return exp;
		}
	}
	

	
}

public class FunctionalizeStencils extends FEReplacer {
	List<processStencil> stencilFuns;
	Map<String, ArrFunction> funmap;
	Map<String, Type> superParams;
	List<StmtVarDecl> outIdxs;
	StreamSpec ss;
	
	/**
	 * Maps a function name to a map of input grid names to abstract grids. 
	 */	
	Map<String, Map<String, AbstractArray> > globalInVars;
	
	public FunctionalizeStencils() {
		super();
		stencilFuns = new ArrayList<processStencil>();
		superParams = new TreeMap<String, Type>();
		funmap = new HashMap<String, ArrFunction>();
		globalInVars = new HashMap<String, Map<String, AbstractArray> >();
	}
	
	
	public void processFuns(Program prog){
		for(Iterator<Entry<String, ArrFunction>> it = funmap.entrySet().iterator(); it.hasNext(); ){
			ArrFunction af = it.next().getValue();
			af.processMax();
			List functions=((StreamSpec)prog.getStreams().get(0)).getFuncs();
			functions.add(af.toAST());
			System.out.println(af);
		}
	}
	
	
	public void printFuns(){
		for(Iterator<Entry<String, ArrFunction>> it = funmap.entrySet().iterator(); it.hasNext(); ){
			ArrFunction af = it.next().getValue();
			System.out.println(af);
		}
		
		for(Iterator<Entry<String, Map<String, AbstractArray>>> it = globalInVars.entrySet().iterator(); it.hasNext(); ){
			Map<String, AbstractArray> af = it.next().getValue();
			
			for(Iterator<Entry<String, AbstractArray>> aait = af.entrySet().iterator(); aait.hasNext(); ){
				AbstractArray aa = aait.next().getValue();
				System.out.println(aa);
			}
		}
		
	}
	
	
	public Map<String, ArrFunction> getFunMap(){
		return funmap;
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
	
	 
	 public Map<String, AbstractArray> getInGrids(Function func){
		 List<StmtVarDecl> othParams = new ArrayList<StmtVarDecl>();
		 for(Iterator<Entry<String, Type>> pIt = superParams.entrySet().iterator(); pIt.hasNext(); ){
	 			Entry<String, Type> par = pIt.next();
	 			othParams.add(new StmtVarDecl(null, par.getValue(), par.getKey(), null));
	 	}
		 
		 
		if( globalInVars.containsKey(func.getName()) ){
			return globalInVars.get(func.getName());
		}else{
			//It may be that the abstract functions haven't been defined for this function,
			//but if they have been defined for it's spec, then we reuse those. 
			//Remember spec and sketch must share the same abstract grids.
			String spec = func.getSpecification();
			if( spec != null ){
				if( globalInVars.containsKey(spec) ){
					Map<String, AbstractArray> inVars = globalInVars.get(spec);
					this.globalInVars.put(func.getName(), inVars);
					return globalInVars.get(spec);
				}
			}
		 
			
			Map<String, AbstractArray> inVars = new HashMap<String, AbstractArray>();
			
			outIdxs = new ArrayList<StmtVarDecl>();
			boolean onlyOneOutput = true;
			List params = func.getParams();
			for(Iterator it = params.iterator(); it.hasNext();  ){
				Parameter param = (Parameter) it.next();
				if( !param.isParameterOutput() ){    		
					int dim = checkIfGrid( param );
					if( dim > 0){    				
						AbstractArray absArr = new AbstractArray(param.getName(), func.getName(), dim, othParams, outIdxs);
						inVars.put(param.getName(), absArr);
						/////////////////
					}else{
						othParams.add( new StmtVarDecl(null, param.getType(), param.getName(), null)  );
					}
				}else{
					assert onlyOneOutput : "The function can have only one output!! ";
					onlyOneOutput = false;
					int dim = checkIfGrid( param );
					
					for(int i=0; i<dim; ++i){
						outIdxs.add(new StmtVarDecl(null, TypePrimitive.inttype, AbstractArray.outIndexParamName(i) , null));
					}
				}
			}
			
			//Now, we need to populate the AbstractArray. 
			//It is always populated from the spec, so if this is
			//a sketch, we need to look for the spec and populate from there.
			Function fToUse = func;
			if( spec != null){
				fToUse = ss.getFuncNamed(spec);
			}
			BuildAbstractArrays builder = new BuildAbstractArrays(inVars);
			//TODO fToUse.accept(builder);
			
			this.globalInVars.put(func.getName(), inVars);
			if( spec != null){
				this.globalInVars.put(spec, inVars);
			}
			return inVars;
		}
	}
	
	 public Object visitFunction(Function func){
		Map<String, AbstractArray> funInGrids = getInGrids(func);
		 	
		 
		processStencil ps = new processStencil(func.getName());
		 	
			
		ps.setSuperParams(superParams);		 	
		ps.setOutIdxsParams(outIdxs);
		
		ps.setInVars(funInGrids);
		stencilFuns.add(ps);
		func.accept(ps);
		funmap.putAll(ps.getAllFunctions());
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

class processStencil extends FEReplacer {
	
	/*
	 * Includes the global variables and the scalar parameters of the function.
	 */
	Map<String, Type> superParams;

	Stack<Expression> conds;
	ParamTree.treeNode currentTN;
	Stack<scopeHist> scopeStack;
	Map<String, arrInfo> smap;
	Map<String, AbstractArray> inVars;
	Set<String> outVar;
	List<StmtVarDecl> outIdxs;
	List<StmtVarDecl> inArrParams;
	ParamTree ptree;
	final String suffix; 
		
	
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
	
	

	public void setSuperParams(Map<String, Type> sp){
		superParams = sp;
	}
	 
	
	public void  setOutIdxsParams(List<StmtVarDecl> outIdxs){
		this.outIdxs = outIdxs;
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
		 ++(currentTN.lh.stage);
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
	        ++(currentTN.lh.stage);
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
	    
	    void populateArrInfo(arrInfo ainf, String var, int dim){	    	
   	 		ainf.fun = new ArrFunction(var, suffix, ainf.sfun.size(), ptree);
   	 		for(int i=0; i<dim; ++i) ainf.fun.idxParams.add( newVD("t"+i, null) );
   	 		
   	 		for(Iterator<Entry<String, Type>> pIt = superParams.entrySet().iterator(); pIt.hasNext(); ){
   	 			Entry<String, Type> par = pIt.next();
   	 			ainf.fun.othParams.add(new StmtVarDecl(null, par.getValue(), par.getKey(), null));
   	 		}
   	 		
   	 		ainf.fun.inputParams = this.inArrParams;
   	 		ainf.fun.outIdxParams = this.outIdxs;
   	 		
   	 		int sz = ainf.sfun.size();
   	 		ainf.fun.addRetStmt( nullMaxIf(sz>0 ? ainf.sfun.peek():null));
   	 		ainf.sfun.add(ainf.fun);
   	 		scopeStack.peek().funs.add(ainf.fun);   	 		
	    }
	    
	    public Object visitStmtVarDecl(StmtVarDecl stmt)
	    {	        
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
    	    		populateArrInfo(ainf, var, 0);
    	    		if( stmt.getInit(i) != null ){
    	    			List<Expression> indices = new ArrayList<Expression>(0);
    	    			processArrAssign(var, indices, stmt.getInit(i));
    	    		}
	        	}
	        }
	        return stmt;
	    }
	    
	    
	    public StmtVarDecl newVD(String name, Expression init){
	    	return new StmtVarDecl(null, TypePrimitive.inttype,  name, init);	    	
	    }
	    public Statement nullMaxIf(ArrFunction prevFun){	    	
	    	Expression cond = new ExprBinary(null, ExprBinary.BINOP_EQ, new ExprVar(null, ArrFunction.IND_VAR), new ExprConstInt(0) );
	    	Statement rval;
	    	if(prevFun != null){
	    		List<Expression>  params = new ArrayList<Expression>();
	    		for(Iterator<StmtVarDecl> it = prevFun.idxParams.iterator(); it.hasNext(); ){
	    			StmtVarDecl par = it.next();
	    			params.add(new ExprVar(null, par.getName(0)));
	    		}
	    		for(Iterator<StmtVarDecl> it = ptree.iterator(); it.hasNext(); ){
	    			StmtVarDecl par = it.next();
	    			params.add( par.getInit(0));
	    		}
	    		for(Iterator<StmtVarDecl> it = prevFun.othParams.iterator(); it.hasNext(); ){
	    			StmtVarDecl par = it.next();
	    			params.add( new ExprVar(null, par.getName(0)));
	    		}	    		
	    		rval = new StmtReturn(null, new ExprFunCall(null, prevFun.getFullName() , params) );
	    	}else{
	    		rval = new StmtReturn(null, new ExprConstInt(0) );
	    	}	    	
	    	return new StmtIfThen(null, cond, rval, null); 
	    	//   "if( max_idx == null ) return prevFun;" 
	    }
	    
	    public Expression buildSecondaryConstr(Iterator<StmtVarDecl> iterIt, String newVar, int jj){
	    	assert iterIt.hasNext();
	    	StmtVarDecl iterPar = iterIt.next();
    		ExprArray ear = new ExprArray(null, new ExprVar(null, newVar), new ExprConstInt(2*jj+1));
    		Expression tmp = new ExprBinary(null,ExprBinary.BINOP_LT, ear, new ExprVar(null, iterPar.getName(0)));
    		Expression eq =  new ExprBinary(null,ExprBinary.BINOP_EQ, ear, new ExprVar(null, iterPar.getName(0)));
    		Expression out;
    		if( iterIt.hasNext()){
    			Expression andExp = new ExprBinary(null, ExprBinary.BINOP_AND, eq, buildSecondaryConstr(iterIt, newVar, jj+1));
    			out = new ExprBinary(null, ExprBinary.BINOP_OR, tmp, andExp);
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
	    		for(Iterator<StmtVarDecl> iterIt = currentTN.pathIter(); iterIt.hasNext(); jj++){
	    			StmtVarDecl iterPar = iterIt.next();
	    			ExprArray ear = new ExprArray(null, new ExprVar(null, newVar), new ExprConstInt(2*jj+1));
	    			cindex = (Expression) cindex.accept(new VarReplacer(iterPar.getName(0), ear ));
	    		}
	    		cindex = new ExprBinary(null, ExprBinary.BINOP_EQ, cindex, new ExprVar(null, idxPar.getName(0)));
	    		smax.primC.add(cindex);
	    	}
	    	{		    	
		    	{
		    		int stage0 = this.ptree.getRoot().lh.stage;	
		    		ExprConstInt val = new ExprConstInt( stage0);
		    		ExprArray ear = new ExprArray(null, new ExprVar(null, newVar), new ExprConstInt(0));
		    		Expression cindex = new ExprBinary(null, ExprBinary.BINOP_EQ, ear, val);
		    		smax.primC.add(cindex);
		    	}		    	
		    	int jj=0;
		    	for(ParamTree.treeNode.PathIterator iterIt = currentTN.pathIter(); iterIt.hasNext();jj++){
		    		loopHist lh = iterIt.lhNext();
		    		ExprConstInt val = new ExprConstInt( lh.stage);
		    		ExprArray ear = new ExprArray(null, new ExprVar(null, newVar), new ExprConstInt(2*jj+2));
		    		Expression cindex = new ExprBinary(null, ExprBinary.BINOP_EQ, ear, val);
		    		smax.primC.add(cindex);
		    	}
		    	//assert jj == indices.size();
	    	}
	    	
	    	
	    	//Now we add the secondary constraints.	    	
	    	//NOTE: Check the priority.	    
	    	Iterator<StmtVarDecl> pIt = currentTN.pathIter();
	    	if(pIt.hasNext()){
		    	Expression binexp = buildSecondaryConstr(pIt, newVar, 0);
		    	smax.secC.add(binexp);
	    	}
	    	
	    	//Finally, we add the tertiary constraints.
	    	for(Iterator<Expression> condIt = conds.iterator(); condIt.hasNext(); ){
	    		Expression cond = condIt.next();
	    		int jj=0;
	    		for(Iterator<StmtVarDecl> iterIt = currentTN.pathIter(); iterIt.hasNext(); jj++){
	    			StmtVarDecl iterPar = iterIt.next();
	    			ExprArray ear = new ExprArray(null, new ExprVar(null, newVar), new ExprConstInt(2*jj+1));
	    			cond = (Expression) cond.accept(new VarReplacer(iterPar.getName(0), ear ));
	    		}
	    		smax.terC.add(cond);
	    	}
	    	return smax;
	    }
	    
	    

		
		public Expression comp(int pos, int dim, ExprVar v1, ExprVar v2){
			ExprArray ear1 = new ExprArray(null, v1, new ExprConstInt(pos));
			ExprArray ear2 = new ExprArray(null, v2, new ExprConstInt(pos));
			Expression tmp = new ExprBinary(null,ExprBinary.BINOP_LT, ear1, ear2);
			Expression eq =  new ExprBinary(null,ExprBinary.BINOP_EQ, ear1, ear2);
			Expression out;
			if(pos<dim-1){
				Expression andExp = new ExprBinary(null, ExprBinary.BINOP_AND, eq, comp(pos+1, dim,  v1, v2));
				out = new ExprBinary(null, ExprBinary.BINOP_OR, tmp, andExp);
			// out = tmp || (eq &&  comp(iterIt))
			}else{
				out = tmp;
			// out = tmp;
			}
			return out;		
		}
		
		
		public Statement processMax(int dim, ExprVar v1, ExprVar v2, String gv2, int id){
			Expression cond1 = new ExprVar(null, gv2);
			Expression cond2 = comp(0, dim, v1, v2);
			
			StmtAssign as1 = new StmtAssign(null, new ExprVar(null, ArrFunction.IND_VAR), new ExprConstInt(id+1));
			StmtAssign as2 = new StmtAssign(null, v1, v2);
			List<Statement> lst = new ArrayList<Statement>(2);
			lst.add(as1);
			lst.add(as2);
			
			StmtIfThen if2 = new StmtIfThen(null, cond2,  new StmtBlock(null, lst), null);
			StmtIfThen if1 = new StmtIfThen(null, cond1,  if2, null);		
			return if1;
		}
	    
	    
	    
	    
	    public Statement pickLargest(int i, int dim){
	    	//"max_idx = max(idx_i|{(stack-smap[x].stack_beg).stage}, max_idx);" 
	    	ExprVar mvar = new ExprVar(null, ArrFunction.MAX_VAR);
	    	ExprVar idxvar = new ExprVar(null, ArrFunction.IDX_VAR + i);
    		return processMax(dim, mvar, idxvar, ArrFunction.GUARD_VAR + i , i);
	    }
	    
	    /**
	     * This class replaces arrays with their corresponding function representations.
	     * expr2[x[l]->x_fun(l, idx_i)]
	     * @author asolar
	     *
	     */
	    class ArrReplacer extends FEReplacer{
	    	ArrFunction fun;
	    	ExprVar idxi;
	    	public ArrReplacer(ArrFunction fun, ExprVar idxi){
	    		this.fun = fun;
	    		this.idxi = idxi;
	    	}
	    	
	    	void setIterPosIterParams(ArrFunction callee,ArrFunction caller,List<Expression> params){
	    		Iterator<StmtVarDecl> globIter = ptree.iterator();
	    		Iterator<StmtVarDecl> locIter = currentTN.pathIter();
	    		
	    		StmtVarDecl loc = locIter.hasNext()? locIter.next() : null;
	    		int ii=0;
	    		while(globIter.hasNext()){
	    			StmtVarDecl par = globIter.next();
	    			if( par == loc){
	    				ExprArray ear = new ExprArray(null,idxi, new ExprConstInt(2*ii+1));
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
	    		//What we will do is first compare the iter and posIter params of arFun with
	    		//those of the current function fun. The two lists should have a matching prefix
	    		//and a diverging suffix. For those arguments corresponding to the prefix, we'll		    		
	    		//pass a current value (an idx_i) for the others, we'll pass the default value.
	    		
	    		setIterPosIterParams(arFun, fun, params);
	    		//Then, we must set the other parameters.
	    		for(Iterator<Entry<String, Type>> pIt = superParams.entrySet().iterator(); pIt.hasNext(); ){		    			
	   	 			Entry<String, Type> par = pIt.next();
	   	 			params.add(new ExprVar(null, par.getKey()));
	   	 		}
	    		
	    		//Finally, we must set the parameters that are passed through to the input arrays.
	    		
	    		for(Iterator<StmtVarDecl> it = inArrParams.iterator(); it.hasNext();  ){
	    			StmtVarDecl svd = it.next();
	    			ExprVar ev = new ExprVar(null, svd.getName(0));
	    			params.add(ev);
	    		}

	    		for(Iterator<StmtVarDecl> it = outIdxs.iterator(); it.hasNext();  ){
	    			StmtVarDecl svd = it.next();
	    			ExprVar ev = new ExprVar(null, svd.getName(0));
	    			params.add(ev);
	    		}
	    			
	    		return new ExprFunCall(null, arFun.getFullName(), params);
	    	}
	    	
	    	
	    	public ExprFunCall doInputReplacement(String bname, List<Expression> mem){
	    		AbstractArray inArr = inVars.get(bname);
//	    		Now we build a function call to replace the array access.
	    		List<Expression> params = new ArrayList<Expression>();	
	    		assert inArr.dim == mem.size();
	    		//First, we add the index parameters.
	    		for(int i=0; i<mem.size(); ++i){	    			
	    			Expression obj=mem.get(i);
	    			Expression newPar = (Expression)obj.accept(this);
	    			params.add(newPar);
	    		}
	    		//Then, we must add the output index parameters.
	    		for(Iterator<StmtVarDecl> it = outIdxs.iterator(); it.hasNext(); ){
	    			StmtVarDecl vd = it.next();
	    			Expression obj = new ExprVar(null, vd.getName(0));
	    			params.add(obj);
	    		}
	    		//And then the symbolic parameters, 
	    		for(int i=0; i<inArr.numSymParams(); ++i){
	    			Expression obj = new ExprVar(null, inArr.symParamName(i) );
	    			params.add(obj);
	    		}	    			    		
	    		//And then the otherParams,
	    		for(Iterator<StmtVarDecl> it = inArr.otherParams.iterator() ;it.hasNext();  ){
	    			StmtVarDecl vd = it.next();
	    			Expression obj = new ExprVar(null, vd.getName(0));
	    			params.add(obj);
	    		}	    		
	    		//And then the globalParameters
	    		for(Iterator<StmtVarDecl> it = inArr.globalParams.iterator() ;it.hasNext();  ){
	    			StmtVarDecl vd = it.next();
	    			Expression obj = new ExprVar(null, vd.getName(0));
	    			params.add(obj);
	    		}
	    		return new ExprFunCall(null, inArr.getFullName(), params);
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
	    
	    
	    
	    public Statement iMaxIf(int i, Expression rhs, ArrFunction fun){
	    	//if(max_idx == idx_i){ return rhs; }
	    	ExprVar indvar = new ExprVar(null, ArrFunction.IND_VAR);
	    	ExprVar idxi = new ExprVar(null, ArrFunction.IDX_VAR + i);
	    	ExprConstInt iiv = new ExprConstInt(i+1);
	    	Expression eq = new ExprBinary(null, ExprBinary.BINOP_EQ, indvar, iiv);
	    	int ii=0;
	    	for(Iterator<StmtVarDecl> it = currentTN.pathIter(); it.hasNext(); ++ii){
	    		StmtVarDecl par = it.next();	    		
	    		ExprArray ear = new ExprArray(null,idxi, new ExprConstInt(2*ii+1));
	    		rhs = (Expression)rhs.accept(new VarReplacer(par.getName(0), ear));
	    	}
	    	
	    	Expression retV = (Expression)rhs.accept(new ArrReplacer(fun, idxi));	    	
	    	return new StmtIfThen(rhs.getContext(), eq, new StmtReturn(null, retV), null); 
	    }
	   
	    
	    public void processArrAssign(String var, List<Expression> indices, Expression rhs){
	    	assert smap.containsKey(var);
	    	arrInfo ainf = smap.get(var);	    		    	
	   	 	assert ( ainf.fun != null);
	   	 	ArrFunction fun = ainf.fun;
	   	 	int i = fun.size();
	   	 	fun.addIdxAss( newStmtMax(i, indices, fun) );	   	 	
	   	 	fun.addMaxAss( pickLargest(i, currentTN.getLevel()*2+1) );
	   	 	fun.addRetStmt( iMaxIf(i, rhs, fun)  );
	   	 	++(currentTN.lh.stage);
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
	
	
	public processStencil(String suffix) {
		super();
		superParams = new TreeMap<String, Type>();
		conds = new Stack<Expression>();		
		scopeStack = new Stack<scopeHist>();
		smap = new HashMap<String, arrInfo>();		
		this.outVar = new TreeSet<String>();
		this.suffix = "_" + suffix;
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
		populateArrInfo(ainf, var, tt);
	}
	
	
	
	public void setInVars(Map<String, AbstractArray> inVars){
		this.inVars = inVars;
		
		//TODO populateInParams.		
		inArrParams = new ArrayList<StmtVarDecl>();
		
		for(Iterator<Entry<String, AbstractArray>> it = inVars.entrySet().iterator(); it.hasNext(); ){
			AbstractArray aar = it.next().getValue();
			
			aar.addParamsToFunction(inArrParams);
		}
		
		
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



