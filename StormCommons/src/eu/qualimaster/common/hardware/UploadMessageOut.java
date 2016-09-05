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
    private int[] portsOut;

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
        this.portsOut = new int[0];
    }

    /**
     * Creates an instance signaling a successful execution.
     * 
     * @param portIn the data input port
     * @param portsOut the data output ports
     * @throws IllegalArgumentException if <code>portsOut</code> is <b>null</b>
     */
    public UploadMessageOut(int portIn, int[] portsOut) {
        if (null == portsOut) {
            throw new IllegalArgumentException("portOut must not be null");
        }
        this.errorMsg = MessageTable.Code.SUCCESS.toMsg();
        this.portIn = portIn;
        this.portsOut = portsOut;
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
     * Returns the (first) data output port for running the algorithm (success case).
     * 
     * @return the first data output port, a negative number if no output port is known 
     * @deprecated use {@link #getPortOutCount()} and {@link #getPortOut(int)} instead.
     */
    @Deprecated
    public int getPortOut() {
        return portsOut.length > 0 ? portsOut[0] : -1;
    }
    
    /**
     * Returns the specified output port (success case).
     * 
     * @param index the 0-based index of the port number
     * @return the port number
     * @throws IndexOutOfBoundsException if <code>index &lt; 0 || index &gt;={@link #getPortOutCount()}</code>
     */
    public int getPortOut(int index) {
        return portsOut[index];
    }
    
    /**
     * Returns the number of output ports (success case).
     * 
     * @return the number
     */
    public int getPortOutCount() {
        return portsOut.length;
    }

    /**
     * Defines the data output ports for running the algorithm (success case).
     * 
     * @param portsOut the data output ports
     */
    void setPortsOut(int[] portsOut) {
        this.portsOut = portsOut;
    }
    
    /**
     * Returns the output ports (use only internal for serialization).
     * 
     * @return the output ports
     */
    int[] getPortsOut() {
        return portsOut;
    }

}
