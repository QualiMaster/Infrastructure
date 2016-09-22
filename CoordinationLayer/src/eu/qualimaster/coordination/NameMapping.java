package eu.qualimaster.coordination;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import eu.qualimaster.common.signal.Constants;
import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;

/**
 * Implements a name mapping. We use the following XML format:
 * <pre>
 *   &lt;pipeline name="xxx"&gt;
 *     &lt;component name = "Topology1"/&gt;
 *     &lt;node name="node1&gt;
 *       &lt;component name="Spout1"/&gt;
 *     &lt;/node&gt;
 *   &lt;/pipeline&gt;
 * </pre>
 * @author Holger Eichelberger
 */
public class NameMapping implements INameMapping {

    public static final String MAPPING_FILE_NAME = "mapping.xml";
    
    private static final String ELEMENT_PIPELINE = "pipeline";
    private static final String ELEMENT_NODE = "node";
    private static final String ELEMENT_COMPONENT = "component";
    private static final String ELEMENT_ALGORITHM = "algorithm";
    private static final String ELEMENT_PARAMETER = "parameter";
                    
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_IMPLNAME = "implName";
    private static final String ATTRIBUTE_CLASS = "class";
    private static final String ATTRIBUTE_TYPE = "type";
    private static final String ATTRIBUTE_RECEIVER = "receiver";
    private static final String ATTRIBUTE_THRIFT = "thrift";
    private static final String ATTRIBUTE_TASKS = "tasks";
    private static final String ATTRIBUTE_CONTAINER = "container";
    private static final String ATTRIBUTE_REF = "ref";
    private static final String ATTRIBUTE_PARAMETER = "parameter";
    private static final String ATTRIBUTE_ALGORITHM = "algorithm";

    private String pipelineName;
    private String containerName;
    private Map<String, ComponentImpl> pipelineNodeComponents = new HashMap<String, ComponentImpl>();
    private Map<String, String> pipelineNodes = new HashMap<String, String>();
    private List<String> containerNames = new ArrayList<String>();
    private List<String> pipelineNames = new ArrayList<String>();
    private Map<String, ISubPipeline> subPipelines = new HashMap<String, ISubPipeline>();
    private Map<String, AlgorithmImpl> algorithms = new HashMap<String, AlgorithmImpl>();
    private Map<String, AlgorithmImpl> algorithmsCls = new HashMap<String, AlgorithmImpl>();
    private Map<String, AlgorithmImpl> algorithmsImpl = new HashMap<String, AlgorithmImpl>();
    private Map<String, ComponentImpl> componentClasses = new HashMap<String, ComponentImpl>();
    private Map<String, ComponentImpl> componentImpl = new HashMap<String, ComponentImpl>();
    private Map<String, String> parameterMapping = new HashMap<String, String>();
    private Map<String, String> parameterBackMapping = new HashMap<String, String>();

    /**
     * Levels for parsing.
     * 
     * @author Holger Eichelberger
     */
    private enum Level {
        TOP,
        PIPELINE,
        NODE,
        COMPONENT, 
        ALGORITHM,
        SUB_PIPELINE
    }
    
    /**
     * Implements a component data structure.
     * 
     * @author Holger Eichelberger
     */
    public static class ComponentImpl implements Component {

        private String container;
        private String name;
        private String className;
        private boolean isReceiver = false;
        private Type type;
        private boolean useThrift = true;
        private int tasks = 1;
        private List<String> alternatives = new ArrayList<String>();
        
        /**
         * Creates a component data structure (no receiver).
         * 
         * @param container the container name
         * @param name the name of the component
         * @param className the class name implementing the component
         * @param type the type of the component
         */
        public ComponentImpl(String container, String name, String className, Type type) {
            this(container, name, className, false, type);
        }

        /**
         * Creates a component data structure.
         * 
         * @param container the container name
         * @param name the name of the component
         * @param className the class name implementing the component
         * @param isReceiver whether the component is a signal receiver
         * @param type the type of the component
         */
        public ComponentImpl(String container, String name, String className, boolean isReceiver, Type type) {
            this.container = container;
            this.name = name;
            this.className = className;
            this.type = type;
            this.isReceiver = isReceiver;
        }

