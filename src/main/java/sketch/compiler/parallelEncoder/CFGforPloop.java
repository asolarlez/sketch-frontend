package streamit.frontend.parallelEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import streamit.frontend.controlflow.CFG;
import streamit.frontend.controlflow.CFGBuilder;
import streamit.frontend.controlflow.CFGNode;
import streamit.frontend.controlflow.CFGNode.EdgePair;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssert;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtAtomicBlock;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtExpr;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtFork;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.passes.CollectStmtTags;
import streamit.frontend.passes.VariableDeclarationMover;

public class CFGforPloop extends CFGBuilder {

	Set<String> locals = new HashSet<String>();
	Map<String, StmtVarDecl> localDecls = new HashMap<String, StmtVarDecl>();




	public static Map<CFGNode, Set<Object>> tagSets(CFG cfg){
		Map<CFGNode, Set<Object>> map = new HashMap<CFGNode, Set<Object>>();
		for(Iterator<CFGNode> it = cfg.getNodes().iterator(); it.hasNext(); ){
			CFGNode n = it.next();
			CollectStmtTags tag = new CollectStmtTags();
			if(n.isStmt()){
				n.getStmt().accept(tag);
			}
			if(n.isExpr()){
				n.getPreStmt().accept(tag);
			}
			map.put(n, tag.oset);
		}
		return map;
	}


	public static CFG cleanCFG(CFG cfg){
		Map<CFGNode, List<EdgePair>> edges = new HashMap<CFGNode, List<EdgePair>>();
	    List<CFGNode> nodes = new ArrayList<CFGNode>();
		Map<CFGNode, CFGNode> equiv = new HashMap<CFGNode, CFGNode>();
	    for(Iterator<CFGNode> it = cfg.getNodes().iterator(); it.hasNext(); ){
	    	CFGNode n = it.next();
	    	if( n.isEmpty() && cfg.getSuccessors(n).size() == 1 ){
	    		EdgePair ep = cfg.getSuccessors(n).get(0);
	    		assert ep.label == null : "The nodes must be connected by an unconditional edge";
	    		equiv.put(n, ep.node);
	    	}
	    }

	    CFGNode entry = cfg.getEntry();
	    while(equiv.containsKey(entry)){
	    	entry = equiv.get(entry);
		}

	    nodes.add(entry);

	    for(Iterator<CFGNode> it = cfg.getNodes().iterator(); it.hasNext(); ){
	    	CFGNode n = it.next();
	    	if(!equiv.containsKey(n)){
	    		if(n != entry){
	    			nodes.add(n);
	    		}
	    		for(Iterator<EdgePair> suc = cfg.getSuccessors(n).iterator(); suc.hasNext(); ){
	    			EdgePair sep = suc.next();
	    			CFGNode sn = sep.node;
	    			while(equiv.containsKey(sn)){
	    				sn = equiv.get(sn);
	    			}
	    			List<EdgePair> target;
	    		    if (edges.containsKey(n))
	    		            target = edges.get(n);
    		        else
    		        {
    		            target = new ArrayList<EdgePair>();
    		            edges.put(n, target);
    		        }
    		        target.add(new EdgePair(sn, sep.label));
	    		}
	    	}
	    }

	    CFG newCFG = new CFG(nodes, entry , cfg.getExit() , edges);
	    newCFG.setNodeIDs();
		return newCFG;
	}


