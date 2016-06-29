package eu.qualimaster.dataManagement.storage.support;

import eu.qualimaster.dataManagement.storage.AbstractStorageTable;

/**
 * Implements storage support based on a producer/consumer pattern, i.e., the producer
 * puts the object into a buffer and returns while the consumer writes the objects
 * in parallel into the storage.
 * 
 * @author Holger Eichelberger
 */
public class ProducerConsumerStorageSupport implements IStorageSupport {

    private AbstractStorageTable table;
    // TODO some buffer
    // TODO producer/consumer implementation
    
    /**
     * Creates a producer consumer storage support. This constructor is intended to be called from 
     * the owning table, but, however, also client code may call it.
     * 
     * @param table the actual storage table
     */
    public ProducerConsumerStorageSupport(AbstractStorageTable table) {
        this.table = table;
    }
    
    @Override
    public void write(Object object) {
        // TODO this is not the final implementation
        table.write(object);
    }

}