        @Override
        public String getContainer() {
            return container;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public boolean isReceiver() {
            return isReceiver;
        }
        
        @Override
        public Type getType() {
            return type;
        }
        
        @Override
        public String toString() {
            return "Comp: " + container + " " + name + " " + className + " " + isReceiver + " " + type;
        }
        
        @Override
        public boolean useThrift() {
            return useThrift;
        }
        
        @Override
        public int getTasks() {
            return tasks;
        }
        
        @Override
        public Collection<String> getAlternatives() {
            return unmodifiableList(alternatives);
        }
        
        /**
         * Sets the thrift flag.
         * 
         * @param useThrift whether thrift shall be used
         */
        public void setUseThrift(boolean useThrift) {
            this.useThrift = useThrift;
        }
        
        /**
         * Defines the (configured) number of tasks.
         * 
         * @param tasks the number of tasks
         */
        public void setTasks(int tasks) {
            this.tasks = tasks;
        }
        
        /**
         * Defines whether this component is a signal receiver.
         * 
         * @param isReceiver whether it is a signal receiver
         */
        public void setReceiver(boolean isReceiver) {
            this.isReceiver = isReceiver;
        }
        
        /**
         * Defines the alternative algorithms.
         * 
         * @param alternatives the alternative algorithm names
         */
        public void setAlternatives(List<String> alternatives) {
            this.alternatives = alternatives;
        }

        @Override
        public boolean equals(Object obj) {
            boolean result = false;
            if (obj instanceof Component) {
                Component other = (Component) obj;
                result = container.equals(other.getContainer());
                result &= name.equals(other.getName());
                result &= className.equals(other.getClassName());
                result &= isReceiver == other.isReceiver();
                result &= type.equals(other.getType());
                result &= useThrift == other.useThrift();
                result &= tasks == other.getTasks();
                result &= alternatives.equals(alternatives);
            }
            return result;
        }
        
        @Override
        public int hashCode() {
            return container.hashCode() + name.hashCode() + className.hashCode() + NameMapping.hashCode(isReceiver)
                + type.hashCode() + NameMapping.hashCode(useThrift)
                + tasks + alternatives.hashCode();
        }
        
    }
    
    /**
     * Implements the algorithm information.
     * 
     * @author Holger Eichelberger
     */
    public static class AlgorithmImpl implements Algorithm {

        private String name;
        private String implName;
        private String className;
        private List<Component> components = new ArrayList<Component>();

        /**
         * Creates an algorithm information instance.
         * 
         * @param name the logical configuration name
         * @param implName the implementation (execution system) name
         * @param className the implementing class name
         */
        public AlgorithmImpl(String name, String implName, String className) {
            this.name = name;
            this.implName = implName;
            this.className = className;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getImplName() {
            return implName;
        }

        @Override
        public String getClassName() {
            return className;
        }

        @Override
        public List<Component> getComponents() {
            return unmodifiableList(components);
        }
        
        /**
         * Adds an implementing pipeline component (sub-topology).
         * 
         * @param component the component
         */
        private void addComponent(Component component) {
            components.add(component);
        }
        
        @Override
        public String toString() {
            return "Alg: " + name + " " + implName + " " + className + " " + components;
        }
        
        @Override
        public boolean equals(Object obj) {
            boolean result = false;
            if (obj instanceof Algorithm) {
                Algorithm other = (Algorithm) obj;
                result = name.equals(other.getName());
                result &= implName.equals(other.getImplName());
                result &= className.equals(other.getClassName());
                result &= components.equals(other.getComponents());
            }
            return result;
        }
        
        @Override
        public int hashCode() {
            return name.hashCode() + implName.hashCode() + className.hashCode() + components.hashCode();
        }
        
    }

    /**
     * Implements the sub-pipeline information.
     * 
     * @author Holger Eichelberger
     */
    public static class SubPipelineImpl implements ISubPipeline {
        
        private String name;
        private String algorithmName;

        /**
         * Creates a sub-pipeline information instance.
         * 
         * @param name the name of the sub-pipeline
         * @param algorithmName the name of the algorithm represented
         */
        public SubPipelineImpl(String name, String algorithmName) {
            this.name = name;
            this.algorithmName = algorithmName;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAlgorithmName() {
            return algorithmName;
        }

        @Override
        public String toString() {
            return "SubPip: " + name + " " + algorithmName;
        }
        
        @Override
        public boolean equals(Object obj) {
            boolean result = false;
            if (obj instanceof ISubPipeline) {
                ISubPipeline other = (ISubPipeline) obj;
                result = name.equals(other.getName());
                result &= algorithmName.equals(other.getAlgorithmName());
            }
            return result;
        }
        
        @Override
        public int hashCode() {
            return name.hashCode() + algorithmName.hashCode();
        }

    }
    
