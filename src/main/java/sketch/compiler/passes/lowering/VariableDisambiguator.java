package streamit.frontend.passes;

import java.util.*;

import streamit.frontend.nodes.*;

/**
 * Visits the code looking for duplicate variables in different scopes of the 
 * same function. Prevents ambiguities/conflicts that may arise in later stages
 * by renaming variables so their names are unique within the function.
 */
public class VariableDisambiguator extends FEReplacer
{
	private Map<String,Object> variables;
    private Map<String,String> renameMap;
    private boolean collecting;
    
    public VariableDisambiguator()
    {
    	variables=new HashMap<String,Object>();
        renameMap=new HashMap<String,String>();
    }

	public Object visitFunction(Function func)
	{
		variables.clear();
		renameMap.clear();
		collecting=true;
		func=(Function) super.visitFunction(func);
		collecting=false;
		func=(Function) super.visitFunction(func);
		variables.clear();
		renameMap.clear();
		return func;
	}

	public Object visitStmtVarDecl(StmtVarDecl stmt)
	{
		if(collecting) {
			for(int i=0;i<stmt.getNumVars();i++) {
				String name=stmt.getName(i);
				if(variables.get(name)==null)
					variables.put(name,stmt);
			}
			return stmt;
		}
		//take care of variable substitution in the initializers first
		stmt=(StmtVarDecl) super.visitStmtVarDecl(stmt);
		
		List<String> names=new ArrayList<String>(stmt.getNames());		
		boolean change=false;
		for(int i=0;i<stmt.getNumVars();i++) {
			final String name=names.get(i);
			if(variables.get(name)!=stmt) {
				for(int suff=2;;suff++) {
					final String newName=name+suff;
					if(variables.get(newName)==null) {
						names.set(i,newName);
						renameMap.put(name,newName);
						variables.put(newName,newName);
						change=true;
						break;
					}
				}
			}
		}
		if(change) {
			stmt=new StmtVarDecl(stmt.getCx(),stmt.getTypes(),names,stmt.getInits());
		}
		return stmt;
	}

    public Object visitExprVar(ExprVar expr)
    {
    	if(collecting) return expr;
        expr = (ExprVar) super.visitExprVar(expr);
        String newName=renameMap.get(expr.getName());
        if(newName!=null) {
        	expr=new ExprVar(expr.getCx(),newName);
        }
        return expr;
    }

}
