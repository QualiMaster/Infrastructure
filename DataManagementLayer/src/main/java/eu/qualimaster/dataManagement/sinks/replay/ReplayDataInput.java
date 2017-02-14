package eu.qualimaster.dataManagement.sinks.replay;

import eu.qualimaster.dataManagement.common.replay.Field;
import static eu.qualimaster.dataManagement.common.replay.Field.DELIMITER;
import eu.qualimaster.dataManagement.common.replay.ReplayUtils;
import eu.qualimaster.dataManagement.common.replay.Tuple;
import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.storage.hbase.HBaseBatchStorageSupport;
import static eu.qualimaster.dataManagement.storage.hbase.HBaseBatchStorageSupport.COLUMN_FAMILY_BYTES;

import eu.qualimaster.dataManagement.storage.support.IStorageSupport;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

/**
 * The wrapper of replay data input. Current version is backed by the
 * HBase storage support.
 *
 * Note 09.06.15 - Tuan: We implement the speed using the simplest
 * form of aggregation: Only keep the first / last tick in the interval,
 * which is determined by the speedFactor (factor 1 ==> 1 second). For
 * the current version, all data is delivered to the client and got
 * aggregated on-the-fly
 *
 * @author tuan
 * @since 08/06/16
 * @version 1.0
 */
public class ReplayDataInput implements IDataInput, Closeable {

    private static Logger LOG = LoggerFactory.getLogger(ReplayDataInput.class);

    /** hold the connection to the underlying Hbase table */
    private HBaseBatchStorageSupport db;

    /** Map fields in the tuple to the HBase-compatible bytes */
    private byte[][] fields;

    /**
     * 2016-10-18 Tuan: Fix a bug related to the way the records are read.
     * Key1 DELIM Key2 DELIM Key3 DELIM timestamp
     * keyIdx contains the sorted indices of the key fields
     */
    private int[] keyIdx;

    /** Index of the timestamp field */
    private int tsIdx;

    /** Cursor to the current field */
    private int idx;

    /** "peek iterator" in-line implementation */
    private ResultScanner scanner;

    /** Keep the isClosed flag to avoid excessive calls to Scanner.close() */
    private boolean isClosed = false;

    private Iterator<Result> iter;
    private Result peekedRow;

    /** Internally cached variables */
    private String[] rowKey;

    private boolean eod;

    /** the current prefix of query */
    private byte[][] filter;

    /* for logging purpose */
    private String queryStr;

    /** Reference to the aggregator */
    private ReplayAggregator aggregator;

    public ReplayDataInput(Tuple schema, IStorageSupport db) {
        if (!(db instanceof HBaseBatchStorageSupport)) {
            throw new RuntimeException("Invalid replay store: "
                    + "Current version only works with HBase");
        }
        this.db = (HBaseBatchStorageSupport)db;
        this.db.connect();
        int n = schema.getFields().size();
        fields = new byte[n][];
        filter = new byte[2][];

        // The first item is the number of key fields
        keyIdx = new int[n];

        for (int i = 0; i < n; i++) {
            Field f = schema.getField(i);
            fields[i] = Bytes.toBytes(f.getName());
            if (f.isKey()) {
                keyIdx[0]++;
                keyIdx[keyIdx[0]] = i;
            }
            else if (f.isTimesamp()) {
                tsIdx = i;
            }
        }
        eod = true;

        this.db.connect();
    }

    public void updateQuery(String query, Date startDate, Date enDate, ReplayAggregator aggregator) {
        parseQuery(query, startDate, enDate);
        this.aggregator = aggregator;
        eod = true;
        if (scanner != null)
            scanner.close();
        peekedRow = null;
        Object obj = null;
        try {
            LOG.info("Querying db with query range: " + new String(filter[0]) + ", " + new String(filter[1]));
            obj = db.get(filter);
            if (obj == null || !(obj instanceof ResultScanner)) {
                LOG.warn("Error getting the scanner from the query " + query);
            }
            scanner = (ResultScanner)obj;
            isClosed = false;

            iter = scanner.iterator();
            _advance();
        } catch (Exception e) {
            LOG.warn("ERROR processing the query " + query, e);
        }
    }

    /**
     * Parse the query: We assume the query is just the key (composite key
     * possible, where individual keys are delimited by char 183) */

    /*
    private void parseQuery(String query, Date startDate, Date endDate) {
        long begin = ReplayUtils.getTimestamp(startDate);
        long end = ReplayUtils.getTimestamp(endDate);
        StringBuilder sb = new StringBuilder(query);
        try {
            filter[0] = sb.append(DELIMITER).append(String.valueOf(begin))
                    .toString().getBytes("UTF-8");
            filter[1] = sb.append(DELIMITER).append(String.valueOf(end))
                    .toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("Error processing query: " + query);
            throw new RuntimeException(e);
        }
    }
    */
    private void parseQuery(String query, Date startDate, Date endDate) {
        long begin = ReplayUtils.getTimestamp(startDate);
        long end = ReplayUtils.getTimestamp(endDate);
        if (query.indexOf(' ') >= 0) {
            queryStr = query.split(" ")[0];
        }
        else {
            queryStr = query;
        }
        LOG.info("Parsing query: " + queryStr);
        try {
            filter[0] = (queryStr + DELIMITER + String.valueOf(begin)).getBytes("UTF-8");
            filter[1] = (queryStr + DELIMITER + String.valueOf(end)).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("Error processing query: " + query);
            throw new RuntimeException(e);
        }
    }

