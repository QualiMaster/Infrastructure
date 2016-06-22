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
package tests.eu.qualimaster.events;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.Configuration;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.infrastructure.PipelineStatusTracker;

/**
 * Tests {@link PipelineStatusTracker}.
 * 
 * @author Holger Eichelberger
 */
public class PipelineStatusTrackerTest {

    /**
     * Tests the pipeline status tracker. This test does not consider the semantics (state machine) of the pipeline 
     * status constants, it just uses them.
     */
    @Test(timeout = 3000 + EventManager.SO_TIMEOUT)
    public void testPiplineStatusTracker() {
        Configuration.configureLocal();
        EventManager.start();

        PipelineStatusTracker tracker = new PipelineStatusTracker();
        EventManager.register(tracker);

        final String pipeline1 = "pipeline1";
        final String pipeline2 = "pipeline2";
        
        // pipeline1 unknown
        Assert.assertFalse(tracker.isKnown(pipeline1));
        Assert.assertNull(tracker.getStatus(pipeline1));
        // pipeline2 unknown
        Assert.assertFalse(tracker.isKnown(pipeline2));
        Assert.assertNull(tracker.getStatus(pipeline2));
        
        EventManager.send(new PipelineLifecycleEvent(pipeline1, Status.STARTING, null));
        EventManager.cleanup();
        Assert.assertTrue(tracker.isKnown(pipeline1));
        Assert.assertEquals(Status.STARTING, tracker.getStatus(pipeline1));
        // pipeline2 still unknown
        Assert.assertFalse(tracker.isKnown(pipeline2));
        Assert.assertNull(tracker.getStatus(pipeline2));

        EventManager.send(new PipelineLifecycleEvent(pipeline1, Status.CREATED, null));
        final int maxWaitingTime = 600;
        long before = System.currentTimeMillis();
        tracker.waitFor(pipeline1, Status.CREATED, maxWaitingTime);
        long after = System.currentTimeMillis();
        Status status = tracker.getStatus(pipeline1);
        Assert.assertTrue((Status.CREATED != status && after - before > maxWaitingTime) || Status.CREATED == status);
        // pipeline2 still unknown
        Assert.assertFalse(tracker.isKnown(pipeline2));
        Assert.assertNull(tracker.getStatus(pipeline2));

        EventManager.send(new PipelineLifecycleEvent(pipeline1, Status.STOPPED, null));
        EventManager.cleanup();
        // pipeline1 again unknown
        Assert.assertFalse(tracker.isKnown(pipeline1));
        Assert.assertNull(tracker.getStatus(pipeline1));
        // pipeline2 still unknown
        Assert.assertFalse(tracker.isKnown(pipeline2));
        Assert.assertNull(tracker.getStatus(pipeline2));

        EventManager.unregister(tracker);

        tracker = new PipelineStatusTracker(false);
        EventManager.register(tracker);

        EventManager.send(new PipelineLifecycleEvent(pipeline1, Status.STARTING, null));
        EventManager.cleanup();
        Assert.assertTrue(tracker.isKnown(pipeline1));
        Assert.assertEquals(Status.STARTING, tracker.getStatus(pipeline1));
        // pipeline2 still unknown
        Assert.assertFalse(tracker.isKnown(pipeline2));
        Assert.assertNull(tracker.getStatus(pipeline2));

        EventManager.send(new PipelineLifecycleEvent(pipeline1, Status.STOPPED, null));
        EventManager.cleanup();
        // now pipeline1 remains known
        Assert.assertTrue(tracker.isKnown(pipeline1));
        Assert.assertEquals(Status.STOPPED, tracker.getStatus(pipeline1));
        // pipeline2 still unknown
        Assert.assertFalse(tracker.isKnown(pipeline2));
        Assert.assertNull(tracker.getStatus(pipeline2));
        
        EventManager.unregister(tracker);
        EventManager.stop();
    }
    
}