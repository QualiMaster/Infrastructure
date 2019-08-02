package eu.qualimaster.common.switching.warmupDataSynchronizationVariant;

import java.util.Map;

import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.common.signal.TopologySignal;
import eu.qualimaster.common.switching.AbstractAlgorithm;
import eu.qualimaster.common.switching.IStrategy;
import eu.qualimaster.common.switching.ISwitchMechanism;
import eu.qualimaster.common.switching.SwitchStrategies;
import eu.qualimaster.common.switching.synchronization.SeparatedINTSynchronizationStrategy;
/**
 * The switch mechanism, i.e., the implementation of the switch, 
 * for the "Warm-up Switch with Data Synchronization" variant. 
 * @author Cui Qin
 *
 */
public class WSDSSwitchMechanism implements ISwitchMechanism {
    private WSDSSignalStrategy signalStrategy;
    private WSDSDeterminationStrategy determinationStrategy;
    private SeparatedINTSynchronizationStrategy synchronizationStrategy;
    private Map<String, IStrategy> strategies;
    
    /**
     * Constructor of the switch mechanism for the "Warm-up Switch with Data Synchronization" variant. 
     * @param signalStrategy the signal strategy
     * @param determinationStrategy the determination strategy
     * @param synchronizationStrategy the synchronization strategy
     */
    public WSDSSwitchMechanism(WSDSSignalStrategy signalStrategy, WSDSDeterminationStrategy determinationStrategy
            , SeparatedINTSynchronizationStrategy synchronizationStrategy) {
        this.signalStrategy = signalStrategy;
        this.determinationStrategy = determinationStrategy;
        strategies = SwitchStrategies.getInstance().getStrategies();
        strategies.put(signalStrategy.getStrategyType(), signalStrategy);
        strategies.put(determinationStrategy.getStrategyType(), determinationStrategy);
        strategies.put(synchronizationStrategy.getStrategyType(), synchronizationStrategy);
    }
    
    @Override
    public void doSwitch(AbstractAlgorithm from, AbstractAlgorithm to) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public IGeneralTuple getNextTuple() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void ack(Object msgId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void handleSignal(TopologySignal signal) {
        // TODO Auto-generated method stub
        
    }
    
    /**
     * Return the synchronization strategy.
     * @return the synchronization strategy
     */
    public SeparatedINTSynchronizationStrategy getSynchronizationStrategy() {
        return synchronizationStrategy;
    }

    /**
     * Set the synchronization strategy.
     * @param synchronizationStrategy the synchronization strategy
     */
    public void setSynchronizationStrategy(SeparatedINTSynchronizationStrategy synchronizationStrategy) {
        this.synchronizationStrategy = synchronizationStrategy;
        strategies.put(synchronizationStrategy.getStrategyType(), synchronizationStrategy);
    }
    
    
}
