package io.deephaven.web.client.api.barrage;

import elemental2.core.*;
import io.deephaven.javascript.proto.dhinternal.arrow.flight.flatbuf.message_generated.org.apache.arrow.flatbuf.FieldNode;
import io.deephaven.javascript.proto.dhinternal.arrow.flight.flatbuf.message_generated.org.apache.arrow.flatbuf.RecordBatch;
import io.deephaven.javascript.proto.dhinternal.arrow.flight.flatbuf.schema_generated.org.apache.arrow.flatbuf.Buffer;
import io.deephaven.javascript.proto.dhinternal.flatbuffers.Builder;
import io.deephaven.javascript.proto.dhinternal.flatbuffers.Long;
import io.deephaven.javascript.proto.dhinternal.io.deephaven.barrage.flatbuf.barrage_generated.io.deephaven.barrage.flatbuf.BarrageMessageType;
import io.deephaven.javascript.proto.dhinternal.io.deephaven.barrage.flatbuf.barrage_generated.io.deephaven.barrage.flatbuf.BarrageMessageWrapper;
import io.deephaven.javascript.proto.dhinternal.io.deephaven.barrage.flatbuf.barrage_generated.io.deephaven.barrage.flatbuf.BarrageModColumnMetadata;
import io.deephaven.javascript.proto.dhinternal.io.deephaven.barrage.flatbuf.barrage_generated.io.deephaven.barrage.flatbuf.BarrageUpdateMetadata;
import io.deephaven.web.shared.data.*;
import io.deephaven.web.shared.data.columns.*;
import jsinterop.base.Js;
import org.gwtproject.nio.TypedArrayHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Utility to read barrage record batches.
 */
public class BarrageUtils {
    private static final int MAGIC = 0x6E687064;

    // TODO #1049 another wrapper that makes something which looks like a stream and manages rpcTicket internally
    public static Uint8Array barrageMessage(Builder innerBuilder, int messageType, Uint8Array rpcTicket, int sequence,
            boolean halfCloseAfterMessage) {
        Builder outerBuilder = new Builder(1024);
        // noinspection deprecation - this deprecation is incorrect, tsickle didn't understand that only one overload is
        // deprecated
        double messageOffset = BarrageMessageWrapper.createMsgPayloadVector(outerBuilder, innerBuilder.asUint8Array());
        // noinspection deprecation - this deprecation is incorrect, tsickle didn't understand that only one overload is
        // deprecated
        double rpcTicketOffset = BarrageMessageWrapper.createRpcTicketVector(outerBuilder, rpcTicket);
        double offset = BarrageMessageWrapper.createBarrageMessageWrapper(outerBuilder, MAGIC, messageType,
                messageOffset, rpcTicketOffset, Long.create(sequence, 0), halfCloseAfterMessage);
        outerBuilder.finish(offset);
        return outerBuilder.asUint8Array();
    }

    public static Uint8Array barrageMessage(Uint8Array rpcTicket, int sequence, boolean halfCloseAfterMessage) {
        Builder builder = new Builder(1024);
        // noinspection deprecation - this deprecation is incorrect, tsickle didn't understand that only one overload is
        // deprecated
        double rpcTicketOffset = BarrageMessageWrapper.createRpcTicketVector(builder, rpcTicket);
        double offset = BarrageMessageWrapper.createBarrageMessageWrapper(builder, MAGIC, BarrageMessageType.None, 0,
                rpcTicketOffset, Long.create(sequence, 0), halfCloseAfterMessage);
        builder.finish(offset);
        return builder.asUint8Array();
    }

    /**
     * Iterator wrapper that allows peeking at the next item, if any.
     */
    private static class Iter<T> implements Iterator<T> {
        private final Iterator<T> wrapped;
        private T next;

        private Iter(Iterator<T> wrapped) {
            this.wrapped = wrapped;
        }

        public T peek() {
            if (next != null) {
                return next;
            }
            return next = next();
        }

        @Override
        public boolean hasNext() {
            return next != null || wrapped.hasNext();
        }

        @Override
        public T next() {
            if (next == null) {
                return wrapped.next();
            }
            T val = next;
            next = null;
            return val;
        }
    }