    /**
     * Creates a pipeline mapping.
     * 
     * @param pipelineName the name of the pipeline
     * @param mapping the mapping, e.g., read as a resource from a pipeline Jar file; mapping will not be closed!
     * @throws IOException in case of reading errors
     */
    public NameMapping(String pipelineName, InputStream mapping) throws IOException {
        this.pipelineName = pipelineName;
        pipelineNames.add(pipelineName); // TODO sub-pipelines
        read(mapping);
        freeze();
    }

    /**
     * Reads the mapping.
     * 
     * @param mapping the mapping to be read; mapping will not be closed!
     * @throws IOException in case of reading errors
     */
    private void read(InputStream mapping) throws IOException {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser saxParser = spf.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();
            xmlReader.setContentHandler(new MappingHandler());
            xmlReader.parse(new InputSource(mapping));
        } catch (SAXException e) {
            throw new IOException(e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        }
    }
    
    /**
     * Freezes the contents so that returned collections are not modifiable.
     */
    private void freeze() {
        Set<String> pTmp = new HashSet<String>();
        Set<String> cTmp = new HashSet<String>();
        for (Map.Entry<String, ComponentImpl> entry : pipelineNodeComponents.entrySet()) {
            Component cont = entry.getValue();
            if (!pipelineName.equals(cont.getContainer())) {
                pTmp.add(cont.getContainer());
            }
        }
        // TODO fill cTmp
        containerNames.add(containerName);
        containerNames.addAll(cTmp);
        pipelineNames.addAll(pTmp);
    }
    
    /**
     * Returns whether a read XML string is ok.
     * 
     * @param string the string to be checked
     * @return <code>true</code> if <code>string</code> is ok, <code>false</code> else
     */
    private static boolean stringOk(String string) {
        return null != string && string.length() > 0;
    }
    
    /**
     * Turns a String into a boolean with given default.
     * 
     * @param text the text to use (may be <b>null</b>)
     * @param dflt the value to be used if <code>text</code> is <b>null</b>
     * @return the boolean value of <code>text</code>
     */
    private static boolean toBoolean(String text, boolean dflt) {
        boolean result = dflt;
        if (null != text) {
            result = Boolean.valueOf(text);
        }
        return result;
    }
    
