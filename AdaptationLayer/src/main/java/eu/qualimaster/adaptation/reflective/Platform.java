package eu.qualimaster.adaptation.reflective;

import java.util.ArrayList;

/**
 * Represents a platform logged in the monitoring log
 * 
 * @author Andrea Ceroni
 */
public class Platform {

    /** The name of the platform */
    private String name;

    /** The observed measures */
    private ArrayList<Double> measures;
    
    /** The names of the measures */
    private ArrayList<String> measuresNames;

    /** The pipelines in the platform */
    private ArrayList<Pipeline> pipelines;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the measures
     */
    public ArrayList<Double> getMeasures() {
        return measures;
    }

    /**
     * @param measures the measures to set
     */
    public void setMeasures(ArrayList<Double> measures) {
        this.measures = measures;
    }

    /**
     * @return the pipelines
     */
    public ArrayList<Pipeline> getPipelines() {
        return pipelines;
    }

    /**
     * @param pipelines the pipelines to set
     */
    public void setPipelines(ArrayList<Pipeline> pipelines) {
        this.pipelines = pipelines;
    }

    /**
     * @return the measuresNames
     */
    public ArrayList<String> getMeasuresNames() {
        return measuresNames;
    }

    /**
     * @param measuresNames the measuresNames to set
     */
    public void setMeasuresNames(ArrayList<String> measuresNames) {
        this.measuresNames = measuresNames;
    }
}
