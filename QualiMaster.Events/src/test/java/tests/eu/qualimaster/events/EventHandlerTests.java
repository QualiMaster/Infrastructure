package tests.eu.qualimaster.events;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eu.qualimaster.Configuration;
import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.IEvent;
import eu.qualimaster.infrastructure.InfrastructureEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;

/**
 * Tests the local event handler.
 * 
 * @author Holger Eichelberger
 */
public class EventHandlerTests {

    private RecordingEventHandler<InfrastructureEvent> rec1;
    private RecordingEventHandler<InfrastructureEvent> rec2;
    private RecordingEventHandler<AdaptationEvent> other;
    private List<RecordingEventHandler<? extends IEvent>> infra;
    private List<RecordingEventHandler<? extends IEvent>> others;
    private List<RecordingEventHandler<? extends IEvent>> all;
    
    /**
     * Handles a message and checks for the recording of the events.
     * 
     * @param event the event to be handled
     * @param handling those handlers that shall have received the event
     * @param noHandling those handlers that shall not have received the event
     * @throws InterruptedException in case that sleeping was interrupted (shall not occur)
     */
    private void handleAndCheck(IEvent event, Iterable<RecordingEventHandler<? extends IEvent>> handling, 
        Iterable<RecordingEventHandler<? extends IEvent>> noHandling) throws InterruptedException {
        EventManager.handle(event);
        EventManager.cleanup();
        if (null != handling) {
            for (RecordingEventHandler<? extends IEvent> h : handling) {
                Assert.assertTrue(h.received(event));
            }
        }
        if (null != noHandling) {
            for (RecordingEventHandler<? extends IEvent> h : noHandling) {
                Assert.assertFalse(h.received(event));
            }
        }
    }
    
    /**
     * Sets up the scene before starting a single test.
     */
    @Before
    public void setUp() {
        rec1 = RecordingEventHandler.create(InfrastructureEvent.class);
        rec2 = RecordingEventHandler.create(InfrastructureEvent.class);
        other = RecordingEventHandler.create(AdaptationEvent.class);

        infra = new ArrayList<RecordingEventHandler<? extends IEvent>>();
        infra.add(rec1);
        infra.add(rec2);

        others = new ArrayList<RecordingEventHandler<? extends IEvent>>();
        others.add(other);

        all = new ArrayList<RecordingEventHandler<? extends IEvent>>();
        all.addAll(infra);
        all.addAll(others);

        EventManager.disableLoggingFor((Class<? extends IEvent>) null);
        EventManager.disableLoggingFor("");
        EventManager.clearLoggingSettingsFor(null);
        
        EventManager.register(rec1);
        EventManager.register(rec2);
        EventManager.register(other);
    }

    /**
     * Cleans up after a single test.
     */
    @After
    public void tearDown() {
    }
    
    /**
     * Tests the event handler.
     * 
     * @throws InterruptedException in case that sleeping was interrupted (shall not occur)
     */
    @Test(timeout = 3000)
    public void testEventManagerNotStarted() throws InterruptedException {
        Configuration.configureLocal();
        EventManager.initLegacy();
        // nothing shall happen as manager is not started
        EventManager.handle(new PipelineLifecycleEvent("test", Status.STOPPED, null));
        handleAndCheck(new PipelineLifecycleEvent("test", Status.STOPPED, null), null, all);
    }

    /**
     * Tests start/stop behavior of the event handler/manager.
     * 
     * @throws InterruptedException in case that sleeping was interrupted (shall not occur)
     */
    @Test(timeout = 3000)
    public void testEventManagerStartStop() throws InterruptedException {
        Configuration.configureLocal();
        EventManager.start();
        Thread.sleep(500);
        handleAndCheck(new PipelineLifecycleEvent("test1", Status.STOPPED, null), infra, others);

        EventManager.cleanup();
        EventManager.stop();
        Thread.sleep(500);
        // nothing shall happen as manager is stopped
        handleAndCheck(new PipelineLifecycleEvent("test2", Status.STOPPED, null), null, all);
    }
    
