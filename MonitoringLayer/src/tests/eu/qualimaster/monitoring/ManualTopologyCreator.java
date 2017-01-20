/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tests.eu.qualimaster.monitoring;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import backtype.storm.generated.ExecutorInfo;
import backtype.storm.generated.ExecutorSummary;
import backtype.storm.generated.StormTopology;
import backtype.storm.generated.TopologyInfo;
import eu.qualimaster.base.algorithm.IMainTopologyCreate;
import eu.qualimaster.base.algorithm.TopologyOutput;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.storm.Utils;

/**
 * Just a helper class for testing. This class contains intentionally fixed strings.
 * 
 * @author Holger Eichelberger
 */
public class ManualTopologyCreator {

    /**
     * Runs the creator.
     * 
     * @throws MalformedURLException in case of a malformed URL
     * @throws ClassNotFoundException in case that the topology class was not found
     * @throws IllegalAccessException in case that the topology class cannot be accessed
     * @throws InstantiationException in case that the topology class cannot be instantiated
     * @throws IOException in case of I/O problems
     */
    public static void createTopology() throws MalformedURLException, ClassNotFoundException, 
        IllegalAccessException, InstantiationException, IOException {
        File file = new File("H:\\Downloads\\SwitchPip.jar"); // -0.0.2-20160226.182014-1850
        URL[] urls = new URL[1];
        urls[0] = file.toURI().toURL();
        System.out.println(urls[0]);
        URLClassLoader loader = new URLClassLoader(urls);
        String pipelineName = "SwitchPip";
        InputStream tmp = urls[0].openStream();
        tmp.close();
        Class<?> cls = loader.loadClass("eu.qualimaster." + pipelineName + ".topology.Topology$MainTopologyCreator");
        if (IMainTopologyCreate.class.isAssignableFrom(cls)) {
            IMainTopologyCreate create = (IMainTopologyCreate) cls.newInstance();
            TopologyOutput output = create.createMainTopology();
            StormTopology topology = output.getBuilder().createTopology();
            URL mappingURL = new URL("jar:" + urls[0] + "!/mapping.xml");
            InputStream in = mappingURL.openStream();
            NameMapping mapping = new NameMapping(pipelineName, in);
            
            Map<String, List<String>> structure = new HashMap<String, List<String>>();
            structure.put("SimpleStateTransferSW", split(
                "Switch1IntermediarySpout;eu.qualimaster.test.algorithms.IntermediarySpoutSW, "
                + "Switch1ProcessBolt;eu.qualimaster.test.algorithms.ProcessBolt"));
            structure.put("SimpleStateTransferSW2", split(
                "Switch2IntermediarySpout;eu.qualimaster.test.algorithms.IntermediarySpoutSW2, "
                + "Switch2ProcessBolt;eu.qualimaster.test.algorithms.ProcessBoltSW2"));
            mapping.considerSubStructures(new SubTopologyMonitoringEvent(pipelineName, structure, null));
            in.close();
            TopologyInfo topoInfo = new TopologyInfo();
            List<String> executors = new ArrayList<String>();
            for (Component c : mapping.getComponents()) {
                executors.add(c.getName());
            }
            executors.add("Switch1ProcessBolt");
            executors.add("Switch2ProcessBolt");
            executors.add("Switch1IntermediarySpout");
            executors.add("Switch2IntermediarySpout");
            addExecutors(topoInfo, executors);
            System.out.println(mapping.getComponents());
            PipelineTopology topo = Utils.buildPipelineTopology(topology, topoInfo, mapping);
            System.out.println(topo);
        }
        loader.close();
    }
    
    /**
     * Represents a loaded topology.
     * 
     * @author Holger Eichelberger
     */
    public static class TopologyDescriptor {
        private TopologyInfo topoInfo;
        private INameMapping mapping;
        private StormTopology topology;

        /**
         * Returns the Storm topology information.
         * 
         * @return the topology information
         */
        public TopologyInfo getTopologyInfo() {
            return topoInfo;
        }
        
        /**
         * Returns the name mapping.
         * 
         * @return the name mapping
         */
        public INameMapping getNameMapping() {
            return mapping;
        }
        
        /**
         * Returns the Storm topology.
         * 
         * @return the topology
         */
        public StormTopology getTopology() {
            return topology;
        }
        
        /**
         * Creates the internal monitoring topology for the loaded topology.
         * 
         * @return the internal monitoring topology
         */
        public PipelineTopology createTopology() {
            return Utils.buildPipelineTopology(topology, topoInfo, mapping);
        }
        
    }

