package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtVarDecl;


/**
 * In this formalism, an array is treated as a function from indices to values.
 * @author asolar
 *
 */
public class ArrFunction{
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
	/**
	 * These are other parameters to the function which may be used to compute the
	 * value of the loop.
	 */
	List<StmtVarDecl> othParams;
	List<Statement> idxAss;
	List<Statement> maxAss;
	List<Statement> retStmts;	
	public ArrFunction(String arrName, int idx, paramTree pt){
		this.arrName = arrName;
		this.idx = idx;
		idxParams = new ArrayList<StmtVarDecl>();
		iterParams = pt;		
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