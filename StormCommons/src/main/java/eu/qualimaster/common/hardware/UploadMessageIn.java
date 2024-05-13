package eu.qualimaster.common.hardware;

import com.google.protobuf.ByteString;

/**
 * Upload message request. (Public for testing)
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
public class UploadMessageIn {

    private String id;
    private ByteString executable;
    private int portCount = 1;

    /**
     * Returns the ID of the algorithm to upload.
     * 
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Defines the ID of the algorithm to upload.
     * 
     * @param id the algorithm ID
     */
    void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the (URL of the) executable of the algorithm to upload.
     * 
     * @return the executable
     */
    public ByteString getExecutable() {
        return executable;
    }

    /**
     * Defines the (URL of the) executable of the algorithm to upload.
     * 
     * @param executable the executable
     */
    void setExecutable(ByteString executable) {
        this.executable = executable;
    }
    
    /**
     * Defines the number of ports to use (output parallelism).
     *  
     * @param count the number of ports
     */
    void setPortCount(int count) {
        this.portCount = Math.max(1,  count);
    }
    
    /**
     * The number of output ports to use.
     * 
     * @return the number of output ports
     */
    public int getPortCount() {
        return portCount;
    }
    
}
