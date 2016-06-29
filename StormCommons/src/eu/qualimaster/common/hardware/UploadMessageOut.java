package eu.qualimaster.common.hardware;

/**
 * Response upload message.
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
public class UploadMessageOut {

    private String errorMsg;
    private int portIn;
    private int portOut;

    /**
     * Creates an empty instance.
     */
    UploadMessageOut() {
    }

    /**
     * Creates an instance signaling an erroneous execution.
     * 
     * @param errorMsg the error message
     */
    public UploadMessageOut(String errorMsg) {
        this.errorMsg = errorMsg;
        this.portIn = -1;
        this.portOut = -1;
    }

    /**
     * Creates an instance signaling a successful execution.
     * 
     * @param portIn the data input port
     * @param portOut the data output port
     */
    public UploadMessageOut(int portIn, int portOut) {
        this.errorMsg = MessageTable.Code.SUCCESS.toMsg();
        this.portIn = portIn;
        this.portOut = portOut;
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
        return !Utils.isError(errorMsg);
    }

    /**
     * Returns the data input port for running the algorithm (success case).
     * 
     * @return the data input port
     */
    public int getPortIn() {
        return portIn;
    }

    /**
     * Defines the data input port for running the algorithm (success case).
     * 
     * @param portIn the data input port
     */
    void setPortIn(int portIn) {
        this.portIn = portIn;
    }

    /**
     * Returns the data output port for running the algorithm (success case).
     * 
     * @return the data output port
     */
    public int getPortOut() {
        return portOut;
    }

    /**
     * Defines the data input port for running the algorithm (success case).
     * 
     * @param portOut the data output port
     */
    void setPortOut(int portOut) {
        this.portOut = portOut;
    }

}
