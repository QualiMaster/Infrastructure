package eu.qualimaster.adaptation.reflective;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents the setup covered by a specific reflective adaptation model, i.e. pipeline(s), 
 * nodes, observables the model has been trained on.
 * 
 * @author  Andrea Ceroni
 *
 */
public class Setup {

    // The pipeline(s) used for reflective adaptation
    private ArrayList<String> pipelines;
    
    // The nodes of each pipeline used for reflective adaptation
    private HashMap<String,ArrayList<String>> nodes;
    
    // The observables used for reflective adaptation (at each level)
    private HashMap<String,ArrayList<String>> observables;
    
    // The number of monitoring stps considered when extracting trend features
    private int historySize;
    
    // The path to the model
    private String modelPath;
    
    public Setup(){
        this.pipelines = new ArrayList<>();
        this.nodes = new HashMap<>();
        this.observables = new HashMap<>();
        this.historySize = -1;
        this.modelPath = null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + historySize;
        result = prime * result
                + ((modelPath == null) ? 0 : modelPath.hashCode());
        result = prime * result + ((nodes == null) ? 0 : nodes.hashCode());
        result = prime * result
                + ((observables == null) ? 0 : observables.hashCode());
        result = prime * result
                + ((pipelines == null) ? 0 : pipelines.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Setup other = (Setup) obj;
        if (historySize != other.historySize)
            return false;
        if (modelPath == null) {
            if (other.modelPath != null)
                return false;
        } else if (!modelPath.equals(other.modelPath))
            return false;
        if (nodes == null) {
            if (other.nodes != null)
                return false;
        } else if (!nodes.equals(other.nodes))
            return false;
        if (observables == null) {
            if (other.observables != null)
                return false;
        } else if (!observables.equals(other.observables))
            return false;
        if (pipelines == null) {
            if (other.pipelines != null)
                return false;
        } else if (!pipelines.equals(other.pipelines))
            return false;
        return true;
    }

    /**
     * @return the pipelines
     */
    public ArrayList<String> getPipelines() {
        return pipelines;
    }

    /**
     * @param pipelines the pipelines to set
     */
    public void setPipelines(ArrayList<String> pipelines) {
        this.pipelines = pipelines;
    }

    /**
     * @return the nodes
     */
    public HashMap<String,ArrayList<String>> getNodes() {
        return nodes;
    }

    /**
     * @param nodes the nodes to set
     */
    public void setNodes(HashMap<String,ArrayList<String>> nodes) {
        this.nodes = nodes;
    }

    /**
     * @return the observables
     */
    public HashMap<String,ArrayList<String>> getObservables() {
        return observables;
    }

    /**
     * @param observables the observables to set
     */
    public void setObservables(HashMap<String,ArrayList<String>> observables) {
        this.observables = observables;
    }

    /**
     * @return the historySize
     */
    public int getHistorySize() {
        return historySize;
    }

    /**
     * @param historySize the historySize to set
     */
    public void setHistorySize(int historySize) {
        this.historySize = historySize;
    }

    /**
     * @return the modelPath
     */
    public String getModelPath() {
        return modelPath;
    }

    /**
     * @param modelPath the modelPath to set
     */
    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }
}
