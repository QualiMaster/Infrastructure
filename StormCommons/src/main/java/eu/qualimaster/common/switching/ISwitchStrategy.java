package eu.qualimaster.common.switching;

import java.io.Serializable;

import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.common.signal.TopologySignal;

/**
 * Provide the switch strategy interface.
 * @author Cui Qin
 *
 */
public interface ISwitchStrategy {
    /**
     * Returns a tuple receiver handler.
     * @return tuple receiver handler
     */
    public TupleReceiverHandler getTupleReceiverHandler();
    /**
     * Produces tuple.
     * @return the tuple
     */
    public IGeneralTuple produceTuple();
    /**
     * Executes the received signal.
     * @param signal the received signal.
     */
    public void doSignal(TopologySignal signal);
    /**
     * Acknowledges the processed tuple.
     * @param msgId the message id
     */
    public void ack(Object msgId);
    /**
     * Returns the signal value based on the signal name.
     * @param signalName the signal name
     * @return the signal value to be sent
     */
    public Serializable getSignalValue(String signalName);
}
