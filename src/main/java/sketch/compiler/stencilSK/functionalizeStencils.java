package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
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
import streamit.frontend.nodes.FEVisitor;
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






class paramTree{
	
	private Map<FENode, treeNode> tnMap = new HashMap<FENode, treeNode>();
	public treeNode getTNode(FENode node){
		return tnMap.get(node);
	}
	class treeNode{
		StmtVarDecl vdecl=null;
		loopHist lh=null;
		private treeNode father=null;
		private List<treeNode> children = new ArrayList<treeNode>();
		private int pos = -1;
		private int level = -1;
		public int nchildren(){
			return children.size();
		}
		public int getLevel(){
			return level;
		}
		public treeNode getFather(){
			return father;
		}
		public treeNode child(int i){
			return children.get(i);
		}
		public treeNode(loopHist lh, treeNode father){
			this.lh = lh;
			if( lh != null){this.vdecl = lh.newVD(); }
			this.father = father;
		}
		public void add(treeNode tn){
			tn.pos = children.size();
			tn.level = level + 1;
			children.add(tn);
		}
		public PathIterator pathIter(){
			PathIterator pi = new PathIterator();			
			return pi;
		}

		public class PathIterator implements Iterator<StmtVarDecl>{
			private int[] path;
			private int pos;
			private treeNode tn;
			public PathIterator(){
				path = new int[level];
				pos = 0;
				tn = root;
				treeNode tn = treeNode.this;
				int ii=level-1;
				while(tn != root){
					path[ii] = tn.pos;
					tn = tn.father;
				}				
			}
			public StmtVarDecl next(){				
				treeNode tmp = tn.child(path[pos]);		
				tn = tmp;
				++pos;
				return tmp.vdecl;
			}
			
			public boolean hasNext(){
				return pos != path.length;
			}
			public void remove(){
				assert false;
			}
			
		}
	}
	
	
	public class FullIterator implements Iterator<StmtVarDecl>{
		private treeNode tn;
		FullIterator(){			
			tn = root;		
			if(hasNext()) next();
		}
		public StmtVarDecl next(){			
			treeNode curr = tn;		
			if( tn.nchildren() > 0 ){
				tn = tn.child(0);				
			}else{
				int pos = tn.pos;
				tn = tn.father;
				while(tn.nchildren()<= pos+1){
					if( tn == root ){tn = null;  return curr.vdecl;}
					pos = tn.pos;
					tn = tn.father;
				}
				tn = tn.child(pos+1);
			}
			return curr.vdecl;
		}		
		public boolean hasNext(){
			return tn != null; 
		}
		public void remove(){
			assert false;
		}
	}
	
	public FullIterator iterator(){
		return new FullIterator();
	}
	
	public String toString(){
		String rv = " ";
		for(FullIterator it = iterator(); it.hasNext(); ){
			rv += it.next().toString() + ", ";
		}
		return rv;
	}
	
	private treeNode cnode;
	private treeNode root;
	private int depth;
	public paramTree(){
		root = new treeNode(new loopHist("BASE", null, null), null);
		root.level = 0;
		cnode = root;
	}
	
	public treeNode beginLevel(loopHist lh, FENode node){
		treeNode tmp = beginLevel(lh);
		this.tnMap.put(node, tmp);
		if( tmp.getLevel() > depth) depth = tmp.getLevel();
		return tmp;
	}
	public treeNode beginLevel(loopHist lh){
		treeNode tmp = new treeNode(lh, cnode);
		cnode.add(tmp);
		cnode = tmp;
		return tmp;
	}
	
	public treeNode getRoot(){
		return root;
	}
	public void endLevel(){
		cnode = cnode.father;
	}	
	
	
}



