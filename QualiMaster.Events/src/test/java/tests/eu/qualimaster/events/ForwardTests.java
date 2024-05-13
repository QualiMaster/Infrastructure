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
import eu.qualimaster.events.AbstractResponseEvent;
import eu.qualimaster.events.AbstractReturnableEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.SynchronousEventStore;
import eu.qualimaster.events.TimerEvent;

/**
 * Tests additional forms of forward events, timer, request responses.
 * 
 * @author Holger Eichelberger
 */
public class ForwardTests {

    /**
     * Tests a timer event handler.
     * 
     * @throws InterruptedException in case that sleeping was interrupted (shall not occur)
     */
    @Test(timeout = 8000 + EventManager.SO_TIMEOUT)
    public void testTimerHandler() throws InterruptedException {
        Configuration.configureLocal();
        EventManager.start(false, true);
        Thread.sleep(100);
        EventManager client = new EventManager();
        client.doStart(false, false);
        Thread.sleep(500);
        
        RecordingEventHandler<TimerEvent> recTimer = RecordingEventHandler.createTimerHandler();
        client.doRegister(recTimer);
        client.doSetTimerPeriod(300);

        // wait for ~10 events
        Thread.sleep(300 * 10);
        client.doSetTimerPeriod(0);
        Thread.sleep(1000);

        Assert.assertTrue(recTimer.getReceivedCount() >= 9); // just to be sure, shall be 10
        double interArrivalTime = recTimer.getAverageInterArrivalTime();
        Assert.assertTrue(Math.abs(interArrivalTime - 300) < 50);

        client.doUnregister(recTimer);
        EventManager.cleanup();
        client.doStop();
        client.doClearRegistrations();
        EventManager.stop();
        EventManager.clearRegistrations();
        Thread.sleep(500);
    }
    
    /**
     * Implements a test request event.
     * 
     * @author Holger Eichelberger
     */
    private static class TestRequestEvent extends AbstractReturnableEvent {

        private static final long serialVersionUID = 7419793978032447598L;
        private String data;

        /**
         * Creates a test request event.
         * 
         * @param data the (test) data
         */
        public TestRequestEvent(String data) {
            this.data = data;
        }
        
        /**
         * Returns (test) data.
         * 
         * @return test data
         */
        public String getData() {
            return data;
        }
        
    }

    /**
     * Implements a test response event.
     * 
     * @author Holger Eichelberger
     */
    private static class TestResponseEvent extends AbstractResponseEvent<TestRequestEvent> {

        private static final long serialVersionUID = 665006919816244728L;
        private String data;
        
        /**
         * Creates a test request event.
         * 
         * @param data the (test) data
         * @param request the request to take the sender/message id from
         */
        public TestResponseEvent(String data, TestRequestEvent request) {
            super(request);
            this.data = data;
        }

        /**
         * Returns (test) data.
         * 
         * @return test data
         */
        public String getData() {
            return data;
        }

    }

    /**
     * Implements a test request handler.
     * 
     * @author Holger Eichelberger
     */
    private static class TestRequestEventHandler extends EventHandler<TestRequestEvent> {

        private int received = 0;
        
        /**
         * Creates an instance.
         */
        protected TestRequestEventHandler() {
            super(TestRequestEvent.class);
        }

        @Override
        protected void handle(TestRequestEvent event) {
            TestResponseEvent evt = new TestResponseEvent(event.getData(), event);
            EventManager.send(evt);
            received++;
        }
        
        /**
         * Returns the number of received events.
         * 
         * @return the number of received events
         */
        public int getReceivedCount() {
            return received;
        }
        
    }
        
    /**
     * Tests request-response messages.
     * 
     * @param serverSide or client side
     * @throws InterruptedException in case that sleeping was interrupted (shall not occur)
     */
    private void requestResponse(boolean serverSide) throws InterruptedException {
        Configuration.configureLocal();
        EventManager.startServer();
        Thread.sleep(100);

        EventManager client = new EventManager();
        client.doStart(false, false);
        Thread.sleep(500);

        TestRequestEventHandler requestHandler = new TestRequestEventHandler();
        RecordingEventHandler<TestResponseEvent> recResponse 
            = new RecordingEventHandler<TestResponseEvent>(TestResponseEvent.class); // not create -> server
        EventManager.register(requestHandler);
        Thread.sleep(200);
        client.doRegister(recResponse);
        Thread.sleep(200);

        // evt -> server -> handler for response -> server -> response
        
        final String data = "myEvent";
        TestRequestEvent evt = new TestRequestEvent(data);
        if (serverSide) {
            EventManager.send(evt);
        } else {
            client.doSend(evt);
        }
        Thread.sleep(600);
        String msgId = evt.getMessageId();
        Assert.assertEquals(1, requestHandler.getReceivedCount());

        Assert.assertEquals(1, recResponse.getReceivedCount());
        TestResponseEvent resp = recResponse.getReceived(0);
        Assert.assertEquals(data, resp.getData());
        Assert.assertEquals(msgId, resp.getMessageId());
        
        EventManager.unregister(requestHandler);
        client.doUnregister(recResponse);
        client.doCleanup();
        EventManager.cleanup();
        client.doStop();
        client.doClearRegistrations();
        EventManager.stop();
        EventManager.clearRegistrations();
        Thread.sleep(500);
    }

    /**
     * Tests request-response messages on server side.
     * 
     * @throws InterruptedException in case that sleeping was interrupted (shall not occur)
     */
    @Test(timeout = 5000 + EventManager.SO_TIMEOUT)
    public void testRequestResponseServer() throws InterruptedException {
        requestResponse(true);
    }
    
    /**
     * Tests request-response messages on client side.
     * 
     * @throws InterruptedException in case that sleeping was interrupted (shall not occur)
     */
    @Test(timeout = 5000 + EventManager.SO_TIMEOUT)
    public void testRequestResponseClient() throws InterruptedException {
        requestResponse(false);
    }

    /**
     * Tests the synchronous store.
     * 
     * @throws InterruptedException in case that sleeping was interrupted (shall not occur)
     */
    @Test(timeout = 5000 + EventManager.SO_TIMEOUT)
    public void testSynchronousStore() throws InterruptedException {
        Configuration.configureLocal();
        EventManager.startServer();
        Thread.sleep(100);
        
        TestRequestEventHandler requestHandler = new TestRequestEventHandler();
        EventManager.register(requestHandler);
        SynchronousEventStore<TestRequestEvent, TestResponseEvent> store 
            = new SynchronousEventStore<>(TestResponseEvent.class);
        
        TestRequestEvent req = new TestRequestEvent("here");
        TestResponseEvent resp = store.waitFor(1000, 100, req);
        Assert.assertNotNull(resp);
        Assert.assertEquals(req.getData(), resp.getData());
        
        EventManager.unregister(requestHandler);
        EventManager.stop();
        EventManager.clearRegistrations();
    }
    
}
