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

package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprArrayInit;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypeArray;

/**
 * Pair up variable declarations and adjacent assignments.  Some of the
 * Kopi code depends on having initialized variables, but the front end
 * code generally goes out of its way to separate declarations and
 * initialization.  This looks for adjacent statements that deal with
 * the same variable, and combine them:
 *
 * <pre>
 * int[] v;
 * v = new int[4];
 * // becomes: int[] v = new int[4];
 * </pre>
 *
 * @author  David Maze &lt;dmaze@cag.lcs.mit.edu&gt;
 * @version $Id$
 */
public class AssembleInitializers extends FEReplacer
{

    class Vinfo {
        public int declpos = 0;
        public int firstuse = -1;

        Vinfo(int declpos) {
            this.declpos = declpos;
        }

        public String toString() {
            return "(" + declpos + ", " + firstuse + ")";
        }
    }

    class VarTrack {
        VarTrack parent;
        Map<String, Vinfo> vmap = new HashMap<String, AssembleInitializers.Vinfo>();
        int curPos = 0;

        Map<Integer, Integer> process() {
            Map<Integer, Integer> rm = new HashMap<Integer, Integer>();
            for (Vinfo vi : vmap.values()) {
                if (vi.firstuse - vi.declpos > 1) {
                    rm.put(vi.declpos, vi.firstuse);
                }
            }
            return rm;
        }

        VarTrack(VarTrack parent) {
            this.parent = parent;
        }

        void next() {
            curPos++;
        }

        void declVar(String name) {
            vmap.put(name, new Vinfo(curPos));
        }

        void varUsed(String name) {
            if (vmap.containsKey(name)) {
                Vinfo vi = vmap.get(name);
                if (vi.firstuse < 0) {
                    vi.firstuse = curPos;
                }
                return;
            }
            if (parent != null) {
                parent.varUsed(name);
            }
        }

    }

    VarTrack vtrack;

    public Object visitExprVar(ExprVar ev) {
        if (vtrack != null) {
            vtrack.varUsed(ev.getName());
        }
        return ev;
    }

    public Object visitStmtBlock(StmtBlock block) {
        VarTrack oldVt = vtrack;
        vtrack = new VarTrack(vtrack);

        List<Statement> slist = new java.util.ArrayList<Statement>();
        for (Statement s : block.getStmts()) {
            slist.add((Statement) s.accept(this));
            if (s instanceof StmtVarDecl) {
                StmtVarDecl svd = (StmtVarDecl) s;
                if (svd.getNumVars() == 1) {
                    if (svd.getInit(0) == null || svd.getInit(0) instanceof ExprConstInt)
                    {
                        vtrack.declVar(svd.getName(0));
                    }
                }
            }
            vtrack.next();
        }
        List<Statement> olist = new java.util.ArrayList<Statement>();
        Map<Integer, Integer> rmap = vtrack.process();
        Map<Integer, List<Statement>> pmap = new HashMap<Integer, List<Statement>>();
        int i = 0;
        for (Statement s : slist) {
            if (pmap.containsKey(i)) {
                olist.addAll(pmap.get(i));
            }
            if (rmap.containsKey(i)) {
                List<Statement> ls;
                if (pmap.containsKey(rmap.get(i))) {
                    ls = pmap.get( rmap.get(i) );
                }else{
                    ls = new ArrayList<Statement>();                    
                    pmap.put(rmap.get(i), ls);
                }
                ls.add(s);
            } else {
                olist.add(s);
            }
            ++i;
        }
        vtrack = oldVt;
        return auxiliary(new StmtBlock(olist));
    }

    public Object auxiliary(StmtBlock block)
    {
        List oldStatements = newStatements;
        newStatements = new java.util.ArrayList<Statement>();
        for (ListIterator iter = block.getStmts().listIterator();
             iter.hasNext(); )
        {
            Statement stmt = (Statement)iter.next();
            while (stmt instanceof StmtVarDecl && iter.hasNext())
            {
                Statement nst = (Statement)iter.next();
                iter.previous();
                if (!(nst instanceof StmtAssign)){
                	StmtVarDecl decl = (StmtVarDecl)stmt;

                	if(decl.getInit(0) == null){
	                	List newInits = new java.util.ArrayList();
	                	 for (int i = 0; i < decl.getNumVars(); i++)
	                     {
	                		Expression init = null;
	                		Type type = decl.getType(i);
	                		if(decl.getInit(i) != null){
	                			init =decl.getInit(i);
	                		}else{
								if( type instanceof TypeArray ){
									 Integer len = ((TypeArray)type).getLength().getIValue();
									if( len != null  ){
										int N =len;
                                        List<Expression> ilist =
                                                new ArrayList<Expression>(N);
                                        Expression def =
                                                ((TypeArray) type).getBase().defaultValue();
										for(int k=0; k<N; ++k){
                                            ilist.add(def);
										}
										init = new ExprArrayInit(decl, ilist);
									}
								}else{
                                    init = type.defaultValue();
								}
	                		}
	                		newInits.add(init);
	                     }
	                	 stmt = new StmtVarDecl(decl,
	                             decl.getTypes(),
	                             decl.getNames(),
	                             newInits);
                	}
                    break;
                }

                // check that the LHS of the next statement is
                // a simple variable
                Expression lhs = ((StmtAssign)nst).getLHS();
                Expression rhs = ((StmtAssign)nst).getRHS();
                if (!(lhs instanceof ExprVar)){
                	StmtVarDecl decl = (StmtVarDecl)stmt;
                	if(decl.getInit(0) == null){
	                	List newInits = new java.util.ArrayList();
	                	 for (int i = 0; i < decl.getNumVars(); i++)
	                     {
	                		Expression init = null;
	                		Type type = decl.getType(i);
	                		if(decl.getInit(i) != null){
	                			init =decl.getInit(i);
	                		}else{
								if( type instanceof TypeArray ){
									 Integer len = ((TypeArray)type).getLength().getIValue();
									 if(len != null){
										 int N = len;
										List<Expression> ilist = new ArrayList<Expression>(N);										
										for(int k=0; k<N; ++k){
											ilist.add( ExprConstInt.zero );
										}
										init = new ExprArrayInit(decl, ilist);
									 }
								}else{

									init = ExprConstInt.zero;
								}
	                		}
	                		newInits.add(init);
	                     }
	                	 stmt = new StmtVarDecl(decl,
	                             decl.getTypes(),
	                             decl.getNames(),
	                             newInits);
                	}
                    break;
                }
                String varName = ((ExprVar)lhs).getName();
                // Now, walk through the declaration.
                StmtVarDecl decl = (StmtVarDecl)stmt;
                List newInits = new java.util.ArrayList();
                boolean found = false;
                for (int i = 0; i < decl.getNumVars(); i++)
                {
                    Expression init = decl.getInit(i);
                    int ttt = rhs.toString().indexOf(lhs.toString());
                    if (decl.getName(i).equals(varName) &&
                        ttt  == -1
                        )
                    {
                        init = ((StmtAssign)nst).getRHS();
                        found = true;
                        iter.next(); // consume the assignment
                    }
                    newInits.add(init);
                }
                if (!found)
                    break;
                // So, if we've made it here, then newInits
                // is different from stmt's initializer list,
                // and we want to iterate.  Reassign stmt.
                stmt = new StmtVarDecl(decl,
                                       decl.getTypes(),
                                       decl.getNames(),
                                       newInits);
            }
            addStatement((Statement) stmt);
        }
        Statement result = new StmtBlock(block, newStatements);
        newStatements = oldStatements;
        return result;
    }
}
