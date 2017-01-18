package tests.eu.qualimaster.monitoring.genTopo;

import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import backtype.storm.Config;

/**
 * Creates a (generated) HY testing topology.
 * 
 * @author Holger Eichelberger
 */
public class GenTopology extends AbstractHyTopology {
    
    public static final String PIP = "testGenPip";
    private static final boolean SEND_EVENTS = true;
    private int maxNumber;
    
    /**
     * Creating the topology processing an unlimited stream.
     */
    public GenTopology() {
    }
    
    /**
     * Creates a topology processing a maximum number of input files.
     * 
     * @param maxNumber the maximum number
     */
    public GenTopology(int maxNumber) {
        this.maxNumber = maxNumber;
    }
    
    /**
     * Creates the testing topology.
     * 
     * @param config the pipeline configuration
     * @param builder the topology builder
     */
    @Override
    public SubTopologyMonitoringEvent createTopology(Config config, RecordingTopologyBuilder builder) {
        TestSourceSource src = new TestSourceSource(getTestSourceName(), PIP, SEND_EVENTS);
        if (maxNumber > 0) {
            src.maxNumEvents(maxNumber);
        }
        builder.setSpout(getTestSourceName(), 
            src, 1)
            .setNumTasks(1);
        builder.setBolt(getTestFamilyName(), 
             new TestFamilyFamilyElement(getTestFamilyName(), PIP, SEND_EVENTS, getAlgorithmName(), true), 1)
            .setNumTasks(1).shuffleGrouping(getTestSourceName());
        builder.setBolt(getHyMapperName(), 
             new SubTopologyFamilyElement0FamilyElement(getHyMapperName(), PIP, SEND_EVENTS, false), 1)
            .setNumTasks(1).shuffleGrouping(getTestFamilyName());
        builder.setBolt(getHyProcessorName(), 
             new SubTopologyFamilyElement1FamilyElement(getHyProcessorName(), PIP, SEND_EVENTS, false), 1)
            .setNumTasks(3).shuffleGrouping(getHyMapperName());
        return builder.createClosingEvent(PIP, config);
    }
    
    // checkstyle: stop exception type check

    @Override
    protected String getAlgorithmName() {
        return "CorrelationSW";
    }
    
    @Override
    public String getName() {
        return PIP;
    }
    
    /**
     * Creates a standalone topology.
     * 
     * @param args the topology arguments
     * @throws Exception in case of creation problems
     */
    public static void main(String[] args) throws Exception {
        main(args, new GenTopology());
    }

    // checkstyle: resume exception type check

    @Override
    protected boolean isThrift() {
        return false;
    }

    @Override
    public String getMappingFileName() {
        return "testGenPip.xml";
    }
    
}
