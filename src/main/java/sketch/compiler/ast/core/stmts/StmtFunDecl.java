package sketch.compiler.ast.core.stmts;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.Function;

public class StmtFunDecl extends Statement {

    Function decl;

    public StmtFunDecl(FENode context, Function decl) {
        super(context);
        this.decl = decl;
    }

    public Function getDecl() {
        return decl;
    }

    public String toString() {
        return decl.toString();
    }
    /**
     * @param context
     * @deprecated
     */
    public StmtFunDecl(FEContext context, Function decl) {
        super(context);
        this.decl = decl;
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitStmtFunDecl(this);
    }

}
