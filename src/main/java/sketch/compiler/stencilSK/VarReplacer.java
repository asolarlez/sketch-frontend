package sketch.compiler.stencilSK;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;



public class VarReplacer extends FEReplacer{
    Map<String, Expression> repl;
    Set<String> rhsVars;
	
    public Expression find(String var) {
        return repl.get(var);
    }

    public Type replace(Type e) {
        return (Type) e.accept(this);
    }

    void popRhsVars() {
        rhsVars = new HashSet<String>();
        FEReplacer fr = new FEReplacer() {
            public Object visitExprVar(ExprVar expr) {
                rhsVars.add(expr.getName());
                return expr;
            }
        };
        for (Expression e : repl.values()) {
            e.accept(fr);
        }
    }

	public VarReplacer(String oldName, String newName){
        repl = new HashMap<String, Expression>();
        repl.put(oldName, new ExprVar((FENode) null, newName));
        rhsVars = new HashSet<String>();
        rhsVars.add(newName);
	}

	public VarReplacer(String oldName, Expression newName){
	    repl = new HashMap<String, Expression>();
	    repl.put(oldName, newName);
        popRhsVars();
	}
	
	public VarReplacer(Map<String, Expression> repl){
	    this.repl = repl;
        popRhsVars();
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

    int cnt = 0;

	public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        List<Expression> newInits = new ArrayList<Expression>();
        List<String> newNames = new ArrayList<String>();
        List<Type> newTypes = new ArrayList<Type>();
        boolean changed = false;
        

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

            if (rhsVars.contains(name)) {
                String nname = name + "__" + (cnt++);
                repl.put(name, new ExprVar(stmt, nname));
                name = nname;
                changed = true;
            }

            newNames.add(name);
        }
        if( !changed ) return stmt;
        return new StmtVarDecl(stmt, newTypes, newNames, newInits);
    }

}

