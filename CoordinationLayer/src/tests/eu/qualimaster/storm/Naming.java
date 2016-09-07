package tests.eu.qualimaster.storm;

import java.io.File;
import java.util.Map;

/**
 * Defines the part names for testing.
 * 
 * @author Holger Eichelberger
 */
public class Naming {
    
    public static final String PROPERTY_DEFAULT_INIT_ALGORITHMS = "qm.coordination.test.defaultAlgorithm";
    
    public static final String PIPELINE_NAME = "pipeline";
    public static final String SUB_PIPELINE_NAME = "subPipeline";
    public static final String CONTAINER_CLASS = Topology.class.getName();
    public static final String NODE_SOURCE = "source";
    public static final String NODE_SOURCE_CLASS = Source.class.getName();
    public static final String NODE_SOURCE_COMPONENT = "source";
    public static final String NODE_PROCESS = "process";
    public static final String NODE_PROCESS_CLASS = Process.class.getName();
    public static final String NODE_PROCESS_COMPONENT = "process";
    public static final String NODE_PROCESS_FAMILY = "fam1";
    public static final String NODE_PROCESS_ALG1 = "alg1";
    public static final String NODE_PROCESS_ALG1_CLASS = Alg1.class.getName();
    public static final String NODE_PROCESS_ALG2 = "alg2";
    public static final String NODE_PROCESS_ALG2_CLASS = Alg2.class.getName();
    public static final String NODE_SINK = "sink";
    public static final String NODE_SINK_CLASS = Sink.class.getName();
    public static final String NODE_SINK_COMPONENT = "sink";

    public static final transient File LOG_PROCESS = createLogFile("process"); 
    public static final transient File LOG_SOURCE = createLogFile("source");
    public static final transient File LOG_SINK = createLogFile("sink");

    /**
     * Creates a log file object in the temp directory.
     * 
     * @param id the identifier
     * @return the log file object
     */
    private static final File createLogFile(String id) {
        File f = new File(System.getProperty("java.io.tmpdir", "/"), "qm_coord_storm_test_" + id + ".log");
        f.setReadable(true, false);
        f.setWritable(true, false);
        return f;
    }
    
    /**
     * Clears the actual log files.
     */
    public static void clearLogs() {
        LOG_PROCESS.delete();
        LOG_SOURCE.delete();
        LOG_SINK.delete();
    }
    
    /**
     * Returns whether processing family algorithms shall be initialized by default. This is required unless the
     * adaptation layer is not present.
     * 
     * @param config the Storm configuration
     * @return <code>true</code> if default initialization of algorithms shall be performed, <code>false</code> else
     */
    public static boolean defaultInitializeAlgorithms(Map<?, ?> config) {
        // for potentially distributed
        Object tmp = config.get(Naming.PROPERTY_DEFAULT_INIT_ALGORITHMS);
        Boolean value;
        if (null == tmp) {
            value = Boolean.TRUE;
        } else if (tmp instanceof Boolean) {
            value = (Boolean) tmp;
        } else {
            value = Boolean.valueOf(tmp.toString());
        }
        return value;
    }

    /**
     * Defines whether family algorithms shall be initialized by default. This is required unless the adaptation layer
     * is not present, thus, the default value is <code>true</code>.
     * 
     * @param config the Storm configuration
     * @param defaultInit whether default initialization of algorithms shall be performed, <code>false</code> else
     * @return <code>config</code>
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Map setDefaultInitializeAlgorithms(Map config, boolean defaultInit) {
        config.put(Naming.PROPERTY_DEFAULT_INIT_ALGORITHMS, defaultInit);
        return config;
    }
    
}
