package eu.qualimaster.common.hardware;

/**
 * Is-running request. (Public for testing)
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
public class IsRunningAlgorithmIn {

    private String id;

    /**
     * Returns the id of the algorithm to check for running.
     * 
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Defines the id of the algorithm to check.
     * 
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }
}
