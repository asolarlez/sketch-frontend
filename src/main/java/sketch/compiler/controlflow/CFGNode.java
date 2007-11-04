/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.frontend.controlflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.Statement;



/**
 * A single-statement or single-expression node in a control flow graph.
 * This class holds at most one expression or one statement; if it
 * holds an expression, that expression must be associated with a
 * statement.  A node can be designated <i>empty</i>, in which case
 * it is a special node used to designate entry or exit from the
 * composite statement identified in the statement.  Otherwise, if its
 * expression is non-null, the node is a conditional node, and the
 * expression must be boolean-valued.  Otherwise, the node is a statement
 * node.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class CFGNode
{
	public static class EdgePair{
		public final CFGNode node;
		public final Integer label;
		public EdgePair(CFGNode node, Integer label){
			this.node = node;
			this.label = label;
		}
		public String toString(){
			return "->" + node.toString() + ((label == null)?"": "[" + label + "]");
		}
	}
	
    private final boolean empty;
    private Statement stmt;
    private Statement preStmt = null;
    private Expression expr;
    private int id;
    private boolean special = false;
    private final List<CFGNode> preds = new ArrayList<CFGNode>();
    private final List<EdgePair> succs = new ArrayList<EdgePair>();
    
    public void makeSpecial(){
    	special = true;
    }
    public boolean isSpecial(){
    	return special;
    }
    
    
    public void changeStmt(Statement stmt){
    	assert isStmt();
    	this.stmt = stmt;    	
    }
    
    public void changeExpr(Expression expr){
    	assert !isEmpty();
    	this.expr = expr;
    }
    
    
    public void setPreStmt(Statement s){
    	preStmt = s;
    }
    
    public Statement getPreStmt(){
    	return preStmt;
    }
    
    // can't both be empty and have an expression.
    private CFGNode(Statement stmt, Expression expr, boolean empty)
    {
        this.stmt = stmt;
        this.expr = expr;
        this.empty = empty;
    }

    /**
     * Create a statement node.
     *
     * @param stmt  Statement associated with the node
     */
    public CFGNode(Statement stmt)
    {
        this(stmt, null /* expr */, false /* empty */);
    }

    /**
     * Create either a placeholder or a statement node.
     *
     * @param stmt  Statement associated with the node
     * @param empty true if this is a placeholder node, false if this is
     *              a statement node
     */
    public CFGNode(Statement stmt, boolean empty)
    {
        this(stmt, null /* expr */, empty);
    }
    
    /**
     * Create an expression node.
     *
     * @param stmt  Statement associated with the node
     * @param expr  Expression associated with the node
     */
    public CFGNode(Statement stmt, Expression expr)
    {
        this(stmt, expr, false /* empty */);
    }
    
    /**
     * Determine if this node is a placeholder node.
     * If so, the statement associated with the node identifies a
     * composite statement, such as a loop, that this is a header
     * or footer for.
     *
     * @return true if this is an empty (placeholder) node
     */
    public boolean isEmpty()
    {
        return empty;
    }

    /**
     * Determine if this node is an expression node.
     * If so, the statement associated with the node identifies
     * a branch statement, such as a loop or if statement, that
     * this is the condition for.
     *
     * @return true if this is an expression (conditional) node
     */
    public boolean isExpr()
    {
        return expr != null;
    }
    
    /**
     * Determine if this node is a statement node.
     *
     * @return true if this is a statement node
     */
    public boolean isStmt()
    {
        return !empty && expr == null;
    }
    
    /**
     * Get the expression associated with an expression node.
     * Returns <code>null</code> if this is not an expression
     * node; in that case, it is either a statement or a
     * placeholder node.
     *
     * @return  expression associated with the node, or null
     */
    public Expression getExpr()
    {
        return expr;
    }
    
    
    public FEContext getCx(){
    	if(expr != null)
    		return expr.getCx();
    	if(stmt != null)
    		return stmt.getCx();
    	return null;
    }
    
    /**
     * Get the statement associated with a node.  Every node has
     * a statement associated with it: for a placeholder node,
     * the statement identifies the containing loop, and for an
     * expression node, the statement identifies the statement
     * containing the expression.
     *
     * @return  statement associated with the node
     */
    public Statement getStmt()
    {
        return stmt;
    }

	/**
	 * @param id the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}
	
	public String toString(){
		if(expr != null){
			String rv = id + ":" ;
			if(preStmt != null){
				rv += preStmt.toString();
			}
			rv += "[" + expr.toString() +"]";
			return rv;
		}else{
			if(!empty && stmt != null){
				return id + ":" + stmt.toString();
			}else{
				return id + ": empty";
			}
		}
	}

	/**
	 * @param preds the preds to set
	 */
	public void addPreds(List<CFGNode> preds) {
		this.preds.addAll(preds);
	}
	
	public void addPred(CFGNode pred) {
		this.preds.add(pred);
	}
		
	public void removeFromChildren(){
		for(Iterator<EdgePair> eit = succs.iterator(); eit.hasNext(); ){
			EdgePair ep = eit.next();
			ep.node.removePred(this);
		}
	}
	
	public void checkNeighbors(){
		CFGNode n = this;
		List<CFGNode> preds = n.getPreds();
		for(int i=0; i<preds.size(); ++i){
			CFGNode p = preds.get(i);
			List<EdgePair> psuc = p.getSuccs();
			boolean found  =false;
			for(int t=0; t<psuc.size(); ++t){
				if( psuc.get(t).node == n ){
					found = true;
					break;
				}
			}
			assert found : "I am not a successor of my predecessor";
		}
		
		List<EdgePair> succs = n.getSuccs();
		for(int i=0; i<succs.size(); ++i){
			CFGNode s = succs.get(i).node;
			if(succs.get(i).label == null){
				assert n.isStmt();
			}else{
				assert n.isExpr();
				assert succs.get(i).label == 0 || succs.get(i).label == 1;
			}
			List<CFGNode> spred = s.getPreds();
			boolean found  =false;
			for(int t=0; t<spred.size(); ++t){
				if(spred.get(t) == n){
					found = true;
					break;
				}
			}
			assert found : "I am not a predecessor of my successor";
		}
		
	}
	
	
	/**
	 * Returns true if this node has become unreachable.
	 * @param oldP
	 * @return
	 */
	public boolean removePred(CFGNode oldP){
		int sz = preds.size();
		this.preds.remove(oldP);
		return sz > 0 && preds.size() == 0;
	}
	
	public boolean removeAllPred(CFGNode oldP){
		int sz = preds.size();
		while(this.preds.remove(oldP)){};
		return sz > 0 && preds.size() == 0;
	}
	
	public void removeSucc(CFGNode oldS){
		for(int i=0; i<succs.size(); ++i){
			EdgePair ep = succs.get(i);
			if(ep.node == oldS){
				succs.remove(i);				
			}			
		}	
	}
	
	public void changePred(CFGNode oldS ,CFGNode newS){		
		for(int i=0; i<preds.size(); ++i){
			if(preds.get(i) == oldS){
				preds.set(i, newS);
			}
		}
	}
	
	public void changeSucc(CFGNode oldS ,CFGNode newS){		
		for(int i=0; i<succs.size(); ++i){
			EdgePair ep = succs.get(i);
			if(ep.node == oldS){
				succs.set(i, new EdgePair(newS, ep.label));				
			}			
		}		
	}

	/**
	 * @return the preds
	 */
	public List<CFGNode> getPreds() {
		return preds;
	}

	/**
	 * @param succs the succs to set
	 */
	public void addSuccs(List<EdgePair> succs) {
		this.succs.addAll(succs);
	}
	/**
	 * @param succs the succs to set
	 */
	public void addSucc(EdgePair succ) {
		this.succs.add(succ);
	}

	/**
	 * @return the succs
	 */
	public List<EdgePair> getSuccs() {
		return succs;
	}
	
}
