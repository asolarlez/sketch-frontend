package sketch.compiler.passes.preprocessing;

import sketch.compiler.ast.core.FEReplacer;
import sketch.compiler.ast.core.FieldDecl;
import sketch.compiler.ast.core.exprs.ExprStar;

/**
 * disallow any ?? in global fields
 * @author gatoatigrado
 */
public class ForbidStarsInFieldDecls extends FEReplacer {
    ForbidStars forbid_stars_visitor = new ForbidStars();

    @Override
    public Object visitFieldDecl(FieldDecl field) {
        forbid_stars_visitor.visitFieldDecl(field);
        return super.visitFieldDecl(field);
    }

    public static class ForbidStars extends FEReplacer {
        @Override
        public Object visitExprStar(ExprStar star) {
            throw new java.lang.IllegalStateException("stars not allowed in field " +
                star.getContext().toString());
        }
    }
}
