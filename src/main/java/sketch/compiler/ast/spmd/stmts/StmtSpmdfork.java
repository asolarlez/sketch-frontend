package sketch.compiler.ast.spmd.stmts;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;

public class StmtSpmdfork extends Statement {
    private String loopVarName;
    private Expression nProc;
    private Statement body;

	    public StmtSpmdfork(FEContext context, StmtVarDecl decl, Expression nProc, Statement body)
	    {
	        super(context);
	        this.nProc = nProc;
	        this.body = body;
	        this.loopVarName = decl.getName(0);
	    }


	    public StmtSpmdfork(FENode context, String vname, Expression nProc, Statement body)
	    {
	        super(context);
	        this.nProc = nProc;
	        this.body = body;
	        this.loopVarName = vname;
	    }

	    /**
	     *
	     * @param context
	     * @param loopVar
	     * @param nProc
	     * @param body
	     * @deprecated
	     */
	    public StmtSpmdfork(FEContext context, String vname, Expression nProc, Statement body)
	    {
	        super(context);
	        this.nProc = nProc;
	        this.body = body;
	        this.loopVarName = vname;
	    }

            public StmtSpmdfork createWithNewBody(Statement newBody)
            {
                return new StmtSpmdfork(this.getContext(), this.getLoopVarName(), this.getNProc(), newBody);
            }

	    /** Return the number of nProcations. */
	    public Expression getNProc()
	    {
	        return nProc;
	    }

	    /** Return the loop body of this. */
	    public Statement getBody()
	    {
	        return body;
	    }

	    public String getLoopVarName(){
	    	return loopVarName;
	    }

	    /** Accept a front-end visitor. */
	    public Object accept(FEVisitor v)
	    {
                //System.out.println(v.getClass().toString() + " visit " + this.toString());
	        return v.visitStmtSpmdfork(this);
	    }

	    public String toString()
	    {
	    	return "spmdfork("+ loopVarName + " ; " + nProc+")...";
	    }

}
