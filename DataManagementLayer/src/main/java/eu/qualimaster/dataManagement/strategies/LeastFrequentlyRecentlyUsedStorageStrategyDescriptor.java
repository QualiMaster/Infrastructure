package eu.qualimaster.dataManagement.strategies;

/**
 * Combination of {@link LeastFrequentlyUsedStorageStrategyDescriptor} and 
 * {@link LeastRecentlyUsedStorageStrategyDescriptor}.
 * 
 * @author Holger Eichelberger
 */
public class LeastFrequentlyRecentlyUsedStorageStrategyDescriptor {

    private int cutoffCapacity;
    private int aegingTimeline;
    
    // Just assumptions, please adjust if required
    
    /**
     * Creates a least frequently and recently used storage strategy, i.e., deletes all data
     * that is not recently used and below the given cutoff capacity and a certain aeging timeline.
     * 
     * @param cutoffCapacity the cutoff capacity in MBytes (must be larger than 0)
     * @param aegingTimeline the aeging timeline in seconds (must be larger than 0)
     */
    public LeastFrequentlyRecentlyUsedStorageStrategyDescriptor(int cutoffCapacity, int aegingTimeline) {
        this.cutoffCapacity = Math.max(0, cutoffCapacity);
        this.aegingTimeline = Math.max(0, aegingTimeline);
    }
    
    /**
     * Returns the cutoff capacity in MBytes.
     * 
     * @return the cutoff capacity MBytes
     */
    public int getCutoffCapacity() {
        return cutoffCapacity;
    }
    
    /**
     * Returns the aeging timeline in seconds.
     * 
     * @return the aeging timeline in seconds
     */
    public int getAegingTimeline() {
        return aegingTimeline;
    }

}
