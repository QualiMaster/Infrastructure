package tests.eu.qualimaster.coordination;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import tests.eu.qualimaster.storm.Naming;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.CoordinationUtils;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;

/**
 * Tests the name mapping.
 * 
 * @author Holger Eichelberger
 */
public class NameMappingTest {

    /**
     * Reads a name mapping from a given file.
     * 
     * @param fileName the file name
     * @param pipelineName the expected pipeline name
     * @return the name mapping
     * @throws IOException in case of I/O problems
     */
    private static NameMapping readNameMapping(String fileName, String pipelineName) throws IOException {
        File file = new File(Utils.getTestdataDir(), fileName);
        FileInputStream in = new FileInputStream(file);
        NameMapping mapping = new NameMapping(pipelineName, in);
        in.close();
        return mapping;
    }
    
    /**
     * Tests reading the name mapping.
     * 
     * @throws IOException in case of I/O problems, shall not occur / test failure
     */
    @Test
    public void testNameMapping() throws IOException {
        NameMapping mapping = readNameMapping("testPipeline.xml", Naming.PIPELINE_NAME);
        
        Assert.assertEquals(Naming.PIPELINE_NAME, mapping.getPipelineName());
        Assert.assertEquals(Naming.CONTAINER_CLASS, mapping.getContainerName());
        
        assertAlgorithm(mapping, Naming.NODE_PROCESS_ALG1, Naming.NODE_PROCESS_ALG1, Naming.NODE_PROCESS_ALG1_CLASS);
        assertAlgorithm(mapping, Naming.NODE_PROCESS_ALG2, Naming.NODE_PROCESS_ALG2, Naming.NODE_PROCESS_ALG2_CLASS);
        
        Assert.assertTrue(mapping.getPipelineNames().contains(Naming.PIPELINE_NAME) 
            && 1 == mapping.getPipelineNames().size());
        Assert.assertTrue(mapping.getContainerNames().contains(Naming.CONTAINER_CLASS) 
            && 1 == mapping.getContainerNames().size());
        
        Assert.assertEquals(Naming.NODE_SOURCE, mapping.getPipelineNodeByImplName(Naming.NODE_SOURCE_COMPONENT));
        Assert.assertEquals(Naming.NODE_SINK, mapping.getPipelineNodeByImplName(Naming.NODE_SINK_COMPONENT));
        Assert.assertEquals(Naming.NODE_PROCESS, mapping.getPipelineNodeByImplName(Naming.NODE_PROCESS_COMPONENT));

        assertNodeComponent(mapping, Naming.NODE_SOURCE, Naming.NODE_SOURCE_CLASS, Naming.PIPELINE_NAME, 
            Type.SOURCE);
        assertNodeComponent(mapping, Naming.NODE_PROCESS, Naming.NODE_PROCESS_CLASS, Naming.PIPELINE_NAME, 
            Type.FAMILY);
        assertNodeComponent(mapping, Naming.NODE_SINK, Naming.NODE_SINK_CLASS, Naming.PIPELINE_NAME, 
            Type.SINK);
        
        Collection<Component> comp = mapping.getComponents();
        Assert.assertTrue(comp.contains(mapping.getPipelineNodeComponent(Naming.NODE_SOURCE)));
        Assert.assertTrue(comp.contains(mapping.getPipelineNodeComponent(Naming.NODE_SINK)));
        Assert.assertTrue(comp.contains(mapping.getPipelineNodeComponent(Naming.NODE_PROCESS)));
        
        Collection<String> names = mapping.getPipelineNodeNames();
        Assert.assertTrue(names.contains(Naming.NODE_SOURCE));
        Assert.assertTrue(names.contains(Naming.NODE_SINK));
        Assert.assertTrue(names.contains(Naming.NODE_PROCESS));
        
        Component cmp = mapping.getPipelineNodeComponent(Naming.NODE_PROCESS);
        Assert.assertNotNull(cmp);
        Collection<String> alts = cmp.getAlternatives();
        Assert.assertTrue(alts.contains(Naming.NODE_PROCESS_ALG1));
        Assert.assertTrue(alts.contains(Naming.NODE_PROCESS_ALG2));
        
        Assert.assertEquals(Naming.NODE_PROCESS, mapping.getParameterMapping(Naming.NODE_PROCESS, "test"));
        Assert.assertEquals(Naming.NODE_PROCESS, mapping.getParameterMapping(Naming.NODE_SOURCE, "test"));
        
        List<String> subPipelines = mapping.getSubPipelines();
        Assert.assertNotNull(subPipelines);
        Assert.assertEquals(1, subPipelines.size());
        Assert.assertEquals("other", subPipelines.get(0));
    }
    
