package sketch.compiler.ast.core.stmts;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.Expression;

/**
 * variable decl in the form of v := expr
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class StmtImplicitVarDecl extends Statement {
    private final String name;
    private final Expression initExpr;

    @SuppressWarnings("deprecation")
    public StmtImplicitVarDecl(FEContext cx, String name, Expression initExpr) {
        super(cx);
        this.name = name;
        this.initExpr = initExpr;
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitStmtImplicitVarDecl(this);
    }

    public String getName() {
        return name;
    }

    public Expression getInitExpr() {
        return initExpr;
    }
}
