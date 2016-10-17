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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

    /** Cursor to the current field */
    private int idx;

    /** "peek iterator" in-line implementation */
    private ResultScanner scanner;
    private Iterator<Result> iter;
    private Result peekedRow;
    private boolean eod;

    /** the current prefix of query */
    private byte[][] filter;

    /* for logging purpose */
    private String queryStr;

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
        for (int i = 0; i < n; i++) {
            Field f = schema.getField(i);
            if (!f.isKey() && !f.isTimesamp()) {
                fields[i] = Bytes.toBytes(f.getName());
            }
        }
        this.db.connect();
    }

    public void updateQuery(String query, Date startDate, Date enDate) {
        parseQuery(query, startDate, enDate);
        eod = true;
        scanner.close();
        peekedRow = null;
        Object obj = null;
        try {
            obj = db.get(filter);
            if (obj == null || !(obj instanceof ResultScanner)) {
                LOG.warn("Error processing the query " + query);
            }
            scanner = (ResultScanner)obj;
            iter = scanner.iterator();
            peekedRow = iter.next();
            eod = false;
        } catch (Exception e) {
            LOG.warn("Error processing the query " + query);
        }
    }

    /**
     * Parse the query: We assume the query is just the key (composite key
     * possible, where individual keys are delimited by char 183) */
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

    @Override
    public int nextInt() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        int data = Bytes.toInt(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public long nextLong() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        long data = Bytes.toLong(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public boolean nextBoolean() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        boolean data = Bytes.toBoolean(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public double nextDouble() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        double data = Bytes.toDouble(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public String nextString() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        String data = Bytes.toString(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
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
        float data = Bytes.toFloat(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public short nextShort() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        short data = Bytes.toShort(peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx]));
        _silentPeek();
        return data;
    }

    @Override
    public byte nextByte() throws IOException {
        if (peekedRow == null) throw new IOException("Corrupted data when reading" +
                " from Hbase result for query " + queryStr);
        byte data = peekedRow.getValue(COLUMN_FAMILY_BYTES, fields[idx])[0];
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
            scanner.close();
        }
        return eod;
    }

    private void _silentPeek() {
        idx++;
        if (idx == fields.length) {
            if (iter.hasNext()) {
                peekedRow = iter.next();
            }
            else {
                peekedRow = null;
                eod = true;
            }
            idx = 0;
        }
    }

    @Override
    public void close() throws IOException {
        db.disconnect();
    }
}
