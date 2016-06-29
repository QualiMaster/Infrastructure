package eu.qualimaster.dataManagement.storage;

/**
 * Returns the key to be used for storing this element. The default if not given
 * is the actual time stamp.
 * 
 * @author Holger Eichelberger
 */
public interface IStorageKeyProvider {
    
    /**
     * Returns the storage key.
     * 
     * @return the storage key
     */
    public String getStorageKey();

}
