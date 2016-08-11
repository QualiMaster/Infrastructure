package eu.qualimaster.common.switching;

import java.util.Map;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import eu.qualimaster.common.signal.BaseSignalSpout;
import eu.qualimaster.common.signal.ParameterChangeSignal;

/**
 * Implements a basic switching Spout, acting as the intermediary source.
 * 
 * @author Cui Qin
 *
 */
@SuppressWarnings("serial")
public abstract class BaseSwitchSpout extends BaseSignalSpout {
    private AbstractSwitchMechanism mechanism;
    /**
     * Creates a switch Spout.
     * 
     * @param name
     *            the name of the Spout
     * @param namespace
     *            the namespace, namely the name of the pipeline which the Spout belongs to
     */
    public BaseSwitchSpout(String name, String namespace) {
        super(name, namespace);
    }
    
    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        super.open(conf, context, collector);
    }
    
    @Override
    public void notifyParameterChange(ParameterChangeSignal signal) {
        super.notifyParameterChange(signal);
        mechanism.handleSignal(signal);
    }
    /**
     * Sets the switch mechanism.
     * @param mechanism the switch mechanism
     */
    protected void setSwitchMechanism(AbstractSwitchMechanism mechanism) {
        this.mechanism = mechanism;
    }
    
}
