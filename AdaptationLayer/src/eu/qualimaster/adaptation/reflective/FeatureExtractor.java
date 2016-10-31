package eu.qualimaster.adaptation.reflective;

import java.util.ArrayList;
import java.util.List;

import eu.qualimaster.adaptation.internal.AdaptationUnit;

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
    
    /** Counter to parse the monitoring log only once when extracting features */
    private int index;
    
    /**
     * Constructor with names of input pipeline and nodes.
     * @param pipeline the name of the pipeline of interest.
     * @param nodes the names of the nodes within the pipeline.
     */
    public FeatureExtractor(String pipeline, ArrayList<String> nodes){
        this.pipeline = pipeline;
        this.nodes = new ArrayList<>(nodes);
        this.index = 0;
    }
    
    /**
     * Extracts features from monitoring units (one pattern for each unit) logged between
     * subsequent adaptation decisions (stored as adaptation units).
     * @param mUnits the list of monitoring units.
     * @param aUnits the list of adaptation units.
     * @param unitsToSkip the number of units to ignore after an adaptation has been finished.
     * @param window the number of units to consider when computing aggregated features for a unit.
     * @return the list of patterns containing the extracted features (without labels)
     */
    public ArrayList<Pattern> extractFeatures(ArrayList<MonitoringUnit> mUnits, ArrayList<AdaptationUnit> aUnits, int unitsToIgnore, int window){
        ArrayList<Pattern> patterns = new ArrayList<>();
        
        // first block of monitoring units before the first adaptation decision
        this.index = 0;
        ArrayList<MonitoringUnit> currUnits = getMonitoringUnits(mUnits, -1, aUnits.get(0).getStartTime(), unitsToIgnore);
        patterns.addAll(extractFeatures(currUnits, window));
        
        // all the other blocks of monitoring units between two consecutive adaptations
        for(int i = 1; i < aUnits.size(); i++){
            currUnits = getMonitoringUnits(mUnits, aUnits.get(i-1).getEndTime(), aUnits.get(i).getStartTime(), unitsToIgnore);
            patterns.addAll(extractFeatures(currUnits, window));
        }
        
        // reset the internal counter for future feature extractions
        this.index = 0;
        
        return patterns;
    }
    
    /**
     * Extracts features from a list of monitoring units (one pattern for each unit).
     * @param units the monitoring units from which the features are extracted.
     * @param windowSize the number of units to consider when computing aggregated features.
     * @return the list of patterns containing the extracted features (without labels).
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
        ArrayList<Double> lastFeatures = extractFeatures(units.get(units.size() - 1));
        pattern.getFeatures().getLastMonitoring().addAll(lastFeatures);
        
        // extract aggregated features from the set of previous units
        ArrayList<Double> aggregateFeatures = extractAggregateFeatures(units);
        pattern.getFeatures().getAggregateMonitoring().addAll(aggregateFeatures);
        
        return pattern;
    }
    
    private ArrayList<MonitoringUnit> getMonitoringUnits(ArrayList<MonitoringUnit> units, long lowerBound, long upperBound, int skip){
        ArrayList<MonitoringUnit> selectedUnits = new ArrayList<>();
        
        // skip the first set of monitoring units based on the skip parameter, proceed until
        // the maximum time is reached.
        if(lowerBound != -1){
            while(units.get(this.index).getTimestamp() <= lowerBound){
                this.index++;
            }
            this.index += skip;
        }
        else{
            // first iteration where there is not any lower bound
            this.index = skip;
        }
        
        // get all the monitoring units within the "valid" period
        MonitoringUnit currUnit = units.get(this.index);
        while(currUnit.getTimestamp() < upperBound){
            selectedUnits.add(currUnit);
            currUnit = units.get(++this.index);
        }
        
        return selectedUnits;
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
        Pipeline pipelineOfInterest = getPipelineOfInterest(unit.getPlatform().getPipelines());

        // check if the desired pipeline was present in the monitoring log
        if(pipelineOfInterest == null){
            System.out.println("ERROR: pipeline " + this.pipeline + " is not present in the monitoring log");
            return null;
        }
        else{
            unitFeatures.addAll(extractFeatures(pipelineOfInterest));
            
            // check if the desired pipeline was made of the desired nodes
            ArrayList<Double> nodesFeatures = extractFeatures(pipelineOfInterest.getNodes());
            if(nodesFeatures == null){
                System.out.println("ERROR: pipeline " + this.pipeline + " is not made of the desired nodes");
                return null;
            }
            else unitFeatures.addAll(nodesFeatures);
        }

        return unitFeatures;
    }
    
    /**
     * Extracts features from a list of nodes.
     * @param nodes the nodes from which the features are extracted.
     * @return the list of features of the nodes.
     */
    private ArrayList<Double> extractFeatures(ArrayList<Node> nodes){
        ArrayList<Double> nodesFeatures = new ArrayList<>();
        
        // for the moment they are ignored
        return nodesFeatures;
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
     * Extracts features from a pipeline.
     * @param pipeline the pipeline from which the features are extracted.
     * @return the list of features of the pipeline.
     */
    private ArrayList<Double> extractFeatures(Pipeline pipeline){
        return pipeline.getMeasures();
    }
    
    private ArrayList<Double> extractAggregateFeatures(List<MonitoringUnit> units){
        ArrayList<Double> aggregateFeatures = new ArrayList<>();
        
        // aggregate platform features
        for(int i = 0; i < units.get(0).getPlatform().getMeasures().size(); i++){
            ArrayList<Double> featureMeasurements = new ArrayList<>();
            for(MonitoringUnit unit : units){
                featureMeasurements.add(unit.getPlatform().getMeasures().get(i));
            }
            aggregateFeatures.addAll(extractAggregateFeatures(featureMeasurements));
        }
        
        // aggregate pipeline features (from the pipeline of interest)
        Pipeline pipelineOfInterest = getPipelineOfInterest(units.get(0).getPlatform().getPipelines());
        if(pipelineOfInterest == null){
            System.out.println("ERROR: pipeline " + this.pipeline + " is not present in the monitoring log");
            return null;
        }
        else{
            for(int i = 0; i < pipelineOfInterest.getMeasures().size(); i++){
                ArrayList<Double> featureMeasurements = new ArrayList<>();
                for(MonitoringUnit unit : units){
                    // get the pipeline of interest
                    pipelineOfInterest = getPipelineOfInterest(unit.getPlatform().getPipelines());
                    if(pipelineOfInterest == null){
                        System.out.println("ERROR: pipeline " + this.pipeline + " is not present in the monitoring log");
                        return null;
                    }
                    featureMeasurements.add(pipelineOfInterest.getMeasures().get(i));
                }
                aggregateFeatures.addAll(extractAggregateFeatures(featureMeasurements));
            }
        }
        
        // aggregate nodes features
        // TODO not supported at the moment
        
        return aggregateFeatures;
    }
    
    private ArrayList<Double> extractAggregateFeatures(ArrayList<Double> measures){
        ArrayList<Double> features = new ArrayList<>();
        
        //TODO clarify where to do the normalization.
        
        // variation between beginning and end of the period
        features.add(computeLinearVariation(measures));
        
        // variation of the variation from step to step (to model non-linear increases)
        features.add(computeNonLinearVariation(measures));
        
        return features;
    }
    
    private Pipeline getPipelineOfInterest(ArrayList<Pipeline> pipelines){
        for(Pipeline pipeline : pipelines){
            if(pipeline.getName().compareTo(this.pipeline) == 0){
                return pipeline;
            }
        }
        return null;
    }
    
    private double computeLinearVariation(ArrayList<Double> measures){
        return measures.get(measures.size() - 1) - measures.get(0);
    }
    
    private double computeNonLinearVariation(ArrayList<Double> measures){
        ArrayList<Double> stepToStepVariations = computeStepToStepVariations(measures);
        return computeLinearVariation(stepToStepVariations);
    }
    
    private ArrayList<Double> computeStepToStepVariations(ArrayList<Double> measures){
        ArrayList<Double> variations = new ArrayList<>();
        for(int i = 1; i < measures.size(); i++){
            variations.add(measures.get(i) - measures.get(i - 1));
        }
        return variations;
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
