package tests.eu.qualimaster.adaptation;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.adaptation.Log4jLoggingBack;
import eu.qualimaster.adaptation.events.ReplayAdaptationEvent;
import eu.qualimaster.adaptation.external.AlgorithmChangedMessage;
import eu.qualimaster.adaptation.external.AuthenticateMessage;
import eu.qualimaster.adaptation.external.ChangeParameterRequest;
import eu.qualimaster.adaptation.external.ClientEndpoint;
import eu.qualimaster.adaptation.external.CloudPipelineMessage;
import eu.qualimaster.adaptation.external.ConfigurationChangeMessage;
import eu.qualimaster.adaptation.external.ConnectedMessage;
import eu.qualimaster.adaptation.external.DisconnectRequest;
import eu.qualimaster.adaptation.external.DispatcherAdapter;
import eu.qualimaster.adaptation.external.Endpoint;
import eu.qualimaster.adaptation.external.ExecutionResponseMessage;
import eu.qualimaster.adaptation.external.ExecutionResponseMessage.ResultType;
import eu.qualimaster.adaptation.external.HardwareAliveMessage;
import eu.qualimaster.adaptation.external.HilariousAuthenticationHelper;
import eu.qualimaster.adaptation.external.IDispatcher;
import eu.qualimaster.adaptation.external.Logging;
import eu.qualimaster.adaptation.external.Logging.ILoggingBack;
import eu.qualimaster.adaptation.internal.ServerEndpoint;
import eu.qualimaster.adaptation.external.LoggingFilterRequest;
import eu.qualimaster.adaptation.external.LoggingMessage;
import eu.qualimaster.adaptation.external.Message;
import eu.qualimaster.adaptation.external.MonitoringDataMessage;
import eu.qualimaster.adaptation.external.PipelineMessage;
import eu.qualimaster.adaptation.external.PipelineStatusRequest;
import eu.qualimaster.adaptation.external.PipelineStatusResponse;
import eu.qualimaster.adaptation.external.ReplayMessage;
import eu.qualimaster.adaptation.external.RequestMessage;
import eu.qualimaster.adaptation.external.ResourceChangeMessage;
import eu.qualimaster.adaptation.external.ResourceChangeMessage.Status;
import eu.qualimaster.adaptation.external.SwitchAlgorithmRequest;
import eu.qualimaster.adaptation.external.UpdateCloudResourceMessage;
import eu.qualimaster.adaptation.external.Utils;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Tests the (preliminary) external connection.
 */
public class ExternalTests {
    
    private static final String[] ACTIVE_PIPELINES = {"pip1", "pip2"};
    private static final String ALGORITHM_RESPONSE_TEXT = "Algorithm";
    private static final String PARAMETER_RESPONSE_TEXT = "Algorithm";
    
    private static final int SERVER_DISPATCHER = 0;
    private static final int CLIENT1_DISPATCHER = 1;
    private static final int CLIENT2_DISPATCHER = 2;
    private static final int COUNT_DISPATCHER = 3;

    /**
     * Defines the basic interface for a message handler. 
     * 
     * @param <M> the message type
     * @author Holger Eichelberger
     */
    private abstract static class MessageHandler<M extends Message> {
        
        private Class<M> cls;
        
        /**
         * Creates a message handler.
         * 
         * @param cls the handled class
         */
        protected MessageHandler(Class<M> cls) {
            this.cls = cls;
        }
        
        /**
         * Called to handle the received message.
         * 
         * @param msg the message
         * @return <code>true</code> if the message was correct, <code>false</code> else
         */
        protected abstract boolean handle(M msg);
        
        /**
         * Returns the class of messages handled.
         * 
         * @return the class of messages
         */
        protected Class<M> handles() {
            return cls;
        }
        
        /**
         * Called internally to handle a generic message.
         * 
         * @param msg the message to handle
         * @return whether the message was handled
         */
        boolean doHandle(Message msg) {
            return handle(handles().cast(msg));
        }

    }
    