    /**
     * Tests the name mapping sub structure function.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testNameMappingSubStructure() throws IOException {
        NameMapping mapping = readNameMapping("testPipeline.xml", Naming.PIPELINE_NAME);
        Map<String, List<String>> structure = new HashMap<String, List<String>>();
        List<String> tmp = new ArrayList<String>();
        final String pipeline = Naming.PIPELINE_NAME;
        final String alg = "alg1";
        final String elt1Name = "RandomProcessor1processor1";
        final String elt1Class = "eu.qualimaster.algorithms.Process1Bolt";
        final String elt2Name = "RandomProcessor1processor2";
        final String elt2Class = "eu.qualimaster.algorithms.Process2Bolt";
        tmp.add(elt1Name + SubTopologyMonitoringEvent.SEPARATOR + elt1Class);
        tmp.add(elt2Name + SubTopologyMonitoringEvent.SEPARATOR + elt2Class);
        structure.put(alg, tmp);
        SubTopologyMonitoringEvent evt = new SubTopologyMonitoringEvent(pipeline, structure);
        mapping.considerSubStructures(evt);
        
        Algorithm algorithm = mapping.getAlgorithmByImplName(alg);
        Assert.assertNotNull(algorithm);
        Map<String, Component> cmpMap = new HashMap<String, Component>();
        for (Component c: algorithm.getComponents()) {
            cmpMap.put(c.getName(), c);
        }
        Component c1 = cmpMap.get(elt1Name);
        Assert.assertNotNull(c1);
        Assert.assertEquals(elt1Name, c1.getName());
        Assert.assertEquals(algorithm.getName(), c1.getContainer());
        Assert.assertEquals(elt1Class, c1.getClassName());
        Component c2 = cmpMap.get(elt2Name);
        Assert.assertNotNull(c2);
        Assert.assertEquals(elt2Name, c2.getName());
        Assert.assertEquals(algorithm.getName(), c2.getContainer());
        Assert.assertEquals(elt2Class, c2.getClassName());
    }

    /**
     * Tests reading the name mapping.
     * 
     * @throws IOException in case of I/O problems, shall not occur / test failure
     */
    @Test
    public void testNameMappingExamplePip() throws IOException {
        File file = new File(Utils.getTestdataDir(), "examplePip.xml");
        FileInputStream in = new FileInputStream(file);
        NameMapping mapping = new NameMapping("examplePip", in);
        in.close();
        
        Assert.assertEquals("examplePip", mapping.getPipelineName());
        Assert.assertEquals("eu.qualimaster.topologies.examplePip.Topology", mapping.getContainerName());
        
        Assert.assertEquals("Example Spring Data", mapping.getPipelineNodeByImplName("src_example"));
    }
    
    /**
     * Asserts the information in a {@link Component}.
     * 
     * @param mapping the mapping to query for
     * @param nodeName the expected name of the node
     * @param className the expected class name representing the implementation of the node (ignored if <b>null</b>)
     * @param container the expected containing container
     * @param type the expected type
     */
    private static void assertNodeComponent(INameMapping mapping, String nodeName, String className, String container, 
        Type type) {
        assertNodeComponent(mapping.getPipelineNodeComponent(nodeName), className, container, type);
    }

    /**
     * Asserts the information in a {@link Component}.
     * 
     * @param component the component
     * @param className the expected class name representing the implementation of the node (ignored if <b>null</b>)
     * @param container the expected containing container
     * @param type the expected type
     */
    private static void assertNodeComponent(Component component, String className, String container, 
        Type type) {
        Assert.assertNotNull(component);
        Assert.assertEquals(className, component.getClassName());
        if (null != container) {
            Assert.assertEquals(container, component.getContainer());
        }
        Assert.assertEquals(type, component.getType());
    }

    /**
     * Asserts the information in a {@link Component}.
     * 
     * @param mapping the mapping to query for
     * @param nodeName the expected name of the node
     * @param componentName the expected component name
     * @param receiver the expected receiver flag
     */
    private static void assertNodeComponent2(INameMapping mapping, String nodeName, String componentName, 
        boolean receiver) {
        assertNodeComponent2(mapping.getPipelineNodeComponent(nodeName), componentName, receiver);
    }

