package sketch.compiler.ast.cuda.stmts;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.core.stmts.Statement;
import sketch.compiler.ast.core.stmts.StmtVarDecl;
import sketch.compiler.ast.core.typs.TypePrimitive;
import sketch.compiler.ast.cuda.exprs.ExprRange;
import static sketch.util.Misc.nonnull;

/**
 * @deprecated
 */
public class StmtParfor extends Statement {
    private final Statement body;
    /** NOTE -- may be null! */
    private final StmtVarDecl iterVarDecl;
    /** nonnull */
    private final StmtVarDecl associatedIterVarDecl;
    private final ExprRange range;

    @SuppressWarnings("deprecation")
    public StmtParfor(FEContext context, Expression varname, Expression range,
            Statement body)
    {
        super(context);
        this.body = nonnull(body);
        String name = ((ExprVar) varname).getName();
        this.iterVarDecl = new StmtVarDecl(context, TypePrimitive.inttype, name, null);
        this.associatedIterVarDecl = this.iterVarDecl;
        this.range = nonnull((ExprRange) range);
    }

    private StmtParfor(FENode context, StmtVarDecl iterVarDecl,
            StmtVarDecl associatedIterVarDecl, ExprRange range, Statement body)
    {
        super(context);
        this.iterVarDecl = iterVarDecl;
        this.associatedIterVarDecl = nonnull(associatedIterVarDecl);
        this.range = nonnull(range);
        this.body = nonnull(body);
    }

    public StmtParfor next(StmtVarDecl iterVarDecl, ExprRange range, Statement body) {
        return new StmtParfor(this, iterVarDecl,
                (iterVarDecl == null ? this.getAssociatedIterVarDecl() : iterVarDecl), range,
                body);
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitStmtParfor(this);
    }

    public Statement getBody() {
        return body;
    }

    public StmtVarDecl getIterVarDecl() {
        return iterVarDecl;
    }

    public ExprRange getRange() {
        return range;
    }

    public StmtVarDecl getAssociatedIterVarDecl() {
        return associatedIterVarDecl;
    }
}
