package sketch.compiler.ast.core.exprs;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.typs.Type;

/**
 * designed for MINVAR's
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class ExprSpecialStar extends ExprStar {
    public final String name;

    public ExprSpecialStar(FENode context, String name, int size, Type typ) {
        super(context, size, typ);
        this.name = name;
        this.starName = "H__BOUND";
    }
    
    @Override
    public Object accept(FEVisitor v) {
        return v.visitExprSpecialStar(this);
    }
}
