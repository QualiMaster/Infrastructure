package eu.qualimaster.adaptation.external;

import java.util.Map;

import eu.qualimaster.observables.IObservable;

/**
 * Implements a UpdateCloudResourceMessage. Indicating a change to the cloud resources or an update of the continuous 
 * updated values like ping or bandwidth.
 * 
 * @author Bendix Harries
 */
public class UpdateCloudResourceMessage extends UsualMessage {
    
    private static final long serialVersionUID = 7363753734209032097L;

    private String name;
    private Map<IObservable, Double> observations;
    
    /**
     * Constructor for Message with 2 attributes that have to be updated very often.
     * @param name name of the cloud environment
     * @param observations the observations
     */
    public UpdateCloudResourceMessage(String name, Map<IObservable, Double> observations) {
        super();
        this.name = name;
        this.observations = observations;
    }
    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleUpdateCloudResourceMessage(this);
    }
    
    /**
     * Returns the observations.
     * @return the observations
     */
    public Map<IObservable, Double> getObservations() {
        return observations;
    }

    /**
     * Returns the name of the cloud environment.
     * @return the name
     */
    public String getName() {
        return name;
    }

    @Override
    public Message toInformation() {
        return null;
    }

}
