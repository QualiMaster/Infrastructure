package tests.eu.qualimaster.events;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.adaptation.events.AdaptationEventResponse;
import eu.qualimaster.adaptation.events.AlgorithmConfigurationAdaptationEvent;
import eu.qualimaster.adaptation.events.ParameterConfigurationAdaptationEvent;
import eu.qualimaster.events.AbstractReturnableEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.EventManager.EventSender;
import eu.qualimaster.events.IEvent;
import eu.qualimaster.events.IReturnableEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.logging.events.LoggingEvent;
import eu.qualimaster.logging.events.LoggingFilterEvent;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.AlgorithmMonitoringEvent;
import eu.qualimaster.monitoring.events.ChangeMonitoringEvent;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.events.HardwareAliveEvent;
import eu.qualimaster.monitoring.events.LoadSheddingChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.ParameterChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.PipelineElementMultiObservationMonitoringEvent;
import eu.qualimaster.monitoring.events.PipelineElementObservationMonitoringEvent;
import eu.qualimaster.monitoring.events.PlatformMonitoringEvent;
import eu.qualimaster.monitoring.events.ReplayChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Simple event tests.
 * 
 * @author Holger Eichelberger
 */
public class EventsTests {

    /**
     * Adaptation events tests.
     */
    @Test
    public void testAdaptationEvents() {
        AlgorithmConfigurationAdaptationEvent aEvent = new AlgorithmConfigurationAdaptationEvent("pip", "proc", 
            "alg", false);
        Assert.assertEquals("pip", aEvent.getPipeline());
        Assert.assertEquals("proc", aEvent.getPipelineElement());
        Assert.assertEquals("alg", aEvent.getAlgorithm());
        Assert.assertEquals(false, aEvent.isUserTrigger());
        
        ParameterConfigurationAdaptationEvent pcae = new ParameterConfigurationAdaptationEvent("pip", "elt", "name", 
            "me", true);
        pcae.setMessageId("aaa1");
        pcae.setSenderId("client1");
        Assert.assertEquals("pip", pcae.getPipeline());
        Assert.assertEquals("elt", pcae.getPipelineElement());
        Assert.assertEquals("name", pcae.getParameter());
        Assert.assertEquals("me", pcae.getValue());
        Assert.assertEquals(true, pcae.isUserTrigger());
        
        AdaptationEventResponse resp = new AdaptationEventResponse(pcae, 
            AdaptationEventResponse.ResultType.FAILED, "whyever");
        Assert.assertEquals(pcae.getMessageId(), resp.getMessageId());
        Assert.assertEquals(pcae.getSenderId(), resp.getReceiverId());
        Assert.assertEquals(AdaptationEventResponse.ResultType.FAILED, resp.getResultType());
        Assert.assertEquals("whyever", resp.getMessage());
    }

    /**
     * Test monitoring events.
     */
    @Test
    public void testPlatformMonitoringEvents() {
        final String key = "key";

        PlatformMonitoringEvent pEvent = new PlatformMonitoringEvent(TimeBehavior.LATENCY, 500.0, key);
        Assert.assertEquals(TimeBehavior.LATENCY, pEvent.getObservable());
        Assert.assertEquals(500.0, pEvent.getObservation(), 0.001);
        Assert.assertEquals(key, pEvent.getKey());
        pEvent = new PlatformMonitoringEvent(TimeBehavior.LATENCY, 500, key);
        Assert.assertEquals(500.0, pEvent.getObservation(), 0.001);
        Assert.assertNull(pEvent.getTopologyId());

        pEvent = new PlatformMonitoringEvent("1234", TimeBehavior.LATENCY, 500.0, null);
        Assert.assertEquals(TimeBehavior.LATENCY, pEvent.getObservable());
        Assert.assertEquals(500.0, pEvent.getObservation(), 0.001);
        Assert.assertNull(pEvent.getKey());
        Assert.assertEquals("1234", pEvent.getTopologyId());
    }
    
