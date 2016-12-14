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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.common.signal.SignalMechanism;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.ReasoningTask;
import eu.qualimaster.monitoring.ReasoningTask.IReasoningModelProvider;
import eu.qualimaster.monitoring.ReasoningTask.PhaseReasoningModelProvider;
import eu.qualimaster.monitoring.events.ConstraintViolationAdaptationEvent;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.events.ViolatingClause;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.AnalysisObservables;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.TimeBehavior;
import tests.eu.qualimaster.coordination.TestNameMapping;
import tests.eu.qualimaster.coordination.Utils;

/**
 * Tests {@link ReasoningTask}.
 * 
 * @author Holger Eichelberger
 */
public class ReasoningTaskTests {

    private static final Double TRUE = 1.0;
    private static final Double FALSE = 0.0;
    private static final double ASSERTION_PRECISION = 0.0005;
    
    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        SignalMechanism.setTestMode(true);
        Utils.setModelProvider(Utils.INFRASTRUCTURE_TEST_MODEL_PROVIDER);
        Utils.configure();
        EventManager.start();
        CoordinationManager.start();
        CoordinationManager.registerTestMapping(TestNameMapping.INSTANCE);
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        CoordinationManager.stop();
        EventManager.stop();
        Utils.dispose();
    }
    
    /**
     * Shuts down the {@link RepositoryConnector} after all tests, to facilitate similar tests.
     */
    @AfterClass
    public static void tearDownAfterClass() {
        RepositoryConnector.shutdown();
    }
    
    /**
     * Tests user constraints.
     */
    @Test
    public void testUserConstraint() {
        // setup test 
        IReasoningModelProvider provider = new PhaseReasoningModelProvider(Phase.MONITORING);
        ReasoningTask task = new ReasoningTask(provider);
        // prepare the system state
        SystemState state = MonitoringManager.getSystemState();
        PipelineSystemPart pip = state.obtainPipeline("pipeline");
        pip.changeStatus(PipelineLifecycleEvent.Status.STARTED, false, null);
        pip.setValue(ResourceUsage.EXECUTORS, 2, null);
        NodeImplementationSystemPart alg1 = pip.getAlgorithm("alg1");
        NodeImplementationSystemPart alg2 = pip.getAlgorithm("alg2");

        // ----------------- force alg 1 -----------------------
        alg1.setValue(TimeBehavior.THROUGHPUT_ITEMS, 22, null);
        alg2.setValue(TimeBehavior.THROUGHPUT_ITEMS, 22, null);
        
        AdaptationEvent event = task.reason(false);
        Assert.assertNotNull(event);
        Assert.assertTrue(event instanceof ConstraintViolationAdaptationEvent);
        ConstraintViolationAdaptationEvent cEvent = (ConstraintViolationAdaptationEvent) event;
        FrozenSystemState frozenState = cEvent.getState();  // state visible to adaptation
        assertAlgorithmIsValid(TRUE, frozenState, alg1);
        assertAlgorithmIsValid(FALSE, frozenState, alg2);
        Assert.assertEquals(1, cEvent.getViolatingClauseCount());
        Assert.assertEquals(AnalysisObservables.IS_VALID, cEvent.getViolatingClause(0).getObservable());
        
        event = task.reason(false);
        frozenState = state.freeze();  // state visible to adaptation (also in event)
        Assert.assertNull(event); // shall not occur multiple times
        assertAlgorithmIsValid(TRUE, frozenState, alg1);
        assertAlgorithmIsValid(FALSE, frozenState, alg2);
        
        alg1.setValue(TimeBehavior.THROUGHPUT_ITEMS, 21, null);
        alg2.setValue(TimeBehavior.THROUGHPUT_ITEMS, 21, null);

        event = task.reason(false);
        frozenState = state.freeze();  // state visible to adaptation (also in event)
        Assert.assertNull(event); // shall not occur multiple times
        assertAlgorithmIsValid(TRUE, frozenState, alg1);
        assertAlgorithmIsValid(FALSE, frozenState, alg2);
        
        // ----------------- force alg 2 -----------------------
        alg1.setValue(TimeBehavior.THROUGHPUT_ITEMS, 28, null);
        alg2.setValue(TimeBehavior.THROUGHPUT_ITEMS, 28, null);
        
        event = task.reason(false);
        Assert.assertNotNull(event);
        Assert.assertTrue(event instanceof ConstraintViolationAdaptationEvent);
        cEvent = (ConstraintViolationAdaptationEvent) event;
        frozenState = cEvent.getState();  // state visible to adaptation
        assertAlgorithmIsValid(FALSE, frozenState, alg1);
        assertAlgorithmIsValid(TRUE, frozenState, alg2);        
        Assert.assertEquals(1, cEvent.getViolatingClauseCount());
        Assert.assertEquals(AnalysisObservables.IS_VALID, cEvent.getViolatingClause(0).getObservable());
        
        event = task.reason(false);
        frozenState = state.freeze();  // state visible to adaptation
        Assert.assertNull(event); // shall not occur multiple times
        assertAlgorithmIsValid(FALSE, frozenState, alg1);
        assertAlgorithmIsValid(TRUE, frozenState, alg2);
        alg1.setValue(TimeBehavior.THROUGHPUT_ITEMS, 33, null);
        alg2.setValue(TimeBehavior.THROUGHPUT_ITEMS, 33, null);

        event = task.reason(false);
        frozenState = state.freeze();  // state visible to adaptation
        Assert.assertNull(event); // shall not occur multiple times
        assertAlgorithmIsValid(FALSE, frozenState, alg1);
        assertAlgorithmIsValid(TRUE, frozenState, alg2);
        task.dispose();
    }
    
    /**
     * Tests the violation clearing capability, i.e., if a constraint is not violated anymore also a specific violation 
     * clause shall be created as result.
     */
    @Test
    public void testClearedConstraint() {
        IReasoningModelProvider provider = new PhaseReasoningModelProvider(Phase.MONITORING);
        ReasoningTask task = new ReasoningTask(provider);
        // prepare the system state
        SystemState state = MonitoringManager.getSystemState();
        PipelineSystemPart pip = state.obtainPipeline("pipeline");
        pip.changeStatus(PipelineLifecycleEvent.Status.STARTED, false, null);

        double capacity = 0.9;
        pip.setValue(ResourceUsage.CAPACITY, capacity, null);
        pip.setValue(ResourceUsage.EXECUTORS, 2, null);
        AdaptationEvent event = task.reason(false);
        Assert.assertNotNull(event);
        Assert.assertTrue(event instanceof ConstraintViolationAdaptationEvent);
        ConstraintViolationAdaptationEvent cEvent = (ConstraintViolationAdaptationEvent) event;
        Assert.assertEquals(1, cEvent.getViolatingClauseCount());
        ViolatingClause cl = cEvent.getViolatingClause(0);
        Assert.assertEquals(ResourceUsage.CAPACITY, cl.getObservable());
        Assert.assertFalse(cl.isCleared());
        double limit = 0.85;
        double deviation = limit - capacity;
        Assert.assertEquals(deviation, cl.getDeviation(), 0.005);
        Assert.assertEquals(cl.getDeviationPercentage(), deviation / limit, 0.005);

        pip.setValue(ResourceUsage.CAPACITY, 0.7, null);
        event = task.reason(false);
        Assert.assertNotNull(event);
        Assert.assertTrue(event instanceof ConstraintViolationAdaptationEvent);
        cEvent = (ConstraintViolationAdaptationEvent) event;
        Assert.assertEquals(1, cEvent.getViolatingClauseCount());
        Assert.assertEquals(ResourceUsage.CAPACITY, cEvent.getViolatingClause(0).getObservable());
        Assert.assertTrue(cEvent.getViolatingClause(0).isCleared());
        
        task.dispose();
    }

    /**
     * Asserts the state of {@link AnalysisObservables#IS_VALID}.
     * 
     * @param expected the expected value
     * @param state the frozen state to look into
     * @param alg the algorithm to took into
     */
    private static void assertAlgorithmIsValid(Double expected, FrozenSystemState state, 
        NodeImplementationSystemPart alg) {
        assertAlgorithmObservation(expected, state, alg, AnalysisObservables.IS_VALID, TRUE);
    }

    /**
     * Assert an algorithm observation. 
     * 
     * @param expected the expected value
     * @param state the frozen state to look into
     * @param alg the algorithm to took into
     * @param observable the observable to assert
     * @param deflt the default value in case that there is no value
     */
    private static void assertAlgorithmObservation(Double expected, FrozenSystemState state, 
        NodeImplementationSystemPart alg, IObservable observable, Double deflt) {
        Double val = state.getAlgorithmObservation(alg.getPipeline().getName(), alg.getName(), observable, deflt);
        if (null == expected) {
            Assert.assertNull(val);
        } else {
            Assert.assertNotNull(val);
            Assert.assertEquals(expected.doubleValue(), val.doubleValue(), ASSERTION_PRECISION);
        }
        
        val = alg.getObservedValue(observable);
        if (null == expected) {
            Assert.assertNull(val);
        } else {
            Assert.assertNotNull(val);
            Assert.assertEquals(expected.doubleValue(), val.doubleValue(), ASSERTION_PRECISION);
        }
    }

}