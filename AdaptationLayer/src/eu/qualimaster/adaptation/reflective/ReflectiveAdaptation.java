package eu.qualimaster.adaptation.reflective;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.input.ReversedLinesFileReader;

import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Main class handling reflective adaptation for algorithm switching.
 * It periodically adjust the switching threshold based on the current
 * system state (observables). 
 * 
 * @author  Andrea Ceroni
 */
public class ReflectiveAdaptation {
    
    /** File reader for the monitoring log (reversed to access most recent data). */
    private ReversedLinesFileReader logReader;
    
    /** Number of recent monitoring steps to consider when extracting features. */
    private int historySize;
    
    /** Recent monitoring units previously extracted from the monitoring log. */
    private ArrayList<MonitoringUnit> recentUnits;
    
    /** The setup (platform, pipeline(s), nodes) the model refers to. */
    private Setup setup;
    
    /** Flag indicating whether compatibility between observables of the setup and
     *  those of the headers in the monitoring trace has been checked (it is done
     *  only once, since the headers are not supposed to change). 
     */
    private boolean checkedCompatibility;
    
    /** Utils to read and parse monitoring data. */
    private ReadUtils reader;
    
    /** Utils for extracting features from monitoring data. */
    private FeatureExtractor featureExtractor;
    
    /** Latest extracted patterns, used to normalize throughput in the new pattern */
    private ArrayList<Pattern> latestPatterns;
    
    /** The weka classifier used for regression. */
    private AttributeSelectedClassifier classifier;
    
    public ReflectiveAdaptation(Setup setup){
        this.historySize = setup.getHistorySize();
        this.recentUnits = new ArrayList<>();
        this.setup = setup;
        this.checkedCompatibility = false;
        this.reader = null;
        this.featureExtractor = null;
        this.latestPatterns = new ArrayList<>();
        
        try{
            classifier = (AttributeSelectedClassifier) weka.core.SerializationHelper.read(setup.getModelPath());
        }
        catch(Exception e){
            System.out.println("ERROR: classifier could not be loaded.");
            e.printStackTrace();
        }
    }
    
    public double predict(Map<String, ArrayList<String>> headers, String monitoring){
        // check compatibility between the model's setup and the observables in the monitoring data.
        // do this only at the first adaptation request (the headers are not supposed to change).
        if(!this.checkedCompatibility){
            if(!checkCompatibility(this.setup, headers)){
                System.out.println("ERROR: observables in the setup are incompatible with the ones in the trace.");
                return -1;
            }
            this.checkedCompatibility = true;
            this.reader = new ReadUtils(headers);
            this.featureExtractor = new FeatureExtractor(this.setup.getPipelines().get(0), //currently only 1 pipeline supported
                    this.setup.getNodes().get(this.setup.getPipelines().get(0)), 
                    this.reader.getFilteredHeader(), 
                    true);
        }
        
        // convert the monitoring string into a proper monitoring unit, then add it to the recent units
        MonitoringUnit current = this.reader.readMonitoringUnit(monitoring);
        addMonitoringUnit(current);
        
        // extract features from the monitoring log and make the prediction
        if(this.recentUnits.size() < this.historySize){
            System.out.println("There is not enough recent data to run the Reflective Adaptation, " + 
                    (this.historySize - this.recentUnits.size()) + " more monitoring steps required.");
            return -1;
        }
        this.latestPatterns.add(this.featureExtractor.extractFeatures(this.recentUnits));
        Pattern notPostprocessed = new Pattern(this.latestPatterns.get(this.latestPatterns.size() - 1));
        if(this.latestPatterns.size() == 2){
            this.latestPatterns = this.featureExtractor.postProcess(this.latestPatterns);
            this.latestPatterns.remove(0);
        }
        
        double prediction = predict(this.latestPatterns.get(0));
        this.latestPatterns.clear();
        this.latestPatterns.add(notPostprocessed);
        
        return prediction;
    }
    
    private boolean checkCompatibility(Setup setup, Map<String, ArrayList<String>> headers){
        for(String component : setup.getObservables().keySet()){
            ArrayList<String> componentObservablesInSetup = setup.getObservables().get(component);
            String componentNameInHeader = component.replace("_", " ").replace("observables", "format").trim();
            ArrayList<String> componentObservablesInHeader = headers.get(componentNameInHeader);
            if(componentObservablesInHeader == null) return false;
            if(componentObservablesInSetup.size() != componentObservablesInHeader.size()) return false;
            for(int i = 0; i < componentObservablesInSetup.size(); i++){
                if(componentObservablesInSetup.get(i).compareTo(componentObservablesInHeader.get(i)) != 0)
                    return false;
            }
        }
        return true;
    }
    
    private double predict(Pattern p){
        ArrayList<Double> featureArray = this.featureExtractor.patternToArray(p);
        return predict(featureArray);
    }
    
    private double predict(ArrayList<Double> features)
    {
        Instances instance = featuresToInstance(features);
        return predict(instance);
    }
    
    private double predict(Instances instance)
    {
        try{
            return this.classifier.classifyInstance(instance.instance(0));
        }
        catch (Exception e){
            // TODO Auto-generated catch block
            e.printStackTrace();
            return -1;
        }
    }
    
    private Instances featuresToInstance(ArrayList<Double> features)
    {
        // Create the instance
        FastVector attributes = new FastVector(features.size());
        for(int i = 0; i < features.size(); i++) attributes.addElement(new Attribute(""+i));
        Instances dataUnlabeled = new Instances("instances", attributes, 0);
        
        Instance instance = new DenseInstance(features.size());
        DecimalFormat df = new DecimalFormat("#.######");
        for(int i = 0; i < features.size(); i++) instance.setValue(i, Double.valueOf(df.format(features.get(i))));
        
        dataUnlabeled.add(instance);
        dataUnlabeled.setClassIndex(dataUnlabeled.numAttributes() - 1);        
    
        return dataUnlabeled;
    }
    
    private void addMonitoringUnit(MonitoringUnit unit){
        if(this.recentUnits.size() >= this.historySize){
            this.recentUnits.remove(0);
        }
        this.recentUnits.add(unit);
    }
}
