package sketch.compiler.dataflow.preprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtAssign;
import sketch.compiler.ast.core.stmts.StmtBlock;
import sketch.compiler.ast.core.stmts.StmtFor;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.Type;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class PropagateFinals extends FEReplacer {

	Map<String, Expression > finals = new HashMap<String, Expression>();
	
	
	public Object visitExprVar(ExprVar exp) {
		if(finals.containsKey(exp.getName())){
			return finals.get( exp.getName() );
		}
		return exp; 
	}
	
	public Object visitStmtBlock(StmtBlock stmt)
    {
	 
	 Object obj = super.visitStmtBlock(stmt);
	 finals.clear();
	 return obj;
	 
    }
	
	 public Object visitStmtFor(StmtFor stmt)
    {	 	 
	 Statement newInit = (Statement)stmt.getInit().accept(this);
	 finals.clear();
     Expression newCond = doExpression(stmt.getCond());
     Statement newIncr = (Statement)stmt.getIncr().accept(this);
     Statement newBody = (Statement)stmt.getBody().accept(this);
     if (newInit == stmt.getInit() && newCond == stmt.getCond() &&
         newIncr == stmt.getIncr() && newBody == stmt.getBody())
         return stmt;
     return new StmtFor(stmt, newInit, newCond, newIncr,
                        newBody);	 
    }
	
	
	public Object visitStmtAssign(StmtAssign stmt)
    {
		String lhsstr = stmt.getLHS().toString();
		if( finals.containsKey(lhsstr) ){
			finals.remove(lhsstr);
		}
        return super.visitStmtAssign(stmt);
    }
	
	
	public Object visitStmtVarDecl(StmtVarDecl stmt)
    {
        List<Expression> newInits = new ArrayList<Expression>();
        List<Type> newTypes = new ArrayList<Type>();
        for (int i = 0; i < stmt.getNumVars(); i++)
        {
            Expression init = stmt.getInit(i);           
            Type t = (Type) stmt.getType(i).accept(this);
            if (init != null){
                init = doExpression(init);
                if(t instanceof TypePrimitive){
                	finals.put(stmt.getName(i), init);
                }
            }
            newInits.add(init);
            newTypes.add(t);
        }
        return new StmtVarDecl(stmt, newTypes,
                               stmt.getNames(), newInits);
    }
}
