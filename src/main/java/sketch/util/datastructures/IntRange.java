package sketch.util.datastructures;

import static sketch.util.DebugOut.not_implemented;

import java.util.Iterator;

public class IntRange extends DefaultNiceObj<IntRange> implements Iterable<Long> {
    public final long min;
    public final long max;
    public final long infemum;
    public final long supremum;

    private IntRange(long min, long max) {
        super(min * 11088164766733L + max, 3773037081491L);
        this.min = min;
        this.max = max;
        this.infemum = min - 1;
        this.supremum = max + 1;
    }

    @Override
    public String toString() {
        return "IntRange( [" + min + ", " + max + "] )";
    }

    public boolean isEmpty() {
        return min > max;
    }
    
    public boolean isSingleton() {
        return min == max;
    }

    public long middle() {
        assert !isEmpty() : "please check Range.isEmpty first";
        return (max + min) / 2;
    }

    public static IntRange inclusive(long min, long max) {
        return new IntRange(min, max);
    }

    public static IntRange exclusive(long infemum, long supremum) {
        return new IntRange(infemum + 1, supremum - 1);
    }

    public IntRange nextSupremum(long supremum) {
        return new IntRange(this.min, supremum - 1);
    }

    public IntRange nextMax(long max) {
        return new IntRange(this.min, max);
    }

    public IntRange nextInfemum(long infemum) {
        return new IntRange(infemum + 1, this.max);
    }

    public IntRange nextMin(long min) {
        return new IntRange(min, this.max);
    }

    @Override
    public int baseCompare(IntRange o) {
        not_implemented("baseCompare()");
        return 0;
    }

    public Iterator<Long> iterator() {
        return new RangeIterator(min, max);
    }

    public static class RangeIterator implements Iterator<Long> {
        private final long max;
        private long curr;

        public RangeIterator(long min, long max) {
            this.max = max;
            this.curr = min;
        }

        public boolean hasNext() {
            return curr < max;
        }

        public Long next() {
            curr += 1;
            return curr - 1;
        }

        public void remove() {
            not_implemented("remove()");
        }
    }
}
