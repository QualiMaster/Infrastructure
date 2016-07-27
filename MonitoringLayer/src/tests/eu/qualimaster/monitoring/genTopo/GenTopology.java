package tests.eu.qualimaster.monitoring.genTopo;

import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import backtype.storm.Config;

/**
 * Creates a (generated) HY testing topology.
 * 
 * @author Holger Eichelberger
 */
public class GenTopology extends AbstractHyTopology {
    
    public static final String PIP = "testGenPip";
    private static final boolean SEND_EVENTS = true;
    
    /**
     * Creates the testing topology.
     * 
     * @param config the pipeline configuration
     * @param builder the topology builder
     */
    @Override
    public void createTopology(Config config, RecordingTopologyBuilder builder) {
        builder.setSpout(getTestSourceName(), 
            new TestSourceSource(getTestSourceName(), PIP, SEND_EVENTS), 1)
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
        builder.close(PIP, config);
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
