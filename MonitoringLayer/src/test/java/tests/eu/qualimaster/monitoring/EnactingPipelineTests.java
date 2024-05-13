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
package tests.eu.qualimaster.monitoring;

import java.io.Serializable;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.IdentityMapping;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CommandSequence;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.EnactingPipelineElements;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.AnalysisObservables;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Tests recording enacting pipeline elements.
 * 
 * @author Holger Eichelberger
 */
public class EnactingPipelineTests {
    
    /**
     * Tests the enacting pipeline elements.
     * 
     * @throws InterruptedException shall not occur
     */
    @Test
    public void testEnacting() throws InterruptedException {
        final String pipName = "RandomPip";
        final String eltSrc = "src";
        final String fEltSrc = pipName + FrozenSystemState.SEPARATOR + eltSrc;
        final String eltProc = "processor";
        final String fEltProc = pipName + FrozenSystemState.SEPARATOR + eltProc;
        final String eltSnk = "snk";
        final String fEltSnk = pipName + FrozenSystemState.SEPARATOR + eltSnk;
        
        INameMapping mapping = new IdentityMapping(pipName);
        CoordinationManager.registerTestMapping(mapping);
        SystemState state = MonitoringManager.getSystemState();
        state.clear();
        
        EnactingPipelineElements elts = EnactingPipelineElements.INSTANCE;
        Assert.assertFalse(elts.isEnacting(pipName));
        Assert.assertFalse(elts.isEnacting(fEltSrc));
        Assert.assertFalse(elts.isEnacting(fEltProc));
        Assert.assertFalse(elts.isEnacting(fEltSnk));
        
        elts.handle(new PipelineLifecycleEvent(pipName, PipelineLifecycleEvent.Status.STARTING, null));
        elts.handle(new PipelineLifecycleEvent(pipName, PipelineLifecycleEvent.Status.STARTED, null));
        Assert.assertFalse(elts.isEnacting(pipName));
        Assert.assertFalse(elts.isEnacting(fEltSrc));
        Assert.assertFalse(elts.isEnacting(fEltProc));
        Assert.assertFalse(elts.isEnacting(fEltSnk));
        
        CommandSequence cmd = new CommandSequence();
        cmd.add(new AlgorithmChangeCommand(pipName, eltSrc, "a"));
        CommandSequence cmd1 = new CommandSequence();
        cmd.add(cmd1);
        cmd1.add(new AlgorithmChangeCommand(pipName, eltProc, "b"));
        cmd1.add(new ParameterChangeCommand<Serializable>(pipName, eltProc, "c", "1"));
        cmd.add(new AlgorithmChangeCommand(pipName, eltSnk, "d"));
        elts.handle(cmd, true);

        Assert.assertTrue(elts.isEnacting(pipName));
        assertEnacting(state, pipName, null, true, false);
        Assert.assertTrue(elts.isEnacting(fEltSrc));
        assertEnacting(state, pipName, eltSrc, true, false);
        Assert.assertTrue(elts.isEnacting(fEltProc));
        assertEnacting(state, pipName, eltProc, true, false);
        Assert.assertTrue(elts.isEnacting(fEltSnk));
        assertEnacting(state, pipName, eltSnk, true, false);

        Thread.sleep(500); // -> enactment delay
        
        elts.handle(cmd, false);
        Assert.assertFalse(elts.isEnacting(pipName));
        assertEnacting(state, pipName, null, false, false);
        Assert.assertFalse(elts.isEnacting(fEltSrc));
        assertEnacting(state, pipName, eltSrc, false, true);
        Assert.assertFalse(elts.isEnacting(fEltProc));
        assertEnacting(state, pipName, eltProc, false, true);
        Assert.assertFalse(elts.isEnacting(fEltSnk));
        assertEnacting(state, pipName, eltSnk, false, true);

        elts.handle(new PipelineLifecycleEvent(pipName, PipelineLifecycleEvent.Status.STOPPED, null));
        Assert.assertFalse(elts.isEnacting(pipName));
        Assert.assertFalse(elts.isEnacting(fEltSrc));
        Assert.assertFalse(elts.isEnacting(fEltProc));
        Assert.assertFalse(elts.isEnacting(fEltSnk));
        
        MonitoringManager.getSystemState().clear();
        CoordinationManager.unregisterNameMapping(mapping);
    }

    /**
     * Test the enacting observation in <code>state</code> for the given parameters.
     * 
     * @param state the system state
     * @param pipName the pipeline name
     * @param eltName the element name (may be <b>null</b> the only the pipeline part is tested)
     * @param isEnacting whether enacting for the pipeline/element shall be true or false
     * @param hasEnactmentDelay whether the pipeline element shall have an enactment delay recorded
     */
    private static void assertEnacting(SystemState state, String pipName, String eltName, boolean isEnacting, 
        boolean hasEnactmentDelay) {
        PipelineSystemPart pip = state.getPipeline(pipName);
        Assert.assertNotNull(pip);
        if (null == eltName) {
            assertEnacting(pip, isEnacting);
        } else {
            PipelineNodeSystemPart node = pip.getPipelineNode(eltName);
            Assert.assertNotNull(node);
            assertEnacting(node, isEnacting);
            if (hasEnactmentDelay) {
                Assert.assertTrue(node.hasValue(TimeBehavior.ENACTMENT_DELAY));
                double val = node.getObservedValue(TimeBehavior.ENACTMENT_DELAY);
                Assert.assertTrue(val > 0);
            }
        }
    }

    /**
     * Tests the enacting observation in <code>part</code>.
     * 
     * @param part the system part
     * @param isEnacting whether enacting shall be true or false
     */
    private static void assertEnacting(SystemPart part, boolean isEnacting) {
        Assert.assertTrue(part.hasValue(AnalysisObservables.IS_ENACTING));
        double val = part.getObservedValue(AnalysisObservables.IS_ENACTING);
        if (isEnacting) {
            Assert.assertTrue(val > 0.5);
        } else {
            Assert.assertTrue(val < 0.5);
        }
    }

}
