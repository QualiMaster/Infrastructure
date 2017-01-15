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
package tests.eu.qualimaster.monitoring.profiling;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.IdentityMapping;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.ParameterChangedMonitoringEvent;
import eu.qualimaster.monitoring.profiling.AlgorithmProfilePredictionManager;
import eu.qualimaster.monitoring.profiling.IAlgorithmProfile;
import eu.qualimaster.monitoring.profiling.MapFile;
import eu.qualimaster.monitoring.profiling.Pipeline;
import eu.qualimaster.monitoring.profiling.PipelineElement;
import eu.qualimaster.monitoring.profiling.Pipelines;
import eu.qualimaster.monitoring.profiling.ProfilingRegistry;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.StateUtils;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;
import tests.eu.qualimaster.monitoring.genTopo.TestProcessor;

/**
 * Tests the prediction manager (black-box test on component level).
 * 
 * @author Holger Eichelberger
 */
public class ManagerTest {

    private static final IObservable[] OBSERVABLES = new IObservable[] {TimeBehavior.LATENCY, 
        TimeBehavior.THROUGHPUT_ITEMS, Scalability.ITEMS};
    private File testFolder = new File(FileUtils.getTempDirectory(), "profilingTest");
    private INameMapping mapping;

    /**
     * Prepares a test.
     */
    @Before
    public void before() {
        mapping = new IdentityMapping("pip");
        CoordinationManager.registerTestMapping(mapping);
        AlgorithmProfilePredictionManager.clear();
        FileUtils.deleteQuietly(testFolder);
        defaultPredictionSteps();
        testFolder.mkdirs();
        AlgorithmProfilePredictionManager.useTestData(testFolder.getAbsolutePath());
    }
    
    /**
     * Cleans up after a test.
     */
    @After
    public void after() {
        AlgorithmProfilePredictionManager.useTestData(null);
        defaultPredictionSteps();
        FileUtils.deleteQuietly(testFolder);
        AlgorithmProfilePredictionManager.clear();
        CoordinationManager.unregisterNameMapping(mapping);
    }
    
    /**
     * Just test whether something fails in the lifecylce.
     */
    @Test
    public void testManagerLifecycle() {
        AlgorithmProfilePredictionManager.start();
        AlgorithmProfilePredictionManager.stop();
        // indeed, no asserts here
    }
    
    /**
     * Tests the calls related to a default pipeline lifecycle - just whether something obvious fails.
     */
    @Test
    public void testManagerPipelineLifecycle() {
        testLifecycle(false, 0);
        FileUtils.deleteQuietly(testFolder);
        testLifecycle(false, 1);
    }

    /**
     * Tests the calls related to a profiling pipeline lifecycle - just whether something obvious fails.
     */
    @Test
    public void testManagerProfilingPipelineLifecycle() {
        testLifecycle(true, 0);
        FileUtils.deleteQuietly(testFolder);
        testLifecycle(true, 1);
    }

    /**
     * Represents a testing pipeline just for simulation.
     * 
     * @author Holger Eichelberger
     */
    private class PipelineDescriptor {
        private final String pipeline = "pip";
        private final String source = "src";
        private final String srcAlgorithm = "srcAlg";
        private final String family = "fam";
        private final String algorithm = "alg";
        private final String paramKey = "key";
        private final String paramKeyValue = "myKey";
        private final String paramWindow = "window";
        private final Integer paramWindowValue = 1000;
        
        private SystemState state;
        private PipelineSystemPart pip;
        private PipelineNodeSystemPart elt;
        private PipelineNodeSystemPart src;
        private NodeImplementationSystemPart impl;
        private long lastUpdate;
        private boolean profiling;
        
        /**
         * Creates a pipeline descriptor.
         * 
         * @param profiling are we in profiling mode?
         */
        private PipelineDescriptor(boolean profiling) {
            this.profiling = profiling;
        }
        
        /**
         * Simulates a profiling event.
         * 
         * @param status the new profiling status
         * @return <code>status</code>
         */
        private AlgorithmProfilingEvent.Status sendProfilingEvent(AlgorithmProfilingEvent.Status status) {
            AlgorithmProfilePredictionManager.notifyAlgorithmProfilingEvent(
                new AlgorithmProfilingEvent(pipeline, family, algorithm, status, null));
            return status;
        }
        