    /**
     * A default execution response message handler doing test assertions.
     * 
     * @author Holger Eichelberger
     */
    private static class DefaultAssertingExecutionResponseMessageHandler 
        extends MessageHandler<ExecutionResponseMessage> {
        
        private RequestMessage msg;
        private ResultType result;
        private String description;

        /**
         * Creates a default handler instance.
         * 
         * @param msg the message to assert for (checking the client id)
         * @param result the expected execution result
         * @param description the expected description
         */
        private DefaultAssertingExecutionResponseMessageHandler(RequestMessage msg, ResultType result, 
            String description) {
            super(ExecutionResponseMessage.class);
            this.msg = msg;
            this.result = result;
            this.description = description;
        }

        @Override
        public boolean handle(ExecutionResponseMessage msg) {
            Assert.assertEquals(this.msg.getClientId(), msg.getClientId());
            Assert.assertEquals(this.msg.getMessageId(), msg.getMessageId());
            Assert.assertEquals(result, msg.getResult());
            Assert.assertEquals(description, msg.getDescription());
            Assert.assertTrue(msg.equals(msg));
            Assert.assertFalse(msg.equals(null));
            msg.toInformation(); // can be null or an instance
            msg.hashCode();
            return true;
        }
        
    }
    
    /**
     * Implements a pipeline status response message handler.
     * 
     * @author Holger Eichelberger
     */
    private static class PipelineStatusResponseMessageHandler extends MessageHandler<PipelineStatusResponse> {
        
        private String[] expectedPipelines;

        /**
         * Creates the handler.
         * 
         * @param expectedPipelines the expected pipeline names
         */
        private PipelineStatusResponseMessageHandler(String... expectedPipelines) {
            super(PipelineStatusResponse.class);
            this.expectedPipelines = expectedPipelines;
        }

        @Override
        protected boolean handle(PipelineStatusResponse msg) {
            if (null == expectedPipelines) {
                Assert.assertEquals(0, msg.getActivePipelinesCount());
            } else {
                Assert.assertEquals(expectedPipelines.length, msg.getActivePipelinesCount());
                for (int p = 0; p < expectedPipelines.length; p++) {
                    Assert.assertEquals(expectedPipelines[p], msg.getActivePipelineName(p));
                }
            }
            msg.toInformation(); // can be null or an instance
            msg.hashCode();
            return true;
        }
        
    }
    
    /**
     * Implements a local dispatcher for testing.
     * 
     * @author Holger Eichelberger
     */
    private class LocalDispatcher implements IDispatcher {

        private int unhandledExecutionResponseMessageCount = 0;
        private int unhandledFailedExecutionResponseMessageCount = 0;
        private boolean allowPassThroughs;
        private Endpoint responseEndpoint;
        private Map<Class<? extends Message>, MessageHandler<? extends Message>> handlers = new HashMap<>();
        private List<Message> required = Collections.synchronizedList(new LinkedList<Message>());
        private List<Message> forbidden = Collections.synchronizedList(new LinkedList<Message>());
        
        /**
         * Creates a local dispatcher without endpoint for automatic responses.
         * 
         * @param allowPassThroughs whether pass throughs are allowed (requires authentication)
         */
        private LocalDispatcher(boolean allowPassThroughs) {
            this(null, allowPassThroughs);
        }

        /**
         * Creates a local dispatcher with given endpoint for automatic responses.
         * 
         * @param responseEndpoint the endpoint for responses (may be <b>null</b> for none)
         * @param allowPassThroughs whether pass throughs are allowed (requires authentication)
         */
        private LocalDispatcher(Endpoint responseEndpoint, boolean allowPassThroughs) {
            setResponseEndpoint(responseEndpoint);
            this.allowPassThroughs = allowPassThroughs;
        }
        
        /**
         * Changes the response endpoint.
         * 
         * @param responseEndpoint the response endpoint
         */
        private void setResponseEndpoint(Endpoint responseEndpoint) {
            this.responseEndpoint = responseEndpoint;
        }

        /**
         * Returns the number of unhandled execution response messages.
         * 
         * @return the number of unhandled execution response messages
         */
        private int getUnhandledExecutionResponseMessageCount() {
            return unhandledExecutionResponseMessageCount;
        }

        /**
         * Returns the number of unhandled successful execution response messages.
         * 
         * @return the number of unhandled successful execution response messages
         */
        private int getUnhandledSuccessfulExecutionResponseMessageCount() {
            return unhandledExecutionResponseMessageCount - unhandledFailedExecutionResponseMessageCount;
        }
        
        /**
         * Returns the number of failed unhandled execution response messages.
         * 
         * @return the number of failed unhandled execution response messages
         */
        private int getUnhandledFailedExecutionResponseMessageCount() {
            return unhandledFailedExecutionResponseMessageCount;
        }
        
