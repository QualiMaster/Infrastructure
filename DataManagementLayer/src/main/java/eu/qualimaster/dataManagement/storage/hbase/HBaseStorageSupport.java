package eu.qualimaster.dataManagement.storage.hbase;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.util.Bytes;

import eu.qualimaster.dataManagement.storage.support.IStorageSupport;

public class HBaseStorageSupport extends HBaseStorageTable implements IStorageSupport {

	HTable table;
	Configuration config;
	boolean sentitableIsInit = false;
	boolean tableIsInit = false;

	public HBaseStorageSupport(String tableName) {
		super(tableName);

		config = HBaseConfiguration.create();
		// config.set("zookeeper.znode.parent", "/hbase-unsecure");
		// config.set("hbase.zookeeper.quorum", "snf-618466.vm.okeanos.grnet.gr");

		// config for cluster at l3s
		config.set("zookeeper.znode.parent", "/hbase");
		config.set("hbase.zookeeper.quorum", "node19.ib,node23.ib,master.ib,master03.ib,node15.ib");
		System.out.println(
				"creating table with name " + tableName + " in servers " + config.get("hbase.zookeeper.quorum"));
		HBaseAdmin admin;

		try {

			admin = new HBaseAdmin(config);
			HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf(tableName));
			tableDescriptor.addFamily(new HColumnDescriptor("GenericObject"));

			if (admin.tableExists(tableDescriptor.getName())) {
				System.out.println("Table " + tableName + " exists");
				HColumnDescriptor[] fams = admin.getTableDescriptor(tableDescriptor.getName()).getColumnFamilies();
				boolean contains = false;
				for (HColumnDescriptor hColumnDescriptor : fams) {
					if (hColumnDescriptor.getNameAsString().equals("GenericObject")) {
						contains = true;
					}
				}
				if (!contains) {
					admin.getTableDescriptor(TableName.valueOf(tableName))
							.addFamily(new HColumnDescriptor("GenericObject"));
					admin.close();
				}

			} else {
				admin.createTable(tableDescriptor);
			}
			admin.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void connect() {
		try {
			table = new HTable(config, this.getTableName());
			System.out.println("Table " + this.getTableName() + " connection OK");

			System.out.println("Buffer Size " + table.getWriteBufferSize());
			table.setAutoFlushTo(true);
			System.out.println("Auto Flush " + table.isAutoFlush());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void disconnect() {
		try {
			table.close();
			System.out.println("Table " + this.getTableName() + " disconnect OK");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void doWrite(Object key, Object object) {
		//
		System.out.println("putting generic elements");
		if (!tableIsInit) {
			initGenericTable();
		}

		Put put;
		Random rand = new Random();
		if (key == null) {
			put = new Put(Bytes.toBytes(System.nanoTime() + " " + rand.nextLong()));
		} else {
			put = new Put(Bytes.toBytes(key.toString()));
		}
		try {
			put.add(Bytes.toBytes("GenericObject"), Bytes.toBytes("ObjectContent"), serialize((Serializable) object));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			table.put(put);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// }
	}

	@Override
	public Object get(Object key) {

		try {
			if (table.getTableDescriptor().getFamiliesKeys().contains(Bytes.toBytes("SentimentOutput"))) {
				System.out.println("Table contains sentiment outputs");
				Get get = new Get(Bytes.toBytes(key.toString()));
				Result result = table.get(get);
				return result;
			} else {
				Get get = new Get(Bytes.toBytes(key.toString()));
				Result result = table.get(get);
				byte[] objectAsByte = result.getValue(Bytes.toBytes("GenericObject"), Bytes.toBytes("ObjectContent"));
				try {
					return deserialize(objectAsByte);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Gets the whole set of keys in a table
	 * @return The list of keys (byte[]) as objects
	 */
	public List<Object> getKeys() {
		List<Object> keys = new ArrayList<Object>();
		try {
			Scan scan = new Scan();
			scan.setFilter(new FirstKeyOnlyFilter());
			ResultScanner scanner = table.getScanner(scan);
			for (Result rr : scanner) {
			  byte[] key = rr.getRow();
			  keys.add(key);
			}
			return keys;
		} catch (IOException e) {
			e.printStackTrace();
			return keys;
		}
	}

	/**
	 * used to create a table for sentiment data this table has a different
	 * format than normal tables in order to keed the data easier to query
	 * 
	 * @return false if the table was not created, true otherwise
	 */
	private boolean initSentimentTable() {

		try {
			if (table == null) {
				connect();
			}
			HTableDescriptor tableDescriptor = table.getTableDescriptor();
			System.out.println(tableDescriptor.getColumnFamilies().length);
			tableDescriptor = new HTableDescriptor(TableName.valueOf(this.getTableName()));
			System.out.println("initilizing table");
			HBaseAdmin admin = new HBaseAdmin(config);

			tableDescriptor.addFamily(new HColumnDescriptor("SentimentOutput"));
			tableDescriptor.addFamily(new HColumnDescriptor("timestamps"));
			tableDescriptor.addFamily(new HColumnDescriptor("values"));
			tableDescriptor.addFamily(new HColumnDescriptor("volumes"));
			createOrOverwrite(admin, tableDescriptor);

			sentitableIsInit = true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * used to initialize a table for storing generic objects
	 * 
	 * @return true if the initialization was successfull
	 */
	private boolean initGenericTable() {
		try {
			if (table == null) {
				connect();
			}

			HTableDescriptor tableDescriptor = table.getTableDescriptor();
			System.out.println(tableDescriptor.getColumnFamilies().length);
			tableDescriptor = new HTableDescriptor(TableName.valueOf(this.getTableName()));
			System.out.println("initilizing table");
			HBaseAdmin admin = new HBaseAdmin(config);
			tableDescriptor.addFamily(new HColumnDescriptor("GenericObject"));

			createOrOverwrite(admin, tableDescriptor);
			tableIsInit = true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Creates or overwrites the table for storage
	 * 
	 * @param admin
	 *            the instanciated admin for performing the actions
	 * @param table
	 *            The table descriptor for the table to be created
	 * @throws IOException
	 */
	public static void createOrOverwrite(HBaseAdmin admin, HTableDescriptor table) throws IOException {

		// do a small random wait,to make sure that other threads havent done
		// this yet (needed for parallel checks)

		Random r = new Random();
		int waiting = r.nextInt(10);
		System.out.println("will wait for " + waiting + " secs");
		try {
			Thread.sleep(waiting * 1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Checking if Table Exists");
		// if (admin.tableExists(table.getName())) {
		// admin.disableTable(table.getName());
		// admin.deleteTable(table.getName());
		// }
		if (!admin.tableExists(table.getName())) {
			System.out.println("Table does not exist and gets created");
			admin.createTable(table);
		} else {
			System.out.println("table is already there, doing nothing");
		}

	}

	/**
	 * Used for serializing generic java objects
	 * 
	 * @param object
	 *            the object to be stored
	 * @return the byte[] for storing in hbase
	 * @throws IOException
	 */
	private byte[] serialize(Object object) throws IOException {
		return AvroSerializationHelper.serializeToByte(object);
	}

	/**
	 * Used for deserializing generic java objects
	 * 
	 * @param bytes
	 *            the byte[] from hbase of the object
	 * @return the desirialized object
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private Object deserialize(byte[] bytes) throws IOException {
		if (bytes != null) {
			return AvroSerializationHelper.deserializeFromByte(bytes);
		} else {
			System.out.println("nothing to deserialize");
			return null;
		}
	}

}