        /**
         * Simulates starting the pipeline.
         */
        private void start() {
            AlgorithmProfilePredictionManager.notifyPipelineLifecycleChange(
                new PipelineLifecycleEvent(pipeline, PipelineLifecycleEvent.Status.CHECKING, null));
            AlgorithmProfilePredictionManager.notifyPipelineLifecycleChange(
                new PipelineLifecycleEvent(pipeline, PipelineLifecycleEvent.Status.CHECKED, null));
            AlgorithmProfilePredictionManager.notifyPipelineLifecycleChange(
                new PipelineLifecycleEvent(pipeline, PipelineLifecycleEvent.Status.INITIALIZED, null));
            AlgorithmProfilePredictionManager.notifyPipelineLifecycleChange(
                new PipelineLifecycleEvent(pipeline, PipelineLifecycleEvent.Status.CREATED, null));
            AlgorithmProfilePredictionManager.notifyPipelineLifecycleChange(
                 new PipelineLifecycleEvent(pipeline, PipelineLifecycleEvent.Status.STARTING, null));

            TestProcessor pSrc = new TestProcessor(source);
            TestProcessor pFam = new TestProcessor(family);
            Stream sSrcFam = new Stream("inStream", pSrc, pFam);
            pSrc.setOutputs(sSrcFam);
            pFam.setInputs(sSrcFam);
            List<Processor> procs = new ArrayList<Processor>();
            procs.add(pSrc);
            procs.add(pFam);
            PipelineTopology topology = new PipelineTopology(procs);
            
            state = new SystemState();
            pip = state.obtainPipeline(pipeline);
            pip.setTopology(topology);
            src = pip.obtainPipelineNode(source);
            elt = pip.obtainPipelineNode(family);
            impl = pip.getAlgorithm(algorithm);
            elt.setCurrent(impl);

            AlgorithmProfilePredictionManager.notifyAlgorithmChanged(
                new AlgorithmChangedMonitoringEvent(pipeline, source, srcAlgorithm));
            AlgorithmProfilePredictionManager.notifyAlgorithmChanged(
                new AlgorithmChangedMonitoringEvent(pipeline, family, algorithm));
            AlgorithmProfilePredictionManager.notifyParameterChangedMonitoringEvent(
                new ParameterChangedMonitoringEvent(pipeline, family, paramKey, paramKeyValue, null));            
            AlgorithmProfilePredictionManager.notifyParameterChangedMonitoringEvent(
                new ParameterChangedMonitoringEvent(pipeline, family, paramWindow, paramWindowValue, null));

            AlgorithmProfilePredictionManager.notifyPipelineLifecycleChange(
                new PipelineLifecycleEvent(pipeline, PipelineLifecycleEvent.Status.STARTED, null));
        }
        
        /**
         * Simulates stopping the pipeline.
         */
        private void stop() {
            AlgorithmProfilePredictionManager.notifyPipelineLifecycleChange(
                new PipelineLifecycleEvent(pipeline, PipelineLifecycleEvent.Status.STOPPING, null));
            AlgorithmProfilePredictionManager.notifyPipelineLifecycleChange(
                new PipelineLifecycleEvent(pipeline, PipelineLifecycleEvent.Status.STOPPED, null));
            state.removePipeline(pipeline);
        }

        /**
         * Simulates a monitoring event.
         * 
         * @param srcLatency the source latency
         * @param srcThroughput the source throughput
         * @param famLatency the family latency
         * @param famThroughput the family throughput
         */
        private void monitor(double srcLatency, double srcThroughput, double famLatency, double famThroughput) {
            StateUtils.setValue(src, TimeBehavior.LATENCY, srcLatency, null);
            StateUtils.setValue(src, TimeBehavior.THROUGHPUT_ITEMS, srcThroughput, null);
            StateUtils.updateCapacity(src, null, false);

            StateUtils.setValue(elt, TimeBehavior.LATENCY, famLatency, null);
            StateUtils.setValue(elt, TimeBehavior.THROUGHPUT_ITEMS, famThroughput, null);
            StateUtils.updateCapacity(elt, null, false);
            long now = System.currentTimeMillis();
            if (0 == lastUpdate || now - lastUpdate >= 1000) {
                AlgorithmProfilePredictionManager.update(pipeline, family, elt);
                lastUpdate = System.currentTimeMillis();
            }
        }
        