    /**
     * Turns a String into a boolean with given default.
     * 
     * @param text the text to use (may be <b>null</b>)
     * @param dflt the value to be used if <code>text</code> is <b>null</b>
     * @return the boolean value of <code>text</code>
     */
    private static int toInteger(String text, int dflt) {
        int result = dflt;
        if (null != text) {
            try {
                result = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return result;
    }

    /**
     * Reads the mapping from XML.
     * 
     * @author Holger Eichelberger
     */
    private class MappingHandler extends DefaultHandler {
        
        private Level level = Level.TOP;
        private String currentParent;
        private String currentType;
        private AlgorithmImpl currentAlgorithm;
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) 
            throws SAXException {
            if (Level.TOP == level && ELEMENT_PIPELINE.equals(qName)) {
                handlePipeline(attributes);
            } else if (Level.PIPELINE == level && ELEMENT_PIPELINE.equals(qName)) {
                handleSubPipeline(attributes);
            } else if ((Level.PIPELINE == level || Level.ALGORITHM == level) && ELEMENT_NODE.equals(qName)) {
                handleNode(attributes);
            } else if (Level.PIPELINE == level && ELEMENT_PARAMETER.equals(qName)) {
                handleParameter(attributes);
            } else if ((Level.NODE == level || Level.ALGORITHM == level) && ELEMENT_COMPONENT.equals(qName)) {
                handleComponent(attributes);
            } else if (Level.PIPELINE == level && ELEMENT_ALGORITHM.equals(qName)) {
                handleAlgorithm(attributes);
            }
        }

        /**
         * Handles an algorithm.
         * 
         * @param attributes the XML attributes
         */
        private void handleAlgorithm(Attributes attributes) {
            String name = attributes.getValue(ATTRIBUTE_NAME);
            currentParent = name;
            String implName = attributes.getValue(ATTRIBUTE_IMPLNAME);
            if (null == implName) { // legacy
                implName = name;
            }
            String className = attributes.getValue(ATTRIBUTE_CLASS);
            if (stringOk(name) && stringOk(className)) {
                AlgorithmImpl alg = new AlgorithmImpl(name, implName, className);
                algorithms.put(name, alg);
                algorithmsCls.put(className, alg);
                algorithmsImpl.put(implName, alg);
                currentAlgorithm = alg;
            }
            level = Level.ALGORITHM;
        }

        /**
         * Handles a component.
         * 
         * @param attributes the XML attributes
         */
        private void handleComponent(Attributes attributes) {
            String ref = attributes.getValue(ATTRIBUTE_REF);
            ComponentImpl comp = null;
            if (null != ref) {
                comp = getPipelineNodeComponentImpl(ref);
                if (null == comp) {
                    Logger.getLogger(NameMapping.class).error("Component ref '" + ref + "' is unknown.");
                }
            } else {
                String container = attributes.getValue(ATTRIBUTE_CONTAINER);
                String name = attributes.getValue(ATTRIBUTE_NAME);
                String className = attributes.getValue(ATTRIBUTE_CLASS);
                Type type = obtainType(currentType);
                String givenType = attributes.getValue(ATTRIBUTE_TYPE);
                if (null != givenType) {
                    if (Type.HARDWARE == obtainType(givenType)) {
                        type = Type.HARDWARE;
                    }
                }
                boolean isReceiver = Boolean.valueOf(attributes.getValue(ATTRIBUTE_RECEIVER));
                boolean useThrift = toBoolean(attributes.getValue(ATTRIBUTE_THRIFT), true); // legacy default
                int tasks = toInteger(attributes.getValue(ATTRIBUTE_TASKS), 1);
                boolean ok = stringOk(container) && stringOk(name) && stringOk(className);
                List<String> alternatives = new ArrayList<String>();
                int pos = 0;
                while (true) {
                    String alt = attributes.getValue("alternative" + pos);
                    if (null == alt) {
                        break;
                    }
                    alternatives.add(alt);
                    pos++;
                }
                // just ignore the nesting here
                if (ok && null != currentParent) {
                    comp = new ComponentImpl(container, name, className, type);
                    if (Level.NODE == level) { // don't map algorithm components to top-level
                        pipelineNodeComponents.put(currentParent, comp);
                        pipelineNodes.put(name, currentParent);
                    }
                    componentClasses.put(comp.getClassName(), comp);
                    componentImpl.put(comp.getName(), comp);
                    comp.setReceiver(isReceiver);
                    comp.setUseThrift(useThrift);
                    comp.setTasks(tasks);
                    comp.setAlternatives(alternatives);
                }
            }
            if (null != comp && null != currentAlgorithm) {
                currentAlgorithm.addComponent(comp);
            }            
        }
        
        /**
         * Handles a pipeline.
         * 
         * @param attributes the XML attributes
         */
        private void handlePipeline(Attributes attributes) {
            String name = attributes.getValue(ATTRIBUTE_NAME);
            if (pipelineName.equals(name)) {
                level = Level.PIPELINE;
                NameMapping.this.containerName = attributes.getValue(ATTRIBUTE_CLASS);
            }
        }

        /**
         * Handles a sub-pipeline.
         * 
         * @param attributes the XML attributes
         */
        private void handleSubPipeline(Attributes attributes) {
            String name = attributes.getValue(ATTRIBUTE_NAME);
            String algoName = attributes.getValue(ATTRIBUTE_ALGORITHM);
            if (stringOk(name) && stringOk(algoName)) {
                subPipelines.put(algoName, new SubPipelineImpl(name, algoName)); // algoName -> subPip
            }
            level = Level.SUB_PIPELINE;
        }

        /**
         * Handles a node.
         * 
         * @param attributes the XML attributes
         */
        private void handleNode(Attributes attributes) {
            currentParent = attributes.getValue(ATTRIBUTE_NAME);
            currentType = attributes.getValue(ATTRIBUTE_TYPE); // propagate, sub-topologies are mapped into
            level = Level.NODE;
        }
        
        /**
         * Handles a parameter.
         * 
         * @param attributes the XML attributes
         */
        private void handleParameter(Attributes attributes) {
            String nodeName = attributes.getValue(ATTRIBUTE_NAME);
            String parameter = attributes.getValue(ATTRIBUTE_PARAMETER);
            String receiver = attributes.getValue(ATTRIBUTE_RECEIVER);
            if (stringOk(nodeName) && stringOk(parameter) && stringOk(receiver)) {
                parameterMapping.put(getParameterKey(nodeName, parameter), receiver);
                parameterBackMapping.put(getParameterKey(receiver, parameter), nodeName);
            }
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (Level.SUB_PIPELINE == level && ELEMENT_PIPELINE.equals(qName)) {
                level = Level.PIPELINE;
            } else if (Level.PIPELINE == level && ELEMENT_PIPELINE.equals(qName)) {
                level = Level.TOP;
            } else if (Level.NODE == level && ELEMENT_NODE.equals(qName)) {
                level = Level.PIPELINE;
                currentParent = null;
                currentType = null;
            } else if (Level.ALGORITHM == level && ELEMENT_ALGORITHM.equals(qName)) {
                level = Level.PIPELINE;
                currentAlgorithm = null;
            }
        }
    }
    
    /**
     * Obtains the component type.
     * 
     * @param text the text to bre translated into a component type
     * @return the component type
     */
    private static Type obtainType(String text) {
        Type result = null;
        if (null != text && text.length() > 0) {
            try {
                result = Type.valueOf(text.toUpperCase());
            } catch (IllegalArgumentException e) {
                // unknown type -> result = null
            }
        }
        if (null == result) {
            result = Type.UNKNOWN;
        }
        return result;
    }
    
    @Override
    public String getPipelineName() {
        return pipelineName;
    }
    
    @Override
    public String getContainerName() {
        return containerName;
    }
    
    /**
     * Turns a <code>list</code> into an unmodifiable list considering that <code>list</code> may be <b>null</b>.
     * 
     * @param <T> the element type
     * @param list the list to be made unmodifiable
     * @return the unmodifiable list or <b>null</b>
     */
    private static final <T> List<T> unmodifiableList(List<T> list) {
        return null == list ? null : Collections.unmodifiableList(list);
    }

    /**
     * Returns the internal component meta information for the given pipeline node name.
     * 
     * @param pipelineNodeName the name of the pipeline node
     * @return the component meta information (may be <b>null</b>)
     */
    private ComponentImpl getPipelineNodeComponentImpl(String pipelineNodeName) {
        return pipelineNodeComponents.get(pipelineNodeName);
    }

    @Override
    public Component getPipelineNodeComponent(String pipelineNodeName) {
        return pipelineNodeComponents.get(pipelineNodeName);
    }
    
    @Override
    public Collection<String> getPipelineNodeNames() {
        return Collections.unmodifiableSet(pipelineNodeComponents.keySet());
    }

    @Override
    public String getPipelineNodeByImplName(String implName) {
        return pipelineNodes.get(implName);
    }

    @Override
    public Collection<Component> getComponents() {
        List<Component> result = new ArrayList<Component>();
        result.addAll(pipelineNodeComponents.values());
        return result;
    }

    @Override
    public Component getComponentByClassName(String className) {
        return componentClasses.get(className);
    }
    
    @Override
    public Component getComponentByImplName(String implName) {
        return componentImpl.get(implName);
    }


    @Override
    public List<String> getContainerNames() {
        return unmodifiableList(containerNames);
    }
    
    @Override
    public List<String> getPipelineNames() {
        return unmodifiableList(pipelineNames);
    }

    /**
     * Returns the internal algorithm meta-data for a given <code>algorithmName</code>.
     * 
     * @param algorithmName the name of the algorithm
     * @return the internal algorithm meta-data (may be <b>null</b>)
     */
    private AlgorithmImpl getAlgorithmImpl(String algorithmName) {
        return algorithms.get(algorithmName);
    }

    @Override
    public Algorithm getAlgorithm(String algorithmName) {
        return getAlgorithmImpl(algorithmName);
    }

    /**
     * Returns the internal algorithm meta-data for a given <code>className</code>.
     * 
     * @param className the name of the algorithm
     * @return the internal algorithm meta-data (may be <b>null</b>)
     */
    public Algorithm getAlgorithmImplByClassName(String className) {
        return algorithmsCls.get(className);
    }

    @Override
    public Algorithm getAlgorithmByClassName(String className) {
        return getAlgorithmImplByClassName(className);
    }

    /**
     * Returns the internal algorithm meta-data for a given implementation name.
     * 
     * @param implName the implementation name of the algorithm
     * @return the internal algorithm meta-data (may be <b>null</b>)
     */
    public AlgorithmImpl getAlgorithmImplByImplName(String implName) {
        return algorithmsImpl.get(implName);
    }

    @Override
    public Algorithm getAlgorithmByImplName(String implName) {
        return getAlgorithmImplByImplName(implName);
    }

    @Override
    public boolean isIdentity() {
        return false;
    }

    @Override
    public void considerSubStructures(SubTopologyMonitoringEvent event) {
        Map<String, List<String>> structures = event.getStructure();
        String pipeline = event.getPipeline();
        if (null != structures && null != pipeline) {
            for (Map.Entry<String, List<String>> entry : structures.entrySet()) {
                String algImplName = entry.getKey(); // impl name
                List<String> subToplogyComponents = entry.getValue();
                if (null != subToplogyComponents) {
                    AlgorithmImpl alg = getAlgorithmImplByImplName(algImplName);
                    if (null == alg) { // just a fallback for now
                        alg = getAlgorithmImpl(algImplName);
                    }
                    if (null != alg) {
                        for (String s : subToplogyComponents) {
                            String[] tmp = s.split(SubTopologyMonitoringEvent.SEPARATOR);
                            if (null != tmp && 2 == tmp.length) {
                                String name = tmp[0];
                                String className = tmp[1];
                                ComponentImpl comp = new ComponentImpl(alg.getName(), name, className, Type.UNKNOWN);
                                configure(comp);
                                alg.addComponent(comp);
                                componentClasses.put(comp.getClassName(), comp);
                                componentImpl.put(comp.getName(), comp);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Configures a component instance as part of considering sub-structures.
     * 
     * @param comp the component to be modified as a side effect
     */
    private static void configure(ComponentImpl comp) {
        // no receiver, tasks irrelevant for now as blackbox
        if (Constants.MEASURE_BY_TASK_HOOKS) {
            comp.setUseThrift(false); // as we use taskhooks now
        }
    }

    @Override
    public Collection<Algorithm> getAlgorithms() {
        List<Algorithm> result = new ArrayList<Algorithm>();
        result.addAll(algorithms.values());
        return result;
    }
    
    /**
     * Returns a parameter mapping key.
     * 
     * @param nodeName the name of the node
     * @param parameterName the name of the parameter
     * @return the key
     */
    private static String getParameterKey(String nodeName, String parameterName) {
        return nodeName + '0' + parameterName;
    }

    @Override
    public String getParameterMapping(String pipelineNodeName, String parameterName) {
        String result = parameterMapping.get(getParameterKey(pipelineNodeName, parameterName));
        if (null == result) {
            result = pipelineNodeName;
        }
        return result;
    }
    
    @Override
    public String getParameterBackMapping(String pipelineNodeName, String parameterName) {
        String result = parameterBackMapping.get(getParameterKey(pipelineNodeName, parameterName));
        if (null == result) {
            result = pipelineNodeName;
        }
        return result;
    }

    @Override
    public String toString() {
        return pipelineName + " " + containerName + " " + pipelineNodeComponents + " " + pipelineNodes + " " 
            + containerNames + " " + pipelineNames + " algs: " + algorithms + " " + algorithmsCls + " " 
            + algorithmsImpl + " comp: " + componentClasses + " " + componentImpl 
            + " params: " + parameterMapping + " " + parameterBackMapping + " subPipelines " + getSubPipelines();
    }

    @Override
    public Collection<ISubPipeline> getSubPipelines() {
        return subPipelines.values();
    }
    
    /**
     * Emulates the JDK 1.8 functionality.
     * 
     * @param value the boolean value
     * @return the hashcode
     */
    private static int hashCode(boolean value) {
        return value ? 1231 : 1237;
    }

    @Override
    public ISubPipeline getSubPipelineByAlgorithmName(String algorithmName) {
        return null == algorithmName ? null : subPipelines.get(algorithmName);
    }

}