    /**
     * Asserts the information in a {@link Component}.
     * 
     * @param component the component
     * @param componentName the expected component name
     * @param receiver the expected receiver flag
     */
    private static void assertNodeComponent2(Component component, String componentName, boolean receiver) {
        Assert.assertNotNull(component);
        Assert.assertEquals(componentName, component.getName());
        Assert.assertEquals(receiver, component.isReceiver());
    }

    /**
     * Asserts the information in a {@link Component}.
     * 
     * @param mapping the mapping to query for
     * @param nodeName the expected name of the node
     * @param thrift the expected thrift flag
     * @param tasks the expected number of tasks (ignored if negative)
     * @param alternatives the alternative algorithms
     */
    private static void assertNodeComponent3(INameMapping mapping, String nodeName, boolean thrift, int tasks, 
        String... alternatives) {
        assertNodeComponent3(mapping.getPipelineNodeComponent(nodeName), thrift, tasks, alternatives);
    }

    /**
     * Asserts the information in a {@link Component}.
     * 
     * @param component the component
     * @param thrift the expected thrift flag
     * @param tasks the expected number of tasks (ignored if negative)
     * @param alternatives the alternative algorithms
     */
    private static void assertNodeComponent3(Component component, boolean thrift, int tasks, String... alternatives) {
        Assert.assertNotNull(component);
        Assert.assertEquals(thrift, component.useThrift());
        if (tasks >= 0) {
            Assert.assertEquals(tasks, component.getTasks());
        }
        List<String> alts = new ArrayList<String>();
        for (String a : alternatives) {
            alts.add(a);
        }
        Assert.assertEquals(alts, component.getAlternatives());
    }

    
    /**
     * Asserts the information in an {@link Algorithm}.
     * 
     * @param mapping the mapping to query for
     * @param algorithmName the expected name of the algorithm
     * @param implName the expected name representing the implementation name 
     * @param className the expected class name representing the implementation 
     */
    private static void assertAlgorithm(INameMapping mapping, String algorithmName, String implName, String className) {
        Algorithm alg = mapping.getAlgorithm(algorithmName);
        Assert.assertNotNull(alg);
        Assert.assertEquals(algorithmName, alg.getName());
        Assert.assertEquals(implName, alg.getImplName());
        Assert.assertEquals(className, alg.getClassName());
    }
    
    /**
     * Asserts a parameter entry.
     * 
     * @param mapping the name mapping
     * @param name the name of the original receiver node
     * @param parameterName the parameter name
     * @param receiver the expected receiver (node name or implementation component name)
     */
    private static void assertParameter(INameMapping mapping, String name, String parameterName, String receiver) {
        String recv = mapping.getParameterMapping(name, parameterName);
        if (null == receiver) {
            Assert.assertNotNull(recv);
            Assert.assertEquals(name, recv);
        } else {
            Assert.assertNotNull(recv);
            Assert.assertEquals(receiver, recv);
        }
        Component recvC = mapping.getPipelineNodeComponent(recv);
        if (null == recvC) {
            recvC = mapping.getComponentByImplName(recv);
        }
        Assert.assertNotNull(recvC);
        Assert.assertEquals(recvC, CoordinationUtils.getParameterReceiverComponent(mapping, name, parameterName));
        
        Assert.assertEquals(name, mapping.getParameterBackMapping(recv, parameterName));
    }
    
