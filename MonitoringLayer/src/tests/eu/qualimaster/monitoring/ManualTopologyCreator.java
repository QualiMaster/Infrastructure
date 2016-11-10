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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtype.storm.generated.ExecutorInfo;
import backtype.storm.generated.ExecutorSummary;
import backtype.storm.generated.StormTopology;
import backtype.storm.generated.TopologyInfo;
import eu.qualimaster.base.algorithm.IMainTopologyCreate;
import eu.qualimaster.base.algorithm.TopologyOutput;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.coordination.INameMapping.Component;
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
     * @param args currently ignored
     * @throws MalformedURLException in case of a malformed URL
     * @throws ClassNotFoundException in case that the topology class was not found
     * @throws IllegalAccessException in case that the topology class cannot be accessed
     * @throws InstantiationException in case that the topology class cannot be instantiated
     * @throws IOException in case of I/O problems
     */
    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, 
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
            createExecutors(topoInfo, executors);
            System.out.println(mapping.getComponents());
            PipelineTopology topo = Utils.buildPipelineTopology(topology, topoInfo, mapping);
            System.out.println(topo);
        }
        loader.close();
    }
    
    /**
     * Splits a sub-topology monitoring event string into a list.
     * 
     * @param string the string to be splitted
     * @return the spliited list
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
     * Create pseudo-executors for testing.
     * 
     * @param topoInfo the information instance to be modified
     * @param executors the names of the executors
     */
    private static void createExecutors(TopologyInfo topoInfo, List<String> executors) {
        for (String n : executors) {
            topoInfo.add_to_executors(new ExecutorSummary(new ExecutorInfo(1, 1), n, "localhost", 1234, 10));
        }
    }

}
