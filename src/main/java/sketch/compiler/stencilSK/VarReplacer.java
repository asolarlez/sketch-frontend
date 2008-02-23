package streamit.frontend.stencilSK;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import streamit.frontend.nodes.ExprVar;
import streamit.frontend.nodes.Expression;
import streamit.frontend.nodes.FEContext;
import streamit.frontend.nodes.FEReplacer;
import streamit.frontend.nodes.Statement;
import streamit.frontend.nodes.StmtBlock;
import streamit.frontend.nodes.StmtVarDecl;



class VarReplacer extends FEReplacer{
	String oldName;
	Expression newName;

	VarReplacer(String oldName, String newName){
		this.oldName = oldName;
		this.newName = new ExprVar((FEContext) null, newName);
	}

	VarReplacer(String oldName, Expression newName){
		this.oldName = oldName;
		this.newName = newName;
	}

	public Object visitExprVar(ExprVar exp) {
		if( exp.getName().equals(oldName)){
			return newName;
		}else{
			return exp;
		}
	}


	public Object visitStmtBlock(StmtBlock stmt)
    {
        List<Statement> oldStatements = newStatements;
        newStatements = new ArrayList<Statement>();
        for (Iterator iter = stmt.getStmts().iterator(); iter.hasNext(); )
        {
            Statement s = (Statement)iter.next();
            // completely ignore null statements, causing them to
            // be dropped in the output
            if (s == null)
                continue;
            doStatement(s);
        }
        Statement result = new StmtBlock(stmt, newStatements);
        newStatements = oldStatements;
        return result;
    }


	public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        List<Expression> newInits = new ArrayList<Expression>();
        List<String> newNames = new ArrayList<String>();
        boolean changed = false;
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            Expression init = stmt.getInit(i);
            Expression oinit = init;
            if (init != null)
                init = doExpression(init);
            if( oinit != init) changed = true;
            newInits.add(init);
            String name = stmt.getName(i);

            if( newName instanceof ExprVar ){
            	String sNewName = ((ExprVar)newName).getName();
            	if( name.equals(oldName) ){
            		name = sNewName;
            		changed = true;
            	}

            	if( name.equals(sNewName) ){
            		//In this case, we need to rename this variable with a fresh name.
            		//This implies going back to the enclosing scope, and renaming all
            		//uses of this variable. Not that easy.
            		assert false : "This has not yet been implemented. Please implemented.";
            	}
            }else{
            	//In this case, we have to search through all the variables that appear
            	//in newName, and if any of them match name, we must rename this variable
            	//with a fresh name.
            	assert false : "Not yet implemented";
            	if( name.equals(oldName) ){
            		assert false : "This has not been implemented";
            	}
            }



            newNames.add(name);
        }
        if( !changed ) return stmt;
        return new StmtVarDecl(stmt, stmt.getTypes(),
                               stmt.getNames(), newInits);
    }

}