class scopeHist{
	/**
	 * funs tracks all the arrays that defined at this scope.
	 * Note that if there are nested loops, arrays modified at deeper
	 * levels of nesting will not be registered in this funs, but in the one corresponding
	 * to the nested scopes.
	 */	
	List<arrFunction> funs;
	scopeHist(){
		funs = new ArrayList<arrFunction>();
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
	arrFunction fun;
	Stack<arrFunction> sfun = new Stack<arrFunction>();
}


/**
 * In this formalism, an array is treated as a function from indices to values.
 * @author asolar
 *
 */
class arrFunction{
	String arrName;
	int idx;
	/**
	 * These parameters correspond to the indices of the array.
	 * They are the "REAL" parameters.
	 */
	List<StmtVarDecl> idxParams;
	/**
	 * These parameters correspond to the loop iteration that we care about.
	 * They have default values corresponding to the last iteration of the loop.
	 */
	paramTree iterParams;
	List<StmtVarDecl> posIterParams;
	/**
	 * These are other parameters to the function which may be used to compute the
	 * value of the loop.
	 */
	List<StmtVarDecl> othParams;
	List<Statement> idxAss;
	List<Statement> maxAss;
	List<Statement> retStmts;	
	public arrFunction(String arrName, int idx, paramTree pt){
		this.arrName = arrName;
		this.idx = idx;
		idxParams = new ArrayList<StmtVarDecl>();
		iterParams = pt;
		posIterParams = new ArrayList<StmtVarDecl>();
		othParams = new ArrayList<StmtVarDecl>();
		idxAss = new ArrayList<Statement>();
		maxAss = new ArrayList<Statement>();
		retStmts = new ArrayList<Statement>();		
	}
	
	public String getFullName(){
		return arrName + "_" + idx;
	}
	
	public void close(){
		//This method signals that from here on,
		//the function will not be modified again.
	}
	