    public static Uint8Array makeUint8ArrayFromBitset(BitSet bitset) {
        int length = (bitset.cardinality() + 7) / 8;
        Uint8Array array = new Uint8Array(length);
        byte[] bytes = bitset.toByteArray();
        for (int i = 0; i < bytes.length; i++) {
            array.setAt(i, (double) bytes[i]);
        }

        return array;
    }

    public static Uint8Array serializeRanges(Set<RangeSet> rangeSets) {
        final RangeSet s;
        if (rangeSets.size() == 0) {
            return new Uint8Array(0);
        } else if (rangeSets.size() == 1) {
            s = rangeSets.iterator().next();
        } else {
            s = new RangeSet();
            for (RangeSet rangeSet : rangeSets) {
                rangeSet.rangeIterator().forEachRemaining(s::addRange);
            }
        }

        ByteBuffer payload = CompressedRangeSetReader.writeRange(s);
        ArrayBufferView buffer = TypedArrayHelper.unwrap(payload);
        return new Uint8Array(buffer);
    }

    public static ByteBuffer typedArrayToLittleEndianByteBuffer(Uint8Array data) {
        ArrayBuffer slicedBuffer = data.<Uint8Array>slice().buffer;
        ByteBuffer bb = TypedArrayHelper.wrap(slicedBuffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb;
    }

    public static ByteBuffer typedArrayToLittleEndianByteBuffer(Int8Array data) {
        ArrayBuffer slicedBuffer = data.<Int8Array>slice().buffer;
        ByteBuffer bb = TypedArrayHelper.wrap(slicedBuffer);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb;
    }

    public static TableSnapshot createSnapshot(RecordBatch header, ByteBuffer body, BarrageUpdateMetadata barrageUpdate,
            boolean isViewport, String[] columnTypes) {
        RangeSet added;

        final RangeSet includedAdditions;
        if (barrageUpdate == null) {
            includedAdditions = RangeSet.ofRange(0, (long) (header.length().toFloat64() - 1));
        } else {
            added = new CompressedRangeSetReader()
                    .read(typedArrayToLittleEndianByteBuffer(barrageUpdate.addedRowsArray()));
            if (isViewport) {
                includedAdditions = new CompressedRangeSetReader()
                        .read(typedArrayToLittleEndianByteBuffer(barrageUpdate.addedRowsIncludedArray()));
            } else {
                // if this isn't a viewport, then a second index isn't sent, because all rows are included
                includedAdditions = added;
            }
        }

        // read the nodes and buffers into iterators so that we can descend into the data columns as necessary
        Iter<FieldNode> nodes =
                new Iter<>(IntStream.range(0, (int) header.nodesLength()).mapToObj(header::nodes).iterator());
        Iter<Buffer> buffers =
                new Iter<>(IntStream.range(0, (int) header.buffersLength()).mapToObj(header::buffers).iterator());
        ColumnData[] columnData = new ColumnData[columnTypes.length];
        for (int columnIndex = 0; columnIndex < columnTypes.length; ++columnIndex) {
            columnData[columnIndex] =
                    readArrowBuffer(body, nodes, buffers, (int) includedAdditions.size(), columnTypes[columnIndex]);
        }

        return new TableSnapshot(includedAdditions, columnData, (long) header.length().toFloat64());// note that this
                                                                                                    // truncates
                                                                                                    // precision if we
                                                                                                    // have more than
                                                                                                    // around 2^52 rows
    }

    public static DeltaUpdatesBuilder deltaUpdates(BarrageUpdateMetadata barrageUpdate, boolean isViewport,
            String[] columnTypes) {
        return new DeltaUpdatesBuilder(barrageUpdate, isViewport, columnTypes);
    }

    public static class DeltaUpdatesBuilder {
        private final DeltaUpdates deltaUpdates = new DeltaUpdates();
        private final BarrageUpdateMetadata barrageUpdate;
        private final String[] columnTypes;
        private int recordBatchesSeen = 0;

        public DeltaUpdatesBuilder(BarrageUpdateMetadata barrageUpdate, boolean isViewport, String[] columnTypes) {
            this.barrageUpdate = barrageUpdate;
            this.columnTypes = columnTypes;

            deltaUpdates.setAdded(new CompressedRangeSetReader()
                    .read(typedArrayToLittleEndianByteBuffer(barrageUpdate.addedRowsArray())));
            deltaUpdates.setRemoved(new CompressedRangeSetReader()
                    .read(typedArrayToLittleEndianByteBuffer(barrageUpdate.removedRowsArray())));

            deltaUpdates.setShiftedRanges(
                    new ShiftedRangeReader().read(typedArrayToLittleEndianByteBuffer(barrageUpdate.shiftDataArray())));

            RangeSet includedAdditions;
            if (isViewport) {
                includedAdditions = new CompressedRangeSetReader()
                        .read(typedArrayToLittleEndianByteBuffer(barrageUpdate.addedRowsIncludedArray()));
            } else {
                // if this isn't a viewport, then a second index isn't sent, because all rows are included
                includedAdditions = deltaUpdates.getAdded();
            }
            deltaUpdates.setIncludedAdditions(includedAdditions);
            deltaUpdates.setSerializedAdditions(new DeltaUpdates.ColumnAdditions[0]);
            deltaUpdates.setSerializedModifications(new DeltaUpdates.ColumnModifications[0]);
        }

        /**
         * Appends a new record batch and payload. Returns true if this was the final record batch that was expected.
         */
        public boolean appendRecordBatch(RecordBatch recordBatch, ByteBuffer body) {
            assert recordBatchesSeen < barrageUpdate.numAddBatches() + barrageUpdate.numModBatches();
            if (barrageUpdate.numAddBatches() > recordBatchesSeen) {
                handleAddBatch(recordBatch, body);
            } else {
                handleModBatch(recordBatch, body);
            }
            recordBatchesSeen++;
            return recordBatchesSeen == barrageUpdate.numAddBatches() + barrageUpdate.numModBatches();
        }

        private void handleAddBatch(RecordBatch recordBatch, ByteBuffer body) {
            Iter<FieldNode> nodes = new Iter<>(
                    IntStream.range(0, (int) recordBatch.nodesLength()).mapToObj(recordBatch::nodes).iterator());
            Iter<Buffer> buffers = new Iter<>(
                    IntStream.range(0, (int) recordBatch.buffersLength()).mapToObj(recordBatch::buffers).iterator());

            DeltaUpdates.ColumnAdditions[] addedColumnData = new DeltaUpdates.ColumnAdditions[columnTypes.length];
            for (int columnIndex = 0; columnIndex < columnTypes.length; ++columnIndex) {
                assert nodes.hasNext() && buffers.hasNext();
                ColumnData columnData = readArrowBuffer(body, nodes, buffers, (int) nodes.peek().length().toFloat64(),
                        columnTypes[columnIndex]);

                addedColumnData[columnIndex] = new DeltaUpdates.ColumnAdditions(columnIndex, columnData);
            }
            deltaUpdates.setSerializedAdditions(addedColumnData);
        }

        private void handleModBatch(RecordBatch recordBatch, ByteBuffer body) {
            Iter<FieldNode> nodes = new Iter<>(
                    IntStream.range(0, (int) recordBatch.nodesLength()).mapToObj(recordBatch::nodes).iterator());
            Iter<Buffer> buffers = new Iter<>(
                    IntStream.range(0, (int) recordBatch.buffersLength()).mapToObj(recordBatch::buffers).iterator());

            DeltaUpdates.ColumnModifications[] modifiedColumnData =
                    new DeltaUpdates.ColumnModifications[columnTypes.length];
            for (int columnIndex = 0; columnIndex < columnTypes.length; ++columnIndex) {
                assert nodes.hasNext() && buffers.hasNext();

                BarrageModColumnMetadata columnMetadata = barrageUpdate.nodes(columnIndex);
                RangeSet modifiedRows = new CompressedRangeSetReader()
                        .read(typedArrayToLittleEndianByteBuffer(columnMetadata.modifiedRowsArray()));

                ColumnData columnData = readArrowBuffer(body, nodes, buffers, (int) nodes.peek().length().toFloat64(),
                        columnTypes[columnIndex]);
                modifiedColumnData[columnIndex] =
                        new DeltaUpdates.ColumnModifications(columnIndex, modifiedRows, columnData);
            }
            deltaUpdates.setSerializedModifications(modifiedColumnData);
        }

        public DeltaUpdates build() {
            return deltaUpdates;
        }
    }

    private static ColumnData readArrowBuffer(ByteBuffer data, Iter<FieldNode> nodes, Iter<Buffer> buffers, int size,
            String columnType) {
        // explicit cast to be clear that we're rounding down
        BitSet valid = readValidityBufferAsBitset(data, size, buffers.next());
        FieldNode thisNode = nodes.next();
        boolean hasNulls = thisNode.nullCount().toFloat64() != 0;
        size = Math.min(size, (int) thisNode.length().toFloat64());

        Buffer positions = buffers.next();
        switch (columnType) {
            // for simple well-supported typedarray types, wrap and return
            case "int":
                assert positions.length().toFloat64() >= size * 4;
                Int32Array intArray = new Int32Array(TypedArrayHelper.unwrap(data).buffer,
                        (int) positions.offset().toFloat64(), size);
                return new IntArrayColumnData(Js.uncheckedCast(intArray));
            case "short":
                assert positions.length().toFloat64() >= size * 2;
                Int16Array shortArray = new Int16Array(TypedArrayHelper.unwrap(data).buffer,
                        (int) positions.offset().toFloat64(), size);
                return new ShortArrayColumnData(Js.uncheckedCast(shortArray));
            case "boolean":
            case "java.lang.Boolean":
            case "byte":
                assert positions.length().toFloat64() >= size;
                Int8Array byteArray =
                        new Int8Array(TypedArrayHelper.unwrap(data).buffer, (int) positions.offset().toFloat64(), size);
                return new ByteArrayColumnData(Js.uncheckedCast(byteArray));
            case "double":
                assert positions.length().toFloat64() >= size * 8;
                Float64Array doubleArray = new Float64Array(TypedArrayHelper.unwrap(data).buffer,
                        (int) positions.offset().toFloat64(), size);
                return new DoubleArrayColumnData(Js.uncheckedCast(doubleArray));
            case "float":
                assert positions.length().toFloat64() >= size * 4;
                Float32Array floatArray = new Float32Array(TypedArrayHelper.unwrap(data).buffer,
                        (int) positions.offset().toFloat64(), size);
                return new FloatArrayColumnData(Js.uncheckedCast(floatArray));
            case "char":
                assert positions.length().toFloat64() >= size * 2;
                Uint16Array charArray = new Uint16Array(TypedArrayHelper.unwrap(data).buffer,
                        (int) positions.offset().toFloat64(), size);
                return new CharArrayColumnData(Js.uncheckedCast(charArray));
            // longs are a special case despite being java primitives
            case "long":
            case "io.deephaven.db.tables.utils.DBDateTime":
                assert positions.length().toFloat64() >= size * 8;
                long[] longArray = new long[size];

                data.position((int) positions.offset().toFloat64());
                for (int i = 0; i < size; i++) {
                    longArray[i] = data.getLong();
                }
                return new LongArrayColumnData(longArray);
            // all other types are read out in some custom way
            case "java.time.LocalTime":// LocalDateArrayColumnData
                assert positions.length().toFloat64() >= size * 6;
                data.position((int) positions.offset().toFloat64());
                LocalDate[] localDateArray = new LocalDate[size];
                for (int i = 0; i < size; i++) {
                    int year = data.getInt();
                    byte month = data.get();
                    byte day = data.get();
                    localDateArray[i] = new LocalDate(year, month, day);
                }
                return new LocalDateArrayColumnData(localDateArray);
            case "java.time.LocalDate":// LocalTimeArrayColumnData
                assert positions.length().toFloat64() == size * 7;
                LocalTime[] localTimeArray = new LocalTime[size];

                data.position((int) positions.offset().toFloat64());
                for (int i = 0; i < size; i++) {
                    int nano = data.getInt();
                    byte hour = data.get();
                    byte minute = data.get();
                    byte second = data.get();
                    data.position(data.position() + 1);// aligned for next read
                    localTimeArray[i] = new LocalTime(hour, minute, second, nano);
                }
                return new LocalTimeArrayColumnData(localTimeArray);
            default:
                // remaining types have an offset buffer to read first
                IntBuffer offsets = readOffsets(data, size, positions);

                if (columnType.endsWith("[]")) {
                    nodes.next();
                    // array type, also read the inner valid buffer and inner offset buffer
                    BitSet innerValid = readValidityBufferAsBitset(data, size, buffers.next());
                    IntBuffer innerOffsets = readOffsets(data, size, buffers.next());

                    Buffer payload = buffers.next();

                    switch (columnType) {
                        case "java.lang.String[]":
                            String[][] strArrArr = new String[size][];

                            for (int i = 0; i < size; i++) {
                                if (hasNulls && !valid.get(i)) {
                                    continue;
                                }
                                int arrayStart = offsets.get(i);
                                int instanceSize = offsets.get(i + 1) - arrayStart;
                                String[] strArr = new String[instanceSize];
                                for (int j = 0; j < instanceSize; j++) {
                                    assert innerOffsets != null;
                                    if (!innerValid.get(j)) {
                                        assert innerOffsets.get(j) == innerOffsets.get(j + 1)
                                                : innerOffsets.get(j) + " == " + innerOffsets.get(j + 1);
                                        continue;
                                    }
                                    // might be cheaper to do views on the underlying bb (which will be copied anyway
                                    // into the String)
                                    data.position((int) (payload.offset().toFloat64()) + offsets.get(i));
                                    byte[] stringBytes = new byte[data.remaining()];
                                    data.get(stringBytes);
                                    strArr[j] = new String(stringBytes, StandardCharsets.UTF_8);
                                }
                                strArrArr[i] = strArr;
                            }

                            return new StringArrayArrayColumnData(strArrArr);
                        default:
                            throw new IllegalStateException("Can't decode column of type " + columnType);
                    }

                } else {
                    // non-array, variable length stuff, just grab the buffer and read ranges specified by offsets
                    Buffer payload = buffers.next();

                    switch (columnType) {
                        case "java.lang.String":
                            String[] stringArray = new String[size];
                            for (int i = 0; i < size; i++) {
                                if (hasNulls && !valid.get(i)) {
                                    continue;
                                }
                                byte[] stringBytes = new byte[offsets.get(i + 1) - offsets.get(i)];
                                data.position((int) (payload.offset().toFloat64()) + offsets.get(i));
                                data.get(stringBytes);
                                stringArray[i] = new String(stringBytes, StandardCharsets.UTF_8);// new
                                                                                                 // String(Js.<char[]>uncheckedCast(stringBytes));
                            }
                            return new StringArrayColumnData(stringArray);
                        case "java.math.BigDecimal":
                            BigDecimal[] bigDecArray = new BigDecimal[size];
                            for (int i = 0; i < size; i++) {
                                if (hasNulls && !valid.get(i)) {
                                    continue;
                                }
                                data.position((int) (payload.offset().toFloat64()) + offsets.get(i));
                                int scale = data.getInt();
                                bigDecArray[i] = new BigDecimal(readBigInt(data), scale);
                            }
                            return new BigDecimalArrayColumnData(bigDecArray);
                        case "java.math.BigInteger":
                            BigInteger[] bigIntArray = new BigInteger[size];
                            for (int i = 0; i < size; i++) {
                                if (hasNulls && !valid.get(i)) {
                                    continue;
                                }
                                data.position((int) (payload.offset().toFloat64()) + offsets.get(i));
                                bigIntArray[i] = readBigInt(data);
                            }

                            return new BigIntegerArrayColumnData(bigIntArray);
                        default:
                            throw new IllegalStateException("Can't decode column of type " + columnType);
                    }
                }
        }
    }

    private static BigInteger readBigInt(ByteBuffer data) {
        int length = data.getInt();
        byte[] bytes = new byte[length];
        data.get(bytes);
        return new BigInteger(bytes);
    }

    private static BitSet readValidityBufferAsBitset(ByteBuffer data, int size, Buffer buffer) {
        if (size == 0 || buffer.length().toFloat64() == 0) {
            // these buffers are optional (and empty) if the column is empty, or if it has primitives and we've allowed
            // DH nulls
            return new BitSet(0);
        }
        data.position((int) buffer.offset().toFloat64());
        BitSet valid = readBitSetWithLength(data, (int) (buffer.length().toFloat64()));
        return valid;
    }

    private static BitSet readBitSetWithLength(ByteBuffer data, int lenInBytes) {
        byte[] array = new byte[lenInBytes];
        data.get(array);

        return BitSet.valueOf(array);
    }

    private static IntBuffer readOffsets(ByteBuffer data, int size, Buffer buffer) {
        if (size == 0) {
            IntBuffer emptyOffsets = IntBuffer.allocate(1);
            return emptyOffsets;
        }
        data.position((int) buffer.offset().toFloat64());
        IntBuffer offsets = data.slice().asIntBuffer();
        offsets.limit(size + 1);
        return offsets;
    }

}
