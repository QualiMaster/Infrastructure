package tests.eu.qualimaster.adaptation;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import backtype.storm.generated.StormTopology;
import backtype.storm.topology.TopologyBuilder;
import eu.qualimaster.adaptation.AdaptationConfiguration;
import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.adaptation.external.ClientEndpoint;
import eu.qualimaster.adaptation.external.DispatcherAdapter;
import eu.qualimaster.adaptation.external.ExecutionResponseMessage;
import eu.qualimaster.adaptation.external.IInformationDispatcher;
import eu.qualimaster.adaptation.external.InformationMessage;
import eu.qualimaster.adaptation.external.LoggingMessage;
import eu.qualimaster.adaptation.external.MonitoringDataMessage;
import eu.qualimaster.adaptation.external.RequestMessage;
import eu.qualimaster.adaptation.external.SwitchAlgorithmRequest;
import eu.qualimaster.common.logging.QmLogging;
import eu.qualimaster.common.signal.ThriftConnection;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.StormUtils;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.ConstraintViolationAdaptationEvent;
import eu.qualimaster.monitoring.events.MonitoringInformationEvent;
import eu.qualimaster.monitoring.events.PipelineElementMultiObservationMonitoringEvent;
import eu.qualimaster.monitoring.events.PipelineElementObservationMonitoringEvent;
import tests.eu.qualimaster.coordination.AbstractCoordinationTests;
import tests.eu.qualimaster.coordination.LocalStormEnvironment;
import tests.eu.qualimaster.coordination.TestNameMapping;
import tests.eu.qualimaster.coordination.Utils;
import tests.eu.qualimaster.storm.Naming;
import tests.eu.qualimaster.storm.Topology;

/**
 * A top-level test of the infrastructure.
 * 
 * @author Holger Eichelberger
 */
public class TopLevelStormTest extends AbstractAdaptationTests {

    private ClientEndpoint endpoint;
    @SuppressWarnings("unused")
    private String authMsgId;
    
    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        Utils.setModelProvider(Utils.INFRASTRUCTURE_TEST_MODEL_PROVIDER);
        Utils.configure();
        super.setUp();
        CoordinationManager.registerTestMapping(TestNameMapping.INSTANCE);

