package sketch.compiler.ast.core.typs;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import sketch.compiler.ast.core.exprs.ExprConstInt;
import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.util.fcns.ZipIdxEnt;

import static sketch.util.DebugOut.assertFalse;

import static sketch.util.fcns.ZipWithIndex.zipwithindex;

/**
 * an array with indices (possibly named) split by commas
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class TypeCommaArray extends Type implements TypeArrayInterface {
    private final List<Expression> lengthParams;
    private final Type base;

    @SuppressWarnings("unchecked")
    public TypeCommaArray(CudaMemoryType memtyp, Type base,
            Vector<Expression> lengthParams)
    {
        super(memtyp);
        this.base = base;
        this.lengthParams =
                Collections.unmodifiableList((List<Expression>) lengthParams.clone());
    }

    public TypeCommaArray(Type base, Vector<Expression> lengthParams) {
        this(CudaMemoryType.UNDEFINED, base, lengthParams);
    }

    public Type getBase() {
        return base;
    }

    public Expression getProdLength() {
        int prod = 1;
        for (Expression e : getLengthParams()) {
            Expression e2 = e.thisOrNamedExpr();
            if (e2 instanceof ExprConstInt) {
                prod *= ((ExprConstInt) e2).getVal();
            } else {
                assertFalse("getProdLength() -- Comma array parameters need "
                        + "to be lowered to int constants for now, sorry!");
            }
        }
        return new ExprConstInt(getLengthParams().get(0), prod);
    }

    public List<Expression> getLengthParams() {
        return lengthParams;
    }

    public int namedIndexToPositional(String name) {
        for (ZipIdxEnt<Expression> e : zipwithindex(getLengthParams())) {
            if (e.entry instanceof ExprNamedParam) {
                if (((ExprNamedParam) e.entry).getName().equals(name)) {
                    return e.idx;
                }
            } else {
                assertFalse("array indices must be all named or unnamed", this);
            }
        }
        assertFalse("Cannot find index named", name);
        return 0;
    }
}
