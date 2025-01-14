/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharRangeComparator and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.select.chunkfilters;

import io.deephaven.db.util.DhByteComparisons;
import io.deephaven.db.v2.select.ChunkFilter;
import io.deephaven.db.v2.sources.chunk.*;
import io.deephaven.db.v2.sources.chunk.Attributes.OrderedKeyIndices;
import io.deephaven.db.v2.sources.chunk.Attributes.Values;

public class ByteRangeComparator {
    private ByteRangeComparator() {} // static use only

    private abstract static class ByteByteFilter implements ChunkFilter.ByteChunkFilter {
        final byte lower;
        final byte upper;

        ByteByteFilter(byte lower, byte upper) {
            this.lower = lower;
            this.upper = upper;
        }

        abstract public void filter(ByteChunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys, WritableLongChunk<OrderedKeyIndices> results);
    }

    static class ByteByteInclusiveInclusiveFilter extends ByteByteFilter {
        private ByteByteInclusiveInclusiveFilter(byte lower, byte upper) {
            super(lower, upper);
        }

        public void filter(ByteChunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys, WritableLongChunk<OrderedKeyIndices> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final byte value = values.get(ii);
                if (DhByteComparisons.geq(value, lower) && DhByteComparisons.leq(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    static class ByteByteInclusiveExclusiveFilter extends ByteByteFilter {
        private ByteByteInclusiveExclusiveFilter(byte lower, byte upper) {
            super(lower, upper);
        }

        public void filter(ByteChunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys, WritableLongChunk<OrderedKeyIndices> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final byte value = values.get(ii);
                if (DhByteComparisons.geq(value, lower) && DhByteComparisons.lt(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    static class ByteByteExclusiveInclusiveFilter extends ByteByteFilter {
        private ByteByteExclusiveInclusiveFilter(byte lower, byte upper) {
            super(lower, upper);
        }

        public void filter(ByteChunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys, WritableLongChunk<OrderedKeyIndices> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final byte value = values.get(ii);
                if (DhByteComparisons.gt(value, lower) && DhByteComparisons.leq(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    static class ByteByteExclusiveExclusiveFilter extends ByteByteFilter {
        private ByteByteExclusiveExclusiveFilter(byte lower, byte upper) {
            super(lower, upper);
        }

        public void filter(ByteChunk<? extends Values> values, LongChunk<OrderedKeyIndices> keys, WritableLongChunk<OrderedKeyIndices> results) {
            results.setSize(0);
            for (int ii = 0; ii < values.size(); ++ii) {
                final byte value = values.get(ii);
                if (DhByteComparisons.gt(value, lower) && DhByteComparisons.lt(value, upper)) {
                    results.add(keys.get(ii));
                }
            }
        }
    }

    public static ChunkFilter.ByteChunkFilter makeByteFilter(byte lower, byte upper, boolean lowerInclusive, boolean upperInclusive) {
        if (lowerInclusive) {
            if (upperInclusive) {
                return new ByteByteInclusiveInclusiveFilter(lower, upper);
            } else {
                return new ByteByteInclusiveExclusiveFilter(lower, upper);
            }
        } else {
            if (upperInclusive) {
                return new ByteByteExclusiveInclusiveFilter(lower, upper);
            } else {
                return new ByteByteExclusiveExclusiveFilter(lower, upper);
            }
        }
    }
}
