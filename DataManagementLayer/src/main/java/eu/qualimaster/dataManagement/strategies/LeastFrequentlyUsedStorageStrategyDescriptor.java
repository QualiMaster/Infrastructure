package eu.qualimaster.dataManagement.strategies;

/**
 * Represents a least frequently used storage strategy with a cutoff capacity.
 * 
 * @author Holger Eichelberger
 */
public class LeastFrequentlyUsedStorageStrategyDescriptor {

    private int cutoffCapacity;

    // Just assumptions, please adjust if required
    
    /**
     * Creates a least frequently used storage strategy descriptor, i.e., deletes all data
     * that is not recently used and below the given cutoff capacity.
     * 
     * @param cutoffCapacity the cutoff capacity in MBytes (must be greater than 0)
     */
    public LeastFrequentlyUsedStorageStrategyDescriptor(int cutoffCapacity) {
        this.cutoffCapacity = Math.max(0, cutoffCapacity);
    }
    
    /**
     * Returns the cutoff capacity in MBytes.
     * 
     * @return the cutoff capacity MBytes
     */
    public int getCutoffCapacity() {
        return cutoffCapacity;
    }
    
}
