package sketch.compiler.stencilSK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;


public class ParamTree{

	public static int MAX_POS = 2000;

	private Map<FENode, treeNode> tnMap = new HashMap<FENode, treeNode>();
	public treeNode getTNode(FENode node){
		return tnMap.get(node);
	}
	class treeNode{
		StmtVarDecl vdecl=null;
		StmtVarDecl posParam = null;
		private loopHist lh=null;
		private treeNode father=null;
		private List<treeNode> children = new ArrayList<treeNode>();
		private int pos = -1;
		private int level = -1;


		public Expression highCond(){
		    return lh.high;
		}
		
		public int getStage(){
			return lh.stage;
		}

		public void incrStage(){
			++lh.stage;
			assert lh.stage < MAX_POS : "The maximum number of statements is not set to a high enough value.";
		}

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
			if( lh != null){
				this.vdecl = lh.newVD();
				this.posParam = new StmtVarDecl((FEContext) null, TypePrimitive.inttype, ArrFunction.PPPREFIX + vdecl.getName(0), new ExprConstInt(MAX_POS));
			}
			this.father = father;
		}
		public void add(treeNode tn){
			tn.pos = children.size();
			tn.level = level + 1;
			children.add(tn);
		}
		public PathIterator pathIter(){
			PathIterator pi = new PathIterator(true);
			return pi;
		}

		/**
		 * Returns a limited PathIterator; that is, an iterator over the set of 
		 * induction variables for all loops surrounding the current position in the loop tree. 
		 * 
		 */
		public PathIterator limitedPathIter(){
			PathIterator pi = new PathIterator(false);
			return pi;
		}

		public class PathIterator implements Iterator<StmtVarDecl>{
			private int[] path;
			private int step;
			private treeNode tn;
			/**
			 * If withPos is true, then the iterator also returns
			 * varDecls for the position parameters. Otherwise,
			 * it only returns them for the inductionVar parameters.
			 */
			private boolean withPos;
			/**
			 * Only used when withPos == true.
			 * Flips from true to false. True means
			 * that next() returns a position parameter.
			 * False means next() returns an indVar parameter.
			 *
			 */
			private boolean wpState;
			public PathIterator(boolean withPos){
				this.withPos = withPos;
				wpState = true;
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
				if( withPos ){
					if(wpState ){
						wpState = false;
						return tn.posParam;
					}else{
						wpState = true;
						treeNode tmp = tn.child(path[step]);
						tn = tmp;
						++step;
						return tmp.vdecl;
					}
				}else{
					treeNode tmp = tn.child(path[step]);
					tn = tmp;
					++step;
					return tmp.vdecl;
				}
			}

			public loopHist lhNext(){
				assert !withPos ;
				treeNode tmp = tn.child(path[step]);
				tn = tmp;
				++step;
				return tmp.lh;
			}

			public void makeUnlimited(){
				assert !withPos ;
				withPos = true;
				wpState = true;
			}

			public treeNode tnNext(){
				assert !withPos ;
				treeNode tmp = tn.child(path[step]);
				tn = tmp;
				++step;
				return tmp;
			}

			public boolean hasNext(){
				if(withPos){
					return step != path.length || wpState;
				}else{
					return step != path.length;
				}

			}
			public void remove(){
				assert false;
			}

		}
	}


	public class FullIterator implements Iterator<StmtVarDecl>{
		private treeNode tn;
		/**
		 * If withPos is true, then the iterator also returns
		 * varDecls for the position parameters. Otherwise,
		 * it only returns them for the inductionVar parameters.
		 */
		private final boolean withPos;
		/**
		 * Only used when withPos == true.
		 * Flips from true to false. True means
		 * that next() returns a position parameter.
		 * False means next() returns an indVar parameter.
		 *
		 */
		private boolean wpState;

		/**
		 *
		 * @param withPos make true if you want the iterator
		 * to include possition parameters.
		 */
		FullIterator(boolean withPos){
			tn = root;
			this.withPos = withPos;
			if(withPos){
				wpState = true;
			}else{
				if(hasNext()) next();
			}
		}

		private void advanceTN(){
			treeNode curr = tn;
			if( tn.nchildren() > 0 ){
				tn = tn.child(0);
            } else {
                if (tn == root) {
                    tn = null;
                    return;
                }
                }
                int pos = tn.pos;
				tn = tn.father;
                while (tn.nchildren() <= pos + 1) {
                    if (tn == root) {
                        tn = null;
                        return;
                    }
					pos = tn.pos;
					tn = tn.father;
				}
                tn = tn.child(pos + 1);
			}
		}

		public StmtVarDecl next(){
			treeNode curr = tn;
			if( withPos ){
				if( wpState ){
					wpState = false;
					advanceTN();
					return curr.posParam;
				}else{
					wpState = true;
					return curr.vdecl;
				}
			}else{
				advanceTN();
				return curr.vdecl;
			}
		}
		public boolean hasNext(){
			return tn != null;
		}
		public void remove(){
			assert false;
		}
	}

	public FullIterator iterator(){
		return new FullIterator(true);
	}

	public FullIterator limitedIterator(){
		return new FullIterator(false);
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