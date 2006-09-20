package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.*;


/**
 * In this formalism, an array is treated as a function from indices to values.
 * @author asolar
 *
 */
public class ArrFunction{
	public static final String MAX_VAR = "max_idx";
	public static final String IDX_VAR = "idx_";
	public static final String GUARD_VAR = "gv_";
	public static final String IND_VAR = "ii";
	public static final ExprVar NULL = new ExprVar(null, "null");
	////////////////////////////////////////////////////////////////
	
	String arrName;
	String suffix;
	int idx;
	int max_size=0;
	/**
	 * These parameters correspond to the indices of the array.
	 * They are the "REAL" parameters.
	 */
	List<StmtVarDecl> idxParams;
	/**
	 * These parameters correspond to the loop iteration that we care about.
	 * They have default values corresponding to the last iteration of the loop.
	 */
	ParamTree iterParams;	
	/**
	 * These are other parameters to the function which may be used to compute the
	 * value of the loop.
	 */
	List<StmtVarDecl> othParams;
	
	private List<StmtMax> idxAss;
	private List<Statement> maxAss;	
	private List<Statement> retStmts;
	
	public void addIdxAss(StmtMax sm){
		idxAss.add(sm);
		if(sm.dim > max_size) max_size = sm.dim;
	}
	
	public void addMaxAss(Statement ma){
		maxAss.add(ma);
	}
	
	public void addRetStmt(Statement rs){
		retStmts.add(rs);
	}
	
	public int size(){
		return idxAss.size();
	}
	
	public ArrFunction(String arrName, String suffix,  int idx, ParamTree pt){
		this.arrName = arrName;
		this.idx = idx;
		idxParams = new ArrayList<StmtVarDecl>();
		iterParams = pt;		
		othParams = new ArrayList<StmtVarDecl>();
		idxAss = new ArrayList<StmtMax>();
		maxAss = new ArrayList<Statement>();
		retStmts = new ArrayList<Statement>();	
		this.suffix = suffix;
	}
	
	public String getFullName(){
		return arrName + "_" + idx + suffix;
	}
	
	public void close(){
		//This method signals that from here on,
		//the function will not be modified again.
	}
	
	public String toString(){
		String rv = getFullName();
		rv += "(" + idxParams + ", " + iterParams + ", " + othParams + "){\n";
		for(Iterator<StmtMax> it = idxAss.iterator(); it.hasNext(); ){
			rv += it.next().toString() + ";\n";
		}
		rv += " int [" + max_size + "] " + MAX_VAR  + " = 0;\n";
		for(Iterator<Statement> it = maxAss.iterator(); it.hasNext(); ){
			rv += it.next().toString() + ";\n";
		}
		
		for(Iterator<Statement> it = retStmts.iterator(); it.hasNext(); ){
			rv += it.next().toString() + ";\n";
		}
		rv += "}";
		return rv;	
	}
	
	public Function toAST() {
		List<StmtVarDecl> params=new ArrayList<StmtVarDecl>();
		{
			params.addAll(idxParams);
			for(Iterator<StmtVarDecl> it=iterParams.iterator();it.hasNext();) {
				StmtVarDecl par=it.next();
				List types=new ArrayList(par.getTypes());
				List names=new ArrayList(par.getNames());
				List inits=new ArrayList(par.getNumVars());
				for(int i=0;i<par.getNumVars();i++) inits.add(null);
				params.add(new StmtVarDecl(par.getContext(),types,names,inits));
			}
			params.addAll(othParams);
		}
		List<Statement> stmts=new ArrayList<Statement>();
		{
			stmts.addAll(maxAss);
			stmts.addAll(retStmts);
		}
		Statement body=new StmtBlock(null,stmts);
		Function ret=Function.newHelper(null,getFullName(),new TypePrimitive(TypePrimitive.TYPE_VOID),params,body);
		return ret;
	}
	
	public void processMax(){
		for(Iterator<StmtMax> it = idxAss.iterator(); it.hasNext(); ){
			StmtMax smax = it.next();
			smax.resolve();
		}
	}
}