package eu.qualimaster.dataManagement.storage.hbase;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.util.Bytes;

import eu.qualimaster.dataManagement.storage.AbstractStorageManager;

/**
 * Implements a storage manager for HBase.
 * 
 * @author Holger Eichelberger
 */
public class HBaseStorageManager extends
		AbstractStorageManager<HBaseStorageTable> {

	// TODO please add your authorship ;)

	/**
	 * Creates the HBase storage manager. Please consider that we allow access
	 * to the interface, but try to keep the creation of the instance within the
	 * data management layer.
	 * 
	 * @param tablePrefix
	 *            the table name prefix
	 */
	protected HBaseStorageManager(String tablePrefix) {
		super(HBaseStorageTable.class, tablePrefix);
	}

	@Override
	protected HBaseStorageTable createTable(String tableName) {
		return new HBaseStorageSupport(tableName);
	}

	@Override
	protected String validateTableName(String name) {
		String qualifiedName = "";
		try {
			qualifiedName = Bytes.toString(TableName
					.isLegalFullyQualifiedTableName(Bytes.toBytes(name)));
			System.out.println("qualified Name " + qualifiedName);

		} catch (IllegalArgumentException e) {
			// e.printStackTrace();
			name = name.replaceAll("\\W", "-");
			qualifiedName = Bytes.toString(TableName
					.isLegalFullyQualifiedTableName(Bytes.toBytes(name)));

		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("returning alternative name for table: " + qualifiedName);
		return qualifiedName;
	}

}
