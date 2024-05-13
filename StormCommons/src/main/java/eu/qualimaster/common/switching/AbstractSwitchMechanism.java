package eu.qualimaster.common.switching;

import java.util.Map;

import org.apache.log4j.Logger;

import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.common.signal.AbstractSignalStrategy;
import eu.qualimaster.common.switching.determination.AbstractDeterminationStrategy;
import eu.qualimaster.common.switching.synchronization.AbstractSynchronizationStrategy;
import eu.qualimaster.common.switching.tupleEmit.AbstractTupleEmitStrategy;
import eu.qualimaster.common.switching.tupleReceiving.AbstractTupleReceiveStrategy;

/**
 * An abstract class for the switch mechanism.
 * 
 * @author Cui Qin
 *
 */
public abstract class AbstractSwitchMechanism implements ISwitchMechanism {
    private static final Logger LOGGER = Logger.getLogger(AbstractSwitchMechanism.class);
    private AbstractSignalStrategy signalStrategy;
    private AbstractDeterminationStrategy determinationStrategy;
    private AbstractSynchronizationStrategy synchronizationStrategy;
    private AbstractTupleEmitStrategy tupleEmitStrategy;
    private AbstractTupleReceiveStrategy tupleReceiveStrategy;
    private Map<String, IStrategy> strategies;

    /**
     * Constructor without parameters. To be deleted.
     */
    public AbstractSwitchMechanism() {
    }

    /**
     * Constructor of the switch mechanism for the
     * "Warm-up Switch with Data Synchronization" variant.
     * 
     * @param signalStrategy
     *            the signal strategy
     */
    public AbstractSwitchMechanism(AbstractSignalStrategy signalStrategy) {
        this.signalStrategy = signalStrategy;
        strategies = SwitchStrategies.getInstance().getStrategies();
        LOGGER.info("Registing the signal strategy: " + signalStrategy.getStrategyType());
        strategies.put(signalStrategy.getStrategyType(), signalStrategy);
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
    public AbstractSwitchMechanism(AbstractSignalStrategy signalStrategy,
            AbstractSynchronizationStrategy synchronizationStrategy) {
        this(signalStrategy);
        strategies = SwitchStrategies.getInstance().getStrategies();
        LOGGER.info("Registing the synchronization strategy: " + synchronizationStrategy.getStrategyType());
        strategies.put(synchronizationStrategy.getStrategyType(), synchronizationStrategy);
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
    public AbstractSwitchMechanism(AbstractSignalStrategy signalStrategy,
            AbstractDeterminationStrategy determinationStrategy,
            AbstractSynchronizationStrategy synchronizationStrategy) {
        this(signalStrategy, synchronizationStrategy);
        this.determinationStrategy = determinationStrategy;
        strategies = SwitchStrategies.getInstance().getStrategies();
        LOGGER.info("Registing the determination strategy: " + determinationStrategy.getStrategyType());
        strategies.put(determinationStrategy.getStrategyType(), determinationStrategy);
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
    public AbstractSwitchMechanism(AbstractSignalStrategy signalStrategy,
            AbstractSynchronizationStrategy synchronizationStrategy, AbstractTupleEmitStrategy tupleEmitStrategy,
            AbstractTupleReceiveStrategy tupleReceiveStrategy) {
        this(signalStrategy, synchronizationStrategy);
        this.tupleEmitStrategy = tupleEmitStrategy;
        this.tupleReceiveStrategy = tupleReceiveStrategy;
        strategies = SwitchStrategies.getInstance().getStrategies();
        LOGGER.info("Registing the tuple emit strategy: " + tupleEmitStrategy.getStrategyType());
        strategies.put(tupleEmitStrategy.getStrategyType(), tupleEmitStrategy);
        LOGGER.info("Registing the tuple receiving strategy: " + tupleReceiveStrategy.getStrategyType());
        strategies.put(tupleReceiveStrategy.getStrategyType(), tupleReceiveStrategy);
    }
    
    /**
     * Constructor of the switch mechanism.
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
    public AbstractSwitchMechanism(AbstractSignalStrategy signalStrategy,
            AbstractDeterminationStrategy determinationStrategy,
            AbstractSynchronizationStrategy synchronizationStrategy, AbstractTupleEmitStrategy tupleEmitStrategy,
            AbstractTupleReceiveStrategy tupleReceiveStrategy) {
        this(signalStrategy, determinationStrategy, synchronizationStrategy);
        this.tupleEmitStrategy = tupleEmitStrategy;
        this.tupleReceiveStrategy = tupleReceiveStrategy;
        strategies = SwitchStrategies.getInstance().getStrategies();
        strategies.put(tupleEmitStrategy.getStrategyType(), tupleEmitStrategy);
        strategies.put(tupleReceiveStrategy.getStrategyType(), tupleReceiveStrategy);
    }

    @Override
    public ISwitchTuple getNextTuple() {
        ISwitchTuple result = null;
        // determine the safepoint
        if (null != determinationStrategy && !determinationStrategy.isDetermined()) {
            determinationStrategy.waitForSwitchPoint();
        }
        // get next tuple to be emitted
        if (null != tupleEmitStrategy) {
            result = tupleEmitStrategy.nextEmittedTuple();
        }
        return result;
    }

    /**
     * Return the signal strategy.
     * 
     * @return the signal strategy
     */
    public AbstractSignalStrategy getSignalStrategy() {
        return signalStrategy;
    }

    /**
     * Return the determination strategy.
     * 
     * @return the determination strategy
     */
    public AbstractDeterminationStrategy getDeterminationStrategy() {
        return determinationStrategy;
    }

    /**
     * Return the synchronization strategy.
     * 
     * @return the synchronization strategy
     */
    public AbstractSynchronizationStrategy getSynchronizationStrategy() {
        return synchronizationStrategy;
    }

    /**
     * Return the tuple emit strategy.
     * 
     * @return the tuple emit strategy
     */
    public AbstractTupleEmitStrategy getTupleEmitStrategy() {
        return tupleEmitStrategy;
    }

    /**
     * Return the tuple receive strategy.
     * @return the tuple receive strategy
     */
    public AbstractTupleReceiveStrategy getTupleReceiveStrategy() {
        return tupleReceiveStrategy;
    }

}