        /**
         * Adds a message handler.
         * 
         * @param handler the message handler
         */
        private void addHandler(MessageHandler<?> handler) {
            handlers.put(handler.handles(), handler);
        }

        /**
         * Removes a message handler.
         * 
         * @param handler the message handler
         */
        private void removeHandler(MessageHandler<?> handler) {
            handlers.remove(handler.handles());
        }
        
        /**
         * Handles a message via {@link #handlers}.
         * 
         * @param message the message
         * @return whether the message was handled
         */
        private boolean handle(Message message) {
            boolean done = false;
            MessageHandler<?> handler = handlers.get(message.getClass());
            if (null != handler) {
                done = handler.doHandle(message);
            }
            return done;
        }

        /**
         * Asserts basic capabilities of <code>msg</code>.
         * 
         * @param msg th4e message
         */
        private void assertMessage(Message msg) {
            Assert.assertTrue(msg.equals(msg));
            Assert.assertFalse(msg.equals(null));
            msg.hashCode();
        }

        /**
         * Tests the message for legal/illegal reception.
         * 
         * @param msg the message
         */
        private void test(Message msg) {
            assertMessage(msg);
            if (forbidden.contains(msg)) {
                Assert.fail("received forbidden message " + msg);
            }
            required.remove(msg);
        }
        
        @Override
        public void handleDisconnectRequest(DisconnectRequest msg) {
            test(msg);
        }

        @Override
        public void handleSwitchAlgorithmRequest(SwitchAlgorithmRequest msg) {
            test(msg);
            autoRespond(msg, ResultType.SUCCESSFUL, ALGORITHM_RESPONSE_TEXT);
        }

        @Override
        public void handleMonitoringDataMessage(MonitoringDataMessage msg) {
            if (!allowPassThroughs) {
                Assert.fail("illegal passthrough");
            } else {
                test(msg);
            }
        }
        
        /**
         * Adds a required message to be received by the local dispatcher.
         * 
         * @param msg the required message
         */
        public void require(Message msg) {
            required.add(msg);
            assertMessage(msg);
        }

        /**
         * Adds a forbidden message not to be received by the local dispatcher.
         * 
         * @param msg the required message
         */
        public void forbidden(Message msg) {
            forbidden.add(msg);
            assertMessage(msg);
        }
        
        @Override
        public void handleAlgorithmChangedMessage(AlgorithmChangedMessage msg) {
            test(msg);
        }

        @Override
        public void handleHardwareAliveMessage(HardwareAliveMessage msg) {
            test(msg);
        }

        @Override
        public void handlePipelineMessage(PipelineMessage msg) {
            test(msg);
        }

        @Override
        public void handleLoggingMessage(LoggingMessage msg) {
            test(msg);
        }

        @Override
        public void handleLoggingFilterRequest(LoggingFilterRequest msg) {
            test(msg);
        }

        @Override
        public void handleChangeParameterRequest(ChangeParameterRequest<?> msg) {
            test(msg);
            autoRespond(msg, ResultType.SUCCESSFUL, PARAMETER_RESPONSE_TEXT);
        }
        
        /**
         * Creates a response in case that {@link #responseEndpoint} is set.
         * 
         * @param request the request to create the response for
         * @param status the execution status
         * @param description the exection description
         */
        private void autoRespond(RequestMessage request, ResultType status, String description) {
            if (null != responseEndpoint) {
                responseEndpoint.schedule(new ExecutionResponseMessage(request, status, description));
            }
        }
        
        @Override
        public void handleExecutionResponseMessage(ExecutionResponseMessage msg) {
            test(msg);
            if (!handle(msg)) {
                unhandledExecutionResponseMessageCount++;
                if (ResultType.FAILED == msg.getResult()) {
                    unhandledFailedExecutionResponseMessageCount++;    
                }
            }
        }

        @Override
        public void handlePipelineStatusRequest(PipelineStatusRequest msg) {
            test(msg);
            if (null != responseEndpoint) {
                responseEndpoint.schedule(new PipelineStatusResponse(msg, ACTIVE_PIPELINES));
            }
        }

        @Override
        public void handlePipelineStatusResponse(PipelineStatusResponse msg) {
            test(msg);
            handle(msg);
        }

