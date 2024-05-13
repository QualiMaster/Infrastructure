package eu.qualimaster.adaptation.external;

import java.util.Map;

/**
 * A simple demo monitoring message.
 * 
 * @author Holger Eichelberger
 */
public class MonitoringDataMessage extends UsualMessage {

    private static final long serialVersionUID = 817780441199448872L;
    private String part;
    private Map<String, Double> observations;
    
    /**
     * Creates a monitoring data message.
     * 
     * @param part the infrastructure part being monitored
     * @param observations the actual observations
     */
    public MonitoringDataMessage(String part, Map<String, Double> observations) {
        this.observations = observations;
        this.part = part;
    }

    /**
     * Returns the observations.
     * 
     * @return the observations
     */
    public Map<String, Double> getObservations() {
        return observations;
    }

    /**
     * Returns the name of the system part.
     * 
     * @return the name of the system part
     */
    public String getPart() {
        return part;
    }

    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleMonitoringDataMessage(this);
    }
    
    @Override
    public int hashCode() {
        return Utils.hashCode(getPart()) + Utils.hashCode(getObservations());
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj instanceof MonitoringDataMessage) {
            MonitoringDataMessage msg = (MonitoringDataMessage) obj;
            equals = Utils.equals(getPart(), msg.getPart());
            equals &= ((null == getObservations() && null == msg.getObservations()) 
                || (null != getObservations() && getObservations().equals(msg.getObservations())));
        }
        return equals;
    }
    
    @Override
    public boolean passToUnauthenticatedClient() {
        return false; // although not privileged, do not pass to reduce traffic and load
    }

    @Override
    public Message toInformation() {
        return null; // do not dispatch
    }

    @Override
    public String toString() {
        return "MonitoringDataMessage " + part + " " + observations;
    }

}
