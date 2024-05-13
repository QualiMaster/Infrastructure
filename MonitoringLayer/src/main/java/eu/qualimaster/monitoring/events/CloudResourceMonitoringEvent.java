package eu.qualimaster.monitoring.events;

import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.observables.IObservable;

/**
 * Implementing a monitoring event to get the updates from the cloud.
 * 
 * @author Bendix Harries
 */
@QMInternal
public class CloudResourceMonitoringEvent extends MonitoringEvent {
   
    private static final long serialVersionUID = -4932745786087325898L;
    private String cloudEnvironment;
    private Map<IObservable, Double> observations;
    
    /**
     * Constructor for updating cloud resources.
     * @param cloudEnvironment the name of the cloud environment
     * @param observations the observations
     */
    public CloudResourceMonitoringEvent(String cloudEnvironment, Map<IObservable, Double> observations) {
        super();
        this.observations = observations;
        this.cloudEnvironment = cloudEnvironment;
    }
    
    /**
     * Returns all observations.
     * 
     * @return the observations
     */
    public Map<IObservable, Double> getObservations() {
        return observations;
    }

    /**
     * Returns the name of the cloud environment.
     * @return the cloud environment
     */
    public String getCloudEnvironment() {
        return cloudEnvironment;
    }
    
}
