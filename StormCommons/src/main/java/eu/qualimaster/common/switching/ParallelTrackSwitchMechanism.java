package eu.qualimaster.common.switching;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.signal.TopologySignal;

/**
 * A switch mechanism using parallel track strategy.
 * @author Cui Qin
 *
 */
public class ParallelTrackSwitchMechanism extends AbstractSwitchMechanism {
    private transient AbstractSwitchStrategy strategy;
    /**
     * Creates a parallel track switch mechanism.
     * @param strategy the switch strategy
     */
    public ParallelTrackSwitchMechanism(AbstractSwitchStrategy strategy) {
        this.strategy = strategy;
    }
    
    @Override
    public ISwitchTuple getNextTuple() {
        return (ISwitchTuple) strategy.produceTuple();
    }

    @Override
    public void handleSignal(TopologySignal signal) {
        strategy.doSignal(signal);
    }

    @Override
    public void ack(Object msgId) {
        strategy.ack(msgId);
    }  

}
