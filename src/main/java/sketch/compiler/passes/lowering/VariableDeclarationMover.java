package streamit.frontend.passes;

import java.util.*;

import streamit.frontend.nodes.*;

/**
 * Collects all variable declarations from a function (except for loop vars)
 * and moves them to the top of that function.
 * It's a good idea to run VariableDisambiguator beforehand to avoid duplicate
 * variable names.
 */
public class VariableDeclarationMover extends FEReplacer
{
	private List<String> variables;
    private Map<String,Type> varTypes;

    public VariableDeclarationMover()
    {
    	variables=new ArrayList<String>();
    	varTypes=new HashMap<String,Type>();
    }

	public Object visitFunction(Function func)
	{
		variables.clear();
		varTypes.clear();
		func=(Function) super.visitFunction(func);
		List bodyStmts=new ArrayList();
		for(String name: variables) {
			bodyStmts.add(new StmtVarDecl(func, varTypes.get(name), name, null));
		}
		StmtBlock bodyBlock=(StmtBlock) func.getBody();
		bodyStmts.addAll(bodyBlock.getStmts());
		variables.clear();
		varTypes.clear();
		return new Function(func, func.getCls(), func.getName(),
			func.getReturnType(), func.getParams(), func.getSpecification(),
			new StmtBlock(bodyBlock, bodyStmts));
	}

	public Object visitStmtFor(StmtFor stmt)
	{
		// we skip visiting the loop variable declaration
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if(newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt, stmt.getInit(), stmt.getCond(),
        	stmt.getIncr(), newBody);
	}

	public Object visitStmtVarDecl(StmtVarDecl stmt)
	{
		for(int i=0;i<stmt.getNumVars();i++) {
			String name=stmt.getName(i);
			if(varTypes.get(name)==null) {
				variables.add(name);
				varTypes.put(name,stmt.getType(i));
				if(stmt.getInit(i)!=null) {
					addStatement(new StmtAssign(
						new ExprVar(stmt,name), stmt.getInit(i)));
				}
			} else {
				assert false: "duplicate variable declaration for"+name+": "+stmt+" at "+stmt;
			}
		}
		return null;
	}

}
