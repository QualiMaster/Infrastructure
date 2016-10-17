package eu.qualimaster.dataManagement.sinks.replay;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import eu.qualimaster.dataManagement.common.replay.Field;
import static eu.qualimaster.dataManagement.common.replay.Field.DELIMITER;
import eu.qualimaster.dataManagement.common.replay.ReplayUtils;
import eu.qualimaster.dataManagement.common.replay.Tuple;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.storage.hbase.HBaseBatchStorageSupport;
import eu.qualimaster.dataManagement.storage.support.IStorageSupport;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The data output that buffers data from the replay recorder and puts them into
 * the underlying replay store
 *
 * @author tuan
 * @version 1.0
 */

public class ReplayDataOutput implements IDataOutput, Closeable {

	/**
	 * Field are used to match to the column (or in Hbase, the column
	 * qualifiers)
	 */
	private Field[] fields;

	/** Cursor to the current field */
	private int idx;

	/** 2016-06-02: current version uses an HBase table */
	private HBaseBatchStorageSupport storer;

	/** Hold values of the current row to be written */
	private HBaseBatchStorageSupport.HBaseRow row;

	private StringBuilder keyBuilder;

	private long timestamp;

	private static final Logger log = LoggerFactory.getLogger(ReplayDataOutput.class);

	public ReplayDataOutput(Tuple schema, IStorageSupport storer) {

		//log.info("Replay: constructing ReplayDataOutput");
		// Current version hooks with HBaseBatchStorageSupport
		if (!(storer instanceof HBaseBatchStorageSupport)) {
			throw new RuntimeException("Invalid replay store: "
					+ "Current version only works with HBase. The provided is " + storer.getClass().toString());
		}

		this.storer = (HBaseBatchStorageSupport) storer;
		this.storer.connect();
		this.fields = new Field[schema.getFields().size()];
		this.fields = schema.getFields().toArray(fields);
		row = new HBaseBatchStorageSupport.HBaseRow();
		for (Field f : fields) {
			if (!f.isKey() && !f.isTimesamp()) {
				row.addColumn(Bytes.toBytes(f.getName()));
			}

			// Tuan - 2016-10-17 17:52:00
			// Hard code to test the random sink
			else if (f.getName().equals("randomInteger")) {
				row.addColumn(Bytes.toBytes(f.getName()));
			}
		}
		keyBuilder = new StringBuilder();
	}

	private void writeIfNeeded() throws UnsupportedEncodingException {
		idx++;
		if (idx == fields.length) {
			// log.info("Update the key: " + keyBuilder.toString());
			keyBuilder.append(DELIMITER);

			/*
			 * In HBase, we append row key with the time step to facilitate
			 * range queries
			 */
			keyBuilder.append(String.valueOf(timestamp));
			if (keyBuilder.length() == 0) {
				String msg = "Key cannot be empty: Remember that every tuple " +
						"must have at least one key and one timestamp field";
				log.error(msg);
				throw new RuntimeException(msg);
			}
			byte[] bytes = keyBuilder.toString().getBytes("UTF-8");
			row.setKey(bytes);
			storer.write(row);
			keyBuilder.delete(0, keyBuilder.length());
			row.resetData();
			idx = 0;
		}
	}

	private void appendToKey(String key) {
		if (keyBuilder.length() > 0) {
			keyBuilder.append(DELIMITER);
		}
		keyBuilder.append(key);
	}

	private void appendToKey(char key) {
		if (keyBuilder.length() > 0) {
			keyBuilder.append(DELIMITER);
		}
		keyBuilder.append(key);
	}

	private void appendToKey(long[] key) {
		if (keyBuilder.length() > 0) {
			keyBuilder.append(DELIMITER);
		}
		for (long k : key) {
			keyBuilder.append(String.valueOf(k));
		}
	}

	private void appendToKey(int[] key) {
		if (keyBuilder.length() > 0) {
			keyBuilder.append(DELIMITER);
		}
		for (int k : key) {
			keyBuilder.append(String.valueOf(k));
		}
	}

	private void appendToKey(short[] key) {
		if (keyBuilder.length() > 0) {
			keyBuilder.append(DELIMITER);
		}
		for (short k : key) {
			keyBuilder.append(String.valueOf(k));
		}
	}

	private void appendToKey(char[] key) {
		if (keyBuilder.length() > 0) {
			keyBuilder.append(DELIMITER);
		}
		for (char k : key) {
			keyBuilder.append(k);
		}
	}

	private void appendToKey(String[] key) {
		if (keyBuilder.length() > 0) {
			keyBuilder.append(DELIMITER);
		}
		for (String k : key) {
			keyBuilder.append(k);
		}
	}

