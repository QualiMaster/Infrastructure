package eu.qualimaster.common.hardware;

/**
 * Is-running response.
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
public class IsRunningAlgorithmOut {

    private boolean isRunning;

    /**
     * Creates an instance.
     */
    IsRunningAlgorithmOut() {
    }

    /**
     * Creates an instance.
     * 
     * @param isRunning <b>true</b> for running, <code>false</code> else
     */
    public IsRunningAlgorithmOut(boolean isRunning) {
        this.isRunning = isRunning;
    }

    /**
     * Returns whether the algorithm specified in the request is running.
     * 
     * @return <b>true</b> for running, <code>false</code> else
     */
    public boolean getIsRunning() {
        return isRunning;
    }

    /**
     * Defines whether the algorithm is running.
     * 
     * @param isRunning <b>true</b> for running, <code>false</code> else
     */
    void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }
    
}
