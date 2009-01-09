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

package streamit.frontend.passes;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import streamit.frontend.nodes.ExprArrayInit;
import streamit.frontend.nodes.ExprConstInt;
import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtAssign;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtVarDecl;
import streamit.frontend.nodes.Type;
import streamit.frontend.nodes.TypeArray;

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
    public Object visitStmtBlock(StmtBlock block)
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
            addStatement((Statement)stmt.accept(this));
        }
        Statement result = new StmtBlock(block, newStatements);
        newStatements = oldStatements;
        return result;
    }
}
