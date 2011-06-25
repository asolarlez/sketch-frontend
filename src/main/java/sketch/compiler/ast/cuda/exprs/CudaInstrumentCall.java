package sketch.compiler.ast.cuda.exprs;

import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.compiler.ast.core.exprs.ExprVar;
import sketch.compiler.ast.core.exprs.Expression;

/**
 * intermediate node; full ast node (versus some other hack) since we want to capture any
 * array renaming.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class CudaInstrumentCall extends Expression {
    protected final ExprVar toImplement;

    private final ExprVar implVariable;
    protected final String implName;

    public CudaInstrumentCall(final FENode prev, final ExprVar toImplement,
            final ExprVar implVariable, final String implName)
    {
        super(prev);
        this.toImplement = toImplement;
        this.implVariable = implVariable;
        this.implName = implName;
    }

    public ExprVar getToImplement() {
        return toImplement;
    }

    public ExprVar getImplVariable() {
        return implVariable;
    }

    public String getImplName() {
        return implName;
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitCudaInstrumentCall(this);
    }
}
