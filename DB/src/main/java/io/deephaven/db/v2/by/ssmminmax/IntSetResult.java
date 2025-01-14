/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharSetResult and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.by.ssmminmax;

import io.deephaven.db.v2.sources.ArrayBackedColumnSource;
import io.deephaven.db.v2.sources.IntegerArraySource;
import io.deephaven.db.v2.ssms.IntSegmentedSortedMultiset;
import io.deephaven.db.v2.ssms.SegmentedSortedMultiSet;

import static io.deephaven.util.QueryConstants.NULL_INT;

public class IntSetResult implements SsmChunkedMinMaxOperator.SetResult {
    private final boolean minimum;
    private final IntegerArraySource resultColumn;

    public IntSetResult(boolean minimum, ArrayBackedColumnSource resultColumn) {
        this.minimum = minimum;
        this.resultColumn = (IntegerArraySource) resultColumn;
    }

    @Override
    public boolean setResult(SegmentedSortedMultiSet ssm, long destination) {
        final int newResult;
        if (ssm.size() == 0) {
            newResult = NULL_INT;
        } else {
            final IntSegmentedSortedMultiset intSsm = (IntSegmentedSortedMultiset) ssm;
            newResult = minimum ? intSsm.getMinInt() : intSsm.getMaxInt();
        }
        return setResult(destination, newResult);
    }

    @Override
    public boolean setResultNull(long destination) {
        return setResult(destination, NULL_INT);
    }

    private boolean setResult(long destination, int newResult) {
        final int oldResult = resultColumn.getAndSetUnsafe(destination, newResult);
        return oldResult != newResult;
    }
}
