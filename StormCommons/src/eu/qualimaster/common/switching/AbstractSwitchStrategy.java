package eu.qualimaster.common.switching;

import java.io.Serializable;

import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.common.signal.TopologySignal;

/**
 * The strategy used in the switch mechanism.
 * @author Cui Qin
 *
 */
public abstract class AbstractSwitchStrategy {
    /**
     * Returns a tuple receiver handler.
     * @return tuple receiver handler
     */
    public abstract TupleReceiverHandler getTupleReceiverHandler();
    /**
     * Produces tuple.
     * @return the tuple
     */
    public abstract IGeneralTuple produceTuple();
    /**
     * Executes the received signal.
     * @param signal the received signal.
     */
    public abstract void doSignal(TopologySignal signal);
    /**
     * Returns the signal value based on the signal name.
     * @param signalName the signal name
     * @return the signal value to be sent
     */
    public abstract Serializable getSignalValue(String signalName);
}
