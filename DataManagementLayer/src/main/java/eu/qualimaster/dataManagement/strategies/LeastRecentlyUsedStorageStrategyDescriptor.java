package eu.qualimaster.dataManagement.strategies;

/**
 * Represents a least recently used storage strategy with a cutoff capacity.
 * 
 * @author Holger Eichelberger
 */
public class LeastRecentlyUsedStorageStrategyDescriptor {

    // Just assumptions, please adjust if required
    
    private int aegingTimeline;
    
    /**
     * Creates a least recently used storage strategy descriptor, i.e., deletes all data
     * that is not frequently used within a certain aeging timeline.
     * 
     * @param aegingTimeline the aeging timeline in seconds (must be larger than 0)
     */
    public LeastRecentlyUsedStorageStrategyDescriptor(int aegingTimeline) {
        this.aegingTimeline = Math.max(0, aegingTimeline);
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
