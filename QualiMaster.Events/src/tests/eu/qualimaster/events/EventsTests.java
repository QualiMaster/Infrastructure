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
import eu.qualimaster.adaptation.events.SourceVolumeAdaptationEvent;
import eu.qualimaster.events.AbstractReturnableEvent;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.EventManager.EventSender;
import eu.qualimaster.events.IEvent;
import eu.qualimaster.events.IResponseEvent;
import eu.qualimaster.events.IReturnableEvent;
import eu.qualimaster.infrastructure.EndOfDataEvent;
import eu.qualimaster.infrastructure.InfrastructurePart;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineOptions;
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
import eu.qualimaster.monitoring.events.SourceVolumeMonitoringEvent;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.MonitoringFrequency;
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
        
        try {
            new SourceVolumeAdaptationEvent("pip", "src", null);
            Assert.fail("no exception");
        } catch (IllegalArgumentException e) {
        }
        try {
            new SourceVolumeAdaptationEvent("pip", "src", new HashMap<String, Double>());
            Assert.fail("no exception");
        } catch (IllegalArgumentException e) {
        }
        SourceVolumeAdaptationEvent svae = new SourceVolumeAdaptationEvent("pip", "src", "$APL", 1000);
        Assert.assertEquals("pip", svae.getPipeline());
        Assert.assertEquals("src", svae.getSource());
        Map<String, Double> expected = new HashMap<String, Double>();
        expected.put("$APL", 1000.0);
        assertEquals(expected, svae.getFindings());
        
        svae = new SourceVolumeAdaptationEvent("pip", "src", expected);
        Assert.assertEquals("pip", svae.getPipeline());
        Assert.assertEquals("src", svae.getSource());
        assertEquals(expected, svae.getFindings());
    }
    
    /**
     * Asserts the equality of two string-double maps.
     * 
     * @param expected the expected map
     * @param actual the actual map
     */
    private void assertEquals(Map<String, Double> expected, Map<String, Double> actual) {
        if (null == expected) {
            Assert.assertNull(actual);    
        } else {
            Assert.assertNotNull(actual);
            Assert.assertEquals(expected.size(), actual.size());
            for (Map.Entry<String, Double> ent : expected.entrySet()) {
                Double actualVal = actual.get(ent.getKey());
                Assert.assertNotNull(actualVal);
                Assert.assertEquals(ent.getValue(), actualVal, 0.005);
            }
        }
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
        final AbstractReturnableEvent req = new AbstractReturnableEvent("abba-1223", "msg-09876551") {
            private static final long serialVersionUID = -4739552560324373138L;
        };
        
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
        
        ChangeMonitoringEvent cEvent = new ChangeMonitoringEvent(MonitoringFrequency.createMap(
            MonitoringFrequency.CLUSTER_MONITORING, 100), null);
        Assert.assertNull(cEvent.getPipeline());
        Assert.assertNull(cEvent.getPipelineElement());
        Assert.assertNull(cEvent.getObservables());
        assertEqualsFrequency(100, MonitoringFrequency.CLUSTER_MONITORING, cEvent);
        assertCause(null, cEvent);

        Map<IObservable, Boolean> eObs = new HashMap<IObservable, Boolean>();
        eObs.put(Scalability.VARIETY, false);
        cEvent = new ChangeMonitoringEvent(MonitoringFrequency.createAllMap(0), eObs, req);
        Assert.assertNull(cEvent.getPipeline());
        Assert.assertNull(cEvent.getPipelineElement());
        Assert.assertEquals(eObs, cEvent.getObservables());
        assertEqualsAllFrequencies(0, cEvent);
        assertCause(req, cEvent);

        eObs.put(Scalability.VARIETY, true);
        cEvent = new ChangeMonitoringEvent(pipeline, null, eObs, req);
        Assert.assertEquals(pipeline, cEvent.getPipeline());
        Assert.assertNull(cEvent.getPipelineElement());
        Assert.assertEquals(eObs, cEvent.getObservables());
        assertEqualsFrequency(null, null, cEvent);
        assertCause(req, cEvent);

        cEvent = new ChangeMonitoringEvent(pipeline, pipelineElement, 
            MonitoringFrequency.createMap(MonitoringFrequency.CLUSTER_MONITORING, 100), eObs, req);
        Assert.assertEquals(pipeline, cEvent.getPipeline());
        Assert.assertEquals(pipelineElement, cEvent.getPipelineElement());
        Assert.assertEquals(eObs, cEvent.getObservables());
        assertEqualsFrequency(100, MonitoringFrequency.CLUSTER_MONITORING, cEvent);
        assertCause(req, cEvent);
        
        SubTopologyMonitoringEvent sEvent = new SubTopologyMonitoringEvent(pipeline, null, null);
        Assert.assertEquals(pipeline, sEvent.getPipeline());
        Assert.assertNull(sEvent.getStructure());
    }

    /**
     * Asserts the message cause.
     * 
     * @param expected the expected cause
     * @param evt the actual event with cause information embedded
     */
    private static final void assertCause(IReturnableEvent expected, IResponseEvent evt) {
        if (null == expected) {
            Assert.assertNull(evt.getReceiverId());
            Assert.assertNull(evt.getMessageId());
        } else {
            Assert.assertEquals(expected.getSenderId(), evt.getReceiverId());
            Assert.assertEquals(expected.getMessageId(), evt.getMessageId());
        }
    }

    /**
     * Asserts equality of all frequency types.
     * 
     * @param expected the expected value
     * @param event the monitoring change event
     */
    private static final void assertEqualsAllFrequencies(Integer expected, ChangeMonitoringEvent event) {
        for (MonitoringFrequency freq : MonitoringFrequency.values()) {
            assertEqualsFrequency(expected, freq, event);
        }
    }
    
    /**
     * Asserts equality of frequencies.
     * 
     * @param expected the expected value
     * @param freq the frequency type (may be <b>null</b>)
     * @param event the monitoring change event
     */
    private static final void assertEqualsFrequency(Integer expected, MonitoringFrequency freq, 
        ChangeMonitoringEvent event) {
        if (null == expected) {
            Assert.assertTrue(null == event.getFrequencies());
            if (null != freq) {
                Assert.assertTrue(null == event.getFrequencies().get(freq));
                Assert.assertNull(event.getFrequency(freq));
            }
        } else {
            Assert.assertNotNull(event.getFrequencies());
            Assert.assertNotNull(freq);
            Assert.assertEquals(expected, event.getFrequencies().get(freq));
            Assert.assertEquals(expected, event.getFrequency(freq));
        }
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
     * Tests source monitoring events.
     */
    @Test
    public void testSourceMonitoringEvents() {
        final String pipeline = "pip";
        final String source = "src";
        final Map<String, Integer> obs = new HashMap<String, Integer>();
        obs.put("tweet1", 100);
        obs.put("tweet2", 1);
        SourceVolumeMonitoringEvent evt = new SourceVolumeMonitoringEvent(pipeline, source, obs);
        Assert.assertEquals(pipeline, evt.getPipeline());
        Assert.assertEquals(source, evt.getPipelineElement());
        Assert.assertEquals(obs, evt.getObservations());
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
        AlgorithmMonitoringEvent evt = new AlgorithmMonitoringEvent("123", "alg", ResourceUsage.USED_MEMORY, 10);
        Assert.assertEquals("alg", evt.getAlgorithmId());
        Assert.assertEquals(ResourceUsage.USED_MEMORY, evt.getObservable());
        Assert.assertEquals(10, evt.getValue(), 0.05);
        Assert.assertEquals("123", evt.getPipeline());
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
        
        PipelineOptions opts = new PipelineOptions();
        lEvt = new PipelineLifecycleEvent("pip", PipelineLifecycleEvent.Status.CHECKING, opts, evt);
        Assert.assertEquals("pip", lEvt.getPipeline());
        Assert.assertEquals(PipelineLifecycleEvent.Status.CHECKING, lEvt.getStatus());
        Assert.assertEquals(opts, lEvt.getOptions());
        Assert.assertEquals("1234", lEvt.getCauseMessageId());
        Assert.assertEquals("abcd", lEvt.getCauseSenderId());
        
        lEvt = new PipelineLifecycleEvent(lEvt, PipelineLifecycleEvent.Status.CHECKED);
        Assert.assertEquals("pip", lEvt.getPipeline());
        Assert.assertEquals(PipelineLifecycleEvent.Status.CHECKED, lEvt.getStatus());
        Assert.assertEquals(opts, lEvt.getOptions());
        Assert.assertEquals("1234", lEvt.getCauseMessageId());
        Assert.assertEquals("abcd", lEvt.getCauseSenderId());
        
        opts = new PipelineOptions();
        lEvt = new PipelineLifecycleEvent(lEvt, PipelineLifecycleEvent.Status.CHECKED, opts);
        Assert.assertEquals("pip", lEvt.getPipeline());
        Assert.assertEquals(PipelineLifecycleEvent.Status.CHECKED, lEvt.getStatus());
        Assert.assertEquals(opts, lEvt.getOptions());
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
    
    /**
     * Tests the end-of-data event.
     */
    @Test
    public void testEndOfDataEvent() {
        EndOfDataEvent eod = new EndOfDataEvent("pip", "src");
        Assert.assertEquals("pip", eod.getPipeline());
        Assert.assertEquals("src", eod.getSource());
    }
    
    /**
     * "Tests" the forward declaration.
     */
    @Test
    public void testInfrastructurePart() {
        InfrastructurePart[] parts = InfrastructurePart.values();
        for (int i = 0; i < parts.length; i++) {
            Assert.assertNotNull(InfrastructurePart.valueOf(parts[i].name()));
        }
    }
    
}
