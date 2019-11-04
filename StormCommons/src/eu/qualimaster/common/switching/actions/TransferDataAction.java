package eu.qualimaster.common.switching.actions;

/**
 * Provides the action of transferring data.
 * @author Cui Qin
 *
 */
public class TransferDataAction implements IAction {
    private ITransferDataStrategy transferStrategy;
    
    /**
     * Creates an action of transferring data.
     * @param transferStrategy the strategy of the transferring data
     */
    public TransferDataAction(ITransferDataStrategy transferStrategy) {
        this.transferStrategy = transferStrategy;
    }
    
    @Override
    public void execute() {
        SwitchStates.setTransferringOrgINT(true);
        transferStrategy.transferData();
    }
}
