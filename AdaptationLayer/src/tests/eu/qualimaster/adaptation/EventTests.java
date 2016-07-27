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
package tests.eu.qualimaster.adaptation;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.adaptation.events.CheckBeforeStartupAdaptationEvent;
import eu.qualimaster.adaptation.events.RegularAdaptationEvent;
import eu.qualimaster.adaptation.events.StartupAdaptationEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;

/**
 * Tests events.
 * 
 * @author Holger Eichelberger
 */
public class EventTests {
    
    /**
     * Tests events.
     */
    @Test
    public void testEvents() {
        long now = System.currentTimeMillis();
        RegularAdaptationEvent rEvt = new RegularAdaptationEvent();
        Assert.assertTrue(rEvt.getTimestamp() >= now);
        
        StartupAdaptationEvent sEvt = new StartupAdaptationEvent("pip");
        Assert.assertEquals("pip", sEvt.getPipeline());
        
        PipelineLifecycleEvent pEvt = new PipelineLifecycleEvent("pip", Status.CHECKING, null);
        CheckBeforeStartupAdaptationEvent cEvt = new CheckBeforeStartupAdaptationEvent(pEvt);
        Assert.assertEquals("pip", cEvt.getPipeline());
    }

}
