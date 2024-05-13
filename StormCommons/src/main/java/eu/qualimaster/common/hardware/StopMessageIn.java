package eu.qualimaster.common.hardware;

/**
 * Stop algorithm message request. (Public for testing)
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
public class StopMessageIn {

    private String id;

    /**
     * Returns the id of the algorithm to stop.
     * 
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Defines the id of the algorithm to stop.
     * 
     * @param id the algorithm id
     */
    void setId(String id) {
        this.id = id;
    }

}