        @Override
        public void handleUpdateCloudResourceMessage(UpdateCloudResourceMessage msg) {
            test(msg);
            handle(msg);
        }

        @Override
        public void handleCloudPipelineMessage(CloudPipelineMessage msg) {
            test(msg);
            handle(msg);
        }

        @Override
        public void handleReplayMessage(ReplayMessage msg) {
            test(msg);
            handle(msg);
        }

        @Override
        public void handleConfigurationChangeMessage(ConfigurationChangeMessage msg) {
            test(msg);
            handle(msg);
        }

        @Override
        public void handleResourceChangeMessage(ResourceChangeMessage msg) {
            test(msg);
            handle(msg);
        }

    }
    
    /**
     * Tests an authentication scenario.
     * 
     * @throws IOException shall not occur
     */
    @Test(timeout = 10000)
    public void testAuthentication() throws IOException {
        final int port = 6000;
        LocalDispatcher serverDispatcher = new LocalDispatcher(false);
        ServerEndpoint server = new ServerEndpoint(serverDispatcher, port, TestAuthenticationSupport.PROVIDER);
        server.start();
        
        LocalDispatcher clientDispatcher = new LocalDispatcher(true);
        ClientEndpoint client = new ClientEndpoint(clientDispatcher, InetAddress.getLocalHost(), port);
        AuthenticateMessage aMsg = new AuthenticateMessage("Me", 
            HilariousAuthenticationHelper.obtainPassphrase("here"));
        client.schedule(aMsg); // shall fail
        TestAuthenticationSupport.authenticate(client, "JohnDoe");
        
        sleep(500);
        Assert.assertTrue(client.isAuthenticated());
       
        ConnectedMessage cMsg = new ConnectedMessage();
        client.schedule(cMsg);
        
        Message msg = new SwitchAlgorithmRequest("pipeline", "correlation", "hardware");
        Message elev = msg.elevate();
        serverDispatcher.require(elev);
        client.schedule(elev); // shall be there as authenticated

        Map<String, Double> observations = new HashMap<String, Double>();
        observations.put(TimeBehavior.ENACTMENT_DELAY.name(), 200.0);
        msg = new MonitoringDataMessage("correlation", observations);
        clientDispatcher.require(msg); // shall be there as authenticated
        server.schedule(msg);

        sleep(1000);
        
        msg = new DisconnectRequest();
        serverDispatcher.require(msg);
        client.schedule(msg);

        sleep(1000);
        Assert.assertTrue(!client.isAuthenticated());

        client.stop();
        server.stop();

        while (client.hasWork() || server.hasWork()) {
            sleep(1000);
        }

        sleep(1000);
        
        Assert.assertEquals(1, clientDispatcher.getUnhandledFailedExecutionResponseMessageCount()); // failed auth
        Assert.assertEquals(2, clientDispatcher.getUnhandledSuccessfulExecutionResponseMessageCount()); // auth, conn
    }
    
    /**
     * Tests unauthenticated messages.
     * 
     * @throws IOException in case of connection failures
     */
    @Test(timeout = 10000)
    public void testConnection() throws IOException {
        final int port = 6000;
        LocalDispatcher serverDispatcher = new LocalDispatcher(false);
        ServerEndpoint server = new ServerEndpoint(serverDispatcher, port, TestAuthenticationSupport.PROVIDER);
        server.start();

        try {
            new ServerEndpoint(serverDispatcher, port, TestAuthenticationSupport.PROVIDER);
        } catch (IOException e) {
            Assert.assertTrue(e instanceof java.net.BindException); // already in use
        }
        
        LocalDispatcher clientDispatcher = new LocalDispatcher(false);
        ClientEndpoint client = new ClientEndpoint(clientDispatcher, InetAddress.getLocalHost(), port);
        Message msg = new SwitchAlgorithmRequest("pipeline", "correlation", "hardware");
        serverDispatcher.require(msg);
        client.schedule(msg);
        Message elev = msg.elevate();
        clientDispatcher.require(elev);
        client.schedule(elev); // shall go through
        sleep(500); // equal object identity with next elev - passed to server and fail - wait for processing
        elev = msg.elevate();
        serverDispatcher.forbidden(msg); // shall not occur as not authenticated
        server.schedule(msg); 
        
        Map<String, Double> observations = new HashMap<String, Double>();
        observations.put(TimeBehavior.ENACTMENT_DELAY.name(), 200.0);
        msg = new MonitoringDataMessage("correlation", observations);
        //clientDispatcher.require(msg); // shall not be sent as not authenticated
        server.schedule(msg);

        msg = new HardwareAliveMessage("aaa");
        clientDispatcher.require(msg);
        server.schedule(msg);

        msg = new HardwareAliveMessage(null);
        clientDispatcher.require(msg);
        server.schedule(msg);
        
        msg = new AlgorithmChangedMessage("pipeline", "correlation", "changed");
        clientDispatcher.require(msg);
        server.schedule(msg);

        msg = new PipelineMessage("pipeline", PipelineMessage.Status.START);
        clientDispatcher.require(msg);
        server.schedule(msg);

        testLogging(server, client, serverDispatcher, clientDispatcher);

        sleep(1000);

        msg = new DisconnectRequest();
        serverDispatcher.require(msg);
        client.schedule(msg);
        
        while (client.hasWork() || server.hasWork()) {
            sleep(1000);
        }

        client.stop();
        server.stop();

        sleep(1000);
    }
    
