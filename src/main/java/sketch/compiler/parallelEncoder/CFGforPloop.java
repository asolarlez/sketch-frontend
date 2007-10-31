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
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtPloop;
import streamit.frontend.nodes.StmtVarDecl;

public class CFGforPloop extends CFGBuilder {

	Set<String> locals = new HashSet<String>();
	
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
	
	
	
	public static CFG buildCFG(StmtPloop ploop)
    {
        CFGforPloop builder = new CFGforPloop();
        CFGNodePair pair = (CFGNodePair)ploop.getBody().accept(builder); 
        CFG rv =cleanCFG(new CFG(builder.nodes, pair.start, pair.end, builder.edges));
        builder.locals.add(ploop.getLoopVarName());
        CFGSimplifier sym = new CFGSimplifier(builder.locals);
        System.out.println("**** was " + rv.size() );
        rv = sym.mergeConsecutiveLocals(rv);
        System.out.println("**** became " + rv.size() );
        rv.setNodeIDs();
        return rv; 
    }
	
	public Object visitStmtVarDecl(StmtVarDecl svd){
		
		 CFGNode entry = null;
		 CFGNode last = null;
	     for(int i=0; i<svd.getNumVars(); ++i){
	    	 locals.add(svd.getName(i));
	    	 if(svd.getInit(i) != null){
	    		 CFGNode tmp = new CFGNode( new StmtAssign(svd.getCx(), new ExprVar(null, svd.getName(i)), svd.getInit(i)));
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
	
	protected CFGforPloop(){
		super();
		
	}
	
	
}
