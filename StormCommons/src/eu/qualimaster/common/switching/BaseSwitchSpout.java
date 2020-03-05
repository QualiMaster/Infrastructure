package eu.qualimaster.common.switching;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import eu.qualimaster.common.logging.DataLogger;
import eu.qualimaster.common.signal.BaseSignalSpout;
import eu.qualimaster.common.switching.actions.IAction;
import eu.qualimaster.common.switching.actions.SwitchStates.ActionState;

/**
 * Implements a basic switching Spout, acting as the intermediary source.
 * 
 * @author Cui Qin
 *
 */
@SuppressWarnings("serial")
public abstract class BaseSwitchSpout extends BaseSignalSpout {
//    private AbstractSwitchMechanism mechanism;
    private Map<ActionState, List<IAction>> actionMap;
//    private transient LogWriter logWriter = null;
    private transient PrintWriter logWriter = null;
    
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
        actionMap = new HashMap<ActionState, List<IAction>>();
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        super.open(conf, context, collector);
        String logDir = (String) conf.get("LOG.DIRECTORY");
//        logWriter = new LogWriter(DataLogger.getPrintWriter(logDir + getName() + ".log"));
        logWriter = DataLogger.getPrintWriter(logDir + getName() + ".log");
    }
    
    /**
     * Adds actions into a map that links the action state and the corresponding actions together.
     * @param state the action state
     * @param action the action to be added
     */
    protected void addAction(ActionState state, IAction action) {
        if (actionMap.containsKey(state)) {
            List<IAction> list = actionMap.get(state);
            list.add(action);
            actionMap.put(state, list);
        } else {
            List<IAction> listNew = new ArrayList<IAction>();
            listNew.add(action);
            actionMap.put(state, listNew);
        }
    }
    
    /**
     * Adds the switch actions.
     */
    public void addSwitchActions() {}
    
    /**
     * Returns the action map.
     * @return the action map
     */
    protected Map<ActionState, List<IAction>> getActionMap() {
        return this.actionMap;
    }
    
//    /**
//     * Returns the log writer.
//     * @return the log writer
//     */
//    protected LogWriter getLogWriter() {
//        return logWriter;
//    }
    
    /**
     * Returns the log writer.
     * @return the log writer
     */
    protected PrintWriter getLogWriter() {
        return logWriter;
    }
    
//    @Override
//    public void notifyParameterChange(ParameterChangeSignal signal) {
//        super.notifyParameterChange(signal);
//        mechanism.handleSignal(signal);
//    }
//    
//    @Override
//    public void ack(Object msgId) {
//        mechanism.ack(msgId);
//    }
    /**
     * Sets the switch mechanism.
     * @param mechanism the switch mechanism
     */
    @Deprecated
    protected void setSwitchMechanism(AbstractSwitchMechanism mechanism) {
//        this.mechanism = mechanism;
    }
    
}
