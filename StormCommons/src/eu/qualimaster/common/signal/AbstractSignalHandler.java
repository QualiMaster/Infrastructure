package eu.qualimaster.common.signal;

import eu.qualimaster.common.switching.SwitchNodeNameInfo;

/**
 * Define an abstract signal handler.
 * @author Cui Qin
 *
 */
public abstract class AbstractSignalHandler implements ISignalHandler {
    private ParameterChangeSignal signal;
    private String node;
    
    /**
     * Constructor for an abstract signal handler.
     * @param signal the switch-related signal
     * @param node the name of the node in which the signal shall be handled
     */
//    public AbstractSignalHandler(ParameterChangeSignal signal, String node) {
//        this.setSignal(signal);
//        this.setNode(node);
//    }
    
    /**
     * Gets the signal.
     * @return the signal
     */
    public ParameterChangeSignal getSignal() {
        return signal;
    }

    /**
     * Sets the signal.
     * @param signal the signal
     */
    public void setSignal(ParameterChangeSignal signal) {
        this.signal = signal;
    }

    /**
     * Gets the node name.
     * @return the node name
     */
    public String getNode() {
        return node;
    }

    /**
     * Sets the node name.
     * @param node the node name
     */
    public void setNode(String node) {
        this.node = node;
    }
    
    /**
     * Gets the information of the names of the switch-related nodes.
     * @return the information of the names of the switch-related nodes
     */
    public static SwitchNodeNameInfo getNameInfo() {
        return SwitchNodeNameInfo.getInstance();
    }
}