    /**
     * Tests registering / unregistering event handlers.
     * 
     * @throws InterruptedException in case that sleeping was interrupted (shall not occur)
     */
    @Test(timeout = 3000 + EventManager.SO_TIMEOUT)
    public void testEventManagerStartUnregister() throws InterruptedException {
        Configuration.configureLocal();
        EventManager.start();
        Thread.sleep(500);
        
        EventManager.unregister(rec2);
        infra.remove(rec2);
        all.remove(rec2);

        handleAndCheck(new PipelineLifecycleEvent("test1", Status.STOPPED, null), infra, others);

        EventManager.cleanup();
        EventManager.stop();
        Thread.sleep(500);
        // nothing shall happen as manager is stopped
        handleAndCheck(new PipelineLifecycleEvent("test2", Status.STOPPED, null), null, all);
    }

    /**
     * Tests inherited event handlers.
     * 
     * @throws InterruptedException in case that sleeping was interrupted (shall not occur)
     */
    @Test(timeout = 3000)
    public void testSuperClassHandler() throws InterruptedException {
        Configuration.configureLocal();
        EventManager.start();
        Thread.sleep(500);

        RecordingEventHandler<IEvent> recAll = RecordingEventHandler.create(IEvent.class);
        EventManager.register(recAll);
        
        PipelineLifecycleEvent evt = new PipelineLifecycleEvent("test1", Status.STOPPED, null);
        handleAndCheck(evt, infra, others);
        Assert.assertTrue(recAll.received(evt));
        
        EventManager.unregister(recAll);
        EventManager.cleanup();
        EventManager.stop();
        Thread.sleep(500);
    }

    /**
     * Tests channeling event handlers.
     * 
     * @throws InterruptedException in case that sleeping was interrupted (shall not occur)
     */
    @Test(timeout = 3000)
    public void testChannelHandler() throws InterruptedException {
        
        final String channel = "myChannel";

        /**
         * A test-local channel event.
         * 
         * @author Holger Eichelberger
         */
        class ChannelEvent implements IEvent {

            private static final long serialVersionUID = 5122771645612942161L;

            @Override
            public String getChannel() {
                return channel;
            }
            
        }

        Configuration.configureLocal();
        EventManager.start();
        Thread.sleep(500);
        RecordingEventHandler<IEvent> recAll = RecordingEventHandler.create(IEvent.class);
        RecordingEventHandler<IEvent> recChannel = RecordingEventHandler.create(IEvent.class, channel);
        RecordingEventHandler<ChannelEvent> recChannel2 = RecordingEventHandler.create(ChannelEvent.class, channel);
        RecordingEventHandler<IEvent> recChannel3 = RecordingEventHandler.create(IEvent.class, "otherChannel");
        EventManager.register(recAll);
        EventManager.register(recChannel);
        EventManager.register(recChannel2);
        EventManager.register(recChannel3);

        // normal event, normal reception also in recAll but not in the channel handlers
        PipelineLifecycleEvent evt1 = new PipelineLifecycleEvent("test1", Status.STOPPED, null);
        handleAndCheck(evt1, infra, others);
        Assert.assertTrue(recAll.received(evt1));
        Assert.assertFalse(recChannel.received(evt1));
        Assert.assertFalse(recChannel2.received(evt1));
        Assert.assertFalse(recChannel3.received(evt1));

        // channel event, no reception in normal handlers due to type, reception in recAll (as it catches all), 
        // reception in the specific channel handlers except for recChannel3 which reacts on another channel
        ChannelEvent evt2 = new ChannelEvent();
        handleAndCheck(evt2, null, all);
        Assert.assertTrue(recAll.received(evt2));
        Assert.assertTrue(recChannel.received(evt2));
        Assert.assertTrue(recChannel2.received(evt2));
        Assert.assertFalse(recChannel3.received(evt2));

        EventManager.unregister(recChannel3);
        EventManager.unregister(recChannel2);
        EventManager.unregister(recChannel);
        EventManager.unregister(recAll);
        EventManager.cleanup();
        EventManager.stop();
        Thread.sleep(500);
    }
    
}
