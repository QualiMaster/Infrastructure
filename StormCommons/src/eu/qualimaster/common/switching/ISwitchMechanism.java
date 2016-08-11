package eu.qualimaster.common.switching;

import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.common.signal.TopologySignal;

/**
 * Defines the interface for the switch mechanism.
 * @author Cui Qin
 *
 */
public interface ISwitchMechanism {
    /**
     * Switches from the algorithm <code>from</code> to the algorithm <code>to</code>.
     * @param from the original algorithm to be switched
     * @param to the target algorithm to switch to
     */
    void doSwitch(AbstractAlgorithm from, AbstractAlgorithm to);
    /**
     * Returns the next tuple.
     * @return the next tuple
     */
    public IGeneralTuple getNextTuple();
    /**
     * Acknowledges the processed tuple.
     * @param msgId the message id
     */
    public void ack(Object msgId);
    /**
     * Handles received signals.
     * @param signal the received signal
     */
    public void handleSignal(TopologySignal signal);
}
