package eu.qualimaster.dataManagement.storage;

import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.dataManagement.common.AbstractDataManager;
import eu.qualimaster.dataManagement.common.LocalReference;
import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;

/**
 * A abstract manager for storage mechanisms. Storage managers may be created
 * with a table prefix name in order to clearly separate the names of the tables
 * of the individual managers. Future versions may also consider the "unit" name
 * in order to separate pipelines etc.
 * 
 * @param <T>
 *            the table type
 * @author Holger Eichelberger
 */
public abstract class AbstractStorageManager<T extends AbstractStorageTable> extends AbstractDataManager<T> {

	private String tablePrefix;
	private Map<String, T> tables = new HashMap<String, T>();
	private String tableSeperator = "-";

	/**
	 * Creates an abstract storage manager. Only required from implementing
	 * subclasses.
	 * 
	 * @param elementClass
	 *            the class of elements handled by this manager
	 * @param tablePrefix
	 *            a prefix to be put before the actual table names handled by
	 *            this storage manager (may be empty)
	 */
	protected AbstractStorageManager(Class<T> elementClass, String tablePrefix) {
		super(elementClass, tablePrefix); // should be "unique" enough
		this.tablePrefix = tablePrefix;
	}

	/**
	 * Validates a given table name. This method shall turn an invalid
	 * <code>tableName</code> into a valid table name if required.
	 * 
	 * @param name
	 *            the table name
	 * @return <code>name</code> or the valid version of <code>name</code>
	 */
	protected abstract String validateTableName(String name);

	/**
	 * Creates a table, i.e., creates it physically if required or just obtains
	 * a handle to an existing table.
	 * 
	 * @param tableName
	 *            the name of the table (supposed to be valid according to the
	 *            implemented storage approach)
	 * @return the storage table
	 */
	protected abstract T createTable(String tableName);

	/**
	 * Allows to access a table. Please note that existing and already accessed
	 * tables are cached and not returned as new instances.
	 * 
	 * @param unit
	 *            the processing unit such as pipelines (may be <b>null</b> or
	 *            empty, but prevents management functions such as
	 *            {@link #connectAll(String)}, {@link #disconnectAll(String)},
	 *            {@link #discardAll(String)})
	 * @param tableName
	 *            the name of the table to be returned
	 * @param strategy
	 *            the desired storage strategy to be applied. This may be
	 *            applied to new tables, but existing tables may reuse their
	 *            actual storage strategies if switching is not adequate.
	 * @return the requested storage table
	 */
	/*
	 * public final T getTable(String unit, String tableName,
	 * IStorageStrategyDescriptor strategy) { String key = unit == null ? "" :
	 * unit; if (null != tablePrefix) { if (key.length() > 0) { key = key +
	 * tableSeperator; } key = key + tablePrefix; } key = key + tableSeperator +
	 * tableName; System.out.println("key " + key); T result = tables.get(key);
	 * if (null == result) { result = createTable(validateTableName(key)); // we
	 * do not use create here / parameters System.out.println("result " +
	 * result); if (null != result) { result.setStrategy(strategy);
	 * register(unit, new LocalReference<T>(result)); } } return result; }
	 */

	public final T getTable(String unit, String tableName, IStorageStrategyDescriptor strategy) {
		String key = unit == null ? "" : unit;
		if (null != tablePrefix) {
			if (key.length() > 0) {
				key = key + tableSeperator;
			}
			key = key + tablePrefix;
		}
		key = key + tableSeperator + tableName;

		StringBuilder msg = new StringBuilder();
		for (String k : tables.keySet()) {
			msg.append(k);
			msg.append(": ");
			msg.append(tables.get(k).getClass().toString());
		}
		throw new RuntimeException("The table mapping: " + msg.toString());
		/*
		 * T result = tables.get(key); if (null == result) { result =
		 * createTable(validateTableName(key)); // we do not use create here /
		 * parameters System.out.println("result " + result); if (null !=
		 * result) { result.setStrategy(strategy); register(unit, new
		 * LocalReference<T>(result)); } } return result;
		 */
	}
}