        /**
         * Predicts the next value of <code>observable</code>.
         * 
         * @param observable the observable
         * @param targetValues the changed context for the prediction (may be <b>null</b> for none)
         * @return the predicted value, <code>Double.MIN_VALUE</code> if no prediction is possible
         */
        private double predict(IObservable observable, Map<Object, Serializable> targetValues) {
            return AlgorithmProfilePredictionManager.predict(pipeline, family, algorithm, observable, targetValues);
        }
        
        /**
         * Returns the numeric parameter in this descriptor.
         * 
         * @return the numeric parameter
         */
        private String getNumericParameterName() {
            return paramWindow;
        }
        
        /**
         * Performs a prediction of parameter values.
         * 
         * @param parameter the parameter
         * @param targetValues the changed context for the prediction (may be <b>null</b> for none)
         * @return parameter-observable-prediction 
         */
        private Map<String, Map<IObservable, Double>> predictParameterValues(String parameter, 
            Map<Object, Serializable> targetValues) {
            Set<IObservable> obs = new HashSet<IObservable>();
            obs.add(TimeBehavior.LATENCY);
            obs.add(TimeBehavior.THROUGHPUT_ITEMS);
            return AlgorithmProfilePredictionManager.predictParameterValues(pipeline, family, parameter, 
                obs, targetValues);
        }

        /**
         * Asserts a correct pipeline structure.
         */
        private void assertPipelineStructure() {
            Pipeline pip = Pipelines.getPipeline(pipeline);
            Assert.assertNotNull(pip);
            Assert.assertEquals(pipeline, pip.getName());
            Assert.assertNotNull(pip.getProfileCreator());
            Assert.assertEquals(testFolder.getAbsolutePath(), pip.getPath());
            Assert.assertEquals(profiling, pip.isInProfilingMode());
            
            PipelineElement src = pip.getElement(source);
            Assert.assertNotNull(src);
            Assert.assertEquals(source, src.getName());
            Assert.assertNotNull(src.getProfileCreator());
            Assert.assertEquals(pip, src.getPipeline());
            Assert.assertEquals(testFolder.getAbsolutePath(), src.getPath());
            Assert.assertEquals(srcAlgorithm, src.getActiveAlgorithm());
            Assert.assertEquals(profiling, src.isInProfilingMode());
            
            PipelineElement fam = pip.getElement(family);
            Assert.assertNotNull(fam);
            Assert.assertEquals(family, fam.getName());
            Assert.assertNotNull(fam.getProfileCreator());
            Assert.assertEquals(pip, fam.getPipeline());
            Assert.assertEquals(testFolder.getAbsolutePath(), fam.getPath());
            Assert.assertEquals(algorithm, fam.getActiveAlgorithm());
            Assert.assertEquals(profiling, fam.isInProfilingMode());
        }

        /**
         * Returns the system part representing the family node.
         * 
         * @return the system part
         */
        private PipelineNodeSystemPart getFamily() {
            return elt;
        }
        
        /**
         * Asserts the storage of the algorithm profile.
         */
        private void assertStorage() {
            Pipeline pip = Pipelines.getPipeline(pipeline);
            Assert.assertNotNull(pip);
            PipelineElement fam = pip.getElement(family);
            Assert.assertNotNull(fam);
            Collection<IAlgorithmProfile> profiles = fam.profiles();
            for (IAlgorithmProfile profile : profiles) {
                for (IObservable obs : OBSERVABLES) {
                    File folder = profile.getFolder(obs);
                    Assert.assertTrue("Profile folder " + folder + " does not exist", folder.exists());
                    MapFile mapFile = new MapFile(folder);
                    for (String key : mapFile.keys()) {
                        File f = mapFile.getFile(key);
                        Assert.assertTrue("Profile " + f + " does not exist", f.exists());    
                    }
                }
            }
        }
        
