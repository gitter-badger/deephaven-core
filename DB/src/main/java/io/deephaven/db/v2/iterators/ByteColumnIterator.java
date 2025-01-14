/*
 * Copyright (c) 2016-2021 Deephaven Data Labs and Patent Pending
 */

package io.deephaven.db.v2.iterators;

import io.deephaven.base.Procedure;
import io.deephaven.db.tables.Table;
import io.deephaven.db.v2.sources.ColumnSource;
import io.deephaven.db.v2.utils.Index;
import org.jetbrains.annotations.NotNull;

import java.util.PrimitiveIterator;

/**
 * Iteration support for boxed or primitive bytes contained with a ColumnSource.
 */
public class ByteColumnIterator extends ColumnIterator<Byte> implements PrimitiveIterator<Byte, Procedure.UnaryByte> {

    public ByteColumnIterator(@NotNull final Index index, @NotNull final ColumnSource<Byte> columnSource) {
        super(index, columnSource);
    }

    public ByteColumnIterator(@NotNull final Table table, @NotNull final String columnName) {
        //noinspection unchecked
        this(table.getIndex(), table.getColumnSource(columnName));
    }

    @SuppressWarnings("WeakerAccess")
    public byte nextByte() {
        return columnSource.getByte(indexIterator.nextLong());
    }

    @Override
    public void forEachRemaining(@NotNull final Procedure.UnaryByte action) {
        while (hasNext()) {
            action.call(nextByte());
        }
    }
}
