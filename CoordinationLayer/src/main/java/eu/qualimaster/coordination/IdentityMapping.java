package eu.qualimaster.coordination;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;

/**
 * An identity mapping as fallback for (legacy) pipelines without mapping
 * specification.
 * 
 * @author Holger Eichelberger
 */
public class IdentityMapping implements INameMapping {

    private String pipeline;
    private List<String> containerNames = new ArrayList<String>();

    /**
     * Creates an identity mapping.
     * 
     * @param pipeline the pipeline name
     */
    public IdentityMapping(String pipeline) {
        this.pipeline = pipeline;
        containerNames.add(pipeline);
        containerNames = Collections.unmodifiableList(containerNames);
    }
     
    @Override
    public String getPipelineName() {
        return pipeline;
    }

    @Override
    public Component getPipelineNodeComponent(String pipelineNodeName) {
        return new NameMapping.ComponentImpl(pipeline, pipelineNodeName, pipelineNodeName, true, Type.UNKNOWN);
    }

    @Override
    public Collection<String> getPipelineNodeNames() {
        return new ArrayList<String>();
    }
    
    @Override
    public List<Component> getComponents() {
        return new ArrayList<Component>(); // unknown for now
    }

    @Override
    public String getPipelineNodeByImplName(String implName) {
        return implName;
    }

    @Override
    public List<String> getPipelineNames() {
        return containerNames;
    }
    
    @Override
    public List<String> getContainerNames() {
        return containerNames;
    }

    @Override
    public Algorithm getAlgorithm(String algorithmName) {
        return new NameMapping.AlgorithmImpl(algorithmName, algorithmName, algorithmName);
    }

    @Override
    public Algorithm getAlgorithmByClassName(String className) {
        String name = className;
        int pos = name.lastIndexOf('.');
        if (pos > 0 && pos < name.length() - 1) {
            name = name.substring(pos + 1);
        }
        return new NameMapping.AlgorithmImpl(name, name, className);
    }
    

    @Override
    public Algorithm getAlgorithmByImplName(String implName) {
        return new NameMapping.AlgorithmImpl(implName, implName, implName);
    }
    
    @Override
    public String getContainerName() {
        return pipeline;
    }

    @Override
    public boolean isIdentity() {
        return true;
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
    public Collection<Algorithm> getAlgorithms() {
        return new ArrayList<Algorithm>(); // unknown for now
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
    public List<ISubPipeline> getSubPipelines() {
        return new ArrayList<ISubPipeline>();
    }

    @Override
    public ISubPipeline getSubPipelineByAlgorithmName(String algorithmName) {
        return null;
    }

}
