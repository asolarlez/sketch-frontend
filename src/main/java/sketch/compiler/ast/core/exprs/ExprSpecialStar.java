package sketch.compiler.ast.core.exprs;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.typs.Type;

/**
 * Designed for MINVAR's; this is like an <code>ExprStar</code>, but the backend will make
 * sure it is the minimal integer value that satisfies the specification. NOTE: should be
 * renamed to ExprMinStar
 * 
 * @author Armando Solar-Lezama from code by gatoatigrado (nicholas tung) [email: ntung at
 *         ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ExprSpecialStar extends ExprHole {
    public final String name;
    private static int cnt = 0;
    public ExprSpecialStar(FENode context, String name, int size, Type typ) {
        super(context, size, typ);
        this.name = name;
        this.holeName = "H__BND" + cnt;
        ++cnt;
    }
    
    @Override
    public Object accept(FEVisitor v) {
        return v.visitExprSpecialStar(this);
    }
}