	private void appendToKey(byte[] key) {
		if (keyBuilder.length() > 0) {
			keyBuilder.append(DELIMITER);
		}
		try {
			String s = new String(key, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void writeInt(int value) throws IOException {
		byte[] bytes = Bytes.toBytes(value);
		if (fields[idx].isKey()) {
			appendToKey(String.valueOf(value));

			// Tuan - 2016-10-17 17:56:00
			// Hard code for the RandomSink
			if (fields[idx].getName().equals("randomInteger")) {
				row.addValue(bytes);
			}

		} else if (fields[idx].isTimesamp()) {
			timestamp = ReplayUtils.getTimestamp(fields[idx], value);
		} else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeLong(long value) throws IOException {
		byte[] bytes = Bytes.toBytes(value);
		if (fields[idx].isKey()) {
			appendToKey(String.valueOf(value));
		} else if (fields[idx].isTimesamp()) {
			timestamp = ReplayUtils.getTimestamp(fields[idx], value);
		} else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeShort(short value) throws IOException {
		byte[] bytes = Bytes.toBytes(value);
		if (fields[idx].isKey()) {
			appendToKey(String.valueOf(value));
		} else if (fields[idx].isTimesamp()) {
			timestamp = ReplayUtils.getTimestamp(fields[idx], value);
		} else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeBoolean(boolean value) throws IOException {
		byte[] bytes = Bytes.toBytes(value);
		if (fields[idx].isKey()) {
			appendToKey(value ? "0" : "1");
		}
		// boolean field cannot be timestamp field
		else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeDouble(double value) throws IOException {
		byte[] bytes = Bytes.toBytes(value);
		if (fields[idx].isKey()) {
			appendToKey(String.valueOf(value));
		}
		// For the moment, double field cannot be timestamp field
		else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeFloat(float value) throws IOException {
		byte[] bytes = Bytes.toBytes(value);
		if (fields[idx].isKey()) {
			appendToKey(String.valueOf(value));
		}
		// For the moment, float field cannot be timestamp field
		else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeChar(char value) throws IOException {
		byte[] bytes = Bytes.toBytes(value);
		if (fields[idx].isKey()) {
			appendToKey(value);
		}
		// For the moment, char field cannot be timestamp field
		else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeString(String value) throws IOException {
		byte[] bytes = Bytes.toBytes(value);
		if (fields[idx].isKey()) {
			appendToKey(value);
		} else if (fields[idx].isTimesamp()) {
			timestamp = ReplayUtils.getTimestamp(fields[idx], value);
		} else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeByte(byte value) throws IOException {
		byte[] bytes = new byte[] { value };
		if (fields[idx].isKey()) {
			appendToKey(String.valueOf(value));
		}
		// For the moment, byte field cannot be timestamp field
		else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeLongArray(long[] array) throws IOException {
		byte[] bytes = ReplayUtils.toBytes(array);
		if (fields[idx].isKey()) {
			appendToKey(array);
		}
		// For now, long array cannot be timestamp field
		else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeIntArray(int[] array) throws IOException {
		byte[] bytes = ReplayUtils.toBytes(array);
		if (fields[idx].isKey()) {
			appendToKey(array);
		}
		// For now, int array cannot be timestamp field
		else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeBooleanArray(boolean[] array) throws IOException {
		byte[] bytes = ReplayUtils.toBytes(array);

		// For now, boolean array can neither be key nor timestamp
		row.addValue(bytes);
		writeIfNeeded();
	}

	@Override
	public void writeDoubleArray(double[] array) throws IOException {
		byte[] bytes = ReplayUtils.toBytes(array);

		// For now, double array can neither be key nor timestamp
		row.addValue(bytes);
		writeIfNeeded();
	}

	@Override
	public void writeFloatArray(float[] array) throws IOException {
		byte[] bytes = ReplayUtils.toBytes(array);

		// For now, float array can neither be key nor timestamp
		row.addValue(bytes);
		writeIfNeeded();
	}

	@Override
	public void writeShortArray(short[] array) throws IOException {
		byte[] bytes = ReplayUtils.toBytes(array);
		if (fields[idx].isKey()) {
			appendToKey(array);
		}
		// For now, short array cannot be timestamp field
		else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeCharArray(char[] array) throws IOException {
		byte[] bytes = ReplayUtils.toBytes(array);
		if (fields[idx].isKey()) {
			appendToKey(array);
		}
		// For now, char array cannot be timestamp field
		else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeStringArray(String[] array) throws IOException {
		byte[] bytes = ReplayUtils.toBytes(array);
		if (fields[idx].isKey()) {
			appendToKey(array);
		}
		// TODO: For now, string array cannot be timestamp field (might
		// reconsider this !)
		else {
			row.addValue(bytes);
		}
		writeIfNeeded();
	}

	@Override
	public void writeByteArray(byte[] array) throws IOException {
		if (fields[idx].isKey()) {
			appendToKey(array);
		}
		// For now, byte array cannot be timestamp field
		else {
			row.addValue(array);
		}
		writeIfNeeded();
	}

	@Override
	public void close() throws IOException {
		storer.disconnect();
	}
}
