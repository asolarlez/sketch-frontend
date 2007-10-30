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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import streamit.frontend.controlflow.CFGNode.EdgePair;
import streamit.frontend.nodes.FENullVisitor;
import streamit.frontend.nodes.Function;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtBreak;
import streamit.frontend.nodes.StmtContinue;
import streamit.frontend.nodes.StmtDoWhile;
import streamit.frontend.nodes.StmtFor;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtReturn;
import streamit.frontend.nodes.StmtWhile;

/**
 * Helper class to build a control-flow graph from linear code.
 * The {@link #buildCFG} method can be called externally to
 * produce a CFG from a function declaration.
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class CFGBuilder extends FENullVisitor
{
    /**
     * Build a control-flow graph from a function.  This is the main
     * entry point to this module.
     *
     * @param func  function to build a CFG for
     * @return      control-flow graph object for the function
     */
    public static CFG buildCFG(Function func)
    {
        CFGBuilder builder = new CFGBuilder();
        CFGNodePair pair = (CFGNodePair)func.getBody().accept(builder);
        return new CFG(builder.nodes, pair.start, pair.end, builder.edges);
    }

    // Visitors return this:
    protected static class CFGNodePair
    {
        public CFGNode start;
        public CFGNode end;
        public CFGNodePair(CFGNode start, CFGNode end)
        {
            this.start = start;
            this.end = end;
        }
    }
    
    // Where to go for particular statements:
    protected CFGNode nodeReturn, nodeBreak, nodeContinue;
    // What edges exist (map from start node to list of end node):
    protected Map<CFGNode, List<EdgePair>> edges;
    // Every node:
    protected List<CFGNode> nodes;
    
    protected CFGBuilder()
    {
        nodeReturn = null;
        nodeBreak = null;
        nodeContinue = null;
        edges = new HashMap();
        nodes = new ArrayList();
    }

    protected void addEdge(CFGNode from, CFGNode to, Integer label)
    {
        List<EdgePair> target;
        if (edges.containsKey(from))
            target = edges.get(from);
        else
        {
            target = new ArrayList<EdgePair>();
            edges.put(from, target);
        }
        //if (!target.contains(to))
        target.add(new EdgePair(to, label));
    }

    protected CFGNodePair visitStatement(Statement stmt)
    {
        // If the visitor works, use its result.
        CFGNodePair pair = (CFGNodePair)stmt.accept(this);
        if (pair != null)
            return pair;
        // Otherwise, create a node, add it, and return a basic pair.
        CFGNode node = new CFGNode(stmt);
        nodes.add(node);
        return new CFGNodePair(node, node);
    }
    
    public Object visitStmtBlock(StmtBlock block)
    {
        // Create entry and exit nodes for the block.
        CFGNode entry = new CFGNode(block, true);
        CFGNode exit = new CFGNode(block, true);
        nodes.add(entry);
        nodes.add(exit);

        // If we haven't declared a return point yet, this must
        // be the top-level block and so the return point is our exit.
        if (nodeReturn == null)
            nodeReturn = exit;
               
        // Also remember where we are in traversing.  Start at the
        // beginning.
        CFGNode current = entry;

        // Walk through all of the contained statements.
        for (Iterator iter = block.getStmts().iterator(); iter.hasNext(); )
        {
            Statement stmt = (Statement)iter.next();
            CFGNodePair pair = visitStatement(stmt);
            // Add an edge from our current end to the start of the pair,
            // but only if the current end is non-null ("we were going
            // somewhere").  This could lead to a node with no
            // forward path to it, but that's okay.
            if (current != null)
                addEdge(current, pair.start, null);
            // Make the end of the pair current.  That could be null if
            // the statement was a break, continue, or return statement
            // that doesn't have an interesting outgoing edge.
            current = pair.end;
        }

        // Add an edge from the current node to the exit, if there is
        // a current node.  (For example, current could be null if the
        // last statement in a function is a return, but that's fine.)
        if (current != null)
            addEdge(current, exit, null);
        
        return new CFGNodePair(entry, exit);
    }

    public Object visitStmtFor(StmtFor stmt)
    {
        // We need an exit node here, but not an explicit entry.
        CFGNodePair pairInit = visitStatement(stmt.getInit());
        CFGNode entry = pairInit.start;
        CFGNode exit = new CFGNode(stmt, true);
        nodes.add(exit);
        // Loop condition:
        CFGNode cond = new CFGNode(stmt, stmt.getCond());
        nodes.add(cond);
        // Increment:
        CFGNodePair pairIncr = visitStatement(stmt.getIncr());
        // Things we know are connected:
        // (claim that pairInit and pairIncr don't have null ends.)
        addEdge(pairInit.end, cond, null);
        addEdge(pairIncr.end, cond, null);
        addEdge(cond, exit, 0);
        // Also, continue statements go to incr, breaks to exit.
        CFGNode lastContinue = nodeContinue;
        CFGNode lastBreak = nodeBreak;
        nodeContinue = pairIncr.start;
        nodeBreak = exit;
        // Get the child pair.
        CFGNodePair pairBody = visitStatement(stmt.getBody());
        // Restore:
        nodeContinue = lastContinue;
        nodeBreak = lastBreak;
        // Connect body.
        addEdge(cond, pairBody.start, 1);
        if (pairBody.end != null)
            addEdge(pairBody.end, pairIncr.start, null);
        // And return the pair.
        return new CFGNodePair(entry, exit);
    }

    public Object visitStmtIfThen(StmtIfThen stmt)
    {
        // Entry node is the condition; exit is artificial.
        CFGNode entry = new CFGNode(stmt, stmt.getCond());
        nodes.add(entry);
        CFGNode exit = new CFGNode(stmt, true);
        nodes.add(exit);
        // Check both branches.
        if (stmt.getCons() != null)
        {
            CFGNodePair pair = visitStatement(stmt.getCons());
            addEdge(entry, pair.start, 1);
            if (pair.end != null)
                addEdge(pair.end, exit, null);
        }
        else
        {
            addEdge(entry, exit, 1);
        }

        if (stmt.getAlt() != null)
        {
            CFGNodePair pair = visitStatement(stmt.getAlt());
            addEdge(entry, pair.start, 0);
            if (pair.end != null)
                addEdge(pair.end, exit, null);
        }
        else
        {
            addEdge(entry, exit, 0);
        }
        
        return new CFGNodePair(entry, exit);
    }

    public Object visitStmtWhile(StmtWhile stmt)
    {
        // similarly.
        CFGNode entry = new CFGNode(stmt, stmt.getCond());
        nodes.add(entry);
        CFGNode exit = new CFGNode(stmt, true);
        nodes.add(exit);
        // continue statements go to cond (entry), breaks to exit.
        CFGNode lastContinue = nodeContinue;
        CFGNode lastBreak = nodeBreak;
        nodeContinue = entry;
        nodeBreak = exit;
        // Get the child pair.
        CFGNodePair pairBody = visitStatement(stmt.getBody());
        // Restore:
        nodeContinue = lastContinue;
        nodeBreak = lastBreak;
        // Connect body.
        addEdge(entry, pairBody.start, 1);
        if (pairBody.end != null)
            addEdge(pairBody.end, entry, null);
        // Conditional can be false.
        addEdge(entry, exit, 0);
        // And return the pair.
        return new CFGNodePair(entry, exit);        
    }

    public Object visitStmtDoWhile(StmtDoWhile stmt)
    {
        // A little different: artificial entry, save the condition
        // separately, since it's neither entry nor exit.
        CFGNode entry = new CFGNode(stmt, true);
        nodes.add(entry);
        CFGNode cond = new CFGNode(stmt, stmt.getCond());
        nodes.add(entry);
        CFGNode exit = new CFGNode(stmt, true);
        nodes.add(exit);
        // continue statements go to cond , breaks to exit.
        CFGNode lastContinue = nodeContinue;
        CFGNode lastBreak = nodeBreak;
        nodeContinue = cond;
        nodeBreak = exit;
        // Get the child pair.
        CFGNodePair pairBody = visitStatement(stmt.getBody());
        // Restore:
        nodeContinue = lastContinue;
        nodeBreak = lastBreak;
        // Connect body.
        addEdge(entry, pairBody.start, null);
        if (pairBody.end != null)
            addEdge(pairBody.end, cond, null);
        // Also connect up loop and exit from cond.
        addEdge(cond, pairBody.start, 1);
        addEdge(cond, exit, 0);
        // And return the pair.
        return new CFGNodePair(entry, exit);        
    }

    public Object visitStmtBreak(StmtBreak stmt)
    {
        // Build a node,
        CFGNode node = new CFGNode(stmt);
        nodes.add(node);
        // but explicitly connect it to the current loop break node.
        addEdge(node, nodeBreak, null);
        // Return an edge pair pointing to null.
        return new CFGNodePair(node, null);
    }

    public Object visitStmtContinue(StmtContinue stmt)
    {
        CFGNode node = new CFGNode(stmt);
        nodes.add(node);
        addEdge(node, nodeContinue, null);
        return new CFGNodePair(node, null);
    }

    public Object visitStmtReturn(StmtReturn stmt)
    {
        CFGNode node = new CFGNode(stmt);
        nodes.add(node);
        addEdge(node, nodeReturn, null);
        return new CFGNodePair(node, null);
    }
}

