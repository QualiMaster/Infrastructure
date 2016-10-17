package eu.qualimaster.dataManagement.common.replay;

/**
 * Represent one field in the schema of the data element to 
 * be stored in the Replay Store
 *
 * Some assumptions:
 * - Fields of one of the types Boolean, Double, Float, Char, Byte, and all
 * arrays (except byte array) cannot be the timestamp fields
 * 
 * @author tuan
 * @since 30.05.2016
 *
 */
public class Field {

	/**
	 * For composite key, we simply concatenate the individual
	 * string representation , because the query in ReplayStreamer
	 * is represented as a string.
	 * We use the middle dot as delimiter.
	 */
	// public static final char DELIMITER = (char) 183;
	public static final char DELIMITER = '-';

	/** 
	 * Name of the field. This is used to match to the column
	 * in the Replay Store
	 */
	private String name;
	
	// Don't know if this is necessary
	private Class<?> type;
	
	private boolean key, timestamp;

	/* Convenience constructors */
	public Field(String name, Class<?> type, boolean key, boolean timestamp) {
		this.name = name;
		this.type = type;
		this.key = key;
		this.timestamp = timestamp;
	}
	
	/* Getter and setter methods */	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public boolean isKey() {
		return key;
	}

	public void setKey(boolean key) {
		this.key = key;
	}

	public boolean isTimesamp() {
		return timestamp;
	}

	public void setTimesamp(boolean timesamp) {
		this.timestamp = timesamp;
	}
}
