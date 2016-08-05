package eu.qualimaster.adaptation.events;

import eu.qualimaster.common.QMInternal;

/**
 * Notifies the adaptation about a resource change.
 * 
 * @author Holger Eichelberger
 */
public class ResourceChangeAdaptationEvent extends AdaptationEvent {

    private static final long serialVersionUID = 3527262870391819901L;
    private String resource;
    private Status status;
    
    /**
     * Defines the target status.
     * 
     * @author Holger Eichelberger
     */
    public enum Status {
        ENABLED,
        DISABLED,
        ADDED,
        REMOVED;
    }

    /**
     * Creates a resource change adaptation event.
     * 
     * @param resource the name of the resource
     * @param status the target status
     */
    @QMInternal
    public ResourceChangeAdaptationEvent(String resource, Status status) {
        this.resource = resource;
        this.status = status;
    }
    
    /**
     * The name of the pipeline being affected.
     * 
     * @return the pipeline name
     */
    public String getResource() {
        return resource;
    }
    
    /**
     * Returns the intended status.
     * 
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

}
