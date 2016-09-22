package eu.qualimaster.coordination;

import java.util.Collection;
import java.util.List;

import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;

/**
 * Provides access to the mapping of a pipeline to its physical implementation components.
 * A pipeline may be implemented by multiple (topology...) components and a pipeline node (see IVML)
 * may also be implemented by multiple (Spout/Bolt...) components.
 * 
 * @author Holger Eichelberger
 */
public interface INameMapping {

    /**
     * Meta-information about an implementation component (Spout, Bolt, ...).
     * 
     * @author Holger Eichelberger
     */
    public interface Component {
        
        /**
         * The type of the component.
         * 
         * @author Holger Eichelberger
         */
        public enum Type {
            
            /**
             * A source node.
             */
            SOURCE,

            /**
             * A family node.
             */
            FAMILY,
            
            /**
             * A sink node.
             */
            SINK,
            
            /**
             * A data management node.
             */
            DATA_MGT,
            
            /**
             * A hardware integration node.
             */
            HARDWARE,
            
            /**
             * A currently unknown node.
             */
            UNKNOWN;
            
        }
        
        /**
         * Returns the Execution System specific container, e.g., a Topology.
         * 
         * @return the container
         */
        public String getContainer();
        
        /**
         * Returns the Execution System specific name of the component, e.g., a Bolt name.
         * 
         * @return the name of the component
         */
        public String getName();
        
        /**
         * Returns the class name implementing the component.
         * 
         * @return the qualified class name
         */
        public String getClassName();
        
        /**
         * Is this component a signal receiver?
         * 
         * @return <code>true</code> if this component is a signal receiver, <code>false</code> else
         */
        public boolean isReceiver();
        
        /**
         * Returns the type of the component.
         * 
         * @return the type
         */
        public Type getType();
        
        /**
         * Whether thrift shall be used for monitoring. This is intended as a hint to the Monitoring layer,
         * i.e., if the result is <code>false</code> the component provides own monitoring events for
         * the respective quality parameters, if <code>true</code> shall but must not be used.
         * 
         * @return <code>true</code> for thrift, <code>false</code> else
         */
        public boolean useThrift();
        
        /**
         * The configured number of tasks.
         * 
         * @return the configured number of tasks
         */
        public int getTasks();

        /**
         * Returns the algorithm names of the alternative algorithms.
         * 
         * @return the algorithm names
         */
        public Collection<String> getAlternatives();

    }
    
    /**
     * Meta-information about an algorithm.
     * 
     * @author Holger Eichelberger
     */
    public interface Algorithm {

        /**
         * Returns the logical (configuration) name of the algorithm.
         * 
         * @return the name of the component
         */
        public String getName();

        /**
         * Returns the Execution System specific name of the algorithm.
         * 
         * @return the implementation name of the component
         */
        public String getImplName();

        /**
         * Returns the class name implementing the component.
         * 
         * @return the qualified class name
         */
        public String getClassName();
        
        /**
         * Returns the components implementing the algorithm.
         * 
         * @return the components
         */
        public List<Component> getComponents();

    }
    
    /**
     * Describes a loosely integrated sub-pipeline.
     * 
     * @author Holger Eichelberger
     */
    public interface ISubPipeline {
        
        /**
         * Returns the name of the sub-pipeline.
         * 
         * @return the name
         */
        public String getName();
        
        /**
         * Returns the name of the algorithm represented by the sub-pipeline.
         * 
         * @return the name of the algorithm
         */
        public String getAlgorithmName();
        
    }
    
    /**
     * String returns the name of the pipeline the mapping is assigned to.
     * 
     * @return the name of the pipeline
     */
    public String getPipelineName();

    /**
     * Returns the implementing container corresponding to the pipeline.
     * 
     * @return the corresponding container name
     */
    public String getContainerName();
    
    /**
     * Returns the implementing component for a pipeline node.
     * 
     * @param pipelineNodeName the name of the pipeline node
     * @return the the implementing components (<b>null</b> if there is no implementation 
     *   in this pipeline)
     */
    public Component getPipelineNodeComponent(String pipelineNodeName);
    
