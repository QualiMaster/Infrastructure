package eu.qualimaster.common.switching.warmupDataSynchronizationVariant;

import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.signal.AbstractSignalStrategy;
import eu.qualimaster.common.signal.SynchronizedSignal;
import eu.qualimaster.common.signal.EnactSignal.*;
import eu.qualimaster.common.signal.DisableSignal.*;
import eu.qualimaster.common.signal.StoppedSignal.*;
import eu.qualimaster.common.signal.LastProcessedIdSignal.*;
import eu.qualimaster.common.signal.HeadIdSignal.*;
import eu.qualimaster.common.signal.TransferSignal.*;
import eu.qualimaster.common.signal.TransferredSignal.*;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;
import eu.qualimaster.common.signal.EmitSignal.*;
import eu.qualimaster.common.signal.GoToActiveSignal.*;
import eu.qualimaster.common.signal.GoToPassiveSignal.*;

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
        //initialize the signal handlers
        new EnactOrgINTSignalHandler();
        new DisableOrgENDSignalHandler(); 
        new DisablePRESignalHandler();
        new DisableTrgINTSignalHandler();
        new StoppedOrgINTSignalHandler();
        new LastProcessedIdTrgINTSignalHandler();
        new HeadIdOrgINTSignalHandler();
        new TransferOrgINTSignalHandler();
        new TransferredTrgINTSignalHandler();
        new EmitTrgENDSignalHandler();
        new GoToPassiveOrgENDSignalHandler();
        new GoToActiveTrgENDSignalHandler();
    }
}