        /**
         * Stores all active profiles in a pipeline.
         */
        private void storeAll() {
            Pipeline pip = Pipelines.getPipeline(pipeline);
            Assert.assertNotNull(pip);
            pip.store();
        }
        
        /**
         * Asserts the state after stopping the profiling manager.
         */
        private void assertStop() {
            Pipeline pip = Pipelines.getPipeline(pipeline);
            Assert.assertNull(pip);
        }

    }
    
    /**
     * Registers the actual prediction steps for all relevant observables.
     * 
     * @param steps the prediction steps (default number of steps if negative)
     */
    private void registerPredictionSteps(int steps) {
        // see testLifecylce
        for (IObservable obs : OBSERVABLES) {
            ProfilingRegistry.registerPredictionSteps(obs, steps);
        }
    }
    
    /**
     * Sets the relevant observables back to default prediction steps.
     */
    private void defaultPredictionSteps() {
        for (IObservable obs : OBSERVABLES) {
            ProfilingRegistry.defaultPredictionSteps(obs);
        }
    }

    /**
     * Tests the calls related to a pipeline lifecycle - just whether something obvious fails.
     * 
     * @param withProfiling pretend that profiling is running
     * @param steps the number of prediction steps
     */
    private void testLifecycle(boolean withProfiling, int steps) {
        registerPredictionSteps(steps);
        AlgorithmProfilePredictionManager.start();
        PipelineDescriptor desc = new PipelineDescriptor(withProfiling);
        AlgorithmProfilingEvent.Status profilingStatus;
        if (withProfiling) {
            profilingStatus = desc.sendProfilingEvent(AlgorithmProfilingEvent.Status.START);
            System.out.println(profilingStatus + " at " + steps + " prediction steps");
        } else {
            System.out.println("normal operation at " + steps + " prediction steps");
            profilingStatus = null;
        }
        
        do {
            desc.start();
            System.out.print(" ");
            double items = 10;
            for (int i = 0; i < 60; i++) { // > 5 for stabilizing prediction
                System.out.print(".");
                desc.monitor(100, items, 100, items - 1);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                items += 10; // constant rate
            }
            System.out.println();
            desc.assertPipelineStructure();
            // see registerPredictionSteps
            assertPrediction(desc, TimeBehavior.LATENCY, 0.2);
            assertPrediction(desc, TimeBehavior.THROUGHPUT_ITEMS, 0.6); // increasing
            assertPrediction(desc, Scalability.ITEMS, 0.2); // Failed with 0.1

            desc.predictParameterValues(desc.getNumericParameterName(), null); // TODO assert
            
            desc.storeAll();
            desc.assertStorage();
            desc.stop();
            desc.assertStop();
            
            if (AlgorithmProfilingEvent.Status.START == profilingStatus) {
                profilingStatus = desc.sendProfilingEvent(AlgorithmProfilingEvent.Status.NEXT);
                System.out.println(profilingStatus);
            } else if (AlgorithmProfilingEvent.Status.NEXT == profilingStatus) {
                profilingStatus = desc.sendProfilingEvent(AlgorithmProfilingEvent.Status.END);
                System.out.println(profilingStatus);
                profilingStatus = null;
            }
        } while (profilingStatus != null);
        AlgorithmProfilePredictionManager.stop();
        registerPredictionSteps(-1);
    }
    
    /**
     * Asserts the prediction for <code>obs</code> on the family of <code>desc</code> against the measured values.
     * 
     * @param desc the descriptor with the actual values
     * @param obs the observable to assert
     * @param diffO the allowed difference in percent (0;1)
     */
    private void assertPrediction(PipelineDescriptor desc, IObservable obs, double diffO) {
        PipelineNodeSystemPart fam = desc.getFamily();
        double p = desc.predict(obs, null);
        double o = fam.getObservedValue(obs);
        Assert.assertTrue(p + " " + o + " diff " + Math.abs(o - p) + " is not less than factor " + diffO + " -> "  
            + (o * diffO), Math.abs(o - p) < o * diffO);
    }
    
}
