package sketch.compiler.passes.lowering;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.Function;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;

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
        return func.creator().body(new StmtBlock(bodyBlock, bodyStmts)).create();
	}

	public Object visitStmtFor(StmtFor stmt)
	{
		// we skip visiting the loop variable declaration
        Statement newBody = (Statement)stmt.getBody().accept(this);
        if(newBody == stmt.getBody())
            return stmt;
        return new StmtFor(stmt, stmt.getInit(), stmt.getCond(),
 stmt.getIncr(), newBody,
                stmt.isCanonical());
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
