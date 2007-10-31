package streamit.frontend.parallelEncoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import streamit.frontend.controlflow.CFG;
import streamit.frontend.controlflow.CFGNode;
import streamit.frontend.controlflow.CFGNode.EdgePair;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.StmtBlock;

public class CFGSimplifier {
	Set<String> locals;
	CFGSimplifier(Set<String> locals){
		this.locals = locals;
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
	
	
	boolean allLocals(CFGNode node){
		AllLocals alloc = new AllLocals();
		if(node.isExpr()){
			node.getExpr().accept(alloc);
		}
		if(node.isStmt()){
			node.getStmt().accept(alloc);
		}		
		return alloc.allLocs;
	}
	
	
	void addNodeToPreds(CFGNode node, CFGNode pred){
		assert pred != node;
		assert pred.isStmt() : "this is weird"; 
		if(node.isStmt()){
			pred.changeStmt(new StmtBlock(pred.getStmt(), node.getStmt() ));
		}else{
			//Pred is currently a statement, but now we need to make it an expression.
			//This is not so bad. We can make it an expression by simply setting its expression field.
			assert node.isExpr();
			pred.changeExpr(node.getExpr());
			if(node.getPreStmt() != null){
				pred.setPreStmt(new StmtBlock(pred.getStmt(), node.getPreStmt()));
			}else{
				pred.setPreStmt(pred.getStmt());
			}			
		}
		
		List<EdgePair> psuccs = pred.getSuccs();
		assert psuccs.size() == 1;
		assert psuccs.get(0).node == node;
		assert psuccs.get(0).label == null;
		psuccs.clear();
		
		//Now, we need to change all successors of node to be successors of pred now.
		for(Iterator<EdgePair> it = node.getSuccs().iterator(); it.hasNext(); ){
			EdgePair ep = it.next();			
			psuccs.add(ep);
			ep.node.changePred(node, pred);
		}
	}
	
	void addNodeToSucc(CFGNode node, CFGNode succ){
		assert node.isStmt();
		if(succ.isStmt()){
			succ.changeStmt(new StmtBlock(node.getStmt(), succ.getStmt() ));						
		}else{
			assert (succ.isExpr());
			if(succ.getPreStmt() == null){
				succ.setPreStmt(node.getStmt());
			}else{
				succ.setPreStmt(new StmtBlock(node.getStmt(), succ.getPreStmt()) );
			}			
		}
		
		//Now we need to change all the predecessors of the node to be predecessors of succ.
		for(Iterator<CFGNode> it = node.getPreds().iterator(); it.hasNext(); ){
			CFGNode p = it.next();
			succ.addPred(p);
			p.changeSucc(node, succ);
		}
		
	}
	
	CFG mergeConsecutiveLocals(CFG cfg){
		List<CFGNode> newNodes = new ArrayList<CFGNode>();
		
		CFGNode newHead = cfg.getEntry();
		
		for(Iterator<CFGNode> nodeIt = cfg.getNodes().iterator(); nodeIt.hasNext(); ){
			CFGNode node = nodeIt.next();
			if(allLocals(node)){
				//If all the predecessors have a single successor, add to the predecessor.
				//We also have an additional requirement that we only do this if there are at most
				//two predecessors, to avoid code bloat.
				List<CFGNode> preds = node.getPreds();
				boolean addToPred = preds.size() <= 2 && preds.size() > 0 && !node.isEmpty();
				for(Iterator<CFGNode> predIt = preds.iterator(); predIt.hasNext(); ){
					CFGNode pred = predIt.next();
					if(pred.getSuccs().size() != 1){
						addToPred = false;
					}
					if(!addToPred){
						break;
					}
				}
				if(addToPred){
					// add to pred.
					for(Iterator<CFGNode> predIt = preds.iterator(); predIt.hasNext(); ){
						addNodeToPreds(node, predIt.next());
					}
					continue;
				}else{
					//If single successor has only one predecessor and is not empty add to successor.	
					List<EdgePair> succs = node.getSuccs();
					if(succs.size() == 1){
						CFGNode succ = succs.get(0).node;
						if(succ.getPreds().size() == 1 && !succ.isEmpty()){
						// add to succ.
							addNodeToSucc(node, succ);
							if(node == newHead){
								newHead = succ;
							}
							continue;
						}						
					}				
				}		
			}
			newNodes.add(node);
		}
		
		newNodes.remove(newHead);
		newNodes.add(0, newHead);
		
		return new CFG(newNodes, newHead, cfg.getExit());
	}
	

}