    /**
     * Tests the name mapping on the level of the coordination layer.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testCoordinationLevelMapping() throws IOException {
        NameMapping mapping = readNameMapping("testPipeline.xml", Naming.PIPELINE_NAME);
        CoordinationManager.registerTestMapping(mapping);
        
        Assert.assertTrue(mapping == CoordinationManager.getNameMapping(mapping.getPipelineName()));
        Assert.assertTrue(mapping == CoordinationManager.getNameMappingForClass(Naming.NODE_PROCESS_ALG1_CLASS));
        Assert.assertTrue(mapping == CoordinationManager.getNameMappingForClass(Naming.NODE_PROCESS_ALG2_CLASS));
        Assert.assertTrue(mapping == CoordinationManager.getNameMappingForClass(Naming.NODE_PROCESS_CLASS));
        Assert.assertTrue(mapping == CoordinationManager.getNameMappingForClass(Naming.NODE_SOURCE_CLASS));
        Assert.assertTrue(mapping == CoordinationManager.getNameMappingForClass(Naming.CONTAINER_CLASS));
        Assert.assertNull(CoordinationManager.getNameMappingForClass(""));
    }

    /**
     * Tests reading the name mapping for <code>genSubTopoMapping.xml</code>.
     * 
     * @throws IOException in case of I/O problems, shall not occur / test failure
     */
    @Test
    public void testGenSubTopoMapping() throws IOException {
        NameMapping mapping = readNameMapping("genSubTopoMapping.xml", "PriorityPip");
        Assert.assertEquals("PriorityPip", mapping.getPipelineName());

        assertNodeComponent(mapping, "FinancialDataSource", 
            "eu.qualimaster.PriorityPip.topology.PriorityPip_Source0Source", "PriorityPip", Component.Type.SOURCE);
        assertNodeComponent2(mapping, "FinancialDataSource", "PriorityPip_Source0", true);
        assertNodeComponent3(mapping, "FinancialDataSource", false, 1);

        assertNodeComponent(mapping, "FinancialCorrelation", 
            "eu.qualimaster.PriorityPip.topology.PriorityPip_FamilyElement0FamilyElement", "PriorityPip", 
            Component.Type.FAMILY);
        assertNodeComponent2(mapping, "FinancialCorrelation", "PriorityPip_FamilyElement0", true);
        assertNodeComponent3(mapping, "FinancialCorrelation", false, 1, "CorrelationSW");
        // TEST ALTERNATIVES
        
        assertNodeComponent(mapping, "Preprocessor", 
            "eu.qualimaster.PriorityPip.topology.PriorityPip_FamilyElement1FamilyElement", "PriorityPip", 
            Component.Type.FAMILY);
        assertNodeComponent2(mapping, "Preprocessor", "PriorityPip_FamilyElement1", false);
        assertNodeComponent3(mapping, "Preprocessor", false, 1, "Preprocessor");
        
        assertAlgorithm(mapping, "CorrelationSW", "CorrelationSWSubTopology", 
            "eu.qualimaster.CorrelationSW.topology.CorrelationSWSubTopology");
        Algorithm alg = mapping.getAlgorithm("CorrelationSW");
        Assert.assertNotNull(alg);
        Assert.assertEquals(2, alg.getComponents().size());
        
        assertNodeComponent(alg.getComponents().get(0), 
            "eu.qualimaster.CorrelationSW.topology.SubTopology_FamilyElement0FamilyElement", "CorrelationSW", 
            Component.Type.UNKNOWN);
        assertNodeComponent2(alg.getComponents().get(0), "SubTopology_FamilyElement0", true);
        assertNodeComponent3(alg.getComponents().get(0), false, 1);
        
        assertNodeComponent(alg.getComponents().get(1), 
            "eu.qualimaster.CorrelationSW.topology.SubTopology_FamilyElement1FamilyElement", "CorrelationSW", 
            Component.Type.UNKNOWN);
        assertNodeComponent2(alg.getComponents().get(1), "SubTopology_FamilyElement1", false);
        assertNodeComponent3(alg.getComponents().get(1), false, 13);

        assertAlgorithm(mapping, "Preprocessor", "Preprocessor", 
            "eu.qualimaster.algorithms.imp.correlation.Preprocessor");
        alg = mapping.getAlgorithm("Preprocessor");
        Assert.assertNotNull(alg);
        Assert.assertEquals(0, alg.getComponents().size());

        assertParameter(mapping, "FinancialCorrelation", "windowSize",  "SubTopology_FamilyElement0"); 
        assertParameter(mapping, "FinancialCorrelation", "windowSize1", "FinancialCorrelation");
    }

