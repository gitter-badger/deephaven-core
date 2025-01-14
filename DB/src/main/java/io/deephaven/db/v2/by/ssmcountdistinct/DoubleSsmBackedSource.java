/* ---------------------------------------------------------------------------------------------------------------------
 * AUTO-GENERATED CLASS - DO NOT EDIT MANUALLY - for any changes edit CharSsmBackedSource and regenerate
 * ------------------------------------------------------------------------------------------------------------------ */
package io.deephaven.db.v2.by.ssmcountdistinct;

import io.deephaven.db.tables.dbarrays.DbDoubleArray;
import io.deephaven.db.v2.sources.AbstractColumnSource;
import io.deephaven.db.v2.sources.ColumnSourceGetDefaults;
import io.deephaven.db.v2.sources.MutableColumnSourceGetDefaults;
import io.deephaven.db.v2.sources.ObjectArraySource;
import io.deephaven.db.v2.ssms.DoubleSegmentedSortedMultiset;
import io.deephaven.db.v2.utils.Index;

/**
 * A {@link SsmBackedColumnSource} for Doubles.
 */
public class DoubleSsmBackedSource extends AbstractColumnSource<DbDoubleArray>
                                 implements ColumnSourceGetDefaults.ForObject<DbDoubleArray>,
                                            MutableColumnSourceGetDefaults.ForObject<DbDoubleArray>,
                                            SsmBackedColumnSource<DoubleSegmentedSortedMultiset, DbDoubleArray> {
    private final ObjectArraySource<DoubleSegmentedSortedMultiset> underlying;
    private boolean trackingPrevious = false;

    //region Constructor
    public DoubleSsmBackedSource() {
        super(DbDoubleArray.class, double.class);
        underlying = new ObjectArraySource<>(DoubleSegmentedSortedMultiset.class, double.class);
    }
    //endregion Constructor

    //region SsmBackedColumnSource
    @Override
    public DoubleSegmentedSortedMultiset getOrCreate(long key) {
        DoubleSegmentedSortedMultiset ssm = underlying.getUnsafe(key);
        if(ssm == null) {
            //region CreateNew
            underlying.set(key, ssm = new DoubleSegmentedSortedMultiset(DistinctOperatorFactory.NODE_SIZE));
            //endregion CreateNew
        }
        ssm.setTrackDeltas(trackingPrevious);
        return ssm;
    }

    @Override
    public DoubleSegmentedSortedMultiset getCurrentSsm(long key) {
        return underlying.getUnsafe(key);
    }

    @Override
    public void clear(long key) {
        underlying.set(key, null);
    }

    @Override
    public void ensureCapacity(long capacity) {
        underlying.ensureCapacity(capacity);
    }

    @Override
    public ObjectArraySource<DoubleSegmentedSortedMultiset> getUnderlyingSource() {
        return underlying;
    }
    //endregion

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public DbDoubleArray get(long index) {
        return underlying.get(index);
    }

    @Override
    public DbDoubleArray getPrev(long index) {
        final DoubleSegmentedSortedMultiset maybePrev = underlying.getPrev(index);
        return maybePrev == null ? null : maybePrev.getPrevValues();
    }

    @Override
    public void startTrackingPrevValues() {
        trackingPrevious = true;
        underlying.startTrackingPrevValues();
    }

    @Override
    public void clearDeltas(Index indices) {
        indices.iterator().forEachLong(key -> {
            final DoubleSegmentedSortedMultiset ssm = getCurrentSsm(key);
            if(ssm != null) {
                ssm.clearDeltas();
            }
            return true;
        });
    }
}
