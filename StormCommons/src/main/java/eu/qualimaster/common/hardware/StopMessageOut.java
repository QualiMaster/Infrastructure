package eu.qualimaster.common.hardware;


/**
 * Stop algorithm response.
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
public class StopMessageOut {

    private String errorMsg;

    /**
     * Creates an empty instance.
     */
    StopMessageOut() {
    }

    /**
     * Creates an instance.
     * 
     * @param errorMsg the error message
     */
    public StopMessageOut(String errorMsg) {
        this.errorMsg = errorMsg;
    }
    
    /**
     * Returns the error message.
     * 
     * @return the error message
     */
    public String getErrorMsg() {
        return errorMsg;
    }

    /**
     * Defines the error message.
     * 
     * @param errorMsg the error message
     */
    void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    /**
     * Returns whether this message indicates a successful execution.
     * 
     * @return <code>true</code> for successful, <code>false</code> else
     */
    public boolean isSuccessful() {
        return Utils.isSuccess(errorMsg);
    }

}
