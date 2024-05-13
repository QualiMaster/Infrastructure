package tests.eu.qualimaster.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.Configuration;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.IEvent;
import eu.qualimaster.infrastructure.InfrastructureEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.logging.events.LoggingFilterEvent;

/**
 * Tests the remote functionality.
 * 
 * @author Holger Eichelberger
 */
public class RemoteHandlerTests {
    
    /**
     * Test remote functionality.
     */
    @Test(timeout = 5000 + EventManager.SO_TIMEOUT)
    public void testRemote() {
        Configuration.configureLocal();
        EventManager.startServer();
        RecordingEventHandler<InfrastructureEvent> rec = RecordingEventHandler.create(InfrastructureEvent.class);
        EventManager.register(rec);
        
        EventManager local = new EventManager();
        local.doStart(false, false); // enforce the case that we are not local
        Map<String, PipelineLifecycleEvent> expected = new HashMap<String, PipelineLifecycleEvent>();
        PipelineLifecycleEvent evt = new PipelineLifecycleEvent("pipeline", Status.STARTING, null);
        registerExpected(expected, evt);
        local.doSend(evt); // the pipeline case
        PipelineLifecycleEvent evt2 = new PipelineLifecycleEvent("pipeline1", Status.STARTING, null);
        registerExpected(expected, evt2);
        local.doCleanup();
        
        try { // wait for transmission
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        
        EventManager.cleanup();
        local.doStop();
        EventManager.stop();
        
        // evt was serialized!
        Assert.assertTrue(rec.getReceivedCount() > 0);
        for (int i = 0; i < rec.getReceivedCount(); i++) {
            IEvent tmp = rec.getReceived(i);
            if (tmp instanceof PipelineLifecycleEvent) {
                PipelineLifecycleEvent iEvent = (PipelineLifecycleEvent) rec.getReceived(i);
                PipelineLifecycleEvent exp = expected.get(iEvent.getPipeline());
                assertLifecylcleEvent(exp, iEvent);
            }
        }
        EventManager.unregister(rec);
        System.out.println();
    }

    /**
     * Asserts the equality of a lifecycle event.
     * 
     * @param expected the expected event
     * @param actual the actual event
     */
    private static void assertLifecylcleEvent(PipelineLifecycleEvent expected, PipelineLifecycleEvent actual) {
        boolean result = null != expected && null != actual; 
        result &= expected.getPipeline().equals(actual.getPipeline()); 
        result &= expected.getStatus().equals(actual.getStatus());
        Assert.assertTrue(result);
    }
    
    /**
     * Registers an expected lifecyle event.
     * 
     * @param expected the name-object map of expected events
     * @param evt the event to be registered
     */
    private static void registerExpected(Map<String, PipelineLifecycleEvent> expected, PipelineLifecycleEvent evt) {
        expected.put(evt.getPipeline(), evt);
    }
    
    /**
     * Tests local functionality.
     */
    @Test(timeout = 5000 + EventManager.SO_TIMEOUT)
    public void testLocal() {
        Configuration.configureLocal();
        EventManager.start(); // legacy
        RecordingEventHandler<InfrastructureEvent> rec = RecordingEventHandler.create(InfrastructureEvent.class);
        EventManager.register(rec);

        PipelineLifecycleEvent evt = new PipelineLifecycleEvent("pipeline", Status.CHECKING, null);
        EventManager.send(evt); // the pipeline case
        EventManager.cleanup();
        EventManager.stop();
        
        // evt not serialized
        Assert.assertTrue(rec.received(evt));
        EventManager.unregister(rec);
        System.out.println();
    }

    /**
     * Tests local functionality.
     */
    @Test(timeout = 5000 + EventManager.SO_TIMEOUT)
    public void testLocalAsync() {
        Configuration.configureLocal();
        EventManager.start(); // legacy
        RecordingEventHandler<InfrastructureEvent> rec = RecordingEventHandler.create(InfrastructureEvent.class);
        EventManager.register(rec);

        PipelineLifecycleEvent evt = new PipelineLifecycleEvent("pipeline", Status.CHECKING, null);
        EventManager.send(evt); // the pipeline case
        while (!rec.received(evt)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        EventManager.cleanup();
        EventManager.stop();
        
        // evt not serialized
        Assert.assertTrue(rec.received(evt));
        EventManager.unregister(rec);
        System.out.println();
    }

    /**
     * Tests local functionality.
     */
    @Test(timeout = 5000 + EventManager.SO_TIMEOUT)
    public void testLocalUnstarted() {
        Configuration.configureLocal();
        // do not start - autostart on send
        RecordingEventHandler<InfrastructureEvent> rec = RecordingEventHandler.create(InfrastructureEvent.class);
        EventManager.register(rec);

        PipelineLifecycleEvent evt = new PipelineLifecycleEvent("pipeline", Status.STARTING, null);
        EventManager.send(evt); // the pipeline case
        
        EventManager.cleanup();
        EventManager.stop();
        
        // evt not serialized
        Assert.assertTrue(rec.received(evt));
        EventManager.unregister(rec);
        System.out.println();
    }
    
    /**
     * Tests the forward functionality, i.e., a handler on the local manager.
     */
    @Test(timeout = 30000)
    public void testForward() {
        Configuration.configureLocal();
        EventManager.startServer();

        EventManager local = new EventManager();
        local.doStart(false, false); // enforce the case that we are not local
        RecordingEventHandler<LoggingFilterEvent> rec = RecordingEventHandler.create(LoggingFilterEvent.class);
        local.doRegister(rec); // this shall start the forwarding
        
        ArrayList<String> additions = new ArrayList<String>();
        additions.add(".*");
        LoggingFilterEvent evt = new LoggingFilterEvent(additions, null);
        EventManager.send(evt); // this shall be now handled by a forward handler - sending to the client for dispatch
        EventManager.cleanup();
        EventManager.stop();

        local.doCleanup();
        local.doStop();

        // evt was serialized!
        Assert.assertTrue(rec.getReceivedCount() > 0);
        boolean found = false;
        for (int i = 0; i < rec.getReceivedCount(); i++) {
            IEvent tmp = rec.getReceived(i);
            if (tmp instanceof LoggingFilterEvent) {
                LoggingFilterEvent iEvent = (LoggingFilterEvent) rec.getReceived(i);
                found = iEvent.getFilterAdditions().equals(evt.getFilterAdditions());
                found &= iEvent.getFilterRemovals().equals(evt.getFilterRemovals());
            }
        }
        Assert.assertTrue(found);
        EventManager.unregister(rec);
        System.out.println();
    }

}