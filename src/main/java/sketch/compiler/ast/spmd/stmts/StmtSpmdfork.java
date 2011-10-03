package sketch.compiler.ast.spmd.stmts;
import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;

import sketch.compiler.ast.spmd.exprs.SpmdPid;
import sketch.compiler.ast.core.stmts.StmtBlock;

import java.util.List;
import java.util.ArrayList;

public class StmtSpmdfork extends Statement {
    private Expression nProc;
    private Statement body;
    private static final SpmdPid SpmdPidConst = new SpmdPid(null);

	    public StmtSpmdfork(FEContext context, String vname, Expression nProc, Statement body)
	    {
	        super(context);
	        this.nProc = nProc;
                if (vname == null) {
	            this.body = body;
                } else {
                    StmtVarDecl decl = new StmtVarDecl(context, TypePrimitive.inttype, vname, SpmdPidConst);
                    this.body = new StmtBlock(decl, body);
                }
	    }

            public StmtSpmdfork createWithNewBody(Statement newBody)
            {
                return new StmtSpmdfork(this.getContext(), null, this.getNProc(), newBody);
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

	    /** Accept a front-end visitor. */
	    public Object accept(FEVisitor v)
	    {
                //System.out.println(v.getClass().toString() + " visit " + this.toString());
	        return v.visitStmtSpmdfork(this);
	    }

	    public String toString()
	    {
	    	return "spmdfork(" + nProc+")...";
	    }

}
