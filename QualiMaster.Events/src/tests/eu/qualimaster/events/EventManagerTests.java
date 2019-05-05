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
package tests.eu.qualimaster.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.Assert;

import eu.qualimaster.events.AbstractEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;

/**
 * Additional tests on the level of the event manager.
 * 
 * @author Holger Eichelberger
 */
public class EventManagerTests {

    /**
     * The number of connections to try in sequence.
     */
    private static final int CONNECTION_COUNT = 10;
    
    /**
     * The time to sleep (ms) for a worker between two connection attempts.
     */
    private static final int CONNECTION_SLEEP = 50;
    
    /**
     * The number of parallel worker threads.
     */
    private static final int WORKER_COUNT = 10;
    
    /**
     * How long to wait per worker (ms) before starting with sending events. This defers all workers to a future 
     * timestamp. 
     */
    private static final int START_WAIT_PER_WORKER = 100;
    
    /**
     * A specific event for testing.
     * 
     * @author Holger Eichelberger
     */
    private static class TestEvent extends AbstractEvent {
        
        private static final long serialVersionUID = 1573279315377857703L;
        private int workerId;
        @SuppressWarnings("unused")
        private int eventId;

        /**
         * Creates a test event.
         * 
         * @param workerId the worker id
         * @param eventId the event id
         */
        private TestEvent(int workerId, int eventId) {
            this.workerId = workerId;
            this.eventId = eventId;
        }

        /**
         * Returns the worker id.
         * 
         * @return the worker id
         */
        private int getWorkerId() {
            return workerId;
        }
        
    }
    
    /**
     * Implements a worker trying to access the event manager startup in sequence (within the worker) and in parallel
     * (among workers).
     * 
     * @author Holger Eichelberger
     */
    private static class Worker implements Runnable {

        private long startTime;
        private int id;
        private boolean completed = false;
        private EventManager client;
        
        /**
         * Creates a worker runnable.
         * 
         * @param id the worker id to be appended to the message
         * @param startTime the common start time for all workers
         * @param client the event manager instance to use
         */
        private Worker(int id, long startTime, EventManager client) {
            this.startTime = startTime;
            this.id = id;
            this.client = client;
        }
        
        @Override
        public void run() {
            long wait = startTime - System.currentTimeMillis();
            if (wait > 0) {
                sleep(wait);
            } else {
                System.out.println("Warning: Start time waiting for worker " + id + " is negative (" + wait 
                    + ") and ignored");
            }
            for (int i = 0; i < CONNECTION_COUNT; i++) {
                TestEvent evt = new TestEvent(id, i);
                if (0 == i) { // as currently done in StormCommons
                    client.doAsyncSend(evt);
                } else {
                    client.doSend(evt);
                }
                sleep(CONNECTION_SLEEP);
            }
            completed = true;
        }
        
        /**
         * Returns whether this worker completed its work.
         * 
         * @return <code>true</code> if completed, <code>false</code> else
         */
        private boolean isCompleted() {
            return completed;
        }
        
    }
    
    /**
     * Records received events for assertion.
     * 
     * @author Holger Eichelberger
     */
    private static class TestEventHandler extends EventHandler<TestEvent> {

        private Map<Integer, List<TestEvent>> receivedEvents = new HashMap<Integer, List<TestEvent>>();

        /**
         * Creates a test event handler.
         */
        private TestEventHandler() {
            super(TestEvent.class);
        }

        @Override
        protected void handle(TestEvent event) {
            int workerId = event.getWorkerId();
            synchronized (receivedEvents) {
                List<TestEvent> evts = receivedEvents.get(workerId);
                if (null == evts) {
                    evts = new ArrayList<TestEvent>();
                    receivedEvents.put(workerId, evts);
                } 
                evts.add(event);
            }
        }
        
        /**
         * Returns the received events per <code>id</code>.
         * 
         * @param id the id
         * @return the received events
         */
        private List<TestEvent> getReceived(int id) {
            List<TestEvent> result = receivedEvents.get(id);
            if (null == result) {
                result = new ArrayList<TestEvent>(); // not efficient, simplifies code
            }
            return result;
        }
        
    }
    
    /**
     * Tests the startup behavior on the same (local) instance.
     */
    @Test
    public void testLocalParallelStartup() {
        EventManager.start();
        EventManager mgr = EventManager.getInstance();
        doTest(mgr, mgr);
        mgr.doStop();
    }
    
    /**
     * Tests the startup behavior with client and server instance.
     */
    @Test
    public void testCsParallelStartup() {
        EventManager server = new EventManager();
        server.doStart(false, true);
        EventManager client = EventManager.getInstance();
        // do not start client
        doTest(server, client);
        client.doStop();
        server.doStop();
    }
    
    /**
     * Performs the test.
     * 
     * @param server the server event manager instance
     * @param client the client event manager instance
     */
    private void doTest(EventManager server, EventManager client) {
        TestEventHandler handler = new TestEventHandler();
        server.doRegister(handler);
        long startTime = System.currentTimeMillis() + START_WAIT_PER_WORKER * WORKER_COUNT;
        Worker[] workers = new Worker[WORKER_COUNT];
        for (int w = 0; w < WORKER_COUNT; w++) {
            workers[w] = new Worker(w, startTime, client);
            Thread t = new Thread(workers[w]);
            t.start();
        }
        
        boolean allCompleted;
        do {
            allCompleted = true;
            for (int w = 0; allCompleted && w < WORKER_COUNT; w++) {
                allCompleted &= workers[w].isCompleted();
            }
            sleep(200);
        } while (!allCompleted);
        client.doCleanup();
        if (client != server) {
            server.doCleanup();
            sleep(1000);
        }
        
        /*for (int w = 0; w < WORKER_COUNT; w++) {
            List<TestEvent> recv = handler.getReceived(w);
            System.out.println("Worker " + w + ": " + recv.size() + " events received " + handler.getReceived(w));
        }*/
        
        for (int w = 0; w < WORKER_COUNT; w++) {
            List<TestEvent> recv = handler.getReceived(w);
            Assert.assertEquals("Number of expected events (" + CONNECTION_COUNT + ") for worker " + w 
                + " does not match: " + recv.size() + " " + recv, CONNECTION_COUNT, recv.size());
        }
        server.doUnregister(handler);
    }

    /**
     * Sleeps for a given amount of milliseconds.
     * 
     * @param ms the amount of milliseconds to sleep
     */
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
    
}
