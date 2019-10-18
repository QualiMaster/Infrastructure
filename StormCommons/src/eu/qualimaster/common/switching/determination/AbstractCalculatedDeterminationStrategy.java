package eu.qualimaster.common.switching.determination;

import eu.qualimaster.common.signal.AbstractSignalConnection;
import eu.qualimaster.common.signal.EnactSignal;
import eu.qualimaster.common.switching.SwitchNodeNameInfo;

/**
 * An abstract class for the determination strategy which needs to calculate the
 * switch point.
 * 
 * @author Cui Qin
 *
 */
public abstract class AbstractCalculatedDeterminationStrategy extends AbstractDeterminationStrategy {
    private long switchRequestedTimestamp;
    private AbstractSignalConnection signalConnection;

    /**
     * Constructor.
     * 
     * @param switchRequestedTimestamp
     *            the timestamp when the switch is requested
     * @param signalConnection
     *            the signal connection used to send signals
     */
    public AbstractCalculatedDeterminationStrategy(long switchRequestedTimestamp,
            AbstractSignalConnection signalConnection) {
        this.switchRequestedTimestamp = switchRequestedTimestamp;
        this.signalConnection = signalConnection;
    }

    /**
     * Delegate the determination task to the original intermediary node.
     */
    public void delegateDetermination() {
        EnactSignal.sendSignal(SwitchNodeNameInfo.getInstance().getTopologyName(),
                SwitchNodeNameInfo.getInstance().getOriginalIntermediaryNodeName(), switchRequestedTimestamp,
                signalConnection);
    }

}
