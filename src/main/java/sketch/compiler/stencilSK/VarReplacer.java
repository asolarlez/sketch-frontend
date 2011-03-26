package sketch.compiler.stencilSK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;



public class VarReplacer extends FEReplacer{
    Map<String, Expression> repl;
	

	public VarReplacer(String oldName, String newName){
	    repl = new HashMap<String, Expression>();
	    repl.put(oldName, new ExprVar((FENode) null, newName));		
	}

	public VarReplacer(String oldName, Expression newName){
	    repl = new HashMap<String, Expression>();
	    repl.put(oldName, newName);
	}
	
	public VarReplacer(Map<String, Expression> repl){
	    this.repl = repl;
	}

	public Object visitExprVar(ExprVar exp) {
		if( repl.containsKey(exp.getName())){
			return repl.get(exp.getName());
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
        List<Type> newTypes = new ArrayList<Type>();
        boolean changed = false;
        assert repl.size() == 1 : "NYI";
        Entry<String, Expression> ent = repl.entrySet().iterator().next();
        String oldName = ent.getKey();
        Expression newName = ent.getValue(); 
        
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            Expression init = stmt.getInit(i);
            Expression oinit = init;
            if (init != null)
                init = doExpression(init);
            if( oinit != init) changed = true;
            newInits.add(init);
            Type otype = stmt.getType(i);
            Type ntype = (Type)otype.accept(this);
            if(otype != ntype ) changed = true;
            newTypes.add(ntype);
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
            	if(!(newName instanceof ExprConstInt)){
//            		In this case, we have to search through all the variables that appear
                	//in newName, and if any of them match name, we must rename this variable
                	//with a fresh name.
                	assert false : "Not yet implemented";
                	if( name.equals(oldName) ){
                		assert false : "This has not been implemented";
                	}	
            	}
            }



            newNames.add(name);
        }
        if( !changed ) return stmt;
        return new StmtVarDecl(stmt, newTypes,
                               stmt.getNames(), newInits);
    }

}

