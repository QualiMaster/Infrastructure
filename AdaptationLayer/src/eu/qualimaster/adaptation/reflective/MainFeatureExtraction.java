package eu.qualimaster.adaptation.reflective;

import java.util.ArrayList;
import java.util.Arrays;

public class MainFeatureExtraction {
    
    private static final String MONITORING_LOG = "./testdata/reflective/monitoring_log.txt";
    private static final int WINDOW_SIZE = 5;
    private static final String OUTPUT_FILE = "./testdata/reflective/features.csv";
    private static final String PIPELINE = "ReflectiveRandomPip";
    private static final ArrayList<String> NODES = new ArrayList<String>(
            Arrays.asList("src", "processor", "snk"));

    static public void main(String[] args) {
        FeatureExtractor fe = new FeatureExtractor(PIPELINE, NODES);
        ReadUtils ru = new ReadUtils();
        
        ArrayList<MonitoringUnit> monitoringUnits = ru.readMonitoringUnits(MONITORING_LOG);
        ArrayList<Pattern> patterns = fe.extractFeatures(monitoringUnits, WINDOW_SIZE);
        fe.storeFeatures(patterns, OUTPUT_FILE);
    }

}
