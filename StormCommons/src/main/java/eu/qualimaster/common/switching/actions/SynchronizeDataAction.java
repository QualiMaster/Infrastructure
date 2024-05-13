package eu.qualimaster.common.switching.actions;
/**
 * Provides the action of synchronizing data.
 * @author Cui Qin
 *
 */
public class SynchronizeDataAction implements IAction {
    private ISynchronizationStrategy synStrategy;
    
    /**
     * Creates an action for synchronizing data.
     * @param synStrategy the synchronization strategy
     */
    public SynchronizeDataAction(ISynchronizationStrategy synStrategy) {
        this.synStrategy = synStrategy;
    }
    
    @Override
    public void execute() {
        synStrategy.synchronizeData();
    }
}
