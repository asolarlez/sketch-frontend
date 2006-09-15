package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
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
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.FieldDecl;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;
import streamit.frontend.nodes.ExprArrayRange.RangeLen;






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
	public FunctionalizeStencils() {
		super();
		stencilFuns = new ArrayList<processStencil>();
		superParams = new HashMap<String, Type>();
		funmap = new TreeMap<String, ArrFunction>();
	}
	
	
	public void processFuns(){
		for(Iterator<Entry<String, ArrFunction>> it = funmap.entrySet().iterator(); it.hasNext(); ){
			ArrFunction af = it.next().getValue();
			af.processMax();
			System.out.println(af);
		}
	}
	
	public Map<String, ArrFunction> getFunMap(){
		return funmap;
	}
	
	 public Object visitFunction(Function func)
	    {
		 	processStencil ps = new processStencil(func.getName());
		 	ps.setSuperParams(superParams);
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
	Map<String, Type> superParams;

	Stack<Expression> conds;
	ParamTree.treeNode currentTN;
	Stack<scopeHist> scopeStack;
	Map<String, arrInfo> smap;
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
	 
	void closeOutOfScopeVars(scopeHist sc2){
		for(Iterator<ArrFunction> it = sc2.funs.iterator(); it.hasNext();  ){
			 ArrFunction t = it.next();
			 t.close();
			 System.out.println(t);
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
	    
	    void fa(arrInfo ainf, String var, int dim){	    	
   	 		ainf.fun = new ArrFunction(var, suffix, ainf.sfun.size(), ptree);
   	 		for(int i=0; i<dim; ++i) ainf.fun.idxParams.add( newVD("t"+i, null) );
   	 		
   	 		for(Iterator<Entry<String, Type>> pIt = superParams.entrySet().iterator(); pIt.hasNext(); ){
   	 			Entry<String, Type> par = pIt.next();
   	 			ainf.fun.othParams.add(new StmtVarDecl(null, par.getValue(), par.getKey(), null));
   	 		}
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
	        		int tt = 0;
	        		while(ta instanceof TypeArray){
	        			ta = ((TypeArray) ta).getBase();
	        			++tt;
	        			assert tt < 100;
	        		}
	        		
	        		String var = stmt.getName(i);
	        		arrInfo ainf = null;
    	    		assert !smap.containsKey(var);
    	    		ainf = new arrInfo();
    	    		smap.put(var, ainf);
    	    		fa(ainf, var, tt);
	        	}else{
	        		String var = stmt.getName(i);
	        		arrInfo ainf = null;
    	    		assert !smap.containsKey(var);
    	    		ainf = new arrInfo();
    	    		smap.put(var, ainf);
    	    		fa(ainf, var, 0);
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
	    			List tmp = new ArrayList();
	    			return fa(bname, tmp);
	    		}else{
	    			return evar;
	    		}
	    	}

	    	public ExprFunCall fa(String bname, List mem){
//	    		First, we get the function representation of the array.
	    		arrInfo ainf = smap.get(bname);
	    		ArrFunction arFun = ainf.sfun.peek();
	    		//Now we build a function call to replace the array access.
	    		List<Expression> params = new ArrayList<Expression>();	    		
	    		assert arFun.idxParams.size() == mem.size();
	    		for(int i=0; i<mem.size(); ++i){	    			
	    			Object obj=mem.get(i);
	    			assert obj instanceof RangeLen;
	    			Expression newPar = (Expression)((RangeLen)obj).start().accept(this);
	    			params.add(newPar);
	    		}
	    		// Now we have to set the other parameters, which is a little trickier.
	    		//What we will do is first compare the iter and posIter params of arFun with
	    		//those of the current function fun. The two lists should have a matching prefix
	    		//and a diverging suffix. For those arguments corresponding to the prefix, we'll		    		
	    		//pass a current value (an idx_i) for the others, we'll pass the default value.
	    		
	    		setIterPosIterParams(arFun, fun, params);
	    		//Finally, we must set all the other parameters.
	    		for(Iterator<Entry<String, Type>> pIt = superParams.entrySet().iterator(); pIt.hasNext(); ){		    			
	   	 			Entry<String, Type> par = pIt.next();
	   	 			params.add(new ExprVar(null, par.getKey()));
	   	 		}
	    		
	    		return new ExprFunCall(null, arFun.getFullName(), params);
	    	}
	    	
	    	public Object visitExprArrayRange(ExprArrayRange exp) {
	    		final Expression newBase=getArrayBase(exp);
	    		assert newBase instanceof ExprVar;
	    		String bname = ((ExprVar) newBase).getName();
	    		if(smap.containsKey(bname)){
	    			List mem = exp.getMembers();
		    		return fa(bname, mem);
	    		}else{
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

	    private ExprVar getArrayBase(ExprArrayRange array) {
	        Expression base=array.getBase();
	        if(base instanceof ExprArrayRange) {
	        	return getArrayBase((ExprArrayRange) base);
	        }
	        assert base instanceof ExprVar: "The base of an array is expected to be a variable expression";
	        return (ExprVar) base;
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
		        String var = getArrayBase(nLHS).getName();
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
		superParams = new HashMap<String, Type>();
		conds = new Stack<Expression>();		
		scopeStack = new Stack<scopeHist>();
		smap = new HashMap<String, arrInfo>();
		this.suffix = "_" + suffix;
	}
	

    public Object visitFunction(Function func)
    {
    	ptree = (new SetupParamTree()).producePtree(func);
    	currentTN = ptree.getRoot();
        return super.visitFunction(func);
    }

}



