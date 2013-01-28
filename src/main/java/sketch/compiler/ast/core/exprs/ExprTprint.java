package sketch.compiler.ast.core.exprs;

import java.util.Arrays;
import java.util.List;

import sketch.compiler.ast.core.FEContext;
import sketch.compiler.ast.core.FENode;
import sketch.compiler.ast.core.FEVisitor;
import sketch.util.datastructures.TprintTuple;

/**
 * List of print tuples. Do not use this, deprecated.
 * 
 * @deprecated
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class ExprTprint extends Expression {
    public final List<TprintTuple> expressions;

    public enum CudaType {
        Unknown, Allthreads, Somethreads
    };

    public final CudaType cuda_type;

    @SuppressWarnings("deprecation")
    public ExprTprint(FEContext context, CudaType typ, List<TprintTuple> expressions) {
        super(context);
        cuda_type = typ;
        this.expressions = expressions;
    }

    public ExprTprint(FENode context, CudaType typ, List<TprintTuple> expressions) {
        super(context);
        cuda_type = typ;
        this.expressions = expressions;
    }

    public ExprTprint(FENode context, CudaType typ, TprintTuple... expressions) {
        super(context);
        cuda_type = typ;
        this.expressions = Arrays.asList(expressions);
    }

    @Override
    public Object accept(FEVisitor v) {
        return v.visitExprTprint(this);
    }
}
