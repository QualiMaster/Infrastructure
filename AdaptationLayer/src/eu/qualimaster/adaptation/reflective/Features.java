package eu.qualimaster.adaptation.reflective;

import java.util.ArrayList;

/**
 * Represents the features of a pattern used for reflective adaptation.
 * 
 * @author  Andrea Ceroni
 */
public class Features {
    
    /** The values of the monitoring features at the most recent time point */
    private ArrayList<Double> lastMonitoring;
    
    /** The values of the monitoring features */
    private ArrayList<Double> aggregateMonitoring;
    
    /** The values of the adaptation features */
    private ArrayList<Double> adaptation;
    
    /**
     * Default constructor.
     */
    public Features(){
        this.lastMonitoring = new ArrayList<>();
        this.aggregateMonitoring = new ArrayList<>();
        this.adaptation = new ArrayList<>();
    }
    
    /**
     * Constructor with input features.
     * @param lastMonitoring the features extracted from the monitoring log at the most recent time point.
     * @param aggregateMonitoring the features extracted and aggregated from a set of recent time points.
     * @param adaptation the features extracted from the adaptation log.
     */
    public Features(ArrayList<Double> lastMonitoring, ArrayList<Double> aggregateMonitoring, ArrayList<Double> adaptation){
        this.lastMonitoring = lastMonitoring;
        this.aggregateMonitoring = aggregateMonitoring;
        this.adaptation = adaptation;
    }
    
    /**
     * Copy constructor.
     * @param f the features to be copied.
     */
    public Features(Features f){
        this.lastMonitoring = new ArrayList<>();
        for(double d : f.getLastMonitoring()) this.lastMonitoring.add(d);
        this.aggregateMonitoring = new ArrayList<>();
        for(double d : f.getAggregateMonitoring()) this.aggregateMonitoring.add(d);
        this.adaptation = new ArrayList<>();
        for(double d : f.getAdaptation()) this.adaptation.add(d);
    }

    /**
     * @return the lastMonitoring
     */
    public ArrayList<Double> getLastMonitoring() {
        return lastMonitoring;
    }

    /**
     * @param lastMonitoring the lastMonitoring to set
     */
    public void setLastMonitoring(ArrayList<Double> lastMonitoring) {
        this.lastMonitoring = lastMonitoring;
    }

    /**
     * @return the aggregateMonitoring
     */
    public ArrayList<Double> getAggregateMonitoring() {
        return aggregateMonitoring;
    }

    /**
     * @param aggregateMonitoring the aggregateMonitoring to set
     */
    public void setAggregateMonitoring(ArrayList<Double> aggregateMonitoring) {
        this.aggregateMonitoring = aggregateMonitoring;
    }

    /**
     * @return the adaptation
     */
    public ArrayList<Double> getAdaptation() {
        return adaptation;
    }

    /**
     * @param adaptation the adaptation to set
     */
    public void setAdaptation(ArrayList<Double> adaptation) {
        this.adaptation = adaptation;
    }
}
