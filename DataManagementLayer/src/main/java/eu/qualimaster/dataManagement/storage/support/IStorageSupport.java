package eu.qualimaster.dataManagement.storage.support;

/**
 * Storage support enables to store key values without blocking the caller,
 * i.e., without waiting that the data storage completes its operation. In
 * the easiest case, this is already the case for a specific storage implementation
 * so that the storage table can implement this interface. This may also be helpful
 * to perform the transparent storage for data sources and data sinks.
 * 
 * @author Holger Eichelberger
 */
public interface IStorageSupport {

    /**
     * Writes an object to the storage.
     * 
     * @param object the object to be written
     */
    public void write(Object object);

}
