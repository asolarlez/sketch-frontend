package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprBinary;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Parameter;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;
import streamit.frontend.nodes.TypePrimitive;


/**
 * In this formalism, an array is treated as a function from indices to values.
 * @author asolar
 *
 */

//TODO add position parameters.
public class ArrFunction{
	public static final String IPARAM = "__t";
	public static final String MAX_VAR = "max_idx";
	public static final String IDX_VAR = "idx_";
	public static final String GUARD_VAR = "gv_";
	public static final String IND_VAR = "_ii_";
	public static final String PPPREFIX = "pp_";
	public static final ExprVar NULL = new ExprVar((FEContext) null, "null");
	////////////////////////////////////////////////////////////////

	String arrName;
	String suffix;
	Type arrType;
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

	ParamTree.treeNode declarationSite;
	
	final List<Expression> dimensions;
	
	/**
	 * For the array function, we only need to care about conditionals that are between the use and the declaration site.
	 * Conditionals that are outside the declaration site are irrelevant because no def-use chain involving this variable will cross that
	 * conditional. 
	 * 
	 * The condsPos field remembers the size of the condition stack when this variable is declared, so that we only consider conditions up to that position.
	 * 
	 * */
	int condsPos = -1;

	private boolean isClosed = false;
	
	List<StmtVarDecl> inputParams;


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

	public ArrFunction(String arrName, Type arrType, List<Expression> dimensions, String suffix, ParamTree pt, ParamTree.treeNode declarationSite, int condsPos){
		this.arrName = arrName;
		this.dimensions = dimensions;
		idxParams = new ArrayList<StmtVarDecl>();
		iterParams = pt;
		othParams = new ArrayList<StmtVarDecl>();
		idxAss = new ArrayList<StmtMax>();
		maxAss = new ArrayList<Statement>();
		retStmts = new ArrayList<Statement>();
		this.suffix = suffix;
		this.arrType = arrType;
		this.declarationSite = declarationSite;
		this.condsPos = condsPos;
	}

	public String getFullName(){
		return arrName + suffix;
	}

	public boolean isClosed(){
		return isClosed;
	}
	
	/**
	 * This method signals that from here on,
	the function will not be modified again.
	*/
	public void close(){
		isClosed = true;		
	}

	public String toString(){
		String rv = getFullName();
		rv += "(" + idxParams + ", " + iterParams + ", " + othParams +  ", " + inputParams +  "){\n";
		for(Iterator<StmtMax> it = idxAss.iterator(); it.hasNext(); ){
			rv += it.next().toString() + ";\n";
		}
		rv += " int [" + max_size + "] " + MAX_VAR  + " = 0;\n";
		rv += " int " + IND_VAR  + " = 0;\n";
		for(Iterator<Statement> it = maxAss.iterator(); it.hasNext(); ){
			rv += it.next().toString() + ";\n";
		}

		for(Iterator<Statement> it = retStmts.iterator(); it.hasNext(); ){
			rv += it.next().toString() + ";\n";
		}
		rv += "}";
		return rv;
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

	private void addIndexCheck(List<Statement> stmts){
		
		assert idxParams.size() == dimensions.size() : "Type missmatch";
		Iterator<StmtVarDecl> svit = idxParams.iterator();
		Iterator<Expression> eit = dimensions.iterator();
		while(eit.hasNext()){
			StmtVarDecl vd = svit.next();
			Expression bound = eit.next();
			if(bound != null){
				String idxName = vd.getName(0);
				Expression idx = new ExprVar(vd,idxName);
				String errmsg = "Index " + idxName + " is out of bounds for array " + arrName;
				stmts.add(new StmtAssert(new ExprBinary(idx, "<", bound  ), errmsg, false));
				stmts.add(new StmtAssert(new ExprBinary(idx, ">=", ExprConstInt.zero),  errmsg, false));
			}
		}
	}
	
	public Function toAST() {
		List<Parameter> params=new ArrayList<Parameter>();
		{
			params.addAll(makeParams(idxParams));
			params.addAll(makeParams(iterParams.iterator()));
			params.addAll(makeParams(othParams));
			params.addAll(makeParams(inputParams));
		}
		List<Statement> stmts=new ArrayList<Statement>();
		
		addIndexCheck(stmts);
		
		
		{
			for(Iterator<StmtMax> it = idxAss.iterator(); it.hasNext(); ){
				Statement stmt=it.next().toAST();
				assert !(stmt instanceof StmtMax);
				if(stmt instanceof StmtBlock) {
					stmts.addAll(((StmtBlock)stmt).getStmts());
				}
				else
					stmts.add(stmt);
			}
			stmts.add(new StmtVarDecl((FENode)null,
				new TypeArray(TypePrimitive.inttype, new ExprConstInt(max_size)),
				MAX_VAR, new ExprConstInt(0)));
			stmts.add(new StmtVarDecl((FENode) null,
					TypePrimitive.bittype,
					IND_VAR, new ExprConstInt(1)));
			for(int i=0; i<maxAss.size(); ++i){
				stmts.add(new StmtVarDecl((FENode) null,
						TypePrimitive.bittype,
						IND_VAR+i, new ExprConstInt(0)));
			}
			stmts.addAll(maxAss);
			stmts.addAll(retStmts);
		}
		Statement body=new StmtBlock(stmts);

		Function ret=Function.newHelper(body,getFullName(), arrType,params,body);
		return ret;
	}

	/**
	 * This method runs the symbolic solver on the max statements in the
	 * ArrFunction to replace them with sequences of guarded assignments.
	 *
	 */
	public void processMax(){
		for(Iterator<StmtMax> it = idxAss.iterator(); it.hasNext(); ){
			StmtMax smax = it.next();
			smax.resolve();
		}
	}
}