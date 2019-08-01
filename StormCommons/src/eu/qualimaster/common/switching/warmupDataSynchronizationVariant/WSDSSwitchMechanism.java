package eu.qualimaster.common.switching.warmupDataSynchronizationVariant;

import java.util.Map;

import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.common.signal.TopologySignal;
import eu.qualimaster.common.switching.AbstractAlgorithm;
import eu.qualimaster.common.switching.IStrategy;
import eu.qualimaster.common.switching.ISwitchMechanism;
import eu.qualimaster.common.switching.SwitchStrategies;
/**
 * The switch mechanism, i.e., the implementation of the switch, 
 * for the "Warm-up Switch with Data Synchronization" variant. 
 * @author Cui Qin
 *
 */
public class WSDSSwitchMechanism implements ISwitchMechanism {
    private WSDSSignalStrategy signalStrategy;
    private WSDSDeterminationStrategy determinationStrategy;
    private Map<String, IStrategy> strategies;
    
    /**
     * Constructor of the switch mechanism for the "Warm-up Switch with Data Synchronization" variant. 
     * @param signalStrategy the signal strategy
     * @param determinationStrategy the determination strategy
     */
    public WSDSSwitchMechanism(WSDSSignalStrategy signalStrategy, WSDSDeterminationStrategy determinationStrategy) {
        this.signalStrategy = signalStrategy;
        this.determinationStrategy = determinationStrategy;
        strategies = SwitchStrategies.getInstance().getStrategies();
        strategies.put(signalStrategy.getStrategyType(), signalStrategy);
        strategies.put(determinationStrategy.getStrategyType(), determinationStrategy);
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

}
