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
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public BaseSwitchSpout(String name, String namespace, boolean sendRegular) {
        super(name, namespace, sendRegular);
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        super.open(conf, context, collector);
    }
    
    @Override
    public void notifyParameterChange(ParameterChangeSignal signal) {
        super.notifyParameterChange(signal);
        mechanism.handleSignal(signal);
    }
    
    @Override
    public void ack(Object msgId) {
        mechanism.ack(msgId);
    }
    /**
     * Sets the switch mechanism.
     * @param mechanism the switch mechanism
     */
    protected void setSwitchMechanism(AbstractSwitchMechanism mechanism) {
        this.mechanism = mechanism;
    }
    
}
