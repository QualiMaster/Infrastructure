package eu.qualimaster.dataManagement.storage.hbase;

import eu.qualimaster.dataManagement.storage.AbstractStorageTable;
import eu.qualimaster.dataManagement.storage.support.IStorageSupport;
import eu.qualimaster.dataManagement.storage.support.ProducerConsumerStorageSupport;

/**
 * Represents a storage table in HBase.
 * 
 * @author Holger Eichelberger
 */
public class HBaseStorageTable extends AbstractStorageTable {
    
    // TODO please add your authorship ;)
    // TODO realize the storage strategies based on the storage strategy descriptors

    /**
     * Creates the storage table. The constructor is package local in order to allow only
     * the related storage manager to create instances.
     * 
     * @param tableName the full table name of the table to be created
     */
    HBaseStorageTable(String tableName) {
        super(tableName);
    }
    
    @Override
    public void connect() {
        // TODO connect to Hbase using getTableName()
    }

    @Override
    public void disconnect() {
        // TODO disconnect from Hbase 
    }

    @Override
    protected void doWrite(Object key, Object object) {
        // TODO write the object to HBase, e.g., via Apache Avro
    }

    @Override
    public Object get(Object key) {
        // TODO query the HBase table for the given key
    	return null;
    }

    @Override
    public IStorageSupport getStorageSupport() {
        return new ProducerConsumerStorageSupport(this); // this may also be an HBase specific implementation
    }
    
    // It would be nice to have an HBase-abstracted query here... but I do not know much of HBase and similar approaches

}
