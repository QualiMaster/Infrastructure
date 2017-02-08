package eu.qualimaster.dataManagement.storage.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import eu.qualimaster.dataManagement.DataManagementConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.regionserver.BloomType;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.qualimaster.dataManagement.storage.support.IStorageSupport;

/**
 * A variant of Patrick's HBaseStorageSupport that allows importing data into
 * the table in batch
 */
public class HBaseBatchStorageSupport extends HBaseStorageTable implements IStorageSupport {

	private static final Logger log = LoggerFactory.getLogger(HBaseBatchStorageSupport.class);

	private Configuration config;
	private HConnection conn;
	private HTableInterface table;

	/** Default: TSI HBase cluster */
	private static final String HBASE_NODE = "/hbase";
	private static final String HBASE_QUORUM = "node19.ib,node23.ib,master.ib,master03.ib,node15.ib";

	private static final String COLUMN_FAMILY = "cf";
	public static final byte[] COLUMN_FAMILY_BYTES = Bytes.toBytes(COLUMN_FAMILY);

	/** Default batch size is 500 */
	//private int batchSize = 500;
	private int batchSize = 10;

	private int counter;

	public HBaseBatchStorageSupport(String tableName) {
		super(tableName);
		log.info("Replay: constructing HBaseBatchStorageSupport");
		// Configuration config = HBaseConfiguration.create();
		config = HBaseConfiguration.create();
		config.set("fs.hdfs.impl", DistributedFileSystem.class.getName());
		config.set("fs.file.impl", LocalFileSystem.class.getName()
		);
		config.set("zookeeper.znode.parent", DataManagementConfiguration.getHbaseZnodeParent());
		config.set("hbase.zookeeper.quorum", DataManagementConfiguration.getHbaseZkeeperQuorum());

		// config.set("zookeeper.znode.parent", HBASE_NODE);
		// config.set("hbase.zookeeper.quorum", HBASE_QUORUM);

		// All tables in the replay store have only one column family, with
		// column qualifiers
		// being the field keys pushed by the replay recorder at run time
		// createIfNotExist();
	}

	public void setBatchSize(int size) {
		batchSize = size;
	}

	/** nfs
	 * Declare an HBase table based on a given schema
	 */
	private void createIfNotExist() {
		log.info("Replay: createIfNotExist");

		config.set("zookeeper.znode.parent", DataManagementConfiguration.getHbaseZnodeParent());
		config.set("hbase.zookeeper.quorum", DataManagementConfiguration.getHbaseZkeeperQuorum());

		// config.set("zookeeper.znode.parent", HBASE_NODE);
		// config.set("hbase.zookeeper.quorum", HBASE_QUORUM);

		try (HBaseAdmin admin = new HBaseAdmin(config)) {
			HTableDescriptor htd = new HTableDescriptor(TableName.valueOf(getTableName()));

			// If the table exists, we need to check if it has column family
			// named "cf",
			// and add if not found
			// if (!admin.tableExists(getTableName())) {
			log.info("check table existence: Table " + getTableName() + " result = " + admin.tableExists(getTableName()));
			if (admin.tableExists(getTableName())) {
				log.info("Table " + getTableName() + " already exists. Check for column family " + COLUMN_FAMILY);
				for (HColumnDescriptor hcd : admin.getTableDescriptor(htd.getTableName()).getColumnFamilies()) {
					if (hcd.getNameAsString().equalsIgnoreCase(COLUMN_FAMILY)) {
						log.info("Column family " + COLUMN_FAMILY + " exists. Do nothing");
						return;
					}
				}
				log.info("Column family \" + COLUMN_FAMILY + \" does not exist. Create one");
				HColumnDescriptor hcd = new HColumnDescriptor(COLUMN_FAMILY);

				// Note 2016-10-19 Tuan: Add a Bloom filter to the HFiles of this column family to increase the read overhead
				hcd.setBloomFilterType(BloomType.ROW);
				htd.addFamily(hcd);
			} else {
				log.info("Table " + getTableName() + " does not exist. Create one");
				HColumnDescriptor hcd = new HColumnDescriptor(COLUMN_FAMILY);
				htd.addFamily(hcd);
				admin.createTable(htd);
			}

		} catch (Exception e) {
			log.error("Cannot declare generic table ", e);
			// e.printStackTrace();
		}
	}

