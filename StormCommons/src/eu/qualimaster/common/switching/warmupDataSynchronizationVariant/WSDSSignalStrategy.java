package eu.qualimaster.common.switching.warmupDataSynchronizationVariant;

import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.signal.AbstractSignalStrategy;
import eu.qualimaster.common.signal.EnactSignalHandler.EnactOrgINTSignalHandler;
import eu.qualimaster.common.signal.DisableSignalHandler.DisableOrgENDSignalHandler;

/**
 * Provide a signal strategy for the switch variant "Warm-up Switch with Data Synchronization".
 * @author Cui Qin
 *
 */
public class WSDSSignalStrategy extends AbstractSignalStrategy {

    /**
     * Constructor of the WSDS signal strategy.
     * @param signalConnection the signal connection
     */
    public WSDSSignalStrategy(AbstractSignalConnection signalConnection) {
        super(signalConnection);
    }

    @Override
    public void init() {
        new EnactOrgINTSignalHandler();
        new DisableOrgENDSignalHandler(); 
    }      

}
