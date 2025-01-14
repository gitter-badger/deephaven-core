/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit FloatRangeComparator and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.select.chunkfilters;

import io.deephaven.db.util.DhDoubleComparisons;
import io.deephaven.db.v2.select.ChunkFilter;
import io.deephaven.db.v2.sources.chunk.*;
import io.deephaven.db.v2.sources.chunk.Attributes.OrderedKeyIndices;
import io.deephaven.db.v2.sources.chunk.Attributes.Values;

public class DoubleRangeComparator {
    private DoubleRangeComparator() {} // static use only

    private abstract static class DoubleDoubleFilter implements ChunkFilter.DoubleChunkFilter {
        final double lower;
        final double upper;

        DoubleDoubleFilter(double lower, double upper) {
            this.lower = lower;
            this.upper = upper;
        }

        abstract public void filter(DoubleChunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys, WritableLongChunk<OrderedKeyIndices> results);
    }

    static class DoubleDoubleInclusiveInclusiveFilter extends DoubleDoubleFilter {
        private DoubleDoubleInclusiveInclusiveFilter(double lower, double upper) {
            super(lower, upper);
        }

        public void filter(DoubleChunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys, WritableLongChunk<OrderedKeyIndices> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final double value = values.get(ii);
                if (DhDoubleComparisons.geq(value, lower) && DhDoubleComparisons.leq(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    static class DoubleDoubleInclusiveExclusiveFilter extends DoubleDoubleFilter {
        private DoubleDoubleInclusiveExclusiveFilter(double lower, double upper) {
            super(lower, upper);
        }

        public void filter(DoubleChunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys, WritableLongChunk<OrderedKeyIndices> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final double value = values.get(ii);
                if (DhDoubleComparisons.geq(value, lower) && DhDoubleComparisons.lt(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    static class DoubleDoubleExclusiveInclusiveFilter extends DoubleDoubleFilter {
        private DoubleDoubleExclusiveInclusiveFilter(double lower, double upper) {
            super(lower, upper);
        }

        public void filter(DoubleChunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys, WritableLongChunk<OrderedKeyIndices> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final double value = values.get(ii);
                if (DhDoubleComparisons.gt(value, lower) && DhDoubleComparisons.leq(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    static class DoubleDoubleExclusiveExclusiveFilter extends DoubleDoubleFilter {
        private DoubleDoubleExclusiveExclusiveFilter(double lower, double upper) {
            super(lower, upper);
        }

        public void filter(DoubleChunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys, WritableLongChunk<OrderedKeyIndices> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final double value = values.get(ii);
                if (DhDoubleComparisons.gt(value, lower) && DhDoubleComparisons.lt(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    public static ChunkFilter.DoubleChunkFilter makeDoubleFilter(double lower, double upper, boolean lowerInclusive, boolean upperInclusive) {
        if (lowerInclusive) {
            if (upperInclusive) {
                return new DoubleDoubleInclusiveInclusiveFilter(lower, upper);
            } else {
                return new DoubleDoubleInclusiveExclusiveFilter(lower, upper);
            }
        } else {
            if (upperInclusive) {
                return new DoubleDoubleExclusiveInclusiveFilter(lower, upper);
            } else {
                return new DoubleDoubleExclusiveExclusiveFilter(lower, upper);
            }
        }
    }
}