	@Override
	public void connect() {
		super.connect();
		log.info("Replay: connect");
		try {
			// config.set("zookeeper.znode.parent", "/hbase");
			// config.set("hbase.zookeeper.quorum", "node19.ib,node23.ib,master.ib,master03.ib,node15.ib");

			config.set("zookeeper.znode.parent", DataManagementConfiguration.getHbaseZnodeParent());
			config.set("hbase.zookeeper.quorum", DataManagementConfiguration.getHbaseZkeeperQuorum());

			conn = HConnectionManager.createConnection(config);
			table = conn.getTable(getTableName());

			// Important: We do the flush manually to allow bulk put
			table.setAutoFlush(false, false);
			counter = 0;
		} catch (IOException e) {
			log.error("Canot establish the connection to the default quorum "); // + HBASE_QUORUM, e);
			// e.printStackTrace();
		}
	}

	@Override
	public void disconnect() {
		counter = 0;
		if (table != null) {
			try {
				table.close();
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (IOException e) {
				log.error("Cannot close the table " + getTableName(), e);
				e.printStackTrace();
			}
		}
		super.disconnect();
	}


	@Override
	// This implementation is fail-fast
	protected void doWrite(Object key, Object object) {
		//log.info("Replay: doWrite");
		if (!(object instanceof HBaseRow)) {
			String msg = "HBaseBatchSupport can only write " + "object of type HBaseRow";
			log.error(msg);
			throw new RuntimeException(msg);
		}
		final HBaseRow row = (HBaseRow) object;
		
		// byte[] keyBytes = ByteBuffer.allocate(Long.SIZE / Byte.SIZE).putLong(System.currentTimeMillis()).array();
		// row.setKey(keyBytes);
		
		Put put = row.createPut();
		try {
			table.put(put);
			counter++;
			if (counter % batchSize == 0) {
				//log.info("Replay: flushCommits");
				table.flushCommits();
				counter = 0;
			}
		} catch (IOException e) {
			String msg = "Error occur after putting data of key " + row.rowKey + " into HBase";
			log.error(msg);
			throw new RuntimeException(msg);
		}
	}

	@Override
	// Caveat: the input type is a pair of 2 byte arrays (the range),
	// and output type is a ResultScanner
	public Object get(Object key) {
		if (!(key instanceof byte[][])) {
			throw new RuntimeException("Can only query from a compiled prefix");
		}
		byte[][] filter = (byte[][]) key;
		if (filter.length != 2) {
			throw new RuntimeException("Invalid query syntax");
		}
		Scan scan = new Scan(filter[0], filter[1]);

		// TODO: Need to check, which queries are supported
		ResultScanner result = null;
		try {
			result = table.getScanner(scan);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	/** Wrapper of the HBase Put */
	public static class HBaseRow {

		private byte[] rowKey;

		/**
		 * Caveat: The column qualifiers are create one time, while rowKey and
		 * values are updated dynamically. We assume that for each row, the
		 * order of column qualifiers and values are always matching
		 */
		private List<byte[]> columnQualifier;

		private List<byte[]> values;

		public HBaseRow() {
			columnQualifier = new ArrayList<>();
			values = new ArrayList<>();
		}

		public void setKey(byte[] key) {
			rowKey = key;
		}

		public void addColumn(byte[] col) {
			columnQualifier.add(col);
		}

		public void addValue(byte[] val) {
			values.add(val);
		}

		public void resetData() {
			values.clear();
			rowKey = null;
		}

		private Put createPut() {
			Put put = new Put(rowKey);
			for (int i = 0; i < columnQualifier.size(); i++) {
				put.add(COLUMN_FAMILY_BYTES, columnQualifier.get(i), values.get(i));
			}
			return put;
		}

	}

	@Override
	public IStorageSupport getStorageSupport() {
		// throw new RuntimeException("HBaseBatchStoreSupport has been declared
		// and called.");
		return this;
	}
}