        EventManager.disableLoggingFor(MonitoringInformationEvent.class);
        EventManager.disableLoggingFor(PipelineElementMultiObservationMonitoringEvent.class);
        EventManager.disableLoggingFor(PipelineElementObservationMonitoringEvent.class);
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        stopEndpoint();
        super.tearDown();
        Utils.dispose();
    }
    
    /**
     * Stops the endpoint.
     */
    private void stopEndpoint() {
        if (null != endpoint) {
            endpoint.stop();
            endpoint = null;
        }
    }
    
    /**
     * A test dispatcher for receiving messages.
     * 
     * @author Holger Eichelberger
     */
    private class TestDispatcher extends DispatcherAdapter implements IInformationDispatcher {

        private AtomicInteger loggingMsgCount = new AtomicInteger(0);
        private List<InformationMessage> infoMsg = Collections.synchronizedList(
            new ArrayList<InformationMessage>());
        private List<MonitoringDataMessage> monMsg = Collections.synchronizedList(
            new ArrayList<MonitoringDataMessage>());
        private ResponseMessageCollector<RequestMessage, ExecutionResponseMessage> collector 
            = ResponseMessageCollector.createExternalCollector();
        
        @Override
        public void handleMonitoringDataMessage(MonitoringDataMessage msg) {
            monMsg.add(msg);
        }

        /**
         * Returns the number of monitoring data messages with the given part.
         * 
         * @param part the part to look for
         * @return the number of messages, the number of all messages if <code>part</code> is <b>null</b>
         */
        public int getMonitoringDataCount(String part) {
            int result = 0;
            if (null == part) {
                result = monMsg.size();
            } else {
                for (int m = 0; m < monMsg.size(); m++) {
                    MonitoringDataMessage msg = monMsg.get(m);
                    if (equals(part, msg.getPart())) {
                        result++;
                    }
                }
            }
            return result;
        }
        
        /**
         * The number of information messages received.
         * 
         * @return the number of information messages
         */
        public int getInformationMessageCount() {
            return infoMsg.size();
        }
        
        /**
         * Returns the number of logging messages received.
         * 
         * @return the number of logging messages
         */
        public int getLoggingMessageCount() {
            return loggingMsgCount.get();
        }

        /**
         * The number of information messages received with a specific content.
         * 
         * @param pipeline the name of the pipeline (may be <b>null</b>, requires event to also have <b>null</b> 
         *     as entry)
         * @param pipelineElement the name of the pipeline element (may be <b>null</b>, requires event to also have 
         *     <b>null</b> as entry)
         * @param text the text that shall at least be given in terms of a substring
         * 
         * @return the number of algorithm changed messages
         */
        @SuppressWarnings("unused")
        public int getInformationMessageCount(String pipeline, String pipelineElement, String text) {
            int result = 0;
            for (int a = 0; a < infoMsg.size(); a++) {
                InformationMessage msg = infoMsg.get(a);
                if (equals(msg.getPipeline(), pipeline) && equals(msg.getPipelineElement(), pipelineElement) 
                    && msg.getDescription().contains(text)) {
                    result++;
                }
            }
            return result;
        }

        /**
         * Returns whether <code>s1</code> and <code>s2</code> are equal considering <b>null</b>.
         * @param s1 the first string to test
         * @param s2 the second string to test
         * @return <code>true</code> if both are <b>null</b> or both are equal, <code>false</code> else
         */
        private boolean equals(String s1, String s2) {
            return s1 == null ? s2 == null : s1.equals(s2);
        }

        @Override
        public void handleLoggingMessage(LoggingMessage msg) {
            loggingMsgCount.incrementAndGet();
        }

        @Override
        public void handleExecutionResponseMessage(ExecutionResponseMessage msg) {
            collector.received(msg);
        }

        /**
         * Registers a message to indicate that a response is expected.
         * 
         * @param msg the message to register
         */
        public void registerForResponse(RequestMessage msg) {
            collector.registerForResponse(msg);
        }

        /**
         * Asserts the expected responses.
         */
        public void assertResponses() {
            collector.assertResponses();
        }

        @Override
        public void handleInformationMessage(InformationMessage message) {
            infoMsg.add(message);
        }

    }

    /**
     * Implements a simple adaptation event handler recording violated constraint messages.
     * 
     * @author Holger Eichelberger
     */
    private class AdaptationEventHandler extends EventHandler<AdaptationEvent> {

        private boolean recordedViolationCount = false;

        /**
         * Creates a handler instance.
         */
        protected AdaptationEventHandler() {
            super(AdaptationEvent.class);
        }
        
        @Override
        protected void handle(AdaptationEvent event) {
            if (event instanceof ConstraintViolationAdaptationEvent) {
                recordedViolationCount = true;
            }
        }

        /**
         * Returns whether a constraint violation was recorded.
         * 
         * @return <code>true</code> in case of a constraint violation, <code>false</code> else
         */
        @SuppressWarnings("unused")
        public boolean recordedConstraintViolations() {
            return recordedViolationCount;
        }
        
    }
    
    /**
     * Tests the entire stack.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testStack() throws IOException {
        if (!AbstractCoordinationTests.isJenkins()) {
            testStackImpl();
        }
    }
    
    /**
     * Tests the entire stack. Please note that the test fails, if the initial enactment is not working
     * correctly, e.g., due to the Reasoner.
     * 
     * @throws IOException shall not occur
     */
    public void testStackImpl() throws IOException {
        int demo = MonitoringManager.setDemoMessageState(MonitoringManager.DEMO_MSG_INFRASTRUCTURE 
            | MonitoringManager.DEMO_MSG_PIPELINE | MonitoringManager.DEMO_MSG_PROCESSING_ALGORITHM 
            | MonitoringManager.DEMO_MSG_PROCESSING_ELEMENT); // required for monitoring msg
        TestDispatcher dispatcher = new TestDispatcher();
        endpoint = new ClientEndpoint(dispatcher, InetAddress.getLocalHost(), 
            AdaptationConfiguration.getAdaptationPort());
        dispatcher.registerForResponse(TestAuthenticationSupport.authenticate(endpoint, "JohnDoe"));
        LocalStormEnvironment env = new LocalStormEnvironment();
        AdaptationEventHandler adaptationEventHandler = new AdaptationEventHandler();
        EventManager.register(adaptationEventHandler);
        
        Topology.setDefaultInitAlgorithms(false); // this requires adaptation layer and rt-VIL model!
        TopologyBuilder builder = new TopologyBuilder();
        Topology.createTopology(builder);
        StormTopology topology = builder.createTopology();
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        // assign the topology data including its logging-enabled configuration (for this test)
        @SuppressWarnings("rawtypes")
        Map topoCfg = Naming.setDefaultInitializeAlgorithms(QmLogging.enable(createTopologyConfiguration()), false);
        topologies.put(Naming.PIPELINE_NAME, new TopologyTestInfo(topology, 
            new File(Utils.getTestdataDir(), "pipeline.xml"), topoCfg));
        env.setTopologies(topologies);
        clear();
        
        EventManager.send(new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.START));
        // broken reasoner may prevent proper startup
        getPipelineStatusTracker().waitFor(Naming.PIPELINE_NAME, Status.STARTED, 30000); 
        clear();
        sleep(3000);

        sleep(1000);
        CoordinationCommand cmd = new ParameterChangeCommand<Integer>(Naming.PIPELINE_NAME, Naming.NODE_SOURCE, 
            "param", 5);
        EventManager.send(cmd);
        waitForExecution(1, 0);
        clear();

        sleep(4000); // let Storm run for a while
        // this is a command send by the adaptation layer itself
        cmd = new AlgorithmChangeCommand(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, Naming.NODE_PROCESS_ALG2);
        EventManager.send(cmd);
        waitForExecution(1, 0);
        clear();
        sleep(1000);
        // this is a command sent via the client
        SwitchAlgorithmRequest saReq = new SwitchAlgorithmRequest(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, 
            Naming.NODE_PROCESS_ALG1);
        endpoint.schedule(saReq);
        dispatcher.registerForResponse(saReq);
        sleep(2000); // let Storm run for a while

        EventManager.send(new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.STOP));
        waitForExecution(1, 0);
        clear();

        EventManager.unregister(adaptationEventHandler);
        env.shutdown();
        ThriftConnection.setLocalCluster(null);
        StormUtils.forTesting(null, null);
        EventManager.cleanup();
        env.cleanup();
        
        asserts(dispatcher);
        //Assert.assertTrue(adaptationEventHandler.recordedConstraintViolations());
        MonitoringManager.setDemoMessageState(demo);
    }

    // pipeline must be running...
    /*ArrayList<String> filterRegEx = new ArrayList<String>();
    filterRegEx.add("tests\\.eu\\.qualimaster\\.storm.*");
    EventManager.send(new LoggingFilterEvent(filterRegEx, null));*/

    /**
     * Does the asserts.
     * 
     * @param dispatcher the dispatcher to assert on.
     */
    private void asserts(TestDispatcher dispatcher) {
        // >= in case of adaptive startup
        Assert.assertTrue(dispatcher.getInformationMessageCount() >= 1);
        //Assert.assertTrue(dispatcher.getInformationMessageCount(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, 
        //     Naming.NODE_PROCESS_ALG2) >= 1);
        //Assert.assertTrue(dispatcher.getInformationMessageCount(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, 
        //     Naming.NODE_PROCESS_ALG1) >= 1);
        
        final String srcName = Naming.PIPELINE_NAME + ":" + Naming.NODE_SOURCE;
        final String pcsName = Naming.PIPELINE_NAME + ":" + Naming.NODE_PROCESS;
        final String snkName = Naming.PIPELINE_NAME + ":" + Naming.NODE_SINK;
        
        System.out.println("#log " + dispatcher.getLoggingMessageCount() 
             + " #src " + dispatcher.getMonitoringDataCount(srcName) 
             + " #pcs " + dispatcher.getMonitoringDataCount(pcsName) 
             + " #snk " + dispatcher.getMonitoringDataCount(snkName)); 

        //disabled //Assert.assertTrue(dispatcher.getLoggingMessageCount() > 0);
        Assert.assertTrue(dispatcher.getMonitoringDataCount(pcsName) > 0);
        Assert.assertTrue(dispatcher.getMonitoringDataCount(srcName) > 0);
        Assert.assertTrue(dispatcher.getMonitoringDataCount(snkName) > 0);
        dispatcher.assertResponses();
    }

}
