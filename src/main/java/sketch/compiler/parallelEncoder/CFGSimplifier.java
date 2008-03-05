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
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprFunCall;
import streamit.frontend.nodes.ExprUnary;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtIfThen;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.TypePrimitive;

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




	/**
	 * Invariants:
	 * <li> Each cluster has a single entry point.
	 * <li> Every path from the entry to the exit contains only a single global block.
	 * <li> The entry point is not global.
	 * @author asolar
	 *
	 */
	private static class Cluster{
		final CFGNode head;
		final Set<CFGNode> rest = new HashSet<CFGNode>();
		Cluster(CFGNode head){
			this.head = head;
		}
		public boolean contains(CFGNode c){
			if(head == c){ return true; }
			return rest.contains(c);
		}
		public void add(CFGNode n){
			rest.add(n);
		}
	}



	/**
	 * This routine creates clusters from the CFG by the following rules.
	 * The cluster gets started by putting the root in the cstack.
	 * The cstack is the stack for the current cluster.
	 *
	 * Each entry in the Cstack has a bit that says whether the path to it has been tainted or not.
	 * The tainted label is set by the following rules:
	 * <li> If the parent is global, all its children are tainted.
	 * <li> If the parent is tainted, all its children are tainted.
	 *
	 * Because a cluster can have only a single entry point, nodes with multiple parents can not be
	 * added to the cluster until we know all its parents belong to the cluster.
	 * When you reach a node with multiple parents, you check if all parents are in a cluster. If not,
	 * you put it in a waitlist with one of the following tags:
	 * <li> global: The node is global. If it is reached by a tainted parent, it should be removed from the
	 * waitlist and added to the outer stack and marked as visited.
	 * <li> tainted: One of its parents is tainted. When all the parents have been visited, it should
	 * be added to the cstack as tainted.
	 * <li> local: None of its parents are tainted. When all parents have been visited, it should be added
	 * to the cstack as untainted.
	 *
	 *
	 * @param cfg
	 * @param clusters
	 */
	public int createClusters(CFG cfg, List<Cluster>/*out*/ clusters){
		final int T/*tainted*/ = 0;final int U/*untainted*/ = 1;
		final int G = 2;
		/**
		 * labeled node.
		 */
		final class lNode{
			final CFGNode n;
			final int label;
			public lNode(CFGNode n, int l){ this.n = n; this.label = l; }
			public String toString(){
				return n + " " + (label == T ? "T" : (label==U? "U" : "G"));
			}
		}
		CFGNode head = cfg.getEntry();
		Stack<lNode> ccstack = new Stack<lNode>();
		Stack<CFGNode> stack = new Stack<CFGNode>();
		stack.push(head);
		Set<CFGNode> visited = new HashSet<CFGNode>();
		visited.add(head);

		List<Cluster> localCCs = clusters;
		Cluster currentcc = null;
		Map<CFGNode, Integer> waitlist = new HashMap<CFGNode, Integer>();

		int headLoc = -1;
		while(stack.size() > 0 || ccstack.size() > 0 || waitlist.size() > 0){
			if(ccstack.size()>0){
				lNode currentLn = ccstack.pop();
				if(currentcc == null){
					//If current cc is new, we add as head.
					//System.out.println("Cluster begin:");
					currentcc = new Cluster(currentLn.n);
					assert waitlist.size() == 0;
					if(currentLn.n == head){
						headLoc = localCCs.size();
					}
				}else{
					currentcc.add(currentLn.n);
				}
				//System.out.println(currentLn);
				List<EdgePair> succList = currentLn.n.getSuccs();
				for(int i=0; i<succList.size(); ++i){
					CFGNode succOfCurr = succList.get(i).node;
					if(!visited.contains(succOfCurr)){
						List<CFGNode> predsOfSucc = succOfCurr.getPreds();
						boolean succIsLocal = allLocals(succOfCurr);

						if(succOfCurr.isEmpty()){
							stack.push(succOfCurr);
							visited.add(succOfCurr);
							continue;
						}

						if(predsOfSucc.size() == 1){
							if(succIsLocal){
								ccstack.push(new lNode(succOfCurr, currentLn.label )); // if it's local, we simply preserve the label.
							}else{
								if(currentLn.label == T){
									//If the path was already tainted, we can't add a non-local.
									stack.push(succOfCurr);
								}else{
									//If the path was not tainted, we add it, but taint the path.
									ccstack.push(new lNode(succOfCurr, T ));
								}
							}
							visited.add(succOfCurr);
						}else{
							boolean allInSet = true;
							for(Iterator<CFGNode> it = predsOfSucc.iterator(); it.hasNext();   ){
								CFGNode sp = it.next();
								if(!currentcc.contains(sp) && sp != currentLn.n){
									allInSet = false;
									break;
								}
							}
							if(allInSet){
								assert waitlist.containsKey(succOfCurr);
								int label = waitlist.get(succOfCurr);
								waitlist.remove(succOfCurr);
								if(label == G ){ // means succOfCurr is global, but all prev paths had been untainted.
									if(currentLn.label == U){ // If this path is untainted too, put in as tainted.
										ccstack.push(new lNode(succOfCurr, T ));
									}else{
										stack.push(succOfCurr);
									}
								}else{
									if(label == T){ // succOfCurr was local, but some path to it was tainted.
										ccstack.push(new lNode(succOfCurr, T ));
									}else{// succOfCurr was local and all paths to it so far have been local.
										assert label == U;
										ccstack.push(new lNode(succOfCurr, currentLn.label ));
									}
								}
								visited.add(succOfCurr);
							}else{
								//Not all predecessors are in CC yet
								if( waitlist.containsKey(succOfCurr) ){
									//If already in the waitlist.
									int label = waitlist.get(succOfCurr);
									if(label == G && currentLn.label == T){
										//If it was global and the current label is tainted, we remove from waitlist and add to stack.
										waitlist.remove(succOfCurr);
										stack.push(succOfCurr);
										visited.add(succOfCurr);
									}
									if(label == U && currentLn.label == T){
										waitlist.put(succOfCurr, T);
									}
								}else{
									if(succIsLocal){
										waitlist.put(succOfCurr, currentLn.label);
									}else{
										if(currentLn.label == T){
											//If the path was already tainted, we can't add a non-local.
											stack.push(succOfCurr);
											visited.add(succOfCurr);
										}else{
											//If the path was not tainted, we add it to the waitlist, but mark it as global.
											waitlist.put(succOfCurr, G);
										}
									}
								}
							}//if(allInSet) else
						}// if(predsOfSucc.size() == 1) else
					}
				}

			}else{ // if(ccstack.size()>0)
				if(currentcc != null){
					localCCs.add(currentcc);
					currentcc = null;
					for(Iterator<CFGNode> it = waitlist.keySet().iterator(); it.hasNext(); ){
						CFGNode wn = it.next();
						stack.push(wn);
						visited.add(wn);
					}
					waitlist.clear();
				}
				CFGNode n = stack.pop();
				if(allLocals(n)){
					ccstack.push(new lNode(n, U));
					continue;
				}else{
					if(!n.isEmpty()){
						ccstack.push(new lNode(n, T));
						continue;
					}
				}
				/*
				List<EdgePair> suc = n.getSuccs();
				for(int i=0; i<suc.size(); ++i){
					CFGNode s = suc.get(i).node;
					if(!visited.contains(s)){
						stack.push(s);
						visited.add(s);
					}
				}
				*/
			}
		}
		if(currentcc != null){
			localCCs.add(currentcc);
			currentcc = null;
			waitlist.clear();
		}
		return headLoc;
	}



	public Statement stmtForNode(CFGNode n, CFGNode pred ,Cluster c, ExprVar ind, List<EdgePair> lst, Set<CFGNode> visited){
		if(!c.contains(n) || visited.contains(n)){
			int sz = lst.size();
			Statement t = new StmtAssign(ind, new ExprConstInt(lst.size()) );
			if(!visited.contains(n)){
				lst.add(new EdgePair(n, sz));
				n.changePred(pred, c.head);
			}else{
				lst.add(new EdgePair(c.head, sz));
				n.removePred(pred);
				c.head.addPred(c.head);
			}
			return t;
		}
		visited.add(n);
		if(n.isStmt()){
			assert n.getSuccs().size() == 1;
			CFGNode succ = n.getSuccs().get(0).node;
			Statement s = stmtForNode(succ, n, c, ind, lst, visited);
			s = new StmtBlock(n.getStmt(), s);
			return s;
		}
		if(n.isExpr()){
			assert n.getPreStmt() == null;

			EdgePair ep1 = n.getSuccs().get(0);
			EdgePair ep2 = n.getSuccs().get(1);

			CFGNode suc[] = new CFGNode[2];
			suc[ep1.label] = ep1.node;
			suc[ep2.label] = ep2.node;
			HashSet<CFGNode> hn = new HashSet<CFGNode>();
			hn.addAll(visited);
			Statement sf = stmtForNode(suc[0], n, c, ind, lst, hn);
			hn.clear();
			hn.addAll(visited);
			Statement st = stmtForNode(suc[1], n, c, ind, lst, hn);

			return new StmtIfThen(n.getStmt(), n.getExpr(), st, sf);
		}
		return null;
	}




	public Statement eliminateConsecutiveIfs(Statement s){
		//TODO fill in the body of this function.
		return s;
	}










	public CFGNode processCluster(Cluster c,  ExprVar ind){
		List<EdgePair> lst = new ArrayList<EdgePair>();
		Statement s = stmtForNode(c.head, null, c, ind, lst, new HashSet<CFGNode>());
		s = new StmtBlock(new StmtVarDecl((FEContext) null, TypePrimitive.inttype, ind.getName(), ExprConstInt.zero ), s);
		assert lst.size() > 0;
		CFGNode n = c.head;
		if(lst.size() == 1){
			n.setPreStmt(null);
			n.changeExpr(null);
			n.changeStmt(s);
			n.getSuccs().clear();
			n.getSuccs().add(new EdgePair(lst.get(0).node, null));
		}else{
			n.setPreStmt(s);
			n.changeExpr(ind);
			n.getSuccs().clear();
			n.getSuccs().addAll(lst);
			n.makeSpecial();
		}
		return n;
	}





	public CFG simplifyAcrossBranches(CFG cfg){
		List<Cluster> clusters = new ArrayList<Cluster>();
		CFGNode head = cfg.getEntry();
		int headLoc = this.createClusters(cfg, clusters);

		for(int i=0; i<clusters.size(); ++i){
			CFGNode tmp = processCluster(clusters.get(i), new ExprVar((FEContext) null, "_ind"));
			if(headLoc == i){
				head = tmp;
			}
		}

		Stack<CFGNode> stack = new Stack<CFGNode>();
		Set<CFGNode> visited = new HashSet<CFGNode>();
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
				n.checkNeighbors();
				simplST = true;
				simplSF = true;
				sf = nsf;
				st = nst;
				if(nst.isStmt() && nsf.isStmt() ){
					Statement s = new StmtIfThen(n.getExpr(), n.getExpr(), st.getStmt(), sf.getStmt() );
					if(n.getPreStmt() == null){
						n.setPreStmt(s);
					}else{
						n.setPreStmt(new StmtBlock(s, n.getPreStmt()));
					}
					assert st.getSuccs().size() == 1 && sf.getSuccs().size() == 1;
					if(st.getSuccs().get(0).node != sf.getSuccs().get(0).node){
						changeSucc(n, st, st.getSuccs().get(0).node);
						changeSucc(n, sf, sf.getSuccs().get(0).node);
						n.checkNeighbors();
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
						n.checkNeighbors();
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
					Statement s = new StmtIfThen(n.getExpr(), n.getExpr(), st.getStmt(), null);
					if(n.getPreStmt() == null){
						n.setPreStmt(s);
					}else{
						n.setPreStmt(new StmtBlock(s, n.getPreStmt()));
					}

					assert st.getSuccs().size() == 1 ;
					if(st.getSuccs().get(0).node != sf){
						changeSucc(n, st,  st.getSuccs().get(0).node);
						n.checkNeighbors();
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
						n.checkNeighbors();
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
					Statement s = new StmtIfThen(n.getExpr(), new ExprUnary(n.getExpr(), ExprUnary.UNOP_NOT, n.getExpr()), sf.getStmt(), null);
					if(n.getPreStmt() == null){
						n.setPreStmt(s);
					}else{
						n.setPreStmt(new StmtBlock(s, n.getPreStmt()));
					}

					assert sf.getSuccs().size() == 1 ;
					if(sf.getSuccs().get(0).node != st){
						changeSucc(n, sf, sf.getSuccs().get(0).node);
						n.checkNeighbors();
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
						n.checkNeighbors();
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
				n.checkNeighbors();
				s1 = ns1;
				if(s1.getPreds().size() <= 2){
					if(s1.isStmt()){
						n.changeStmt(new StmtBlock(n.getStmt(), s1.getStmt() ));
						assert s1.getSuccs().size() == 1;
						changeSucc(n, s1, s1.getSuccs().get(0).node);
						n.checkNeighbors();
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
							EdgePair ep = it.next();
							n.addSucc(ep);
							ep.node.changePred(s1, n);
						}
						n.checkNeighbors();
						return n;
					}
				}
			}
		}
		return n;
	}

	CFGNode processCC(List<CFGNode> cc){
		Set<CFGNode> ccset = new HashSet<CFGNode>(cc);
		assert cc.size() > 0;
		CFGNode head = cc.get(0);
		head = simplifyNode(head, ccset, new HashSet<CFGNode>());
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

