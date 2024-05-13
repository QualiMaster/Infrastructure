package eu.qualimaster.monitoring.events;

import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractEvent;

/**
 * Informs external clients about changes in the monitored system state via the adaptation layer.
 * As no adaptation shall be triggered, this is not an adaptation event.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class MonitoringInformationEvent extends AbstractEvent {

    private static final long serialVersionUID = -4771542594405836672L;
    private String part;
    private Map<String, Double> observations;
    private String partType;
    
    /**
     * Creates a monitoring data message.
     * 
     * @param partType the part type
     * @param part the system / infrastructure part(name)
     * @param observations the observations characterized by the message
     */
    public MonitoringInformationEvent(String partType, String part, Map<String, Double> observations) {
        this.part = part;
        this.partType = partType;
        this.observations = observations;
    }
    
    /**
     * Returns the part type.
     * 
     * @return the part type
     */
    public String getPartType() {
        return partType;
    }

    /**
     * Returns the name of the observable.
     * 
     * @return the observable
     */
    public String getPart() {
        return part;
    }

    /**
     * Returns the value of the observable.
     * 
     * @return the value of the observable
     */
    public Map<String, Double> getObservations() {
        return observations;
    }

}
