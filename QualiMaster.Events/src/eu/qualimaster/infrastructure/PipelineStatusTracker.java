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
package eu.qualimaster.infrastructure;

import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.events.EventHandler;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;

/**
 * A reusable pipeline status tracker. To use, register an instance with the
 * {@link eu.qualimaster.events.EventManager#register(EventHandler) event manager}.
 * 
 * @author Holger Eichelberger
 */
public class PipelineStatusTracker extends EventHandler<PipelineLifecycleEvent> {

    private Map<String, Status> status = new HashMap<String, Status>();
    private boolean clearIfStopped = true;
    private int sleepDistance;
    
    /**
     * Creates a new tracker which clears a pipeline after stopping with a sleep distance of 100 ms.
     */
    public PipelineStatusTracker() {
        this(true, 100);
    }

    /**
     * Creates a new tracker with a sleep distance of 100 ms.
     * 
     * @param clearIfStopped whether a pipeline shall be removed from the tracker if it enters the stopped state
     */
    public PipelineStatusTracker(boolean clearIfStopped) {
        this(clearIfStopped, 100);
    }
    
    /**
     * Creates a new tracker.
     * 
     * @param clearIfStopped whether a pipeline shall be removed from the tracker if it enters the stopped state
     * @param sleepDistance the atomic distance to wait for a status change (min 10 ms)
     */
    public PipelineStatusTracker(boolean clearIfStopped, int sleepDistance) {
        super(PipelineLifecycleEvent.class);
        this.clearIfStopped = clearIfStopped;
        this.sleepDistance = Math.max(10, sleepDistance);
    }

    @Override
    protected void handle(PipelineLifecycleEvent event) {
        String pipeline = event.getPipeline();
        Status status = event.getStatus();
        this.status.put(pipeline, status);
        if (clearIfStopped && Status.STOPPED == status) {
            this.status.remove(pipeline);
        }
    }

    /**
     * Returns whether the given <code>pipeline</code> is known / has a status.
     * 
     * @param pipeline the pipeline name
     * @return <code>true</code> if the pipeline is known, <code>false</code> else
     */
    public boolean isKnown(String pipeline) {
        return status.containsKey(pipeline);
    }
   
    /**
     * Returns the current status of <code>pipeline</code>.
     * 
     * @param pipeline the pipeline to return the status for (may be <b>null</b>)
     * @return the status of the pipeline (<b>null</b> if the pipeline is not known or <code>pipeline</code> is null)
     */
    public Status getStatus(String pipeline) {
        return null == pipeline ? null : status.get(pipeline);
    }
    
    /**
     * Waits until the pipeline is in the given state or <code>maxTime</code> passed by.
     * 
     * @param pipeline the pipeline name (may be <b>null</b>)
     * @param status the expected status (may be <b>null</b>)
     * @param maxTime the maximum waiting time (ms)
     */
    public void waitFor(String pipeline, Status status, int maxTime) {
        long end = System.currentTimeMillis() + Math.max(0, maxTime);
        while (status != this.status.get(pipeline) && System.currentTimeMillis() < end) {
            sleep(sleepDistance);
        }
    }
    
    /**
     * Waits until the pipeline is in the given state.
     * 
     * @param pipeline the pipeline name (may be <b>null</b>)
     * @param status the expected status (may be <b>null</b>)
     */
    public void waitFor(String pipeline, Status status) {
        while (status != this.status.get(pipeline)) {
            sleep(sleepDistance);
        }
    }

    /**
     * Sleeps the current thread for the given time.
     * 
     * @param millis the time to sleep
     */
    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

}