    /**
     * Loads a topology and returns a related descriptor.
     * 
     * @param jar the JAR file implementing the pipeline
     * @param pipelineName the name of the pipeline
     * @param options the pipeline options to use 
     * @return the corresponding topology descriptor
     * 
     * @throws MalformedURLException in case of a malformed URL
     * @throws ClassNotFoundException in case that the topology class was not found
     * @throws IllegalAccessException in case that the topology class cannot be accessed
     * @throws InstantiationException in case that the topology class cannot be instantiated
     * @throws IOException in case of I/O problems
     * @throws NoSuchFieldException if the options field in the topology creator does not exist
     */
    public static TopologyDescriptor loadTopology(File jar, String pipelineName, PipelineOptions options) 
        throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, 
        InstantiationException {
        TopologyDescriptor result = new TopologyDescriptor();
        String topoClsName = "eu.qualimaster." + pipelineName + ".topology.Topology";
        URLClassLoader urlLoader;
        ClassLoader loader;
        URL[] urls = new URL[1];
        urls[0] = jar.toURI().toURL();
        try {
            Class.forName(topoClsName);
            loader = ManualTopologyCreator.class.getClassLoader();
            urlLoader = null;
        } catch (ClassNotFoundException e) {
            System.out.println(urls[0]);
            urlLoader = new URLClassLoader(urls);
            loader = urlLoader;
        }
        
        Class<?> mCls = loader.loadClass(topoClsName);
        Class<?> cls = loader.loadClass(topoClsName + "$MainTopologyCreator");
        if (IMainTopologyCreate.class.isAssignableFrom(cls)) {
            Field fld = mCls.getDeclaredField("options");
            fld.setAccessible(true);
            fld.set(null, options);

            EventManager.start(false, true);

            URL mappingURL = new URL("jar:" + urls[0] + "!/mapping.xml");
            InputStream in = mappingURL.openStream();
            result.mapping = new NameMapping(pipelineName, in);
            in.close();
            System.out.println(result.mapping);            

            SubTopologyStructureEventHandler handler = new SubTopologyStructureEventHandler(result.mapping);
            EventManager.register(handler);

            IMainTopologyCreate create = (IMainTopologyCreate) cls.newInstance();
            TopologyOutput output = create.createMainTopology();
            result.topology = output.getBuilder().createTopology();
            EventManager.cleanup();
            
            result.topoInfo = new TopologyInfo();
            List<String> executors = new ArrayList<String>();
            for (Component c : result.mapping.getComponents()) {
                System.out.println("Faking " + c.getName());
                executors.add(c.getName());
            }
            for (Algorithm a : result.mapping.getAlgorithms()) {
                for (Component c : a.getComponents()) {
                    System.out.println("Faking " + c.getName());
                    executors.add(c.getName());
                }
            }
            executors.addAll(handler.executors);
            addExecutors(result.topoInfo, executors);

            EventManager.stop();
            EventManager.unregister(handler);
        }
        if (null != urlLoader) {
            urlLoader.close();
        }
        return result;
    }
    
    /**
     * Tests a topology creation.
     * 
     * @param args ignored
     * 
     * @throws MalformedURLException in case of a malformed URL
     * @throws ClassNotFoundException in case that the topology class was not found
     * @throws IllegalAccessException in case that the topology class cannot be accessed
     * @throws InstantiationException in case that the topology class cannot be instantiated
     * @throws IOException in case of I/O problems
     * @throws NoSuchFieldException if the options field in the topology creator does not exist
     */
    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, 
        IllegalAccessException, InstantiationException, IOException, NoSuchFieldException {
        createTightTopology();
        //createHwProfilingTopology();
    }
    
    /**
     * A handler for sub-topology structure events. Fills the name mapping with relevant information from 
     * a hand-crafted sub-topology and collects the reported executors.
     * 
     * @author Holger Eichelberger
     */
    private static class SubTopologyStructureEventHandler extends EventHandler<SubTopologyMonitoringEvent> {

        private INameMapping mapping;
        private List<String> executors = new ArrayList<String>();

        /**
         * Creates the handler based on a given name mapping.
         * 
         * @param mapping the name mapping
         */
        protected SubTopologyStructureEventHandler(INameMapping mapping) {
            super(SubTopologyMonitoringEvent.class);
            this.mapping = mapping;
        }

        @Override
        protected void handle(SubTopologyMonitoringEvent event) {
            mapping.considerSubStructures(event);
            Map<String, List<String>> struct = event.getStructure();
            for (Map.Entry<String, List<String>> ent : struct.entrySet()) {
                for (String e : ent.getValue()) {
                    String[] tmp = e.split(SubTopologyMonitoringEvent.SEPARATOR);
                    if (null != tmp && 2 == tmp.length) {
                        executors.add(tmp[0]);
                    }
                }
            }
        }
        
    }

