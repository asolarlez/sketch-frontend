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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import streamit.frontend.controlflow.CFGNode.EdgePair;

/**
 * A control-flow graph.  This has a set of nodes and a set of edges.
 * An entry and exit node are designated.  This representation is
 * immutable: converting from arbitrary control flow back to our
 * high-level statement representation can be tricky, especially when
 * things like for loops are involved.  The {@link CFGBuilder} class
 * can be used to build control-flow graphs from straight-line code.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class CFG
{
    private List<CFGNode> nodes;
    private CFGNode entry, exit;
    
    /**
     * Create a new control-flow graph (or fraction thereof).
     *
     * @param nodes  list of {@link CFGNode}
     * @param entry  specific node that is the entry node
     * @param exit   specific node that is the exit node
     * @param edges  mapping of from-node to to-node making up
     *               edges in the graph
     */
    public CFG(List<CFGNode> nodes, CFGNode entry, CFGNode exit, Map<CFGNode, List<CFGNode.EdgePair>> edges)
    {
        this.nodes = nodes;
        this.entry = entry;
        this.exit = exit;
        
        for(Iterator<CFGNode> nit = nodes.iterator(); nit.hasNext(); ){
        	CFGNode n = nit.next();
        	n.getSuccs().clear();
        	n.getPreds().clear();
        }
        
        for(Iterator<Entry<CFGNode, List<CFGNode.EdgePair>>> edgeIt = edges.entrySet().iterator(); edgeIt.hasNext(); ){
        	Entry<CFGNode, List<CFGNode.EdgePair>> elist = edgeIt.next();        	
        	elist.getKey().addSuccs(elist.getValue());
        	
        	for(Iterator<CFGNode.EdgePair> succIt = elist.getValue().iterator(); succIt.hasNext();  ){
        		CFGNode succ  = succIt.next().node;
        		succ.addPred(elist.getKey());
        	}        	
        }        
    }

    public CFG(List<CFGNode> nodes, CFGNode entry, CFGNode exit)
    {
        this.nodes = nodes;
        this.entry = entry;
        this.exit = exit;
    }
    
    
    /**
     * Get the list of nodes.
     *
     * @return  list of nodes
     */
    public List<CFGNode> getNodes()
    {
        return nodes;
    }
    
    public int size(){
    	return nodes.size();
    }
    
    /**
     * Get the entry node.
     *
     * @return  the entry node
     */
    public CFGNode getEntry()
    {
        return entry;
    }
    
    /**
     * Get the exit node.
     *
     * @return  the exit node
     */
    public CFGNode getExit()
    {
        return exit;
    }

    /**
     * Get the list of edges exiting a particular node.
     *
     * @param node  node to query
     * @return      list of {@link CFGNode} exiting that node
     */
    public List<EdgePair> getSuccessors(CFGNode node)
    {        
        return node.getSuccs();
    }

    /**
     * Get the list of edges entering a particular node.
     *
     * @param node  node to query
     * @return      list of {@link CFGNode} entering that node
     */
    public List<CFGNode> getPredecessors(CFGNode node)
    {
      return node.getPreds();
    }

    public void setNodeIDs(){
    	int i=0;
    	for(Iterator<CFGNode> it = nodes.iterator(); it.hasNext(); ++i){
    		CFGNode n = it.next();
    		n.setId(i);
    	}
    }
    
    
    /**
     * Get a dot(1) representation of a CFG.  The output can be processed
     * by the graphviz toolset, including dot, dotty, lefty, etc.
     *
     * @return  string containing the dot representation of the graph
     */
    public String toDot()
    {
        StringBuffer result = new StringBuffer();
        result.append("digraph cfg {\n");
        // dump all the nodes; assign a number to each
        int seq = 1;
        Map nodeName = new HashMap();
        for (Iterator iter = nodes.iterator(); iter.hasNext(); )
        {
            String name = "node" + seq;
            CFGNode node = (CFGNode)iter.next();
            nodeName.put(node, name);
            // shape is box for statement, circle for placeholder,
            // diamond for expression.  label is node number and
            // class.
            String shape, label;
            if (node.isEmpty())
            {
                shape = "ellipse";
                label = seq + "";
            }
            else if (node.getExpr() != null)
            {
                shape = "diamond";
                // label = seq + ": " + node.getExpr().getClass();
                label = seq + ": " + node.getExpr();
            }
            else
            {
                shape = "box";
                // label = seq + ": " + node.getStmt().getClass();
                label = seq + ": " + node.getStmt();
            }
            result.append(name + " [ label=\"" + label + "\", shape=" +
                          shape + " ]\n");
            
            
            seq++;
        }
        
        for (Iterator iter = nodes.iterator(); iter.hasNext(); )
        {
        	CFGNode node = (CFGNode)iter.next();
        	for (Iterator<EdgePair> titer = node.getSuccs().iterator(); titer.hasNext(); )
            {
                CFGNode to = titer.next().node;
                result.append(nodeName.get(node) + " -> " +
                              nodeName.get(to) + "\n");
            }
        }
        
        // All done.
        result.append("}\n");
        return result.toString();
    }
}

