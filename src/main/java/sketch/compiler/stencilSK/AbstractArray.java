package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TypePrimitive;

/**
 * 
 * This class represents an abstracted dense array.
 * We are assuming that output out[idx] is computed from
 * inputs { in[ex1(idx)], in[ex2(idx)]...,in[exn(idx)] }.
 * The expressions are stored in the idxArr class.
 * 
 * The function will have parameters in the following order.
 * ( index parameters, outIndexParameters, symbolic parameters, otherParams, globalParameters).
 * There are exactly dim index parameters.
 * The global parameters include the global index parameters.
 * 
 * @author asolar
 *
 */


public class AbstractArray {
	public static String IDXNAME="i";
	public static String outIndexParamName(int i){
		return "OUT_" + IDXNAME + "_" + i;
	}
	String arrName;
	String suffix;
	int dim;
	
	/**
	 * This class represents the different expressions that can index this array in the 
	 * spec.
	 */
	List<Expression[] > idxArr;
	
	
	/**
	 * They correspond to the current index of the output.
	 * These are here because remember we are looking for indices relative
	 * to the output indices, so we need to know what the output indices are.  
	 */
	List<StmtVarDecl> outIndexParameters;
	
	/**
	 * Other parameters used in the expressions.  
	 */
	List<StmtVarDecl> otherParams; 
	
	/**
	 * Global parameters used in the expressions.  
	 */
	List<StmtVarDecl> globalParams; 
	
	public void addAssignStruct(AssignStruct ass, List<Expression> indices){
		/*
		 * I need to get the output indices from the assign structure,
		 * and then replace them with the outIndexParameters in each
		 * of the indices. 
		 */
		
		assert indices.size() == this.dim;		
		assert ass.indices.size() == outIndexParameters.size() : " This is because we require the lhs to be equal tou out[i,j,k]";
		Expression[] expr = new Expression[dim];
		int i=0;
		for(Iterator<Expression> inIdxIt = indices.iterator(); inIdxIt.hasNext(); ++i){
			Expression inIdx = inIdxIt.next();
			Iterator<StmtVarDecl> outIdxNewIt = outIndexParameters.iterator();
			for(Iterator<Expression> outIdxOldIt = ass.indices.iterator(); outIdxOldIt.hasNext(); ){
				Expression outIdxOld = outIdxOldIt.next();
				assert outIdxOld instanceof ExprVar : "Currently, it is assumed that the spec only has assignments of the form " 
														+ "out[i,j,k] = blah; so the ori indices must be single variables.";
				ExprVar outIdxOldVar = (ExprVar) outIdxOld;
				StmtVarDecl svd = outIdxNewIt.next();

				inIdx = (Expression)  inIdx.accept(new VarReplacer(outIdxOldVar.getName(), new ExprVar(null, svd.getName(0))));
			}
			expr[i] = inIdx;
		}
		//TODO : Still need ot add the statements corresponding to the dependencies of the AssignStruct.
		idxArr.add(expr);
		
	}
	
	public void makeDefault(int vars){
		for(int i=0; i<vars; ++i){
			Expression[] expr = new Expression[dim];
			idxArr.add( expr );
			String vname = symParamName(i);
			for(int j=0; j<dim; ++j){
				String lvn = vname + "_" + j;
				StmtVarDecl op = new StmtVarDecl(null, TypePrimitive.inttype, lvn , null);
				otherParams.add(op);
				//Note that we are adding stuff to the global parameters.
				expr[ j ] = new ExprVar(null ,lvn);
			}
		}
	}
	
	
	AbstractArray(String arrName, String suffix, int dim, List<StmtVarDecl> globalParams, List<StmtVarDecl> outIdxParams){
		this.arrName = arrName;
		this.suffix = suffix;
		this.dim = dim;
		this.globalParams = globalParams;
		this.outIndexParameters = outIdxParams;
		this.idxArr = new ArrayList<Expression[]>();
		this.otherParams = new ArrayList<StmtVarDecl>();
	}
	
	
	public void addParamsToFunction(List<StmtVarDecl> svd){
		
		for(int i=0; i<numSymParams(); ++i){
			StmtVarDecl obj = new StmtVarDecl(null, TypePrimitive.inttype, symParamName(i) , null) ;
			svd.add(obj);
		}
		for(Iterator<StmtVarDecl> it = otherParams.iterator(); it.hasNext(); ){
			svd.add(  it.next() );			
		}
	}
	
	public String getFullName(){
		return arrName + "_" + suffix;
	}
	
	public String symParamName(int i){
		return getFullName() + "_" + i;
	}
	
	public int numSymParams(){
		return idxArr.size();
	}
	
	public String toString(){
		String s = arrName + suffix + "( ";
		String body="";
		//( index parameters, outIndexParameters, symbolic parameters, otherParams, globalParameters).
		for(int i=0; i<dim; ++i){
			s += "int " + IDXNAME + i + ", ";			
		}
		
		for(Iterator<StmtVarDecl> it = outIndexParameters.iterator(); it.hasNext(); ){
			s +=  it.next() + ", ";	
		}
		
		
		
		String fullName = getFullName();
		int i=0;
		for(Iterator<Expression[]> it = idxArr.iterator(); it.hasNext(); ++i ){
			String spname = symParamName(i);
			s += "int " +  spname;
			Expression[] earr = it.next();
			s += ", ";
			body += "if ( " ;
			for(int j=0; j<dim; ++j){
				body += IDXNAME + j + "==" + earr[j];
				if( j != dim-1) body += " && ";
			}
			body += ") return "  + spname + "; \n";			
		}
		
		for(Iterator<StmtVarDecl> it = otherParams.iterator(); it.hasNext(); ){
			s += it.next() + ", ";	
		}
		
		for(Iterator<StmtVarDecl> it = globalParams.iterator(); it.hasNext(); ){
			s += it.next() + ", ";	
		}
		
		s += "){ \n" + body + "}\n"; 		
		return s;
	}
	

}
