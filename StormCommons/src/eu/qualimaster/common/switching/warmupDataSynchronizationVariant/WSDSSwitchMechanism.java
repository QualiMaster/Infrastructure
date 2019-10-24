package eu.qualimaster.common.switching.warmupDataSynchronizationVariant;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.signal.AbstractSignalStrategy;
import eu.qualimaster.common.signal.TopologySignal;
import eu.qualimaster.common.switching.AbstractAlgorithm;
import eu.qualimaster.common.switching.AbstractSwitchMechanism;
import eu.qualimaster.common.switching.synchronization.AbstractSynchronizationStrategy;
import eu.qualimaster.common.switching.tupleEmit.AbstractTupleEmitStrategy;
import eu.qualimaster.common.switching.tupleReceiving.AbstractTupleReceiveStrategy;

/**
 * The switch mechanism, i.e., the implementation of the switch, for the
 * "Warm-up Switch with Data Synchronization" variant.
 * 
 * @author Cui Qin
 *
 */
public class WSDSSwitchMechanism extends AbstractSwitchMechanism {
    /**
     * Constructor of the switch mechanism for the
     * "Warm-up Switch with Data Synchronization" variant.
     * 
     * @param signalStrategy
     *            the signal strategy
     */
    public WSDSSwitchMechanism(WSDSSignalStrategy signalStrategy) {
        super(signalStrategy);
    }

    /**
     * Constructor of the switch mechanism for the
     * "Warm-up Switch with Data Synchronization" variant.
     * 
     * @param signalStrategy
     *            the signal strategy
     * @param synchronizationStrategy
     *            the synchronization strategy
     */
    public WSDSSwitchMechanism(WSDSSignalStrategy signalStrategy,
            AbstractSynchronizationStrategy synchronizationStrategy) {
        super(signalStrategy, synchronizationStrategy);
    }

    /**
     * Constructor of the switch mechanism for the
     * "Warm-up Switch with Data Synchronization" variant.
     * 
     * @param signalStrategy
     *            the signal strategy
     * @param determinationStrategy
     *            the determination strategy
     * @param synchronizationStrategy
     *            the synchronization strategy
     */
    public WSDSSwitchMechanism(WSDSSignalStrategy signalStrategy, WSDSDeterminationStrategy determinationStrategy,
            AbstractSynchronizationStrategy synchronizationStrategy) {
        super(signalStrategy, determinationStrategy, synchronizationStrategy);
    }

    /**
     * Constructor of the switch mechanism.
     * 
     * @param signalStrategy
     *            the signal strategy
     * @param synchronizationStrategy
     *            the synchronization strategy
     * @param tupleEmitStrategy
     *            the tuple emit strategy
     * @param tupleReceiveStrategy
     *            the tuple receive strategy
     */
    public WSDSSwitchMechanism(AbstractSignalStrategy signalStrategy,
            AbstractSynchronizationStrategy synchronizationStrategy, AbstractTupleEmitStrategy tupleEmitStrategy,
            AbstractTupleReceiveStrategy tupleReceiveStrategy) {
        super(signalStrategy, synchronizationStrategy, tupleEmitStrategy, tupleReceiveStrategy);
    }
    
    /**
     * Constructor of the switch mechanism for the
     * "Warm-up Switch with Data Synchronization" variant.
     * 
     * @param signalStrategy
     *            the signal strategy
     * @param determinationStrategy
     *            the determination strategy
     * @param synchronizationStrategy
     *            the synchronization strategy
     * @param tupleEmitStrategy
     *            the tuple emit strategy
     * @param tupleReceiveStrategy
     *            the tuple receive strategy
     */
    public WSDSSwitchMechanism(WSDSSignalStrategy signalStrategy, WSDSDeterminationStrategy determinationStrategy,
            AbstractSynchronizationStrategy synchronizationStrategy,
            AbstractTupleEmitStrategy tupleEmitStrategy, AbstractTupleReceiveStrategy tupleReceiveStrategy) {
        super(signalStrategy, determinationStrategy, synchronizationStrategy, tupleEmitStrategy, tupleReceiveStrategy);
    }

    @Override
    public ISwitchTuple getNextTuple() {
        return super.getNextTuple();
    }

    @Override
    public void ack(Object msgId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void handleSignal(TopologySignal signal) {
        // TODO Auto-generated method stub

    }

}