    /**
     * Tests reading the name mapping for <code>genSubTopoMapping2.xml</code>.
     * 
     * @throws IOException in case of I/O problems, shall not occur / test failure
     */
    @Test
    public void testGenSubTopoMapping2() throws IOException {
        NameMapping mapping = readNameMapping("genSubTopoMapping2.xml", "PriorityPip");
        Assert.assertEquals("PriorityPip", mapping.getPipelineName());

        assertNodeComponent(mapping, "FinancialDataSource", 
            "eu.qualimaster.PriorityPip.topology.PriorityPip_Source0Source", "PriorityPip", Component.Type.SOURCE);
        assertNodeComponent2(mapping, "FinancialDataSource", "PriorityPip_Source0", true);
        assertNodeComponent3(mapping, "FinancialDataSource", false, 1);

        assertNodeComponent(mapping, "FinancialCorrelation", 
            "eu.qualimaster.PriorityPip.topology.PriorityPip_FamilyElement0FamilyElement", "PriorityPip", 
            Component.Type.FAMILY);
        assertNodeComponent2(mapping, "FinancialCorrelation", "PriorityPip_FamilyElement0", true);
        assertNodeComponent3(mapping, "FinancialCorrelation", false, 1, "CorrelationSW");
        // TEST ALTERNATIVES
        
        assertNodeComponent(mapping, "Preprocessor", 
            "eu.qualimaster.PriorityPip.topology.PriorityPip_FamilyElement1FamilyElement", "PriorityPip", 
            Component.Type.FAMILY);
        assertNodeComponent2(mapping, "Preprocessor", "PriorityPip_FamilyElement1", false);
        assertNodeComponent3(mapping, "Preprocessor", false, 1, "Preprocessor");
        
        assertAlgorithm(mapping, "CorrelationSW", "CorrelationSWSubTopology", 
            "eu.qualimaster.CorrelationSW.topology.CorrelationSWSubTopology");
        Algorithm alg = mapping.getAlgorithm("CorrelationSW");
        Assert.assertNotNull(alg);
        Assert.assertEquals(2, alg.getComponents().size());

        assertNodeComponent(mapping, "Mapper", 
            "eu.qualimaster.CorrelationSW.topology.SubTopology_FamilyElement0FamilyElement", "CorrelationSW", 
            Component.Type.FAMILY);
        assertNodeComponent(alg.getComponents().get(0), 
            "eu.qualimaster.CorrelationSW.topology.SubTopology_FamilyElement0FamilyElement", "CorrelationSW", 
            Component.Type.FAMILY);
        assertNodeComponent2(alg.getComponents().get(0), "CorrelationSWMapper", true);
        assertNodeComponent3(alg.getComponents().get(0), false, 1);
        
        assertNodeComponent(mapping, "HayashiYoshida", 
            "eu.qualimaster.CorrelationSW.topology.SubTopology_FamilyElement1FamilyElement", "CorrelationSW", 
            Component.Type.FAMILY);
        assertNodeComponent(alg.getComponents().get(1), 
            "eu.qualimaster.CorrelationSW.topology.SubTopology_FamilyElement1FamilyElement", "CorrelationSW", 
            Component.Type.FAMILY);
        assertNodeComponent2(alg.getComponents().get(1), "CorrelationSWHayashiYoshida", false);
        assertNodeComponent3(alg.getComponents().get(1), false, 13);

        assertAlgorithm(mapping, "Preprocessor", "Preprocessor", 
            "eu.qualimaster.algorithms.imp.correlation.Preprocessor");
        alg = mapping.getAlgorithm("Preprocessor");
        Assert.assertNotNull(alg);
        Assert.assertEquals(0, alg.getComponents().size());

        assertParameter(mapping, "FinancialCorrelation", "windowSize", "CorrelationSWMapper"); // TODO replace by Mapper
        assertParameter(mapping, "FinancialCorrelation", "windowSize1", "FinancialCorrelation");
    }

    /**
     * Test two identical mapping files except for a nested pipeline element.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testComparison() throws IOException {
        NameMapping mapping1 = readNameMapping("mappingComparison/mapping1.xml", "TestPip1473329124467");
        NameMapping mapping2 = readNameMapping("mappingComparison/mapping2.xml", "TestPip1473329124467");
        
        // not all considered by now
        Assert.assertEquals(mapping1.getPipelineName(), mapping2.getPipelineName());
        Assert.assertEquals(mapping1.getContainerName(), mapping2.getContainerName());
        assertEqualsBySet(mapping1.getAlgorithms(), mapping2.getAlgorithms());
        assertEqualsBySet(mapping1.getComponents(), mapping2.getComponents());
        assertEqualsBySet(mapping1.getContainerNames(), mapping2.getContainerNames());
        // not sub-pipelines
    }

    /**
     * Asserts equality of collections through converting them to sets.
     * 
     * @param <T> the element type
     * @param expected the expected value
     * @param actual the actual value
     */
    private <T> void assertEqualsBySet(Collection<T> expected, Collection<T> actual) {
        Set<T> e = new HashSet<T>();
        e.addAll(expected);
        Set<T> a = new HashSet<T>();
        a.addAll(actual);
        Assert.assertEquals(e, a);
    }
}
