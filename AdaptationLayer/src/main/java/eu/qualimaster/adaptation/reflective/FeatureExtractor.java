package eu.qualimaster.adaptation.reflective;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
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
    
    /** The headers of the features (one list for each component) */
    private HashMap<String, ArrayList<String>> headers;
    
    /** Flag indicating whether differences at different time points should be
     * normalized or not. */
    private boolean normalize;
    
    /** Counter to parse the monitoring log only once when extracting features */
    private int index;
    
    /**
     * Constructor with names of input pipeline and nodes.
     * @param pipeline the name of the pipeline of interest.
     * @param nodes the names of the nodes within the pipeline.
     * @param headers the names of the measurements, one set for each component.
     * @param normalize flag indicating whether differences of values should be normalized or not.
     */
    public FeatureExtractor(String pipeline, ArrayList<String> nodes, HashMap<String, ArrayList<String>> headers, boolean normalize){
        this.pipeline = pipeline;
        this.nodes = new ArrayList<>(nodes);
        this.headers = new HashMap<>(headers);
        this.normalize = normalize;
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
        for(int i = windowSize; i <= units.size(); i++){
            patterns.add(extractFeatures(units.subList(i - windowSize, i)));
        }
        
        // post process features
        patterns = postProcess(patterns);
        
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
        pattern.setId(String.valueOf(units.get(units.size() - 1).getTimestamp()));
        
        // extract features from the recent (last) unit
        ArrayList<Double> lastFeatures = extractFeatures(units.get(units.size() - 1));
        pattern.getFeatures().getLastMonitoring().addAll(lastFeatures);
        
        // extract aggregated features from the set of previous units
        ArrayList<Double> aggregateFeatures = extractAggregateFeatures(units, this.normalize);
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
     * measures that grow over time (e.g. throughput_volume and throughput_items).
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
            ArrayList<Node> nodesOfInterest = getNodesOfInterest(pipelineOfInterest.getNodes());
            if(nodesOfInterest == null || nodesOfInterest.isEmpty()){
                System.out.println("ERROR: pipeline " + this.pipeline + " is not made of the desired nodes");
                return null;
            }
            else{
                // extract features from the nodes of interest
                unitFeatures.addAll(extractFeatures(nodesOfInterest));
                unitFeatures.addAll(extractDifferenceFeatures(nodesOfInterest));
            }
        }

        return unitFeatures;
    }
    
    /**
     * Extracts differences of features among any possible couple of nodes.
     * @param nodes the nodes from which the features are extracted.
     * @return the list of features of the nodes.
     */
    private ArrayList<Double> extractDifferenceFeatures(ArrayList<Node> nodes){
        ArrayList<Double> nodesFeatures = new ArrayList<>();
        for(int i = 0; i < nodes.size() - 1; i++){
            for(int j = i + 1; j < nodes.size(); j++){
                nodesFeatures.addAll(extractDifferenceFeatures(nodes.get(i), nodes.get(j), this.headers.get("pipeline node")));
            }
        }
        return nodesFeatures;
    }
    
    private ArrayList<Double> extractDifferenceFeatures(Node node1, Node node2, ArrayList<String> header){
        ArrayList<Double> nodesFeatures = new ArrayList<>();
        
        // difference of THROUGHPUT_VOLUME
        int volumeIndex = getMeasureIndex("pipeline node", "THROUGHPUT_VOLUME");
        double volumeNode1 = node1.getMeasures().get(volumeIndex);
        double difference = volumeNode1 - node2.getMeasures().get(volumeIndex);
        if(volumeNode1 != 0)
            nodesFeatures.add(difference / Math.abs(volumeNode1));
        else if(difference == 0.0)
            nodesFeatures.add(difference);
        else
            nodesFeatures.add(1.0);
        
        // difference of THROUGHPUT_ITEMS
        int itemsIndex = getMeasureIndex("pipeline node", "THROUGHPUT_ITEMS");
        double itemsNode1 = node1.getMeasures().get(itemsIndex);
        difference = itemsNode1 - node2.getMeasures().get(itemsIndex);
        if(itemsNode1 != 0)
            nodesFeatures.add(difference / Math.abs(itemsNode1));
        else if(difference == 0.0)
            nodesFeatures.add(difference);
        else
            nodesFeatures.add(1.0);
        
        return nodesFeatures;
    }
    
    /**
     * Extracts features from a list of nodes.
     * @param nodes the nodes from which the features are extracted.
     * @return the list of features of the nodes.
     */
    private ArrayList<Double> extractFeatures(ArrayList<Node> nodes){
        ArrayList<Double> nodesFeatures = new ArrayList<>();
        for(Node node : nodes) nodesFeatures.addAll(extractFeatures(node));
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
    
    /**
     * Extracts features from a node.
     * @param node the node from which the features are extracted.
     * @return the list of features of the node.
     */
    private ArrayList<Double> extractFeatures(Node node){
        return node.getMeasures();
    }
    
    private ArrayList<Double> extractAggregateFeatures(List<MonitoringUnit> units, boolean normalize){
        ArrayList<Double> aggregateFeatures = new ArrayList<>();
        
        // aggregate platform features
        for(int i = 0; i < units.get(0).getPlatform().getMeasures().size(); i++){
            ArrayList<Double> featureMeasurements = new ArrayList<>();
            for(MonitoringUnit unit : units){
                featureMeasurements.add(unit.getPlatform().getMeasures().get(i));
            }
            aggregateFeatures.addAll(extractAggregateFeatures(featureMeasurements, normalize));
        }
        
        // get the pipeline of interest in each unit
        HashMap<Long, Pipeline> pipelinesOfInterest = new HashMap<>();
        for(MonitoringUnit unit : units){
            Pipeline pipelineOfInterest = getPipelineOfInterest(unit.getPlatform().getPipelines());
            if(pipelineOfInterest == null){
                System.out.println("ERROR: pipeline " + this.pipeline + " is not present in the monitoring log");
                return null;
            }
            else pipelinesOfInterest.put(unit.getTimestamp(), pipelineOfInterest);
        }
        
        // aggregate pipeline features (from the pipeline of interest in each unit)
        for(int i = 0; i < pipelinesOfInterest.values().iterator().next().getMeasures().size(); i++){
            ArrayList<Double> featureMeasurements = new ArrayList<>();
            for(MonitoringUnit unit : units){
                Pipeline pipelineOfInterest = pipelinesOfInterest.get(unit.getTimestamp());
                featureMeasurements.add(pipelineOfInterest.getMeasures().get(i));
            }
            aggregateFeatures.addAll(extractAggregateFeatures(featureMeasurements, normalize));
        }
        
        // get the nodes of interest in each pipeline of interest in each unit
        HashMap<Long, ArrayList<Node>> nodesOfInterest = new HashMap<>();
        for(MonitoringUnit unit : units){
            Pipeline pipelineOfInterest = pipelinesOfInterest.get(unit.getTimestamp());
            ArrayList<Node> nodesOfInterestInUnit = getNodesOfInterest(pipelineOfInterest.getNodes());
            if(nodesOfInterestInUnit == null){
                System.out.println("ERROR: pipeline " + this.pipeline + " is not made of the desired nodes");
                return null;
            }
            else nodesOfInterest.put(unit.getTimestamp(), nodesOfInterestInUnit);
        }
        
        // aggregate nodes features
        for(String nodeOfInterestName : this.nodes){
            for(int i = 0; i < pipelinesOfInterest.values().iterator().next().getNodes().get(0).getMeasures().size(); i++){
                ArrayList<Double> featureMeasurements = new ArrayList<>();
                for(MonitoringUnit unit : units){
                    ArrayList<Node> nodesOfInterestInUnit = nodesOfInterest.get(unit.getTimestamp());
                    Node nodeOfInterestInUnit = getNodeByName(nodesOfInterestInUnit, nodeOfInterestName);
                    featureMeasurements.add(nodeOfInterestInUnit.getMeasures().get(i));
                }
                aggregateFeatures.addAll(extractAggregateFeatures(featureMeasurements, normalize));
            }
        }
        
        return aggregateFeatures;
    }
    
    private Node getNodeByName(ArrayList<Node> nodes, String name){
        for(Node node : nodes){
            if(node.getName().compareTo(name) == 0)
                return node;
        }
        return null;
    }
    
    private ArrayList<Double> extractAggregateFeatures(ArrayList<Double> measures, boolean normalize){
        ArrayList<Double> features = new ArrayList<>();
        
        // variation between beginning and end of the period
        features.add(computeLinearVariation(measures, normalize));
        
        // variation of the variation from step to step (to model non-linear increases)
        features.add(computeNonLinearVariation(measures, normalize));
        
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
    
    private ArrayList<Node> getNodesOfInterest(ArrayList<Node> nodes){
        ArrayList<Node> nodesOfInterest = new ArrayList<>();
        for(String nodeOfInterest : this.nodes){
            for(Node node : nodes){
                if(node.getName().compareTo(nodeOfInterest) == 0){
                    nodesOfInterest.add(node);
                    break;
                }
            }
        }
        if(nodesOfInterest.size() != this.nodes.size()) return null;
        else return nodesOfInterest;
    }
    
    private double computeLinearVariation(ArrayList<Double> measures, boolean normalize){
        double variation = measures.get(measures.size() - 1) - measures.get(0);
        if(normalize){
            if(measures.get(0) != 0) return variation / Math.abs(measures.get(0));
            else if (variation == 0.0) return variation;
            else return 1.0;
        }
        else return variation;
    }
    
    private double computeNonLinearVariation(ArrayList<Double> measures, boolean normalize){
        ArrayList<Double> stepToStepVariations = computeStepToStepVariations(measures, normalize);
        return computeLinearVariation(stepToStepVariations, !normalize);
    }
    
    private ArrayList<Double> computeStepToStepVariations(ArrayList<Double> measures, boolean normalize){
        ArrayList<Double> variations = new ArrayList<>();
        for(int i = 1; i < measures.size(); i++){
            double variation = measures.get(i) - measures.get(i - 1);
            if(normalize){
                if(measures.get(i - 1) != 0) variations.add(variation / Math.abs(measures.get(i - 1)));
                else if (variation == 0.0) variations.add(variation);
                else variations.add(1.0);
            }
            else variations.add(variation);
        }
        return variations;
    }
    
    /**
     * Saves a list of features as csv file.
     * @param patterns The list of patterns (only features, no labels) to be stored.
     * @param filePath The path to the output file.
     */
    public void storeFeatures(ArrayList<Pattern> patterns, String filePath){
        BufferedWriter writer = null;
        
        try{
            writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(makeHeader(patterns.get(0)));
            writer.newLine();
            
            for(Pattern p : patterns){
                writer.write(patternToString(p));
                writer.newLine();
            }
            writer.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private String makeHeader(Pattern example){
        
        // TODO add more explanatory names of features by hand
        
        String header = "";
        Features f = example.getFeatures();
        
        header += "ID" + ",";
        for(int i = 0; i < f.getLastMonitoring().size(); i++){
            header += "LAST_MONITORING_" + i + ",";
        }
        for(int i = 0; i < f.getAggregateMonitoring().size(); i++){
            header += "AGGREGATE_MONITORING_" + i + ",";
        }
        header = header.substring(0, header.length() - 1);
        
        return header;
    }
    
    private String patternToString(Pattern p){
        String line = "";
        Features f = p.getFeatures();
        
        line += p.getId() + ",";
        for(Double feature : f.getLastMonitoring()){
            line += feature + ",";
        }
        for(Double feature : f.getAggregateMonitoring()){
            line += feature + ",";
        }
        line = line.substring(0, line.length() - 1);
        
        return line;
    }
    
    public ArrayList<Double> patternToArray(Pattern p){
        ArrayList<Double> values = new ArrayList<>();
        Features f = p.getFeatures();
        
        values.add(Double.valueOf(p.getId()));
        for(Double feature : f.getLastMonitoring()){
            values.add(feature);
        }
        for(Double feature : f.getAggregateMonitoring()){
            values.add(feature);
        }
        values.add(p.getLabel());
        
        return values;
    }
    
    private int getMeasureIndex(String component, String measure){
        ArrayList<String> header = this.headers.get(component);
        for(int i = 0; i < header.size(); i++){
            if(header.get(i).compareTo(measure) == 0){
                return i;
            }
        }
        return -1;
    }
    
    public ArrayList<Pattern> postProcess(ArrayList<Pattern> patterns){
        
        // replace the absolute THROUGHPUT_ITEMS and THROUGHPUT_VOLUME with the difference wrt the previous values
        int platformMeasures = this.headers.get("platform").size();
        int pipelineMeasures = this.headers.get("pipeline").size();
        int nodeMeasures = this.headers.get("pipeline node").size();
        int throughputItemsIndex = platformMeasures + getMeasureIndex("pipeline", "THROUGHPUT_ITEMS");
        int throughputVolumeIndex = platformMeasures + getMeasureIndex("pipeline", "THROUGHPUT_VOLUME");
        int throughputItemsIndexNodeOffset = getMeasureIndex("pipeline node", "THROUGHPUT_ITEMS");
        int throughputVolumeIndexNodeOffset = getMeasureIndex("pipeline node", "THROUGHPUT_VOLUME");
        
        double oldThroughputVolume = patterns.get(0).getFeatures().getLastMonitoring().get(throughputVolumeIndex);
        double oldThroughputItems = patterns.get(0).getFeatures().getLastMonitoring().get(throughputItemsIndex);
        for(int i = 1; i < patterns.size(); i++){
            double currThroughputVolume = patterns.get(i).getFeatures().getLastMonitoring().get(throughputVolumeIndex);
            patterns.get(i).getFeatures().getLastMonitoring().set(throughputVolumeIndex,
                    currThroughputVolume - oldThroughputVolume);
            oldThroughputVolume = currThroughputVolume;
            
            double currThroughputItems = patterns.get(i).getFeatures().getLastMonitoring().get(throughputItemsIndex);
            patterns.get(i).getFeatures().getLastMonitoring().set(throughputItemsIndex,
                    currThroughputItems - oldThroughputItems);
            oldThroughputItems = currThroughputItems;
        }
        
        for(int n = 0; n < this.nodes.size(); n++){
            int throughputVolumeIndexNode = platformMeasures + pipelineMeasures + nodeMeasures*n + throughputVolumeIndexNodeOffset;
            int throughputItemsIndexNode = platformMeasures + pipelineMeasures + nodeMeasures*n + throughputItemsIndexNodeOffset;
            
            oldThroughputVolume = patterns.get(0).getFeatures().getLastMonitoring().get(throughputVolumeIndexNode);
            oldThroughputItems = patterns.get(0).getFeatures().getLastMonitoring().get(throughputItemsIndexNode);
            for(int i = 1; i < patterns.size(); i++){
                double currThroughputVolume = patterns.get(i).getFeatures().getLastMonitoring().get(throughputVolumeIndexNode);
                patterns.get(i).getFeatures().getLastMonitoring().set(throughputVolumeIndexNode,
                        currThroughputVolume - oldThroughputVolume);
                oldThroughputVolume = currThroughputVolume;
                
                double currThroughputItems = patterns.get(i).getFeatures().getLastMonitoring().get(throughputItemsIndexNode);
                patterns.get(i).getFeatures().getLastMonitoring().set(throughputItemsIndexNode,
                        currThroughputItems - oldThroughputItems);
                oldThroughputItems = currThroughputItems;
            }
        }
        
        return patterns;
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

    /**
     * @return the normalize
     */
    public boolean isNormalize() {
        return normalize;
    }

    /**
     * @param normalize the normalize to set
     */
    public void setNormalize(boolean normalize) {
        this.normalize = normalize;
    }
}
