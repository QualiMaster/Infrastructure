package eu.qualimaster.common.switching.actions;
/**
 * Provides the action of transferring all unprocessed data. This action uses 
 * the <code>TransferDataAction</code> to transfer data.
 * @author Cui Qin
 *
 */
public class TransferAllDataAction implements IAction {
    private ITransferDataStrategy transferStrategy;
    
    /**
     * Creates an action of transferring all unprocessed data.
     * @param transferStrategy the strategy of the transferring data
     */
    public TransferAllDataAction(ITransferDataStrategy transferStrategy) {
        this.transferStrategy = transferStrategy;
    }
    
    @Override
    public void execute() {
        SwitchStates.setTransferAll(true);
        new TransferDataAction(transferStrategy).execute();
    }

}
