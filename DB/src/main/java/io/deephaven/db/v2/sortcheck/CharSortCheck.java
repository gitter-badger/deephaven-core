package io.deephaven.db.v2.sortcheck;

import io.deephaven.db.util.DhCharComparisons;
import io.deephaven.db.v2.sources.chunk.Attributes;
import io.deephaven.db.v2.sources.chunk.CharChunk;
import io.deephaven.db.v2.sources.chunk.Chunk;

public class CharSortCheck implements SortCheck {
    static final SortCheck INSTANCE = new CharSortCheck();

    @Override
    public int sortCheck(Chunk<? extends Attributes.Values> valuesToCheck) {
        return sortCheck(valuesToCheck.asCharChunk());
    }

    private int sortCheck(CharChunk<? extends Attributes.Values> valuesToCheck) {
        if (valuesToCheck.size() == 0) {
            return -1;
        }
        char last = valuesToCheck.get(0);
        for (int ii = 1; ii < valuesToCheck.size(); ++ii) {
            final char current = valuesToCheck.get(ii);
            if (!leq(last, current)) {
                return ii - 1;
            }
            last = current;
        }
        return -1;
    }

    // region comparison functions
    private static int doComparison(char lhs, char rhs) {
        return DhCharComparisons.compare(lhs, rhs);
    }
    // endregion comparison functions

    private static boolean leq(char lhs, char rhs) {
        return doComparison(lhs, rhs) <= 0;
    }
}
