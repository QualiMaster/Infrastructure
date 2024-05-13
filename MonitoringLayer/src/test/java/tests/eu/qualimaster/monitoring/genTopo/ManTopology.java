package tests.eu.qualimaster.monitoring.genTopo;

import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import backtype.storm.Config;

/**
 * Creates a (handcrafted) HY testing topology.
 * 
 * @author Holger Eichelberger
 */
public class ManTopology extends AbstractHyTopology {
    
    public static final String PIP = "testManPip";
    private static final boolean SEND_EVENTS = true;
    
    /**
     * Creates the testing topology.
     * 
     * @param config the topology configuration
     * @param builder the topology builder
     */
    @Override
    public SubTopologyMonitoringEvent createTopology(Config config, RecordingTopologyBuilder builder) {
        builder.setSpout(getTestSourceName(), 
            new TestSourceSource(getTestSourceName(), PIP, SEND_EVENTS), 1)
            .setNumTasks(1);
        builder.setBolt(getTestFamilyName(), 
            new TestFamilyFamilyElement(getTestFamilyName(), PIP, SEND_EVENTS, getAlgorithmName(), false), 1)
            .setNumTasks(1).shuffleGrouping(getTestSourceName());
        builder.startRecording(getAlgorithmName());
        TopoSoftwareCorrelationFinancial corr = new TopoSoftwareCorrelationFinancial(PIP);
        corr.createSubTopology(builder, config, "swCorrFin", getTestFamilyName(), AbstractProcessor.STREAM_NAME);
        builder.endRecording();
        return builder.createClosingEvent(PIP, config);
    }
    
    @Override
    protected String getAlgorithmName() {
        return "TopoSoftwareCorrelationFinancial";
    }
    
    @Override
    public String getName() {
        return PIP;
    }
    
    // checkstyle: stop exception type check

    /**
     * Creates a standalone topology.
     * 
     * @param args the topology arguments
     * @throws Exception in case of creation problems
     */
    public static void main(String[] args) throws Exception {
        main(args, new ManTopology());
    }

    // checkstyle: resume exception type check

    @Override
    protected boolean isThrift() {
        return true;
    }

    @Override
    public String getMappingFileName() {
        return "testManPip.xml";
    }

}
