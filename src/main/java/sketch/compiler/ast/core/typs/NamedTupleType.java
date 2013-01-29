package sketch.compiler.ast.core.typs;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import sketch.compiler.ast.core.exprs.ExprNamedParam;
import sketch.compiler.ast.core.exprs.ExprType;
import sketch.compiler.ast.core.exprs.Expression;
import sketch.compiler.ast.cuda.typs.CudaMemoryType;
import sketch.util.fcns.ZipEnt;

import static sketch.util.DebugOut.not_implemented;

import static sketch.util.Misc.nonnull;
import static sketch.util.fcns.Zip.zip;

/**
 * Do not use. named tuple, associating keys with values.
 * 
 * @deprecated
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class NamedTupleType extends Type {
    public final List<NamedTupleEntry> entries;

    /** temporary initial instance, before subtypes are resolved */
    public NamedTupleType(List<Expression> args) {
        super(CudaMemoryType.UNDEFINED);

        Vector<NamedTupleEntry> v = new Vector<NamedTupleEntry>();
        for (Expression e : args) {
            ExprNamedParam realE = (ExprNamedParam) e;
            v.add(new NamedTupleEntry(realE.getName(), realE.getExpr(), v.size()));
        }
        this.entries = Collections.unmodifiableList(v);
    }

    public NamedTupleType(CudaMemoryType cudaMemType, Vector<NamedTupleEntry> nextEntries)
    {
        super(cudaMemType);
        entries = Collections.unmodifiableList(nonnull(nextEntries));
    }

    public NamedTupleType nextOrThis(Vector<NamedTupleEntry> nextEntries) {
        for (ZipEnt<NamedTupleEntry, NamedTupleEntry> s : zip(this.entries, nextEntries))
        {
            if (s.right != s.left) {
                // create a new value
                return new NamedTupleType(this.getCudaMemType(), nextEntries);
            }
        }

        // wasn't changed
        return this;
    }

    /** check that all entries have been resolved to type expressions */
    public boolean checkAllEntriesResolved() {
        for (NamedTupleEntry e : this.entries) {
            if (e.maybeEntryType == null) {
                return false;
            }
        }
        return true;
    }

    /** an entry, e.g. "value=(int mod 40)" */
    public static class NamedTupleEntry {
        public final String name;
        public final Expression exprType;
        /** present in lower (later) stages of compilation */
        public final Type maybeEntryType;
        public final int idxZeroBased;

        public NamedTupleEntry(String name, Expression exprType, int idxZeroBased) {
            this.name = name;
            this.exprType = exprType;
            if (exprType instanceof ExprType) {
                this.maybeEntryType = ((ExprType) exprType).getType();
            } else {
                this.maybeEntryType = null;
            }
            this.idxZeroBased = idxZeroBased;
        }

        public NamedTupleEntry nextOrThis(Expression nextExpr) {
            if (nextExpr != this.exprType) {
                return new NamedTupleEntry(name, nextExpr, idxZeroBased);
            }
            return this;
        }
    }

    @Override
    public TypeComparisonResult compare(Type that) {
        return not_implemented();
    }
}