	public String toString(){
		String rv = getFullName();
		rv += "(" + idxParams + ", " + iterParams + ", " + othParams + "){\n";
		for(Iterator<Statement> it = idxAss.iterator(); it.hasNext(); ){
			rv += it.next().toString() + ";\n";
		}
		for(Iterator<Statement> it = maxAss.iterator(); it.hasNext(); ){
			rv += it.next().toString() + ";\n";
		}
		for(Iterator<Statement> it = retStmts.iterator(); it.hasNext(); ){
			rv += it.next().toString() + ";\n";
		}
		rv += "}";
		return rv;	
	}
}

/**
 * Represents a max statement of the form:
 * int[dim] lhsvar = max{ primPred(lhsvar) && secPred(lhsvar) };
 */
class StmtMax extends Statement{
	int dim;
	String lhsvar;	
	/**
	 * Primary constraints.
	 * They are of the form expr(idx)==t
	 */
	List<Expression> primC;
	/**
	 * Secondary constraints.
	 * They are of the form idx < something
	 */
	List<Expression> secC;
	/**
	 * Tertiary constraints.
	 * They are pretty arbitrary.
	 */
	List<Expression> terC;
	public Object accept(FEVisitor visitor){
		assert false;
		return null;
	}
	StmtMax(int dim, String lhsvar){super(null);
		this.dim = dim;
		this.lhsvar = lhsvar;
		this.primC = new ArrayList<Expression>();
		this.secC = new ArrayList<Expression>();
		this.terC = new ArrayList<Expression>();
	}
	String getVar(){ assert false; return null;}
	public String toString(){
		String rv = "int[" + dim + "] " + lhsvar + "= max{";
		for(Iterator<Expression> eit = primC.iterator(); eit.hasNext(); ){
			rv += eit.next().toString() + " & ";
		}
		for(Iterator<Expression> eit = secC.iterator(); eit.hasNext(); ){
			rv += eit.next().toString() + " & ";
		}
		for(Iterator<Expression> eit = terC.iterator(); eit.hasNext(); ){
			rv += eit.next().toString() + " & ";
		}
		rv += "}";
		return rv;
	}
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

public class functionalizeStencils extends FEReplacer {
	List<processStencil> stencilFuns;
	Map<String, Type> superParams;
	public functionalizeStencils() {
		super();
		stencilFuns = new ArrayList<processStencil>();
		superParams = new HashMap<String, Type>();
	}
	 public Object visitFunction(Function func)
	    {
		 	processStencil ps = new processStencil();
		 	ps.setSuperParams(superParams);
		 	stencilFuns.add(ps);
		 	func.accept(ps);	        
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
	paramTree.treeNode currentTN;
	Stack<scopeHist> scopeStack;
	Map<String, arrInfo> smap;
	paramTree ptree;
	
	class SetupParamTree extends FEReplacer{
		paramTree ptree;
		public paramTree producePtree(Function func)
	    {
			ptree = new paramTree();
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
		for(Iterator<arrFunction> it = sc2.funs.iterator(); it.hasNext();  ){
			 arrFunction t = it.next();
			 t.close();
			 System.out.println(t);
			 smap.get(t.arrName).fun=null;
		 }
	}
	
	
	 public void processForLoop(StmtFor floop, String indVar, Expression exprStartPred, Expression exprEndPred, Statement body, boolean direction){
		 currentTN = ptree.getTNode(floop);
		 loopHist lh = currentTN.lh;
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
   	 		ainf.fun = new arrFunction(var, ainf.sfun.size(), ptree);
   	 		for(int i=0; i<dim; ++i) ainf.fun.idxParams.add( newVD("t"+i, null) );
   	 		
   	 		for(Iterator<Entry<String, Type>> pIt = superParams.entrySet().iterator(); pIt.hasNext(); ){
   	 			Entry<String, Type> par = pIt.next();
   	 			ainf.fun.othParams.add(new StmtVarDecl(null, par.getValue(), par.getKey(), null));
   	 		}
   	 		int sz = ainf.sfun.size();
   	 		ainf.fun.retStmts.add( nullMaxIf(sz>0 ? ainf.sfun.peek():null));
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
	    public Statement nullMaxIf(arrFunction prevFun){	    	
	    	Expression cond = new ExprBinary(null, ExprBinary.BINOP_EQ, new ExprVar(null, "max_idx"), new ExprVar(null, "null") );
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
	    		rval = new StmtReturn(null, new ExprFunCall(null, prevFun.getFullName(), params) );
	    	}else{
	    		rval = new StmtReturn(null, new ExprConstInt(0) );
	    	}	    	
	    	return new StmtIfThen(null, cond, rval, null); 
	    	//   "if( max_idx == null ) return prevFun;" 
	    }
	    
	    public Expression buildSecondaryConstr(Iterator<StmtVarDecl> iterIt, String newVar, int jj){
	    	assert iterIt.hasNext();
	    	StmtVarDecl iterPar = iterIt.next();
    		ExprArray ear = new ExprArray(null, new ExprVar(null, newVar), new ExprConstInt(jj));
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
	    
	    
	    public StmtMax newStmtMax(int i, List<Expression> indices, arrFunction fun){	
	    	assert indices.size() == fun.idxParams.size();
	    	String newVar = "idx_" + i;
	    	
	    	//"idx_i := max{expr1==t, idx < in_idx, conds }; "
	    	int ii=0;
	    	StmtMax smax = new StmtMax(currentTN.getLevel(), newVar);
	    	// First we add the primary constraints.
	    	// indices[k][fun.iterParam[j] -> idx_i[j]]==fun.idxParams[k]
	    	for(Iterator<StmtVarDecl> idxIt = fun.idxParams.iterator(); idxIt.hasNext(); ii++){
	    		StmtVarDecl idxPar = idxIt.next();	   
	    		Expression cindex = indices.get(ii);
	    		int jj=0;
	    		for(Iterator<StmtVarDecl> iterIt = currentTN.pathIter(); iterIt.hasNext(); jj++){
	    			StmtVarDecl iterPar = iterIt.next();
	    			ExprArray ear = new ExprArray(null, new ExprVar(null, newVar), new ExprConstInt(jj));
	    			cindex = (Expression) cindex.accept(new VarReplacer(iterPar.getName(0), ear ));
	    		}
	    		cindex = new ExprBinary(null, ExprBinary.BINOP_EQ, cindex, new ExprVar(null, idxPar.getName(0)));
	    		smax.primC.add(cindex);	    		
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
	    			ExprArray ear = new ExprArray(null, new ExprVar(null, newVar), new ExprConstInt(jj));
	    			cond = (Expression) cond.accept(new VarReplacer(iterPar.getName(0), ear ));
	    		}
	    		smax.terC.add(cond);
	    	}
	    	return smax;
	    }
	    
	    public Statement pickLargest(int i, int dim){
	    	//"max_idx = max(idx_i|{(stack-smap[x].stack_beg).stage}, max_idx);" 
	    	ExprVar mvar = new ExprVar(null, "max_idx");
	    	List<Expression> param = new ArrayList<Expression>(2);
	    	param.add(mvar);
	    	param.add(new ExprVar(null, "idx_" + i));
	    	ExprFunCall fc = new ExprFunCall(null, "max", param);
    		return new StmtAssign(null, mvar, fc);
	    }
	    
	    /**
	     * This class replaces arrays with their corresponding function representations.
	     * expr2[x[l]->x_fun(l, idx_i)]
	     * @author asolar
	     *
	     */
	    class ArrReplacer extends FEReplacer{
	    	arrFunction fun;
	    	ExprVar idxi;
	    	public ArrReplacer(arrFunction fun, ExprVar idxi){
	    		this.fun = fun;
	    		this.idxi = idxi;
	    	}
	    	
	    	void setIterPosIterParams(arrFunction callee,arrFunction caller,List<Expression> params){
	    		//TODO need to modify this method to take into account also the posIter parameters.
	    		Iterator<StmtVarDecl> globIter = ptree.iterator();
	    		Iterator<StmtVarDecl> locIter = currentTN.pathIter();
	    		
	    		StmtVarDecl loc = locIter.hasNext()? locIter.next() : null;
	    		int ii=0;
	    		while(globIter.hasNext()){
	    			StmtVarDecl par = globIter.next();
	    			if( par == loc){
	    				ExprArray ear = new ExprArray(null,idxi, new ExprConstInt(ii));
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
	    		arrFunction arFun = ainf.sfun.peek();
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
	    		final Expression newBase=exp.getBase();
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
	    
	    
	    
	    public Statement iMaxIf(int i, Expression rhs, arrFunction fun){
	    	//if(max_idx == idx_i){ return rhs; }
	    	ExprVar maxV = new ExprVar(null, "max_idx");
	    	ExprVar idxi = new ExprVar(null, "idx_" + i);
	    	Expression eq = new ExprBinary(null, ExprBinary.BINOP_EQ, maxV, idxi);
	    	int ii=0;
	    	for(Iterator<StmtVarDecl> it = currentTN.pathIter(); it.hasNext(); ++ii){
	    		StmtVarDecl par = it.next();	    		
	    		ExprArray ear = new ExprArray(null,idxi, new ExprConstInt(ii));
	    		rhs = (Expression)rhs.accept(new VarReplacer(par.getName(0), ear));
	    	}
	    	
	    	Expression retV = (Expression)rhs.accept(new ArrReplacer(fun, idxi));	    	
	    	return new StmtIfThen(rhs.getContext(), eq, new StmtReturn(null, retV), null); 
	    }
	   
	    
	    public void processArrAssign(String var, List<Expression> indices, Expression rhs){
	    	assert smap.containsKey(var);
	    	arrInfo ainf = smap.get(var);	    	
	    	int dim = indices.size();
	   	 	assert ( ainf.fun != null);
	   	 	arrFunction fun = ainf.fun;
	   	 	int i = fun.idxAss.size();
	   	 	fun.idxAss.add( newStmtMax(i, indices, fun) );	   	 	
	   	 	fun.maxAss.add( pickLargest(i, dim) );
	   	 	fun.retStmts.add( iMaxIf(i, rhs, fun)  );
	   	 	++(currentTN.lh.stage);
	    }

	    public Object visitStmtAssign(StmtAssign stmt)
	    {
	        Expression lhs = stmt.getLHS();
	        Expression rhs = stmt.getRHS();
	        if( lhs instanceof ExprArrayRange ){
		        assert lhs instanceof ExprArrayRange ;	        
		        ExprArrayRange nLHS = (ExprArrayRange) lhs;
		        assert nLHS.getBase() instanceof ExprVar;
		        String var = ((ExprVar) nLHS.getBase() ).getName();
		        Iterator it = nLHS.getMembers().iterator();
		        List<Expression> indices = new ArrayList<Expression>(nLHS.getMembers().size());
		        while(it.hasNext()){
		        	Object o = it.next();
		        	assert o instanceof RangeLen;
		        	Expression exp = ((RangeLen) o).start();
		        	indices.add(exp);
		        }
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
	
	
	public processStencil() {
		super();
		superParams = new HashMap<String, Type>();
		conds = new Stack<Expression>();		
		scopeStack = new Stack<scopeHist>();
		smap = new HashMap<String, arrInfo>();	
	}
	

    public Object visitFunction(Function func)
    {
    	ptree = (new SetupParamTree()).producePtree(func);
    	currentTN = ptree.getRoot();
        return super.visitFunction(func);
    }

}