	/**
	 *
	 * Create a CFG for the parallel program where each node corresponds to an atomic step
	 * in the execution.
	 *
	 * The locals array will contain those local variables that span multiple blocks, and therefore have to be communicated
	 * through the interface of the rest function.
	 *
	 * @param ploop
	 * @param locals
	 * @return
	 */
	public static CFG buildCFG(StmtFork ploop, Set<StmtVarDecl>/*out*/ locals)
    {
        CFGforPloop builder = new CFGforPloop();

        builder.locals.add(ploop.getLoopVarName());
        builder.localDecls.put(ploop.getLoopVarName(), ploop.getLoopVarDecl());



        CFGNodePair pair = (CFGNodePair)ploop.getBody().accept(builder);
        CFG rv =cleanCFG(new CFG(builder.nodes, pair.start, pair.end, builder.edges));

        CFGSimplifier sym = new CFGSimplifier(builder.locals);
        //System.out.println("**** was " + rv.size() );
        rv.repOK();
        //rv = sym.mergeConsecutiveLocals(rv);
        rv.repOK();
        rv = sym.simplifyAcrossBranches(rv);
        rv.repOK();
        rv = sym.cleanLocalState(rv, builder.localDecls, ploop.getLoopVarDecl());
        rv.repOK();
        locals.addAll(builder.localDecls.values());
        //System.out.println("**** became " + rv.size() );

        rv.eliminateUnnecessaryExpr();

        rv.setNodeIDs();
        return rv;
    }

	public Object visitStmtVarDecl(StmtVarDecl svd){

		 CFGNode entry = null;
		 CFGNode last = null;
	     for(int i=0; i<svd.getNumVars(); ++i){
	    	 String name = svd.getName(i);
	    	 locals.add(name);
	    	 localDecls.put(name, new StmtVarDecl(svd, svd.getType(i), name, ExprConstInt.zero));
	    	 if(svd.getInit(i) != null){
	    		 CFGNode tmp = new CFGNode( new StmtAssign(new ExprVar(svd, name), svd.getInit(i)));
    			 this.nodes.add(tmp);
	    		 if(entry == null){
	    			 entry = tmp;
	    			 last = tmp;
	    		 }else{
	    			 addEdge(last, tmp, null);
	    			 last = tmp;
	    		 }
	    	 }
	     }
	     if(entry == null){
	    	 CFGNode node = new CFGNode(svd, true);
	         nodes.add(node);
	         return new CFGNodePair(node, node);
	     }else{
	    	 return new CFGNodePair(entry, last);
	     }
	}



	/**
	 * @deprecated Use {@link #isAtomic(Statement)} instead
	 */
	boolean isSingleStmt(Statement s){
		return isAtomic(s);
	}


	boolean isAtomic(Statement s){

		if(s instanceof StmtAssign) return true;
		
		if(s instanceof StmtAtomicBlock){
			return true;
		}
		
		if(s instanceof StmtAssert)
			return true;
		
		if(s instanceof StmtBlock){
			StmtBlock sb = (StmtBlock) s;
			if(sb.getStmts().size() != 1){ return false; }
			return isAtomic(sb.getStmts().get(0));
		}
		if(s instanceof StmtExpr){
			return true;
		}
		
		if(s instanceof StmtIfThen){
			StmtIfThen sit = (StmtIfThen) s;
			if( allLocals(sit.getCond()) ){
	    		if( isAtomic(sit.getCons()) ){
	    			if(sit.getAlt() == null){
	    				return true;
	    			}
	    			if(isAtomic(sit.getAlt()) ){
	    				return true;
	    			}
	    		}
	    	}
		}
		
		return false;
	}


	class AllLocals extends FEReplacer{
		boolean allLocs = true;
		@Override
		public Object visitExprVar(ExprVar exp){
			if(!locals.contains(exp.getName())){
				allLocs = false;
			}
			return exp;
		}
		public Object visitExprFunCall(ExprFunCall exp){
			allLocs = false;
			return exp;
		}
	}

	boolean allLocals(FENode s ){
		AllLocals al = new AllLocals();
		s.accept(al);
		return al.allLocs;
	}


    public Object visitStmtIfThen(StmtIfThen stmt)
    {

    	if( allLocals(stmt.getCond()) ){
    		if( isAtomic(stmt.getCons()) ){
    			if(stmt.getAlt() == null){
    				return null;
    			}
    			if(isAtomic(stmt.getAlt()) ){
    				return null;
    			}
    		}
    	}


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





	protected CFGforPloop(){
		super();

	}


}
