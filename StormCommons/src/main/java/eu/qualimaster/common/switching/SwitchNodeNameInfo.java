package eu.qualimaster.common.switching;

import java.util.Map;

/**
 * Provide the names of the nodes that are involved in the switching. These are used to support signal sending.
 * @author Cui Qin
 *
 */
public class SwitchNodeNameInfo {
    public static final String TOPOLOGYNAME = "TOPOLOGY";
    public static final String PRECEDINGNODE = "PRE";
    public static final String ORIGINALINTERMEDIARYNODE = "ORGINT";
    public static final String TARGETINTERMEDIARYNODE = "TGTINT";
    public static final String ORIGINALENDNODE = "ORGEND";
    public static final String TARGETENDNODE = "TGTEND";
    
    private static SwitchNodeNameInfo nameInfo;
    
    private static String precedingNodeName;
    private static String originalIntermediaryNodeName;
    private static String targetIntermediaryNodeName;
    private static String originalEndNodeName;
    private static String targetEndNodeName;
    

    private static String topologyName;
    
    /**
     * A private constructor for the class.
     */
    private SwitchNodeNameInfo() {}
    
    /**
     * Returns an instance of the singleton class.
     * @return an instance
     */
    public static SwitchNodeNameInfo getInstance() {
        if (null == nameInfo) {
            nameInfo = new SwitchNodeNameInfo();
        }
        return nameInfo;
    }
    
    /**
     * Capture all the needed information.
     * @param conf the configuration from the topology
     */
    public static void init(Map<String, String> conf) {
        SwitchNodeNameInfo.getInstance().setOriginalIntermediaryNodeName(conf.get(ORIGINALINTERMEDIARYNODE));
        SwitchNodeNameInfo.getInstance().setTopologyName(conf.get(TOPOLOGYNAME));
        SwitchNodeNameInfo.getInstance().setOriginalEndNodeName(conf.get(ORIGINALENDNODE));
        SwitchNodeNameInfo.getInstance().setPrecedingNodeName(conf.get(PRECEDINGNODE));
        SwitchNodeNameInfo.getInstance().setTargetIntermediaryNodeName(conf.get(TARGETINTERMEDIARYNODE));
        SwitchNodeNameInfo.getInstance().setTargetEndNodeName(conf.get(TARGETENDNODE));
    }
    
    /**
     * Returns the name of the preceding node.
     * @return the name of the preceding node
     */
    public String getPrecedingNodeName() {
        return precedingNodeName;
    }
    
    /**
     * Sets the name of the preceding node.
     * @param precedingNodeName the name of the preceding node
     */
    public void setPrecedingNodeName(String precedingNodeName) {
        SwitchNodeNameInfo.precedingNodeName = precedingNodeName;
    }
    
    /**
     * Returns the name of the original intermediary node.
     * @return the name of the original intermediary node
     */
    public String getOriginalIntermediaryNodeName() {
        return originalIntermediaryNodeName;
    }
    
    /**
     * Sets the name of the original intermediary node.
     * @param originalIntermediaryNodeName the name of the original intermediary node
     */
    public void setOriginalIntermediaryNodeName(String originalIntermediaryNodeName) {
        SwitchNodeNameInfo.originalIntermediaryNodeName = originalIntermediaryNodeName;
    }
    
    /**
     * Returns the name of the target intermediary node.
     * @return the name of the target intermediary node
     */
    public String getTargetIntermediaryNodeName() {
        return targetIntermediaryNodeName;
    }
    
    /**
     * Sets the name of the target intermediary node.
     * @param targetIntermediaryNodeName the name of the target intermediary node
     */
    public void setTargetIntermediaryNodeName(String targetIntermediaryNodeName) {
        SwitchNodeNameInfo.targetIntermediaryNodeName = targetIntermediaryNodeName;
    }
    
    /**
     * Returns the name of the original end node.
     * @return the name of the original end node
     */
    public String getOriginalEndNodeName() {
        return originalEndNodeName;
    }
    
    /**
     * Sets the name of the original end node.
     * @param originalEndNodeName the name of the original end node
     */
    public void setOriginalEndNodeName(String originalEndNodeName) {
        SwitchNodeNameInfo.originalEndNodeName = originalEndNodeName;
    }
    
    /**
     * Returns the name of the target end node.
     * @return the name of the target end node
     */
    public String getTargetEndNodeName() {
        return targetEndNodeName;
    }
    
    /**
     * Sets the name of the target end node.
     * @param targetEndNodeName the name of the target end node
     */
    public void setTargetEndNodeName(String targetEndNodeName) {
        SwitchNodeNameInfo.targetEndNodeName = targetEndNodeName;
    }
    
    /**
     * Returns the topology name.
     * @return the topology name
     */
    public String getTopologyName() {
        return topologyName;
    }
    
    /**
     * Sets the topology name.
     * @param topologyName the topology name
     */
    public void setTopologyName(String topologyName) {
        SwitchNodeNameInfo.topologyName = topologyName;
    }
}