    /**
     * Loads the HW profiling topology.
     * 
     * @throws MalformedURLException in case of a malformed URL
     * @throws ClassNotFoundException in case that the topology class was not found
     * @throws IllegalAccessException in case that the topology class cannot be accessed
     * @throws InstantiationException in case that the topology class cannot be instantiated
     * @throws IOException in case of I/O problems
     * @throws NoSuchFieldException if the options field in the topology creator does not exist
     */
    public static void createHwProfilingTopology() throws MalformedURLException, ClassNotFoundException, 
        IllegalAccessException, InstantiationException, IOException, NoSuchFieldException {
        File model = new File("W:\\runtime-EclipseApplication26\\QM2.devel");
        Properties prop = new Properties();
        prop.put(MonitoringConfiguration.LOCAL_CONFIG_MODEL_ARTIFACT_LOCATION, model.getAbsolutePath());
        MonitoringConfiguration.configure(prop);

        String mainTopoName = "ProfileTestPip";
        File file = new File("C:\\Users\\eichelbe\\Desktop\\tmp\\tsi\\ProfileTestPip.jar");
        PipelineOptions mainTopoOptions = new PipelineOptions();
        TopologyDescriptor mainTopoD = loadTopology(file, mainTopoName, mainTopoOptions);
        
        PipelineTopology topo = mainTopoD.createTopology();
        System.out.println(topo);
    }
    
    /**
     * Creates Cui's loose topology. 
     * 
     * @throws MalformedURLException in case of a malformed URL
     * @throws ClassNotFoundException in case that the topology class was not found
     * @throws IllegalAccessException in case that the topology class cannot be accessed
     * @throws InstantiationException in case that the topology class cannot be instantiated
     * @throws IOException in case of I/O problems
     * @throws NoSuchFieldException if the options field in the topology creator does not exist
     */
    public static void createLooseTopology() throws MalformedURLException, ClassNotFoundException, 
        IllegalAccessException, InstantiationException, IOException, NoSuchFieldException {
        File model = new File("C:\\Users\\eichelbe\\Desktop\\tmp\\tsi\\infrastructure_model_cui");
        Properties prop = new Properties();
        prop.put(MonitoringConfiguration.LOCAL_CONFIG_MODEL_ARTIFACT_LOCATION, model.getAbsolutePath());
        MonitoringConfiguration.configure(prop);
        
        String mainTopoName = "RandomPip";
        File file = new File("C:\\Users\\eichelbe\\Desktop\\tmp\\tsi\\RandomPip.jar.loose");
        PipelineOptions mainTopoOptions = new PipelineOptions();
        TopologyDescriptor mainTopoD = loadTopology(file, mainTopoName, mainTopoOptions);
    
        file = new File("C:\\Users\\eichelbe\\Desktop\\tmp\\tsi\\RandomSubPipeline1.jar.loose.switch");
        PipelineOptions subTopo1Options = new PipelineOptions();
        subTopo1Options.markAsSubPipeline(mainTopoName);
        TopologyDescriptor subTopo1D = loadTopology(file, "RandomSubPipeline1", subTopo1Options);
    
        file = new File("C:\\Users\\eichelbe\\Desktop\\tmp\\tsi\\RandomSubPipeline2.jar.loose.switch");
        PipelineOptions subTopo2Options = new PipelineOptions();
        subTopo1Options.markAsSubPipeline(mainTopoName);
        TopologyDescriptor subTopo2D = loadTopology(file, "RandomSubPipeline2", subTopo2Options);
        
        // create over pipelines!
        PipelineTopology topoD = createTopology(mainTopoD, subTopo1D, subTopo2D);
        System.out.println(topoD);
    }

