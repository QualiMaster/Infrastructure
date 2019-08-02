package eu.qualimaster.common.switching.synchronization;

/**
 * Provide an abstract synchronization strategy.
 * @author Cui Qin
 *
 */
public abstract class AbstractSynchronizationStrategy implements ISynchronizationStrategy {
    private static final String STRATEGYTYPE = "synchronization";
    
    @Override
    public String getStrategyType() {
        return STRATEGYTYPE;
    }
    
}
