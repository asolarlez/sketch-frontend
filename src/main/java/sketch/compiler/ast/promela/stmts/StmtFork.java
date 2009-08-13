package sketch.compiler.ast.promela.stmts;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class StmtFork extends Statement {
	private StmtVarDecl loopVar;
	   private Expression iter;
	    private Statement body;

	    /** Creates a new loop. */
	    public StmtFork(FENode context, String vname, Expression iter, Statement body)
	    {
	    	this (context,
	    		  new StmtVarDecl(context, TypePrimitive.inttype, vname, null),
	    		  iter, body);
	    }

	    /** Creates a new loop.
	     * @deprecated
	     */
	    public StmtFork(FEContext context, String vname, Expression iter, Statement body)
	    {
	    	this (context,
	    		  new StmtVarDecl(context, TypePrimitive.inttype, vname, null),
	    		  iter, body);
	    }

	    public StmtFork(FENode context, StmtVarDecl loopVar, Expression iter, Statement body)
	    {
	        super(context);
	        this.iter = iter;
	        this.body = body;
	        this.loopVar = loopVar;
	    }

	    /**
	     *
	     * @param context
	     * @param loopVar
	     * @param iter
	     * @param body
	     * @deprecated
	     */
	    public StmtFork(FEContext context, StmtVarDecl loopVar, Expression iter, Statement body)
	    {
	        super(context);
	        this.iter = iter;
	        this.body = body;
	        this.loopVar = loopVar;
	    }

	    /** Return the number of iterations. */
	    public Expression getIter()
	    {
	        return iter;
	    }

	    /** Return the loop body of this. */
	    public Statement getBody()
	    {
	        return body;
	    }

	    public String getLoopVarName(){
	    	return loopVar.getName(0);
	    }

	    public StmtVarDecl getLoopVarDecl(){
	    	return loopVar;
	    }

	    /** Accept a front-end visitor. */
	    public Object accept(FEVisitor v)
	    {
	        return v.visitStmtFork(this);
	    }

	    public String toString()
	    {
	    	return "ploop("+ loopVar + " < " + iter+")...";
	    }

}