    /**
     * Creates Andreas's tight topology. 
     * 
     * @throws MalformedURLException in case of a malformed URL
     * @throws ClassNotFoundException in case that the topology class was not found
     * @throws IllegalAccessException in case that the topology class cannot be accessed
     * @throws InstantiationException in case that the topology class cannot be instantiated
     * @throws IOException in case of I/O problems
     * @throws NoSuchFieldException if the options field in the topology creator does not exist
     */
    public static void createTightTopology() throws MalformedURLException, ClassNotFoundException, 
        IllegalAccessException, InstantiationException, IOException, NoSuchFieldException {

        //File model = new File("C:\\Users\\eichelbe\\Desktop\\tmp\\tsi\\infrastructure_model_cui");
        File model = new File("W:\\runtime-EclipseApplication26\\QM2.devel");
        Properties prop = new Properties();
        prop.put(MonitoringConfiguration.LOCAL_CONFIG_MODEL_ARTIFACT_LOCATION, model.getAbsolutePath());
        MonitoringConfiguration.configure(prop);

        String mainTopoName = "RandomPip";
        //File file = new File("C:\\Users\\eichelbe\\Desktop\\tmp\\tsi\\RandomPip.jar.loose");
        File file = new File("W:\\runtime-EclipseApplication26\\QM2.devel\\pipelines\\eu\\qualimaster\\RandomPip"
            + "\\target\\RandomPip-0.0.1-SNAPSHOT-jar-with-dependencies.jar");
        PipelineOptions mainTopoOptions = new PipelineOptions();
        TopologyDescriptor mainTopoD = loadTopology(file, mainTopoName, mainTopoOptions);
        Map<String, List<String>> structure = new HashMap<String, List<String>>();
        
        structure.put("RandomProcessor2", split(
            //"RandomProcessor2Intermediary;eu.qualimaster.RandomPip.topology.RandomProcessor2Intermediary, "
            //+ "RandomProcessor2processor2;eu.qualimaster.algorithms.Process2Bolt, "
            //+ "RandomProcessor2EndBolt;eu.qualimaster.RandomPip.topology.RandomProcessor2EndBolt"
            "RandomProcessor2processor2;eu.qualimaster.algorithms.Process2Bolt"
            ));
        structure.put("RandomProcessor1", split(
            //"RandomProcessor1Intermediary;eu.qualimaster.RandomPip.topology.RandomProcessor1Intermediary, "
            //+ "RandomProcessor1processor1;eu.qualimaster.algorithms.Process1Bolt, "
            //+ "RandomProcessor1EndBolt;eu.qualimaster.RandomPip.topology.RandomProcessor1EndBolt"
            "RandomProcessor1processor1;eu.qualimaster.algorithms.Process1Bolt"
            ));
        mainTopoD.mapping.considerSubStructures(new SubTopologyMonitoringEvent(mainTopoName, structure, null));
        
        List<String> executors = new ArrayList<String>();
        executors.add("RandomSubPipelineAlgorithm1DataProcessor");
        executors.add("RandomSubPipelineAlgorithm1Intermediary");
        executors.add("RandomSubPipelineAlgorithm1EndBolt");
        executors.add("RandomSubPipelineAlgorithm2DataProcessor");
        executors.add("RandomSubPipelineAlgorithm2Intermediary");
        executors.add("RandomSubPipelineAlgorithm2EndBolt");        
        addExecutors(mainTopoD.topoInfo, executors);
        
        PipelineTopology topo = mainTopoD.createTopology();
        System.out.println(topo);
    }

    /**
     * Creates a topology from multiple topology descriptors.
     * 
     * @param main the main topology descriptor
     * @param sub the sub topology descriptors
     * @return the unified pipeline topology
     */
    private static PipelineTopology createTopology(TopologyDescriptor main, TopologyDescriptor... sub) {
        Map<StormTopology, TopologyInfo> topologies = new HashMap<StormTopology, TopologyInfo>();
        topologies.put(main.topology, main.topoInfo);
        for (TopologyDescriptor s : sub) {
            topologies.put(s.topology, s.topoInfo);
        }
        return Utils.buildPipelineTopology(topologies, main.mapping);
    }

    /**
     * Splits a sub-topology monitoring event string into a list.
     * 
     * @param string the string to be splitted
     * @return the splitted list
     */
    private static List<String> split(String string) {
        String[] tmp = string.split(", ");
        ArrayList<String> result = new ArrayList<String>();
        for (String t : tmp) {
            result.add(t);
        }
        return result;
    }
    
    /**
     * Adds pseudo-executors for testing to <code>topoInfo</code>. Avoids adding already known executors twice.
     * 
     * @param topoInfo the information instance to be modified
     * @param executors the names of the executors
     */
    private static void addExecutors(TopologyInfo topoInfo, List<String> executors) {
        Set<String> known = new HashSet<String>();
        if (topoInfo.get_executors_size() > 0) {
            for (ExecutorSummary e : topoInfo.get_executors()) {
                known.add(e.get_component_id()); 
            }
        }
        for (String n : executors) {
            if (!known.contains(n)) {
                topoInfo.add_to_executors(new ExecutorSummary(new ExecutorInfo(1, 1), n, "localhost", 1234, 10));
            }
        }
    }

}