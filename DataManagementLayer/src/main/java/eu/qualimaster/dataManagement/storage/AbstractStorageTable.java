package eu.qualimaster.dataManagement.storage;

import eu.qualimaster.dataManagement.common.IDataElement;
import eu.qualimaster.dataManagement.storage.support.IStorageSupport;
import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;

/**
 * Defines the interface and basic functionality of a storage table.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractStorageTable implements IDataElement { // not measurable

    private String tableName;
    private IStorageStrategyDescriptor strategy;
    
    /**
     * Creates a new storage table. Just visible to implementing classes.
     * 
     * @param tableName the actual table name (valid for the underlying storage technology) 
     */
    protected AbstractStorageTable(String tableName) {
        this.tableName = tableName;
    }
    
    /**
     * Returns the (storage-specific) table name.
     * 
     * @return the table name
     */
    protected String getTableName() {
        return tableName;
    }
    
    /**
     * Writes an object to this table, considering the key provided by {@link IStorageKeyProvider}, 
     * using the current time as a fallback key.
     * 
     * @param object the object to write
     * @see #doWrite(Object, Object)
     */
    public void write(Object object) {
        Object key = null;
        if (object instanceof IStorageKeyProvider) {
            key = ((IStorageKeyProvider) object).getStorageKey();
        }
        if (null == key) {
            key = String.valueOf(System.nanoTime());
        }
        doWrite(key, object);
    }

    @Override
    public IStorageStrategyDescriptor getStrategy() {
        return strategy;
    }
    
    /**
     * Does the actual writing.
     * 
     * @param key the key of the <code>object</code> to be written
     * @param object the object to be written
     */
    protected abstract void doWrite(Object key, Object object);

    /**
     * Returns an object according to its <code>key</code>.
     * 
     * @param key the key to return toe stored object for
     * @return the stored object or <b>null</b> if there is none
     */
    public abstract Object get(Object key);
    
    /**
     * Returns the non-blocking storage support for this table.
     * 
     * @return the non-blocking storage support (may be the table itself)
     */
    public abstract IStorageSupport getStorageSupport();
    
    @Override
    public void setStrategy(IStorageStrategyDescriptor strategy) {
        this.strategy = strategy;
    }

    // It would be nice to have an HBase-abstracted query here... but I do not know much of HBase and similar approaches

}
