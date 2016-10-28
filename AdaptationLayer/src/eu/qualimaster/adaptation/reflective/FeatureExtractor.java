package eu.qualimaster.adaptation.reflective;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts features for reflective adaptation from monitoring and adaptation logs.
 * To cope with the presence of different pipelines within the monitoring log, the
 * name of the pipeline of interested as well as its nodes have to be specified.
 * 
 * @author  Andrea Ceroni
 */
public class FeatureExtractor {

    /** The name of the pipeline of interest */
    private String pipeline;
    
    /** The names of the nodes of the pipeline of interest */
    private ArrayList<String> nodes;
    
    public FeatureExtractor(String pipeline, ArrayList<String> nodes){
        this.pipeline = pipeline;
        this.nodes = new ArrayList<>(nodes);
    }
    
    /**
     * Extracts features from a list of monitoring units (one pattern for each unit).
     * @param units the monitoring units from which the features are extracted.
     * @param windowSize the number of units to consider when computing aggregated features.
     * @return the list of patterns containing the extracted features (no labels).
     */
    public ArrayList<Pattern> extractFeatures(ArrayList<MonitoringUnit> units, int windowSize){
        ArrayList<Pattern> patterns = new ArrayList<>();
        
        // start from the first unit having enough previous units to compute aggregate features
        for(int i = windowSize; i < units.size(); i++){
            patterns.add(extractFeatures(units.subList(i - windowSize, i)));
        }
        
        return patterns;
    }
    
    /**
     * Extracts features from a list of monitoring units. Only one pattern is created from
     * the list (values of the last unit plus aggregated values from the others).
     * @param units the monitoring units from which the features are extracted.
     * @return a pattern containing the extracted features (no label).
     */
    public Pattern extractFeatures(List<MonitoringUnit> units){
        Pattern pattern = new Pattern();
        
        // extract features from the recent (last) unit
        ArrayList<Double> recentFeatures = extractFeatures(units.get(units.size() - 1));
        pattern.getFeatures().getMonitoringFeatures().addAll(recentFeatures);
        
        // extract aggregated features from the set of previous units
        //TODO
        
        return pattern;
    }
    
    /**
     * Extracts features from a single monitoring unit (checking if the desired pipeline and
     * nodes are present).
     * @param unit the monitoring unit from which the features are extracted.
     * @return the list of features of the monitoring unit.
     */
    private ArrayList<Double> extractFeatures(MonitoringUnit unit){
        ArrayList<Double> unitFeatures = new ArrayList<>();
        
        // extract features from the platform
        unitFeatures.addAll(extractFeatures(unit.getPlatform()));
        
        // extract features from the pipeline of interest
        ArrayList<Double> pipelineFeatures = null;
        ArrayList<Double> nodesFeatures = null;
        for(Pipeline pipeline : unit.getPlatform().getPipelines()){
            if(pipeline.getName().compareTo(this.pipeline) == 0){
                pipelineFeatures = new ArrayList<>(pipeline.getMeasures());
                nodesFeatures = extractFeatures(pipeline.getNodes());
                break;
            }
        }
        
        // check if the desired pipeline was present in the monitoring log
        if(pipelineFeatures == null){
            System.out.println("ERROR: pipeline " + this.pipeline + " is not present in the monitoring log");
            return null;
        }
        else unitFeatures.addAll(pipelineFeatures);
        
        // check if the desired pipeline was made of the desired nodes
        if(nodesFeatures == null){
            System.out.println("ERROR: pipeline " + this.pipeline + " is not made of the desired nodes");
            return null;
        }
        else unitFeatures.addAll(nodesFeatures);
        
        return unitFeatures;
    }
    
    /**
     * Extracts features from a list of nodes.
     * @param nodes the nodes from which the features are extracted.
     * @return the list of features of the nodes.
     */
    private ArrayList<Double> extractFeatures(ArrayList<Node> nodes){
        // TODO
        return null;
    }
    
    /**
     * Extracts features from a platform.
     * @param platform the platform from which the features are extracted.
     * @return the list of features of the platform.
     */
    private ArrayList<Double> extractFeatures(Platform platform){
        return platform.getMeasures();
    }

    /**
     * @return the pipeline
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * @return the nodes
     */
    public ArrayList<String> getNodes() {
        return nodes;
    }
}
