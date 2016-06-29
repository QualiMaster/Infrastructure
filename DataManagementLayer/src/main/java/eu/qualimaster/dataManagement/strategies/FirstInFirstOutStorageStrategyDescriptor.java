package eu.qualimaster.dataManagement.strategies;


/**
 * Provides an abstract basic implementation of a storage strategy.
 * 
 * @author Holger Eichelberger
 */
public class FirstInFirstOutStorageStrategyDescriptor implements IStorageStrategyDescriptor {

    /**
     * Stores the singleton instance.
     */
    public static final IStorageStrategyDescriptor INSTANCE = new FirstInFirstOutStorageStrategyDescriptor();

    /**
     * Prevents external creation.
     */
    private FirstInFirstOutStorageStrategyDescriptor() {
    }

}