    /**
     * Test monitoring events.
     */
    @Test
    public void testMonitoringEvents() {
        final String pipeline = "pip";
        final String pipelineElement = "proc";
        
        ComponentKey key = new ComponentKey("localhost", 7777, 4);
        PipelineElementObservationMonitoringEvent oEvent = new PipelineElementObservationMonitoringEvent(pipeline, 
            pipelineElement, key, TimeBehavior.ENACTMENT_DELAY, 500.0);
        Assert.assertEquals(pipeline, oEvent.getPipeline());
        Assert.assertEquals(pipelineElement, oEvent.getPipelineElement());
        Assert.assertEquals(TimeBehavior.ENACTMENT_DELAY, oEvent.getObservable());
        Assert.assertEquals(500.0, oEvent.getObservation(), 0.001);
        Assert.assertEquals(key, oEvent.getKey());
        oEvent = new PipelineElementObservationMonitoringEvent(pipeline, 
            pipelineElement, null, TimeBehavior.ENACTMENT_DELAY, 500);
        Assert.assertEquals(500.0, oEvent.getObservation(), 0.001);
        Assert.assertNull(oEvent.getKey());

        Map<IObservable, Double> obs = new HashMap<IObservable, Double>();
        obs.put(TimeBehavior.THROUGHPUT_ITEMS, 5.0);
        PipelineElementMultiObservationMonitoringEvent moEvent = new PipelineElementMultiObservationMonitoringEvent(
            pipeline, pipelineElement, key, obs);
        Assert.assertEquals(pipeline, moEvent.getPipeline());
        Assert.assertEquals(pipelineElement, moEvent.getPipelineElement());
        Assert.assertEquals(obs, moEvent.getObservations());
        Assert.assertEquals(key, moEvent.getKey());
        
        long timestamp = System.currentTimeMillis();
        ChangeMonitoringEvent cEvent = new ChangeMonitoringEvent(true, timestamp);
        Assert.assertNull(cEvent.getPipeline());
        Assert.assertNull(cEvent.getPipelineElement());
        Assert.assertNull(cEvent.getObservable());
        Assert.assertEquals(true, cEvent.isEnabled());
        Assert.assertEquals(timestamp, cEvent.getTimestamp());
        
        timestamp = System.currentTimeMillis();
        cEvent = new ChangeMonitoringEvent(Scalability.VARIETY, false, timestamp);
        Assert.assertNull(cEvent.getPipeline());
        Assert.assertNull(cEvent.getPipelineElement());
        Assert.assertEquals(Scalability.VARIETY, cEvent.getObservable());
        Assert.assertEquals(false, cEvent.isEnabled());
        Assert.assertEquals(timestamp, cEvent.getTimestamp());

        timestamp = System.currentTimeMillis();
        cEvent = new ChangeMonitoringEvent(pipeline, Scalability.VARIETY, true, timestamp);
        Assert.assertEquals(pipeline, cEvent.getPipeline());
        Assert.assertNull(cEvent.getPipelineElement());
        Assert.assertEquals(Scalability.VARIETY, cEvent.getObservable());
        Assert.assertEquals(true, cEvent.isEnabled());
        Assert.assertEquals(timestamp, cEvent.getTimestamp());

        timestamp = System.currentTimeMillis();
        cEvent = new ChangeMonitoringEvent(pipeline, pipelineElement, Scalability.VARIETY, true, timestamp);
        Assert.assertEquals(pipeline, cEvent.getPipeline());
        Assert.assertEquals(pipelineElement, cEvent.getPipelineElement());
        Assert.assertEquals(Scalability.VARIETY, cEvent.getObservable());
        Assert.assertEquals(true, cEvent.isEnabled());
        Assert.assertEquals(timestamp, cEvent.getTimestamp());
        
        SubTopologyMonitoringEvent sEvent = new SubTopologyMonitoringEvent(pipeline, null);
        Assert.assertEquals(pipeline, sEvent.getPipeline());
        Assert.assertNull(sEvent.getStructure());
    }

    /**
     * Test monitoring events.
     */
    @Test
    public void testMonitoringEvents2() {
        final String pipeline = "pip";
        final String pipelineElement = "proc";
        final String algorithm = "alg";
        final String parameter = "param";
        final String value = "value";
        final String msgId = "abba-1234";
        
        AlgorithmChangedMonitoringEvent aEvent = 
            new AlgorithmChangedMonitoringEvent(pipeline, pipelineElement, algorithm);
        Assert.assertEquals(pipeline, aEvent.getPipeline());
        Assert.assertEquals(pipelineElement, aEvent.getPipelineElement());
        Assert.assertEquals(algorithm, aEvent.getAlgorithm());
        
        aEvent = new AlgorithmChangedMonitoringEvent(pipeline, pipelineElement, algorithm, msgId);
        Assert.assertEquals(pipeline, aEvent.getPipeline());
        Assert.assertEquals(pipelineElement, aEvent.getPipelineElement());
        Assert.assertEquals(algorithm, aEvent.getAlgorithm());
        Assert.assertEquals(msgId, aEvent.getCauseMessageId());
        
        ParameterChangedMonitoringEvent pEvent = new ParameterChangedMonitoringEvent(pipeline, pipelineElement, 
            parameter, value, msgId);
        Assert.assertEquals(pipeline, pEvent.getPipeline());
        Assert.assertEquals(pipelineElement, pEvent.getPipelineElement());
        Assert.assertEquals(parameter, pEvent.getParameter());
        Assert.assertEquals(value, pEvent.getValue());
        Assert.assertEquals(msgId, pEvent.getCauseMessageId());
        
        ReplayChangedMonitoringEvent rEvent = new ReplayChangedMonitoringEvent(pipeline, pipelineElement, 1, true, 
            msgId);
        Assert.assertEquals(pipeline, rEvent.getPipeline());
        Assert.assertEquals(pipelineElement, rEvent.getPipelineElement());
        Assert.assertEquals(1, rEvent.getTicket());
        Assert.assertEquals(true, rEvent.getStartReplay());
        Assert.assertEquals(msgId, rEvent.getCauseMessageId());
        
        LoadSheddingChangedMonitoringEvent lEvent = new LoadSheddingChangedMonitoringEvent(pipeline, pipelineElement, 
            "aaa", "bbb", msgId);
        Assert.assertEquals(pipeline, lEvent.getPipeline());
        Assert.assertEquals(pipelineElement, lEvent.getPipelineElement());
        Assert.assertEquals("aaa", lEvent.getShedder());
        Assert.assertEquals("bbb", lEvent.getActualShedder());
        Assert.assertEquals(msgId, lEvent.getCauseMessageId());
    }    
    
