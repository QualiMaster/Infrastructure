package eu.qualimaster.adaptation.reflective;

import java.util.ArrayList;
import java.util.Arrays;

public class MainFeatureExtraction {
    
    private static final String MONITORING_LOG = "./testdata/reflective/monitoring_traces/10_custom_0.00007_2ms.rtrace";
    private static final int WINDOW_SIZE = 5;
    private static final String OUTPUT_FILE = "./testdata/reflective/features/features_10_custom_000007_2ms.csv";
    private static final String PIPELINE = "RandomPip";
    private static final ArrayList<String> NODES = new ArrayList<String>(
            Arrays.asList("src", "RandomProcessor1Intermediary", "RandomProcessor1EndBolt", "processor", "snk"));
    private static final boolean NORMALIZE = true;

    static public void main(String[] args) {
        ReadUtils ru = new ReadUtils();
        ArrayList<MonitoringUnit> monitoringUnits = ru.readMonitoringUnits(MONITORING_LOG);
        
        FeatureExtractor fe = new FeatureExtractor(PIPELINE, NODES, ru.getFilteredHeader(), NORMALIZE);
        ArrayList<Pattern> patterns = fe.extractFeatures(monitoringUnits, WINDOW_SIZE);
        fe.storeFeatures(patterns, OUTPUT_FILE);
    }

}
