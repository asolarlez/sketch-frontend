package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import streamit.frontend.nodes.FENode;
import streamit.frontend.nodes.StmtVarDecl;


public class ParamTree{
	
	private Map<FENode, treeNode> tnMap = new HashMap<FENode, treeNode>();
	public treeNode getTNode(FENode node){
		return tnMap.get(node);
	}
	class treeNode{
		StmtVarDecl vdecl=null;
		loopHist lh=null;
		private treeNode father=null;
		private List<treeNode> children = new ArrayList<treeNode>();
		private int pos = -1;
		private int level = -1;
		public int nchildren(){
			return children.size();
		}
		public int getLevel(){
			return level;
		}
		public treeNode getFather(){
			return father;
		}
		public treeNode child(int i){
			return children.get(i);
		}
		public treeNode(loopHist lh, treeNode father){
			this.lh = lh;
			if( lh != null){this.vdecl = lh.newVD(); }
			this.father = father;
		}
		public void add(treeNode tn){
			tn.pos = children.size();
			tn.level = level + 1;
			children.add(tn);
		}
		public PathIterator pathIter(){
			PathIterator pi = new PathIterator();			
			return pi;
		}

		public class PathIterator implements Iterator<StmtVarDecl>{
			private int[] path;
			private int step;
			private treeNode tn;
			public PathIterator(){
				path = new int[level];
				step = 0;
				tn = root;
				treeNode ltn = treeNode.this;
				int ii=level-1;
				while(ltn != root){
					path[ii] = ltn.pos;
					ltn = ltn.father;
					--ii;
				}		
				assert ii==-1;
			}
			public StmtVarDecl next(){				
				treeNode tmp = tn.child(path[step]);		
				tn = tmp;
				++step;
				return tmp.vdecl;
			}
			
			public loopHist lhNext(){				
				treeNode tmp = tn.child(path[step]);		
				tn = tmp;
				++step;
				return tmp.lh;
			}
			
			public boolean hasNext(){
				return step != path.length;
			}
			public void remove(){
				assert false;
			}
			
		}
	}
	
	
	public class FullIterator implements Iterator<StmtVarDecl>{
		private treeNode tn;
		FullIterator(){			
			tn = root;		
			if(hasNext()) next();
		}
		public StmtVarDecl next(){			
			treeNode curr = tn;		
			if( tn.nchildren() > 0 ){
				tn = tn.child(0);				
			}else{
				int pos = tn.pos;
				tn = tn.father;
				while(tn.nchildren()<= pos+1){
					if( tn == root ){tn = null;  return curr.vdecl;}
					pos = tn.pos;
					tn = tn.father;
				}
				tn = tn.child(pos+1);
			}
			return curr.vdecl;
		}		
		public boolean hasNext(){
			return tn != null; 
		}
		public void remove(){
			assert false;
		}
	}
	
	public FullIterator iterator(){
		return new FullIterator();
	}
	
	public String toString(){
		String rv = " ";
		for(FullIterator it = iterator(); it.hasNext(); ){
			rv += it.next().toString() + ", ";
		}
		return rv;
	}
	
	private treeNode cnode;
	private treeNode root;
	private int depth;
	public ParamTree(){
		root = new treeNode(new loopHist("BASE", null, null), null);
		root.level = 0;
		cnode = root;
	}
	
	public treeNode beginLevel(loopHist lh, FENode node){
		treeNode tmp = beginLevel(lh);
		this.tnMap.put(node, tmp);
		if( tmp.getLevel() > depth) depth = tmp.getLevel();
		return tmp;
	}
	public treeNode beginLevel(loopHist lh){
		treeNode tmp = new treeNode(lh, cnode);
		cnode.add(tmp);
		cnode = tmp;
		return tmp;
	}
	
	public treeNode getRoot(){
		return root;
	}
	public void endLevel(){
		cnode = cnode.father;
	}	
	
	
}