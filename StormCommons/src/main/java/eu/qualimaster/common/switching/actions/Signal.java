package eu.qualimaster.common.switching.actions;
/**
 * Provides signals used in the algorithm switch.
 * @author Cui Qin
 *
 */
public enum Signal {
    ENACT("enact"), 
    DISABLE("disable"),
    ENABLE("enable"),
    STOPPED("stopped"), 
    TRANSFERRED("transferred"),
    EMIT("emit"),
    TRANSFER("transfer"),
    LASTPROCESSEDID("lastProcessedId"),
    HEADID("headId"),
    GOTOPASSIVE("goToPassive"),
    GOTOACTIVE("goToActive"),
    //SYNCHRONIZED("synchronized"),
    COMPLETED("completed");
    
    private String signalName;
    
    /**
     * Constructor.
     * @param signalName the signal name
     */
    Signal(String signalName) {
        this.signalName = signalName;
    }
    
    /**
     * Returns the signal name.
     * @return the signal name
     */
    public String getSignalName() {
        return signalName;
    }
};