    /**
     * Test hardware events.
     */
    @Test
    public void testHardwareEvents() {
        HardwareAliveEvent hwEvent = new HardwareAliveEvent("TEST");
        Assert.assertEquals("TEST", hwEvent.getIdentifier());
        hwEvent = new HardwareAliveEvent(null);
        Assert.assertEquals("", hwEvent.getIdentifier());
    }

    /**
     * Test algorithm events.
     */
    @Test
    public void testAlgorithmEvents()  {
        AlgorithmMonitoringEvent evt = new AlgorithmMonitoringEvent("123", "alg", ResourceUsage.MEMORY_USE, 10);
        Assert.assertEquals("alg", evt.getAlgorithmId());
        Assert.assertEquals(ResourceUsage.MEMORY_USE, evt.getObservable());
        Assert.assertEquals(10, evt.getValue(), 0.05);
        Assert.assertEquals("123", evt.getTopologyId());
    }
    
    /**
     * Tests logging events.
     * 
     * @throws UnknownHostException shall not occur
     */
    @Test
    public void testLoggingEvents() throws UnknownHostException {
        LoggingEvent event = new LoggingEvent(1000, "test", "test", "test");
        Assert.assertEquals("test", event.getLevel());
        Assert.assertEquals("test", event.getMessage());
        Assert.assertEquals("test", event.getThreadName());
        Assert.assertEquals(InetAddress.getLocalHost(), event.getHostAddress());
        
        LoggingFilterEvent fEvent = new LoggingFilterEvent(null, null);
        Assert.assertTrue(fEvent.getFilterAdditions().isEmpty());
        Assert.assertTrue(fEvent.getFilterRemovals().isEmpty());
        
        ArrayList<String> tmp = new ArrayList<String>();
        tmp.add(".*");
        fEvent = new LoggingFilterEvent(tmp, null);
        Assert.assertEquals(tmp, fEvent.getFilterAdditions());
        Assert.assertTrue(fEvent.getFilterRemovals().isEmpty());
    }

    /**
     * Tests the lifecycle event.
     */
    @Test
    public void testLifecycleEvent() {
        @SuppressWarnings("serial")
        IReturnableEvent evt = new AbstractReturnableEvent() { };
        evt.setMessageId("1234");
        evt.setSenderId("abcd");
        
        PipelineLifecycleEvent lEvt = new PipelineLifecycleEvent("pip", PipelineLifecycleEvent.Status.CREATED, evt);
        Assert.assertEquals("pip", lEvt.getPipeline());
        Assert.assertEquals(PipelineLifecycleEvent.Status.CREATED, lEvt.getStatus());
        Assert.assertEquals("1234", lEvt.getCauseMessageId());
        Assert.assertEquals("abcd", lEvt.getCauseSenderId());

        lEvt = new PipelineLifecycleEvent(lEvt, PipelineLifecycleEvent.Status.STOPPED);
        Assert.assertEquals("pip", lEvt.getPipeline());
        Assert.assertEquals(PipelineLifecycleEvent.Status.STOPPED, lEvt.getStatus());
        Assert.assertEquals("1234", lEvt.getCauseMessageId());
        Assert.assertEquals("abcd", lEvt.getCauseSenderId());
    }

    /**
     * A test handler instance.
     * 
     * @author Holger Eichelberger
     */
    private class TestEventHandler extends EventHandler<IEvent> {

        private int count;
        
        /**
         * Creates a handler instance.
         */
        protected TestEventHandler() {
            super(IEvent.class);
        }

        @Override
        protected void handle(IEvent event) {
            count++;
        }
        
        /**
         * Returns the number of events received.
         * 
         * @return the number of events received
         */
        private int getEventCount() {
            return count;
        }
        
    }
    
    /**
     * Tests the event sender.
     */
    @Test
    public void testEventSender() {
        EventManager.start(false, true);
        TestEventHandler handler = new TestEventHandler();
        EventManager.register(handler);
        EventSender sender = new EventSender();
        sender.send(new PipelineLifecycleEvent("pip", PipelineLifecycleEvent.Status.CREATED, null));
        sender.send(new PipelineLifecycleEvent("pip", PipelineLifecycleEvent.Status.STOPPED, null));
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
        sender.close();
        EventManager.cleanup();
        EventManager.stop();
        Assert.assertEquals(2, handler.getEventCount());
    }
    
}