    /**
     * Returns all known pipeline node names.
     * 
     * @return all pipeline node names
     */
    public Collection<String> getPipelineNodeNames();
    
    /**
     * Returns the component related to the given class name.
     * 
     * @param className the class name
     * @return the component (may be <b>null</b> if it does not exist)
     */
    public Component getComponentByClassName(String className);

    /**
     * Returns the implementing component for the given <code>implName</code> (including components used in 
     * sub-topologies).
     * 
     * @param implName the implementation name of the component
     * @return the component (may be <b>null</b>)
     */
    public Component getComponentByImplName(String implName);

    /**
     * Returns all components known for this topology.
     * 
     * @return all components
     */
    public Collection<Component> getComponents();
    
    /**
     * Returns the pipeline node for a given implementation component.
     * 
     * @param componentName the name of the implementation component
     * @return the name of the related pipeline node (<b>null</b> if no such relation exists)
     */
    public String getPipelineNodeByImplName(String componentName);

    /**
     * Returns the names of all pipelines in this mapping.
     * 
     * @return the names of all pipelines
     */
    public List<String> getPipelineNames();
    
    /**
     * Returns the names of all containers in this mapping.
     * 
     * @return the names of all containers
     */
    public List<String> getContainerNames();
    
    /**
     * Returns the information for an algorithm.
     * 
     * @param algorithmName the logical name of the algorithm
     * @return the meta information about the algorithm (<b>null</b> if not found)
     */
    public Algorithm getAlgorithm(String algorithmName);

    /**
     * Returns the information for an algorithm.
     * 
     * @param className the class name of the algorithm
     * @return the meta information about the algorithm (<b>null</b> if not found)
     */
    public Algorithm getAlgorithmByClassName(String className);
    
    /**
     * Returns the information for an algorithm.
     * 
     * @param implName the implementation name of the algorithm
     * @return the meta information about the algorithm (<b>null</b> if not found)
     */
    public Algorithm getAlgorithmByImplName(String implName);

    /**
     * Returns all algorithms.
     * 
     * @return all algorithms
     */
    public Collection<Algorithm> getAlgorithms();
    
    /**
     * Returns whether this mapping is just an identity mapping without backing pipeline.
     * 
     * @return <code>true</code> if it is an identity mapping, <code>false</code> else
     */
    public boolean isIdentity();
    
    /**
     * Considers sub-structures determined while building up pipelines.
     *  
     * @param event the causing event
     */
    public void considerSubStructures(SubTopologyMonitoringEvent event);
    
    /**
     * Returns whether the parameter <code>parameterName</code> is actually implemented (mapped)
     * by another pipeline node so that the parameter shall be redirected.
     * 
     * @param pipelineNodeName the pipeline node name of the original request 
     * @param parameterName the parameter name
     * @return the pipeline node name or the component implementation name of the actual receiver 
     *     (may be <code>pipelineNodeName</code>)
     */
    public String getParameterMapping(String pipelineNodeName, String parameterName);

    /**
     * Returns whether the parameter <code>parameterName</code> was sent by another pipeline node and was, thus, 
     * redirected.
     * 
     * @param pipelineNodeName the pipeline node name of the original request 
     * @param parameterName the parameter name
     * @return the pipeline node name or the component implementation name of the actual receiver 
     *     (may be <code>pipelineNodeName</code>)
     */
    public String getParameterBackMapping(String pipelineNodeName, String parameterName);

    /**
     * Returns the direct sub-pipelines of this pipeline.
     * 
     * @return the sub pipelines, may be empty if there are none
     */
    public Collection<ISubPipeline> getSubPipelines();

    /**
     * Returns the pipeline realizing the given algorithm.
     * 
     * @param algorithmName the name of the algorithm
     * @return the sub-pipeline or <b>null</b> if there is none
     */
    public ISubPipeline getSubPipelineByAlgorithmName(String algorithmName);
    
}