    /**
     * Sleeps for <code>millis</code> milliseconds.
     * 
     * @param millis the milliseconds to sleep for
     */
    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }
    
    /**
     * Sends some logging messages.
     * 
     * @param server the server endpoint
     * @param client the client endpoint
     * @param serverDispatcher the dispatcher requiring server messages
     * @param clientDispatcher the dispatcher requiring client messages
     * 
     * @throws UnknownHostException shall not occur
     */
    private void testLogging(ServerEndpoint server, ClientEndpoint client, LocalDispatcher serverDispatcher, 
        LocalDispatcher clientDispatcher) throws UnknownHostException {
        LoggingFilterRequest req = new LoggingFilterRequest(null, null);
        client.schedule(req);
        serverDispatcher.require(req);
        
        ArrayList<String> tmp = new ArrayList<String>();
        tmp.add(".*");
        req = new LoggingFilterRequest(tmp, null);
        client.schedule(req);
        serverDispatcher.require(req);

        req = new LoggingFilterRequest(null, tmp);
        client.schedule(req);
        serverDispatcher.require(req);

        req = new LoggingFilterRequest(tmp, tmp);
        client.schedule(req);
        serverDispatcher.require(req);

        LoggingMessage msg = new LoggingMessage(1000, "Level", "test", "test", InetAddress.getLocalHost());
        server.schedule(msg);
        clientDispatcher.require(msg);
        
        msg = new LoggingMessage(1000, null, "test", null, InetAddress.getLocalHost());
        server.schedule(msg);
        clientDispatcher.require(msg);
    }
    
    /**
     * Tests the methods in {@link Utils}.
     */
    @Test
    public void testUtils() {
        Assert.assertTrue(Utils.equals(null, null));
        Assert.assertFalse(Utils.equals(null, ""));
        Assert.assertFalse(Utils.equals("", null));
        Assert.assertTrue(Utils.equals("", ""));
        
        Assert.assertEquals(0, Utils.hashCode(null));
        Assert.assertEquals("".hashCode(), Utils.hashCode(""));
        
        TestLogBack lb = new TestLogBack();
        Logging.setBack(null);
        Logging.setBack(lb);
        Logging.error("ERROR");
        Assert.assertEquals("ERROR", lb.getText());
        Assert.assertNull(lb.getThrowable());
        Throwable t = new Throwable("INTENDED THROWABLE!!!");
        Logging.error("ERROR", t);
        Assert.assertEquals("ERROR", lb.getText());
        Assert.assertEquals(t, lb.getThrowable());
        Logging.info("INFO");
        Assert.assertEquals("INFO", lb.getText());
        Assert.assertNull(lb.getThrowable());
        Logging.debug("DEBUG");
        Assert.assertEquals("DEBUG", lb.getText());
        Assert.assertNull(lb.getThrowable());

        Logging.error("ERROR");
        Logging.error("ERROR", t);
        Logging.info("INFO");
        Logging.debug("DEBUG");

        Logging.setBack(Log4jLoggingBack.INSTANCE);
        Logging.error("ERROR");
        Logging.error("ERROR", t);
        Logging.info("INFO");
        Logging.debug("DEBUG");
        
        ResultType.valueOf(ResultType.values()[0].name());
        
        DispatcherAdapter adapt = new DispatcherAdapter();
        adapt.handleAlgorithmChangedMessage(null);
        adapt.handleChangeParameterRequest(null);
        adapt.handleDisconnectRequest(null);
        adapt.handleExecutionResponseMessage(null);
        adapt.handleHardwareAliveMessage(null);
        adapt.handleLoggingFilterRequest(null);
        adapt.handleLoggingMessage(null);
        adapt.handleMonitoringDataMessage(null);
        adapt.handlePipelineMessage(null);
        adapt.handleSwitchAlgorithmRequest(null);
    }

    /**
     * A logback for testing.
     * 
     * @author Holger Eichelberger
     */
    private class TestLogBack implements ILoggingBack {

        private String text;
        private Throwable throwable;
        
        @Override
        public void error(String text, Throwable throwable) {
            this.text = text;
            this.throwable = throwable;
        }

        @Override
        public void error(String text) {
            this.text = text;
            this.throwable = null;
        }

        @Override
        public void info(String text) {
            this.text = text;
            this.throwable = null;
        }

        @Override
        public void debug(String text) {
            this.text = text;
            this.throwable = null;
        }

        /**
         * Returns the last text.
         * 
         * @return the text
         */
        private String getText() {
            return text;
        }
        
        /**
         * Returns the last throwable.
         * 
         * @return the throwable (may be <b>null</b>)
         */
        private Throwable getThrowable() {
            return throwable;
        }
        
    }
    
    /**
     * Tests the request/response capabilities.
     * 
     * @throws IOException shall not occur
     */
    @Test(timeout = 10000)
    public void testRequestResponse() throws IOException {
        final int port = 6000;

        LocalDispatcher[] dispatchers = new LocalDispatcher[COUNT_DISPATCHER];
        dispatchers[SERVER_DISPATCHER] = new LocalDispatcher(false);
        ServerEndpoint server = new ServerEndpoint(dispatchers[SERVER_DISPATCHER], port, 
            TestAuthenticationSupport.PROVIDER);
        dispatchers[SERVER_DISPATCHER].setResponseEndpoint(server);
        server.start();

        dispatchers[CLIENT1_DISPATCHER] = new LocalDispatcher(false);
        ClientEndpoint client1 = new ClientEndpoint(dispatchers[CLIENT1_DISPATCHER], InetAddress.getLocalHost(), port);

        dispatchers[CLIENT2_DISPATCHER] = new LocalDispatcher(false);
        ClientEndpoint client2 = new ClientEndpoint(dispatchers[CLIENT2_DISPATCHER], InetAddress.getLocalHost(), port);

        Assert.assertNotNull(client1.getClientId());
        Assert.assertNotNull(client2.getClientId());
        Assert.assertNotEquals(client1.getClientId(), client2.getClientId());

        ChangeParameterRequest<String> pReq = new ChangeParameterRequest<String>("pip", "elt", "name", "me");
        MessageHandler<?> handler = new DefaultAssertingExecutionResponseMessageHandler(
            pReq, ResultType.SUCCESSFUL, PARAMETER_RESPONSE_TEXT);
        assertRequestResponse(pReq, client1, dispatchers, handler);
        
        SwitchAlgorithmRequest sReq = new SwitchAlgorithmRequest("pip", "elt", "alg");
        handler = new DefaultAssertingExecutionResponseMessageHandler(sReq, ResultType.SUCCESSFUL, 
            PARAMETER_RESPONSE_TEXT);
        assertRequestResponse(sReq, client1, dispatchers, handler);
        
        assertRequestResponse(new PipelineStatusRequest(), client1, dispatchers, 
            new PipelineStatusResponseMessageHandler(ACTIVE_PIPELINES));
    
        // send to all??
        
        DisconnectRequest msg = new DisconnectRequest();
        dispatchers[SERVER_DISPATCHER].require(msg);
        client1.schedule(msg);
        
        msg = new DisconnectRequest();
        dispatchers[SERVER_DISPATCHER].require(msg);
        client2.schedule(msg);

        while (client1.hasWork() || client2.hasWork() || server.hasWork()) {
            sleep(1000);
        }

        client1.stop();
        client2.stop();
        server.stop();

        sleep(1000);
    }
    
    /**
     * Asserts a request response.
     * 
     * @param req the request
     * @param client the client to handle the request
     * @param dispatchers the actual dispachers
     * @param handler the handler for <code>req</code>
     */
    private static void assertRequestResponse(RequestMessage req, ClientEndpoint client, 
        LocalDispatcher[] dispatchers, MessageHandler<?> handler) {
        dispatchers[CLIENT1_DISPATCHER].addHandler(handler);
        dispatchers[SERVER_DISPATCHER].require(req);
        client.schedule(req);
        Assert.assertEquals(client.getClientId(), req.getClientId());
        Assert.assertNotNull(req.getMessageId());        
        sleep(1000);
        dispatchers[CLIENT1_DISPATCHER].removeHandler(handler);
        Assert.assertEquals(0, dispatchers[CLIENT1_DISPATCHER].getUnhandledExecutionResponseMessageCount());
        Assert.assertEquals(0, dispatchers[CLIENT2_DISPATCHER].getUnhandledExecutionResponseMessageCount());
    }

    /**
     * Tests the replay message (contents).
     */
    @Test
    public void testReplayMessages() {
        ReplayMessage rMsg = new ReplayMessage("pip", "elt", false, 1);
        Assert.assertEquals("pip", rMsg.getPipeline());
        Assert.assertEquals("elt", rMsg.getPipelineElement());
        Assert.assertEquals(false, rMsg.getStartReplay());
        Assert.assertEquals(1, rMsg.getTicket());

        ReplayAdaptationEvent evt = new ReplayAdaptationEvent(rMsg);

        Assert.assertEquals("pip", evt.getPipeline());
        Assert.assertEquals("elt", evt.getPipelineElement());
        Assert.assertEquals(false, evt.getStartReplay());
        Assert.assertEquals(1, evt.getTicket());
        
        ReplayMessage rMsg2 = new ReplayMessage("pip", "elt", true, 2);
        Date start = new Date();
        Date end = new Date();
        rMsg2.setReplayStartInfo(start, end, 10, "aa");

        Assert.assertEquals("pip", rMsg2.getPipeline());
        Assert.assertEquals("elt", rMsg2.getPipelineElement());
        Assert.assertEquals(true, rMsg2.getStartReplay());
        Assert.assertEquals(2, rMsg2.getTicket());
        Assert.assertEquals(start, rMsg2.getStart());
        Assert.assertEquals(end, rMsg2.getEnd());
        Assert.assertEquals(10, rMsg2.getSpeed());
        Assert.assertEquals("aa", rMsg2.getQuery());
        
        ReplayAdaptationEvent evt2 = new ReplayAdaptationEvent(rMsg2);
        
        Assert.assertEquals("pip", evt2.getPipeline());
        Assert.assertEquals("elt", rMsg2.getPipelineElement());
        Assert.assertEquals(true, evt2.getStartReplay());
        Assert.assertEquals(2, evt2.getTicket());
        Assert.assertEquals(start, evt2.getStart());
        Assert.assertEquals(end, evt2.getEnd());
        Assert.assertEquals(10, evt2.getSpeed());
        Assert.assertEquals("aa", evt2.getQuery());

    }

    /**
     * Tests the configuration message (contents).
     */
    @Test
    public void testConfigurationMessage() {
        ConfigurationChangeMessage msg = new ConfigurationChangeMessage(null);
        Assert.assertNotNull(msg.getValues());
        Assert.assertTrue(msg.getValues().isEmpty());

        Map<String, Serializable> values = new HashMap<String, Serializable>();
        values.put("global", Boolean.TRUE);
        msg = new ConfigurationChangeMessage(values);
        Assert.assertNotNull(msg.getValues());
        Assert.assertEquals(values, msg.getValues());
        
        msg.hashCode();
        Assert.assertFalse(msg.equals(null));
        Assert.assertTrue(msg.equals(msg));
    }

    /**
     * Tests the resource change message (contents).
     */
    @Test
    public void testResourceChangeMessage() {
        ResourceChangeMessage msg = new ResourceChangeMessage("hardware", Status.ENABLE);
        Assert.assertEquals(msg.getResource(), "hardware");
        Assert.assertEquals(msg.getStatus(), Status.ENABLE);

        msg.hashCode();
        Assert.assertFalse(msg.equals(null));
        Assert.assertTrue(msg.equals(msg));
    }
    
}
