package eu.qualimaster.common.switching.synchronization;

import eu.qualimaster.common.switching.IStrategy;

/**
 * Interface of the synchronization strategy.
 * @author Cui Qin
 *
 */
public interface ISynchronizationStrategy extends IStrategy {
    /**
     * Synchronize the queue state of both intermediary nodes.
     */
    public void doSynchronization();
    
    /**
     * Transfer data.
     */
    public void doDataTransfer();
}
