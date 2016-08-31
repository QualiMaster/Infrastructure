package tests.eu.qualimaster.coordination;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tests.eu.qualimaster.storm.Naming;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;

/**
 * Implements a name mapping for tests of the monitoring layer.
 * 
 * @author Holger Eichelberger
 */
public class TestNameMapping implements INameMapping {

    public static final String PIPELINE_NAME = Naming.PIPELINE_NAME;
    public static final String CONTAINER_NAME = Naming.CONTAINER_CLASS;
    public static final String NODE_SOURCE = Naming.NODE_SOURCE;
    public static final String NODE_SOURCE_COMPONENT = Naming.NODE_SOURCE_COMPONENT;
    public static final String NODE_PROCESS = Naming.NODE_PROCESS;
    public static final String NODE_PROCESS_COMPONENT = Naming.NODE_PROCESS_COMPONENT;
    public static final String NODE_PROCESS_ALG1 = Naming.NODE_PROCESS_ALG1;
    public static final String NODE_PROCESS_ALG1_CLASS = Naming.NODE_PROCESS_ALG1_CLASS;
    public static final String NODE_PROCESS_ALG2 = Naming.NODE_PROCESS_ALG2;
    public static final String NODE_PROCESS_ALG2_CLASS = Naming.NODE_PROCESS_ALG2_CLASS;
    public static final String NODE_SINK = Naming.NODE_SINK;
    public static final String NODE_SINK_COMPONENT = Naming.NODE_SINK_COMPONENT;
    public static final INameMapping INSTANCE = new TestNameMapping();

    private List<String> containers;
    private List<String> pipelines;
    private Map<String, Algorithm> algs;
    private Map<String, Component> components;
    private Map<String, String> componentsC;
    private Map<String, Component> componentClass;
    
    /**
     * Creates the test name mapping.
     */
    private TestNameMapping() {
        containers = new ArrayList<String>();
        containers.add(CONTAINER_NAME);
        containers = Collections.unmodifiableList(containers);

        pipelines = new ArrayList<String>();
        pipelines.add(PIPELINE_NAME);
        pipelines = Collections.unmodifiableList(pipelines);

        algs = new HashMap<String, Algorithm>();
        algs.put(NODE_PROCESS_ALG1, new NameMapping.AlgorithmImpl(
            NODE_PROCESS_ALG1, NODE_PROCESS_ALG1, NODE_PROCESS_ALG1_CLASS));
        algs.put(NODE_PROCESS_ALG1_CLASS, algs.get(NODE_PROCESS_ALG1));
        algs.put(NODE_PROCESS_ALG2, new NameMapping.AlgorithmImpl(
            NODE_PROCESS_ALG2, NODE_PROCESS_ALG2, NODE_PROCESS_ALG2_CLASS));
        algs.put(NODE_PROCESS_ALG2_CLASS, algs.get(NODE_PROCESS_ALG2));
        algs = Collections.unmodifiableMap(algs);

        components = new HashMap<String, Component>();
        NameMapping.ComponentImpl cSource = new NameMapping.ComponentImpl(CONTAINER_NAME, NODE_SOURCE, 
            NODE_SOURCE_COMPONENT, true, Type.SOURCE);
        cSource.setUseThrift(false);
        put(components, cSource);
        NameMapping.ComponentImpl cProcess = new NameMapping.ComponentImpl(CONTAINER_NAME, NODE_PROCESS, 
            NODE_PROCESS_COMPONENT, true, Type.FAMILY);
        cProcess.setUseThrift(false);
        put(components, cProcess);
        NameMapping.ComponentImpl cSink = new NameMapping.ComponentImpl(CONTAINER_NAME, NODE_SINK, 
            NODE_SINK_COMPONENT, true, Type.SINK);
        cSink.setUseThrift(false);
        put(components, cSink);
        components = Collections.unmodifiableMap(components);
        
        componentClass = new HashMap<String, Component>();
        componentClass.put(NODE_SOURCE_COMPONENT, cSource);
        componentClass.put(NODE_SINK_COMPONENT, cSink);
        componentClass.put(NODE_PROCESS_COMPONENT, cProcess);

        componentsC = new HashMap<String, String>();
        componentsC.put(NODE_SOURCE_COMPONENT, NODE_SOURCE);
        componentsC.put(NODE_SOURCE, NODE_SOURCE_COMPONENT);
        componentsC.put(NODE_PROCESS_COMPONENT, NODE_PROCESS);
        componentsC.put(NODE_PROCESS, NODE_PROCESS_COMPONENT);
        componentsC.put(NODE_SINK_COMPONENT, NODE_SINK);
        componentsC.put(NODE_SINK, NODE_SINK_COMPONENT);
        componentsC = Collections.unmodifiableMap(componentsC);
    }
    
    /**
     * Stores a component.
     * 
     * @param map the storage map
     * @param component the component
     */
    private static void put(Map<String, Component> map, Component component) {
        map.put(component.getName(), component);
    }
    
    @Override
    public String getPipelineName() {
        return PIPELINE_NAME;
    }

    @Override
    public String getContainerName() {
        return CONTAINER_NAME;
    }

    @Override
    public Component getPipelineNodeComponent(String pipelineNodeName) {
        return components.get(pipelineNodeName);
    }
    
    @Override
    public Collection<String> getPipelineNodeNames() {
        return components.keySet();
    }
    
    @Override
    public Collection<Component> getComponents() {
        return Collections.unmodifiableCollection(components.values());
    }

    @Override
    public String getPipelineNodeByImplName(String implName) {
        return componentsC.get(implName);
    }

    @Override
    public List<String> getContainerNames() {
        return containers;
    }

    @Override
    public Algorithm getAlgorithm(String algorithmName) {
        return algs.get(algorithmName);
    }
    

    @Override
    public Algorithm getAlgorithmByClassName(String className) {
        return algs.get(className);
    }

    @Override
    public List<String> getPipelineNames() {
        return pipelines;
    }

    @Override
    public boolean isIdentity() {
        return false;
    }

    @Override
    public void considerSubStructures(SubTopologyMonitoringEvent event) {
        // just ignore
    }

    @Override
    public Component getComponentByClassName(String className) {
        return null;
    }

    @Override
    public Component getComponentByImplName(String implName) {
        return null;
    }

    @Override
    public Algorithm getAlgorithmByImplName(String implName) {
        return algs.get(implName);
    }

    @Override
    public Collection<Algorithm> getAlgorithms() {
        return algs.values();
    }

    @Override
    public String getParameterMapping(String pipelineNodeName, String parameterName) {
        return pipelineNodeName;
    }

    @Override
    public String getParameterBackMapping(String pipelineNodeName, String parameterName) {
        return pipelineNodeName;
    }

    @Override
    public List<String> getSubPipelines() {
        return null;
    }
    
}
