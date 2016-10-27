package eu.qualimaster.adaptation.reflective;

import java.util.ArrayList;

/**
 * Represents a pipeline logged in the monitoring log.
 * 
 * @author Andrea Ceroni
 */
public class Pipeline {

    /** The name of the pipeline */
    private String name;

    /** The observed measures */
    private ArrayList<Double> measures;

    /** The nodes of the pipeline */
    private ArrayList<Node> nodes;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
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
     * @return the nodes
     */
    public ArrayList<Node> getNodes() {
        return nodes;
    }

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(ArrayList<Node> nodes) {
        this.nodes = nodes;
    }
}