    /** Perform binary search over the keyIdx, or -1 if not found */
    private int _searchKeyIndex() {
        int i = Arrays.binarySearch(keyIdx, 1, keyIdx[0] + 1, idx);
        assert (i != 0);
        return i;
    }

    /** Extract value of the key field */
    private String _extractKey( int idx) {
        if (rowKey == null) return null;
        return rowKey[idx-1];
    }

    /** Extract timestamp value */
    private long _extractTimestamp() {
        if (rowKey == null) return Long.MAX_VALUE;
        return Long.parseLong(rowKey[rowKey.length - 1]);
    }

    @Override
    public int nextInt() throws IOException {
        // LOG.info("Check integer at index " + idx + "( name: " + new String(fields[idx],Charset.forName("UTF-8")) + " )");
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        int i = _searchKeyIndex();
        int data = -1;
        if (i > 0) {
            data = Integer.parseInt(_extractKey(i));
        }
        else if (idx == tsIdx) {
            data = (int) _extractTimestamp();
        }
        else {
            data = Bytes.toInt(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        }
        _silentPeek();
        return data;
    }

    @Override
    public long nextLong() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        long data = -1L;
        int i = _searchKeyIndex();
        if (i > 0) {
            data = Long.parseLong(_extractKey(i));
        }
        else if (idx == tsIdx) {
            data =  _extractTimestamp();
        }
        else {
            data = Bytes.toLong(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        }
        _silentPeek();
        return data;
    }

    @Override
    public boolean nextBoolean() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        boolean data = false;
        Bytes.toBoolean(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public double nextDouble() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        double data = 0d;
        int i = _searchKeyIndex();
        if (i > 0) {
            data = Double.parseDouble(_extractKey(i));
        }
        else if (idx == tsIdx) {
            data = (double) _extractTimestamp();
        }
        else {
            data = Bytes.toDouble(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        }
        _silentPeek();
        return data;
    }

    @Override
    public String nextString() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        String data = null;
        int i = _searchKeyIndex();
        if (i > 0) {
            data = _extractKey(i);
        }
        else if (idx == tsIdx) {
            data = String.valueOf(_extractTimestamp());
        }
        else {
            data = new String(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]), Charset.forName("UTF-8"));
        }
        _silentPeek();
        return data;
    }

    @Override
    public char nextChar() throws IOException {
        return 0;
    }

    @Override
    public float nextFloat() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        float data = 0f;
        int i = _searchKeyIndex();
        if (i > 0) {
            data = Float.parseFloat(_extractKey(i));
        }
        else if (idx == tsIdx) {
            data = (float) _extractTimestamp();
        }
        else {
            data = Bytes.toFloat(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        }
        _silentPeek();
        return data;
    }

    @Override
    public short nextShort() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        short data = 0;
        int i = _searchKeyIndex();
        if (i > 0) {
            data = Short.parseShort(_extractKey(i));
        }
        else if (idx == tsIdx) {
            data = (short) _extractTimestamp();
        }
        else {
            data = Bytes.toShort(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        }
        _silentPeek();
        return data;
    }

    @Override
    public byte nextByte() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        byte data = Byte.MAX_VALUE;
        int i = _searchKeyIndex();
        if (i > 0) {
            data = Byte.parseByte(_extractKey(i));
        }
        else if (idx == tsIdx) {
            data = (byte) _extractTimestamp();
        }
        else {
            data = peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx])[0];
        }

        _silentPeek();
        return data;
    }

    @Override
    public long[] nextLongArray() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        long[] data = ReplayUtils.toLongs(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public int[] nextIntArray() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        int[] data = ReplayUtils.toInts(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public boolean[] nextBooleanArray() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        boolean[] data = ReplayUtils.toBooleans(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public double[] nextDoubleArray() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        double[] data = ReplayUtils.toDoubles(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public String[] nextStringArray() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        String[] data = ReplayUtils.toStrings(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public char[] nextCharArray() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        char[] data = ReplayUtils.toChars(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public float[] nextFloatArray() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        float[] data = ReplayUtils.toFloats(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public short[] nextShortArray() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        short[] data = ReplayUtils.toShorts(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public byte[] nextByteArray() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        byte[] data = peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]);
        _silentPeek();
        return data;
    }

    @Override
    public boolean isEOD() {
        // silently close the connection when reaching the end
        if (eod) {
            if (scanner != null && !isClosed) {
                LOG.info("Silently close the connection because EOD = true");
                scanner.close();
                isClosed = true;
            }
        }
        return eod;
    }

    private void _silentPeek() {
        idx++;
        if (idx == fields.length) {
            // LOG.info("Going to the next item");
            _advance();
            idx = 0;
        }
    }

    /** Silently iterate and aggregate the items */
    private void _advance() {
        Result lastRow = null; // Keep the last raw item to emit in case aggregation does not go the full cycle
        while (iter.hasNext()) {
            Result r = iter.next();
            String[] rKey = new String(r.getRow(), Charset.forName("UTF-8")).split("-");
            LOG.info("Get data: " + StringUtils.join("-", rKey));
            lastRow = r;
            Result aggregatedRow = aggregator.aggregate(rKey, r);
            if (aggregatedRow != null) {
                peekedRow = aggregatedRow;
                rowKey = rKey;
                eod = false;
                return;
            }
        }
        LOG.info("The iterator is empty or exhausted. Return the last item if any");
        peekedRow = lastRow;
        eod = (lastRow == null);
    }

    @Override
    public void close() throws IOException {
        db.disconnect();
    }

}
