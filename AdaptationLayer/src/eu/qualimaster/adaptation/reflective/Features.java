package eu.qualimaster.adaptation.reflective;

import java.util.ArrayList;

/**
 * Represents the features of a pattern used for reflective adaptation.
 * 
 * @author  Andrea Ceroni
 */
public class Features {
    
    /** The values of the monitoring features */
    private ArrayList<Double> monitoringFeatures;
    
    /** The values of the adaptation features */
    private ArrayList<Double> adaptationFeatures;
    
    /**
     * Default constructor.
     */
    public Features(){
        this.monitoringFeatures = new ArrayList<>();
        this.adaptationFeatures = new ArrayList<>();
    }
    
    /**
     * Constructor with input features.
     * @param monitoringFeatures the features extracted from the monitoring log.
     * @param adaptationFeatures the features extracted from the adaptation log.
     */
    public Features(ArrayList<Double> monitoringFeatures, ArrayList<Double> adaptationFeatures){
        this.monitoringFeatures = monitoringFeatures;
        this.adaptationFeatures = adaptationFeatures;
    }

    /**
     * @return the monitoringFeatures
     */
    public ArrayList<Double> getMonitoringFeatures() {
        return monitoringFeatures;
    }

    /**
     * @param monitoringFeatures the monitoringFeatures to set
     */
    public void setMonitoringFeatures(ArrayList<Double> monitoringFeatures) {
        this.monitoringFeatures = monitoringFeatures;
    }

    /**
     * @return the adaptationFeatures
     */
    public ArrayList<Double> getAdaptationFeatures() {
        return adaptationFeatures;
    }

    /**
     * @param adaptationFeatures the adaptationFeatures to set
     */
    public void setAdaptationFeatures(ArrayList<Double> adaptationFeatures) {
        this.adaptationFeatures = adaptationFeatures;
    }
}
