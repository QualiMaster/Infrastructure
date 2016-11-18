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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.events.AlgorithmMonitoringEvent;
import eu.qualimaster.monitoring.handlers.AlgorithmMonitoringEventHandler;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.ResourceUsage;
import tests.eu.qualimaster.coordination.Utils;

/**
 * Tests an algorithm monitoring event.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmMonitoringEventTest {

    /**
     * Tests an algorithm monitoring event.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testEvent() throws IOException {
        File f = new File(Utils.getTestdataDir(), "randomSubTopoMapping.xml");
        FileInputStream fis = new FileInputStream(f);
        NameMapping mapping = new NameMapping("RandomPip", fis);
        fis.close();
        
        CoordinationManager.registerTestMapping(mapping);
        SystemState state = new SystemState();
        PipelineSystemPart pip = state.obtainPipeline("RandomPip");
        pip.changeStatus(PipelineLifecycleEvent.Status.CREATED, false);
        AlgorithmMonitoringEvent evt = new AlgorithmMonitoringEvent("RandomPip", 
            "eu.qualimaster.RandomPip.topology.PipelineVar_1_Sink0Sink", ResourceUsage.USED_MEMORY, 432.0);
        AlgorithmMonitoringEventHandler.INSTANCE.doHandle(evt, state);
        CoordinationManager.unregisterNameMapping(mapping);

        // knowledge from mapping
        PipelineNodeSystemPart node = pip.getPipelineNode("PipelineVar_1_Sink0");
        Assert.assertNotNull(node);
        Assert.assertEquals(432, node.getObservedValue(ResourceUsage.USED_MEMORY), 0.05);
    }
    
}
