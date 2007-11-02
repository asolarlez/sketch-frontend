package streamit.frontend.parallelEncoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import streamit.frontend.controlflow.CFG;
import streamit.frontend.controlflow.CFGNode;
import streamit.frontend.controlflow.CFGNode.EdgePair;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtVarDecl;

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
		if(node.isEmpty()){
			return false;
		}
		return alloc.allLocs;
	}
	
	
	void addNodeToPreds(CFGNode node, CFGNode pred){
		System.out.println(" adding " + node.getId() + " to pred " + pred.getId());
		//node.checkNeighbors();
		assert pred != node;
		assert pred.isStmt() : "this is weird";
		assert !node.isEmpty();
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
			ep.node.addPred(pred);
		}
		pred.checkNeighbors();
	}
	
	void addNodeToSucc(CFGNode node, CFGNode succ){
		System.out.println(" adding " + node.getId() + " to succ " + succ.getId());
		assert node.isStmt();
		assert succ.getPreds().size() == 1 : "succ should have a single predecessor, and that's node";
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
		succ.getPreds().clear();
		//Now we need to change all the predecessors of the node to be predecessors of succ.
		for(Iterator<CFGNode> it = node.getPreds().iterator(); it.hasNext(); ){
			CFGNode p = it.next();
			succ.addPred(p);
			p.changeSucc(node, succ);
		}
		
	}
	
	
	public CFG cleanLocalState(CFG cfg, final Map<String, StmtVarDecl> locals, StmtVarDecl pidVar){
		final Set<String> usedOnceVariables = new HashSet<String>();
		final Set<String> usedAlotVariables = new HashSet<String>();
		usedAlotVariables.add(pidVar.getName(0)); // The ploop vari corresponding to the pid can not be
		usedOnceVariables.add(pidVar.getName(0));//in the usedOnce set, even if it is used only once. Remember later we'll do usedOnce = usedOnce - usedAlot;
		final Map<CFGNode, Set<String> > varsforNode = new HashMap<CFGNode, Set<String>>();
		
		for(Iterator<CFGNode> nodeIt = cfg.getNodes().iterator(); nodeIt.hasNext(); ){
			CFGNode node = nodeIt.next();
			final Set<String> nodeLocals = new HashSet<String>();
			FEReplacer lfind = new FEReplacer(){
				public Object visitExprVar(ExprVar exp){
					if(locals.containsKey(exp.getName())){
						nodeLocals.add(exp.getName()); 
					}
					return exp;
				}
			};
			
			if(node.isStmt()){
				node.getStmt().accept(lfind);
			}
			if(node.isExpr()){
				if(node.getPreStmt() != null){
					node.getPreStmt().accept(lfind);
				}
				node.getExpr().accept(lfind);
			}
			
			for(Iterator<String> lit = nodeLocals.iterator(); lit.hasNext(); ){
				String name = lit.next();
				if( usedOnceVariables.contains(name) ){
					usedAlotVariables.add(name);
				}else{
					usedOnceVariables.add(name);
				}
			}
			varsforNode.put(node, nodeLocals);
		}
		int oldSz = usedOnceVariables.size();
		System.out.println("*** #locals before " + locals.size());
		usedOnceVariables.removeAll(usedAlotVariables);
		assert usedOnceVariables.size() + usedAlotVariables.size() == oldSz;
		assert oldSz <= locals.size();
		//The used alot variables are going to be the state that gets passed around, while the used once
		//variables can just be declared in the node where they are used, and we don't have to worry about passing them around.
		
		for(Iterator<CFGNode> nodeIt = cfg.getNodes().iterator(); nodeIt.hasNext(); ){
			CFGNode node = nodeIt.next();
			final Set<String> nodeLocals = varsforNode.get(node);
			for(Iterator<String> it = nodeLocals.iterator(); it.hasNext(); ){
				String name = it.next();
				if( usedOnceVariables.contains(name) ){
					assert locals.containsKey(name);
					if(node.isStmt()){						
						node.changeStmt(new StmtBlock( locals.get(name), node.getStmt() ) );
					}
					if(node.isExpr()){
						if(node.getPreStmt() != null){
							node.setPreStmt(new StmtBlock(locals.get(name), node.getPreStmt() ));
						}else{
							node.setPreStmt(locals.get(name));
						}
					}
					locals.remove(name);
				}
			}
		}
		
		return cfg;
	}
		
	
	
	public CFG simplifyAcrossBranches(CFG cfg){
		
		Stack<CFGNode> ccstack = new Stack<CFGNode>();
		Stack<CFGNode> stack = new Stack<CFGNode>();
		stack.push(cfg.getEntry());
		
		Set<CFGNode> visited = new HashSet<CFGNode>();
		///Connected components of nodes containing all local variables.
		List<List<CFGNode>> localCCs = new ArrayList<List<CFGNode>>();
		
		List<CFGNode> currentCC = null;
		//The set of extras is a set of nodes that do have
		//globals, but that get appended to the current CC because
		//it is safe to do so.
		//The key rule about adding extras one extra can not
		//be a successor of another extra, because if that
		//were to happen, you could execute two statements with
		//globals in the same step, which is ilegal.
		Set<CFGNode> extras = new HashSet<CFGNode>();
		CFGNode head = cfg.getEntry();
		int headLoc = -1;
		while(stack.size() > 0 || ccstack.size() > 0){
			if(ccstack.size()>0){
				CFGNode n = ccstack.pop();
				if(visited.contains(n)){
					continue;
				}
				visited.add(n);
				if(currentCC == null){
					currentCC = new ArrayList<CFGNode>();
					extras.clear();
				}
				
				if(n == head){
					headLoc = localCCs.size();
				}
				
				currentCC.add(n);
				
				List<EdgePair> suc = n.getSuccs();
				for(int i=0; i<suc.size(); ++i){
					CFGNode snode = suc.get(i).node;
					if(allLocals(snode)){
						ccstack.push(snode);
					}else{
						stack.push( snode );
						if(!extras.contains(snode)){
							//If none of the predecessors of snode is in extras, we can make snode an extra.
							boolean allgood = true;
							for(Iterator<CFGNode> it = snode.getPreds().iterator(); it.hasNext(); ){
								if(extras.contains(it.next())){
									allgood = false;
									break;
								}
							}
							if(allgood){
								extras.add(snode);
								currentCC.add(snode);
							}
						}
					}
				}			
			}else{
				if(currentCC != null){
					localCCs.add(currentCC);
					currentCC = null;
				}				
				CFGNode n = stack.pop();
				if(visited.contains(n)){
					continue;
				}
				if(allLocals(n)){
					ccstack.push(n);
					continue;
				}
				visited.add(n);
				List<EdgePair> suc = n.getSuccs();
				for(int i=0; i<suc.size(); ++i){
					if(!visited.contains(suc.get(i).node)){
						stack.push(suc.get(i).node);
					}					
				}
			}
		}
		if(currentCC != null){
			localCCs.add(currentCC);
			currentCC = null;
		}	
		///At this point, all nodes are either in newNodes or in one of the localCCs.
		///Each localcc is a list of topologically sorted nodes containing all local 
		///variables that form a connected component in the dag.
		
		for(int i=0; i<localCCs.size(); ++i){
			CFGNode tmp = processCC(localCCs.get(i));
			if(headLoc == i){
				head = tmp;
			}
		}
		assert stack.size() == 0;
		visited.clear();
		stack.push(head);
		List<CFGNode> newNodes = new ArrayList<CFGNode>();
		newNodes.add(head);
		while(stack.size() > 0){
			CFGNode n = stack.pop();
			if(visited.contains(n)){
				continue;
			}
			visited.add(n);
			if(n != head){
				newNodes.add(n);
			}
			List<EdgePair> suc = n.getSuccs();
			for(int i=0; i<suc.size(); ++i){
				if(!visited.contains(suc.get(i).node)){
					stack.push(suc.get(i).node);
				}					
			}			
		}
		
		return new CFG(newNodes, head, cfg.getExit());
	}
	
	
	public void changeSucc(CFGNode n, CFGNode oldS, CFGNode newS){
		if(oldS != newS){
			n.changeSucc(oldS, newS);
			newS.addPred(n);
			boolean tmp = oldS.removePred(n);
			if(tmp){
				oldS.removeFromChildren();
			}
		}
	}
	
	private CFGNode simplifyNode(CFGNode n, Set<CFGNode> ccset, Set<CFGNode> visited){
		if(visited.contains(n)){System.out.println(" already visited " + n); return n;}
		System.out.println("visiting node " + n);
		visited.add(n);
		List<EdgePair> succ = n.getSuccs();		
		if(n.isExpr()){
			assert succ.size() == 2;
			CFGNode sf = succ.get(0).node;
			CFGNode st = succ.get(1).node;
			
			if(succ.get(0).label == 1){
				assert succ.get(1).label == 0;
				sf = st;
				st = succ.get(0).node;
			}else{
				assert succ.get(0).label == 0;
				assert succ.get(1).label == 1;
			}
			boolean simplST = false;
			boolean simplSF = false;
			if( ccset.contains(sf) && ccset.contains(st) ){
				CFGNode nsf = simplifyNode(sf, ccset, visited);
				CFGNode nst = simplifyNode(st, ccset, visited);
				changeSucc(n, st, nst);
				changeSucc(n, sf, nsf);
				simplST = true;
				simplSF = true;
				sf = nsf;
				st = nst;
				if(nst.isStmt() && nsf.isStmt() ){
					Statement s = new StmtIfThen(null, n.getExpr(), st.getStmt(), sf.getStmt() );
					if(n.getPreStmt() == null){
						n.setPreStmt(s);
					}else{
						n.setPreStmt(new StmtBlock(s, n.getPreStmt()));
					}
					assert st.getSuccs().size() == 1 && sf.getSuccs().size() == 1; 
					if(st.getSuccs().get(0).node != sf.getSuccs().get(0).node){
						changeSucc(n, st, st.getSuccs().get(0).node);
						changeSucc(n, sf, sf.getSuccs().get(0).node);						
					}else{
						n.changeExpr(null);
						n.changeStmt(n.getPreStmt());
						n.removeSucc(st);
						n.removeSucc(sf);
						CFGNode newSuc = st.getSuccs().get(0).node;
						n.addSucc(new EdgePair(newSuc, null));
						st.removePred(n);
						sf.removePred(n);
						newSuc.addPred(n);						
					}
					return n;
				}
			}
			if( ccset.contains(st) ){
				if(!simplST){
					CFGNode nst = simplifyNode(st, ccset, visited);
					changeSucc(n, st, nst);
					st = nst;
				}
				if(st.isStmt()){
					Statement s = new StmtIfThen(null, n.getExpr(), st.getStmt(), null);
					if(n.getPreStmt() == null){
						n.setPreStmt(s);
					}else{
						n.setPreStmt(new StmtBlock(s, n.getPreStmt()));
					}
					
					assert st.getSuccs().size() == 1 ; 
					if(st.getSuccs().get(0).node != sf){
						changeSucc(n, st,  st.getSuccs().get(0).node);
						return n;
					}else{
						n.changeExpr(null);
						n.changeStmt(n.getPreStmt());						
						n.removeSucc(st);
						n.removeSucc(sf);
						
						CFGNode newSuc = st.getSuccs().get(0).node;
						n.addSucc(new EdgePair(newSuc, null));
						st.removePred(n);
						sf.removePred(n);
						newSuc.addPred(n);	
						return simplifyNode(n, ccset, visited);
						
					}
				}
			}
			
			if( ccset.contains(sf) ){
				if(!simplSF){
					CFGNode nsf = simplifyNode(sf, ccset, visited);
					changeSucc(n, sf, nsf);
					sf = nsf;
				}
				if(sf.isStmt()){
					Statement s = new StmtIfThen(null, new ExprUnary(null, ExprUnary.UNOP_NOT, n.getExpr()), sf.getStmt(), null);
					if(n.getPreStmt() == null){
						n.setPreStmt(s);
					}else{
						n.setPreStmt(new StmtBlock(s, n.getPreStmt()));
					}
					
					assert sf.getSuccs().size() == 1 ; 
					if(sf.getSuccs().get(0).node != st){
						changeSucc(n, sf, sf.getSuccs().get(0).node);
						return n;
					}else{
						n.changeStmt(n.getPreStmt());
						n.changeExpr(null);
						n.removeSucc(st);
						n.removeSucc(sf);
						
						CFGNode newSuc = sf.getSuccs().get(0).node;
						n.addSucc(new EdgePair(newSuc, null));
						st.removePred(n);
						sf.removePred(n);
						newSuc.addPred(n);	
						return simplifyNode(n, ccset, visited);
						
					}
				}
			}
			
		}
		
		if(n.isStmt()){
			assert succ.size() == 1; 
			CFGNode s1 = succ.get(0).node;
			if( ccset.contains(s1) ){
				CFGNode ns1 = simplifyNode(s1, ccset, visited);
				changeSucc(n, s1, ns1);
				s1 = ns1;
				if(s1.getPreds().size() <= 2){
					if(s1.isStmt()){
						n.changeStmt(new StmtBlock(n.getStmt(), s1.getStmt() ));
						assert s1.getSuccs().size() == 1;
						changeSucc(n, s1, s1.getSuccs().get(0).node);
						return n;
					}
					if(s1.isExpr() && s1.getPreds().size() == 1){
						Statement s;
						if( s1.getPreStmt()== null ){
							s = n.getStmt();
						}else{
							s = new StmtBlock(n.getStmt(), s1.getPreStmt());
						}
						
						n.changeExpr(s1.getExpr());
						n.setPreStmt(s);
						n.removeSucc(s1);
						for(Iterator<EdgePair> it = s1.getSuccs().iterator(); it.hasNext(); ){
							n.addSucc(it.next());
						}
						return n;						
					}
				}
			}			
		}	
		return n;
	}
	
	CFGNode processCC(List<CFGNode> cc){
		Set<CFGNode> ccset = new HashSet<CFGNode>(cc);
		Set<CFGNode> in = new HashSet<CFGNode>();
		Set<CFGNode> out = new HashSet<CFGNode>();
		assert cc.size() > 0;
		CFGNode head = cc.get(0);
		//head = simplifyNode(head, ccset, new HashSet<CFGNode>());
		return head;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	public CFG mergeConsecutiveLocals(CFG cfg){
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
					preds.clear();
					for(Iterator<EdgePair> it = node.getSuccs().iterator(); it.hasNext(); ){
						EdgePair ep = it.next();
						ep.node.removeAllPred(node);
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

