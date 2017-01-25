/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
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
package tests.eu.qualimaster.monitoring.genTopo;

import java.util.Map;

import org.junit.Assert;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;

/**
 * Reusable topology base class.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractTopology {

    /**
     * Creates the topology.
     * 
     * @param config the pipeline configuration
     * @param builder the topology builder
     * @return the monitoring event to be executed when the topology name mapping is registered, may be <b>null</b>
     */
    public SubTopologyMonitoringEvent createTopology(@SuppressWarnings("rawtypes") Map config, 
        RecordingTopologyBuilder builder) {
        Config cfg = new Config();
        for (Object o : config.entrySet()) {
            if (o instanceof Map.Entry) {
                @SuppressWarnings("rawtypes")
                Map.Entry ent = (Map.Entry) o;
                cfg.put(ent.getKey().toString(), ent.getValue());
            }
        }
        return createTopology(cfg, builder);
    }
    
    /**
     * Creates the topology.
     * 
     * @param config the pipeline configuration
     * @param builder the topology builder
     * @return the monitoring event to be executed when the topology name mapping is registered, may be <b>null</b>
     */
    public abstract SubTopologyMonitoringEvent createTopology(Config config, RecordingTopologyBuilder builder);

    /**
     * Returns the name of the topology / pipeline.
     * 
     * @return the name
     */
    public abstract String getName();
    
    /**
     * Asserts the results of the test.
     * 
     * @param state the (preserved) system state to assert
     * @param mapping the (preserved) name mapping
     * @param pipRunTime the (rough) runtime in ms of the executed pipeline
     */
    public abstract void assertState(SystemState state, INameMapping mapping, long pipRunTime);
    
    // checkstyle: stop exception type check

    /**
     * Creates a standalone topology.
     * 
     * @param args the topology arguments
     * @param topo the topology instance
     * @throws Exception in case of creation problems
     */
    public static void main(String[] args, AbstractTopology topo) throws Exception {
        Config config = new Config();
        config.setMessageTimeoutSecs(100);
        PipelineOptions options = new PipelineOptions(args);
        RecordingTopologyBuilder b = new RecordingTopologyBuilder(options);
        topo.createTopology(config, b);
        
        // main topology: int numWorkers = options.getNumberOfWorkers(2);
        options.toConf(config);
        
        if (args != null && args.length > 0) {
            config.setNumWorkers(2);
            StormSubmitter.submitTopology(args[0], config, b.createTopology());
        } else {
            config.setMaxTaskParallelism(2);
            final LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("testGenPip", config, b.createTopology());
        }
    }

    // checkstyle: resume exception type check

    /**
     * Returns the mapping file name.
     * 
     * @return the mapping file
     */
    public abstract String getMappingFileName();

    /**
     * Asserts that the observed value is greater than <code>expected</code>.
     * 
     * @param expected the expected value
     * @param part the part to get the actual value from
     * @param obs the observable denoting the actual value
     */
    protected static void assertGreater(double expected, PipelineNodeSystemPart part, IObservable obs) {
        Assert.assertTrue("no value for " + obs + " on " + part, part.hasValue(obs));
        Assert.assertTrue("not greater " + part, expected < part.getObservedValue(obs));
    }
    
    /**
     * Registers sub-topologies.
     * 
     * @param info topology information to be modified as a side effect
     */
    public void registerSubTopologies(Map<String, TopologyTestInfo> info) {
    }
    
    /**
     * Notifies the topology that it has been started.
     */
    public void started() {
    }
    
    /**
     * Returns the planned execution time.
     * 
     * @return the planned execution time in ms (by default 10000)
     */
    public int plannedExecutionTime() {
        return 10000;
    }
    
    /**
     * Asserts the <code>expected</code> value for the given <code>part</code> and <code>observable</code> within
     * a given tolerance range around <code>expected</code>.
     * 
     * @param expected the expected value
     * @param part the system part to check
     * @param observable the observable to check
     * @param absTolerance the absolute tolerance in (0-1)
     */
    protected void assertValue(double expected, SystemPart part, IObservable observable, double absTolerance) {
        Double actual = part.getObservedValue(observable);
        Assert.assertNotNull(actual);
        double tolerance = expected * absTolerance;
        Assert.assertTrue(actual + " for " + observable + " not within " + (absTolerance * 100) + "%/" + tolerance 
            + " tolerance around " + expected + " - " + part, 
            expected - tolerance <= actual && actual <= expected + tolerance);
    }

    /**
     * Asserts that <code>min</code> is less or equal than the value of <code>part</code> and <code>observable</code>.
     * 
     * @param min the minimum expected value to pass
     * @param part the system part to check
     * @param observable the observable to check
     */
    protected void assertGreaterEquals(double min, SystemPart part, IObservable observable) {
        Double actual = part.getObservedValue(observable);
        Assert.assertNotNull(actual);
        Assert.assertTrue("not " + actual + ">=" + min, actual >= min);
    }
    
    /**
     * Asserts that <code>min</code> is less or equal than the value of <code>part</code> and <code>observable</code>.
     * 
     * @param expected the expected value to pass
     * @param part the system part to check
     * @param observable the observable to check
     * @param tolerance the tolerance in comparison
     */
    protected void assertEquals(double expected, SystemPart part, IObservable observable, double tolerance) {
        Double actual = part.getObservedValue(observable);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual.doubleValue(), tolerance);
    }

    /**
     * Returns a pipeline node for a given implementation name.
     * 
     * @param name the name
     * @param pip the pipeline
     * @param mapping the name mapping (<b>null</b> for no mapping if <code>name</code> is already a logical name)
     * @param assertExistence assert the existence of the node or not
     * @return the node
     */
    protected PipelineNodeSystemPart getNode(String name, PipelineSystemPart pip, INameMapping mapping, 
        boolean assertExistence) {
        String tmp = name;
        if (null != mapping) {
            tmp = mapping.getPipelineNodeByImplName(name);
            Assert.assertNotNull(tmp);
        }
        PipelineNodeSystemPart node = pip.getNode(tmp);
        if (assertExistence) {
            Assert.assertNotNull(node);
        }
        return node;
    }
    
    /**
     * Returns a pipeline node for a given implementation name.
     * 
     * @param name the name
     * @param pip the pipeline
     * @param mapping the name mapping (<b>null</b> for no mapping if <code>name</code> is already a logical name)
     * @param assertExistence assert the existence of the node or not
     * @return the node
     */
    protected PipelineNodeSystemPart getNode(String name, PipelineNodeSystemPart pip, INameMapping mapping, 
        boolean assertExistence) {
        String tmp = name;
        if (null != mapping) {
            tmp = mapping.getPipelineNodeByImplName(name);
            Assert.assertNotNull(tmp);
        }
        PipelineNodeSystemPart node = pip.getNode(tmp);
        if (assertExistence) {
            Assert.assertNotNull(node);
        }
        return node;
    }

    /**
     * Returns a pipeline node for a given implementation name.
     * 
     * @param name the name
     * @param pip the pipeline
     * @param mapping the name mapping (<b>null</b> for no mapping if <code>name</code> is already a logical name)
     * @param assertExistence assert the existence of the node or not
     * @return the algorithm
     */
    protected NodeImplementationSystemPart getAlgorithm(String name, PipelineSystemPart pip, INameMapping mapping, 
        boolean assertExistence) {
        String tmp = name;
        if (null != mapping) {
            Algorithm alg = mapping.getAlgorithmByImplName(name);
            Assert.assertNotNull(alg);
            tmp = alg.getName();
        }
        NodeImplementationSystemPart node = pip.getAlgorithm(tmp);
        if (assertExistence) {
            Assert.assertNotNull(node);
        }
        return node;
    }
    
    /**
     * Cares for the pipeline options.
     * 
     * @param conf the Storm configuration options to be passed to the topology
     * @param opt the pipeline options
     */
    @SuppressWarnings("rawtypes")
    public void handleOptions(Map conf, PipelineOptions opt) {
        // typically empty
    }
    
    /**
     * Returns whether the generic end-of-data event handler shall be installed.
     * 
     * @return <code>true</code> for installing, <code>false<</code> else (default)
     */
    public boolean installGenericEoDEventHandler() {
        return false;
    }

}
