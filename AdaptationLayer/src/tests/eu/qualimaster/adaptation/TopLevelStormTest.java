package tests.eu.qualimaster.adaptation;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.apache.commons.io.FileUtils;
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
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.coordination.StormUtils;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.easy.extension.QmConstants;
import eu.qualimaster.easy.extension.internal.BindValuesInstantiator;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.InitializationMode;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.ReasoningTask;
import eu.qualimaster.monitoring.ReasoningTask.IReasoningModelProvider;
import eu.qualimaster.monitoring.ReasoningTask.PhaseReasoningModelProvider;
import eu.qualimaster.monitoring.events.CloudResourceMonitoringEvent;
import eu.qualimaster.monitoring.events.ConstraintViolationAdaptationEvent;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.events.MonitoringInformationEvent;
import eu.qualimaster.monitoring.events.PipelineElementMultiObservationMonitoringEvent;
import eu.qualimaster.monitoring.events.PipelineElementObservationMonitoringEvent;
import eu.qualimaster.monitoring.events.PipelineObservationMonitoringEvent;
import eu.qualimaster.monitoring.events.PlatformMonitoringEvent;
import eu.qualimaster.observables.CloudResourceUsage;
import eu.qualimaster.observables.FunctionalSuitability;
import eu.qualimaster.observables.IObservable;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.Configuration;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.DecisionVariable;
import net.ssehub.easy.instantiation.core.model.vilTypes.configuration.NoVariableFilter;
import net.ssehub.easy.varModel.model.IvmlDatatypeVisitor;
import net.ssehub.easy.varModel.model.ModelQueryException;
import net.ssehub.easy.varModel.model.datatypes.DerivedDatatype;
import net.ssehub.easy.varModel.model.datatypes.IDatatype;
import net.ssehub.easy.varModel.model.datatypes.RealType;
import net.ssehub.easy.varModel.model.values.ValueDoesNotMatchTypeException;
import tests.eu.qualimaster.coordination.LocalStormEnvironment;
import tests.eu.qualimaster.coordination.TestNameMapping;
import tests.eu.qualimaster.coordination.Utils;
import tests.eu.qualimaster.storm.Naming;
import tests.eu.qualimaster.storm.Topology;
import tests.eu.qualimaster.testSupport.TestExcludeHosts;

/**
 * A top-level test of the infrastructure.
 * 
 * @author Holger Eichelberger
 */
public class TopLevelStormTest extends AbstractAdaptationTests {

    private ClientEndpoint endpoint;
    @SuppressWarnings("unused")
    private String authMsgId;
    private File profiles;
    
    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        profiles = configureProfilesFolder();
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
        FileUtils.deleteQuietly(profiles);
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
     * Tests the entire stack in static initialization mode.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testStackStatic() throws IOException {
        if (!TestExcludeHosts.isExcludedHost()) {
            testStackImpl(true, InitializationMode.STATIC);
        }
    }


    /**
     * Tests the entire stack in dynamic initialization mode.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testStackDynamic() throws IOException {
        if (!TestExcludeHosts.isExcludedHost()) {
            testStackImpl(true, InitializationMode.DYNAMIC);
        }
    }

    /**
     * Tests the entire stack in adaptive initialization mode.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testStackAdaptive() throws IOException {
        if (!TestExcludeHosts.isExcludedHost()) {
            testStackImpl(false, InitializationMode.ADAPTIVE);
        }
    }
    
    /**
     * Configures the infrastructure for testing.
     * 
     * @param initMode the initialization mode
     * @return the old initialization mode
     */
    private InitializationMode configure(InitializationMode initMode) {
        InitializationMode before = AdaptationConfiguration.getInitializationMode();
        Properties prop = new Properties();
        prop.put(AdaptationConfiguration.INIT_MODE, initMode.name());
        prop.put(AdaptationConfiguration.TIME_STORM_EXECUTOR_STARTUP, 0);
        AdaptationConfiguration.configure(prop);
        return before;
    }

    /**
     * Configures the profiles folder (for cleanup).
     * 
     * @return the created temporary profiles folder
     */
    private static File configureProfilesFolder() {
        File profiles = new File(FileUtils.getTempDirectory(), "profiles");
        FileUtils.deleteQuietly(profiles);
        profiles.mkdirs();
        Properties prop = new Properties();
        prop.put(AdaptationConfiguration.PROFILE_LOCATION, profiles.getAbsolutePath());
        AdaptationConfiguration.configure(prop);
        return profiles;
    }
    
    /**
     * Turns an observation into a map.
     * 
     * @param obs the observable
     * @param value the observed value
     * @return the resulting map
     */
    private static Map<IObservable, Double> toMap(IObservable obs, Double value) {
        Map<IObservable, Double> result = new HashMap<IObservable, Double>();
        result.put(obs, value);
        return result;
    }

    /**
     * Tests the entire stack. Please note that the test fails, if the initial enactment is not working
     * correctly, e.g., due to the Reasoner.
     * 
     * @param defaultInit initialize the algorithm implementations by default (false requires adaptation layer 
     *     and rt-VIL)
     * @param initMode the configuration initialization mode
     * @throws IOException shall not occur
     */
    public void testStackImpl(boolean defaultInit, InitializationMode initMode) throws IOException {
        InitializationMode iMode = configure(initMode); 
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
        
        TopologyBuilder builder = new TopologyBuilder();
        Topology.createTopology(builder);
        StormTopology topology = builder.createTopology();
        Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
        // assign the topology data including its logging-enabled configuration (for this test)
        @SuppressWarnings("rawtypes")
        Map topoCfg = Naming.setDefaultInitializeAlgorithms(QmLogging.enable(createTopologyConfiguration()), 
            defaultInit);
        topologies.put(Naming.PIPELINE_NAME, new TopologyTestInfo(topology, 
            new File(Utils.getTestdataDir(), "pipeline.xml"), topoCfg));
        env.setTopologies(topologies);
        clear();
        
        EventManager.send(new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.START));
        // broken reasoner may prevent proper startup
        getPipelineStatusTracker().waitFor(Naming.PIPELINE_NAME, Status.STARTED, 30000); 
        clear();
        sleep(4000);

        CoordinationCommand cmd = new ParameterChangeCommand<Integer>(Naming.PIPELINE_NAME, Naming.NODE_SOURCE, 
            "param", 5);
        EventManager.send(cmd);
        waitForExecution(1, 0);
        clear();
        sendAdditionalEvents();

        sleep(4000); // let Storm run for a while
        // this is a command send by the adaptation layer itself
        cmd = new AlgorithmChangeCommand(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, Naming.NODE_PROCESS_ALG2);
        EventManager.send(cmd);
        waitForExecution(1, 0);
        clear();
        sleep(1000);
        SwitchAlgorithmRequest saReq = new SwitchAlgorithmRequest(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, 
            Naming.NODE_PROCESS_ALG1); // this is a command sent via the client
        endpoint.schedule(saReq);
        dispatcher.registerForResponse(saReq);
        sleep(2000); // let Storm run for a while

        FrozenSystemState state = MonitoringManager.getSystemState().freeze();
        EventManager.send(new PipelineCommand(Naming.PIPELINE_NAME, PipelineCommand.Status.STOP));
        waitForExecution(1, 0);
        clear();

        EventManager.unregister(adaptationEventHandler);
        env.shutdown();
        ThriftConnection.setLocalCluster(null);
        StormUtils.forTesting(null, null);
        EventManager.cleanup();
        env.cleanup();
        asserts(state);
        asserts(dispatcher);
        //Assert.assertTrue(adaptationEventHandler.recordedConstraintViolations());
        MonitoringManager.setDemoMessageState(demo);
        configure(iMode); 
    }

    /**
     * Sends additional (monitoring) events.
     */
    private void sendAdditionalEvents() {
        EventManager.send(new PlatformMonitoringEvent(CloudResourceUsage.BANDWIDTH, 15000, null));
        EventManager.send(new PipelineObservationMonitoringEvent(Naming.PIPELINE_NAME, null, 
            FunctionalSuitability.ACCURACY_CONFIDENCE, 100.0));
        EventManager.send(new PipelineElementMultiObservationMonitoringEvent(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, 
            null, toMap(FunctionalSuitability.COMPLETENESS, 50.0)));
        EventManager.send(new CloudResourceMonitoringEvent("AWS", toMap(CloudResourceUsage.PING, 1500.0)));
        sleep(1000);
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
    
    /**
     * Asserts entries in the frozen system state captured before shutdown.
     * 
     * @param state the state
     */
    private void asserts(FrozenSystemState state) {
        //EventManager.send(new PlatformMonitoringEvent(CloudResourceUsage.BANDWIDTH, 15000, null));
        assertEquals(15000.0, state.getInfrastructureObservation(CloudResourceUsage.BANDWIDTH, 0.0));
        //EventManager.send(new CloudResourceMonitoringEvent("AWS", toMap(CloudResourceUsage.PING, 1500.0)));
        assertEquals(1500.0, state.getCloudObservation("AWS", CloudResourceUsage.PING, 0.0));
        //SwitchAlgorithmRequest saReq = new SwitchAlgorithmRequest(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, 
        //  Naming.NODE_PROCESS_ALG1);
        Assert.assertTrue(state.hasActiveAlgorithm(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, 
            Naming.NODE_PROCESS_ALG1));
        Assert.assertEquals(Naming.NODE_PROCESS_ALG1, 
            state.getActiveAlgorithm(Naming.PIPELINE_NAME, Naming.NODE_PROCESS));
        //EventManager.send(new PipelineObservationMonitoringEvent(Naming.PIPELINE_NAME, null, 
        //  FunctionalSuitability.ACCURACY_CONFIDENCE, 100.0));
        assertEquals(100.0, state.getPipelineObservation(Naming.PIPELINE_NAME, 
            FunctionalSuitability.ACCURACY_CONFIDENCE));
        //EventManager.send(new PipelineElementMultiObservationMonitoringEvent(Naming.PIPELINE_NAME, 
        //  Naming.NODE_PROCESS, null, toMap(FunctionalSuitability.COMPLETENESS, 50.0)));
        assertEquals(50.0, state.getPipelineElementObservation(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, 
            FunctionalSuitability.COMPLETENESS));
        
        assertMonitoringModel(state);
    }
    
    /**
     * Asserts a Double vs a double with default delta 1.
     * 
     * @param expected the expected value
     * @param actual the actual value (must not be <b>null</b>)
     */
    private static void assertEquals(double expected, Double actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual, 1);
    }
    
    /**
     * Asserts (faked) information in the monitoring model.
     * 
     * @param state the state
     * @throws ModelQueryException in case that querying the model fails
     * @throws ValueDoesNotMatchTypeException
     */
    private void assertMonitoringModel(FrozenSystemState state) {
        IReasoningModelProvider modelProvider = new PhaseReasoningModelProvider(Phase.MONITORING);
        net.ssehub.easy.varModel.confModel.Configuration config = modelProvider.getConfiguration();  
        Assert.assertTrue(null != config && null != modelProvider.getScript());
        ReasoningTask reasoningTask = new ReasoningTask(modelProvider);
        reasoningTask.reason(false);
        
        // assert access as in rtVIL
        Configuration cfg = new Configuration(config, NoVariableFilter.INSTANCE);
        
        Map<String, Double> mapping = state.getMapping();
        Map<String, Object> tmp = new HashMap<String, Object>();
        tmp.putAll(mapping);
        
        // test model does this directly via VIL, fake call here and test it
        BindValuesInstantiator.storeValueBinding(cfg, tmp);

        //EventManager.send(new PlatformMonitoringEvent(CloudResourceUsage.BANDWIDTH, 15000, null));
        assertDouble(cfg, "bandwidth", 15000.0);
        //EventManager.send(new CloudResourceMonitoringEvent("AWS", toMap(CloudResourceUsage.PING, 1500.0)));
        assertDouble(cfg, "AWS", "ping", 1500.0);
        //SwitchAlgorithmRequest saReq = new SwitchAlgorithmRequest(Naming.PIPELINE_NAME, Naming.NODE_PROCESS, 
        //  Naming.NODE_PROCESS_ALG1);
        assertActual(cfg, "famElt1", Naming.NODE_PROCESS_ALG1);
        //EventManager.send(new PipelineObservationMonitoringEvent(Naming.PIPELINE_NAME, null, 
        //  FunctionalSuitability.COMPLETENESS, 100.0));
        assertDouble(cfg, "pip", "accuracyConfidence", 100); 
        //EventManager.send(new PipelineElementMultiObservationMonitoringEvent(Naming.PIPELINE_NAME, 
        //  Naming.NODE_PROCESS, null, toMap(FunctionalSuitability.COMPLETENESS, 50.0)));
        assertDouble(cfg, "famElt1", "completeness", 50);
    }
    
    /**
     * Asserts the existence of a compound slot and returns the slot using VIL accessors.
     * 
     * @param cfg the configuration to obtain the slot from
     * @param var the top-level variable name
     * @param slot the slot name
     * @return the slot variable
     */
    private static DecisionVariable assertSlot(Configuration cfg, String var, String slot) {
        DecisionVariable dVar = cfg.getByName(var);
        Assert.assertNotNull("Variable " + var + " not expected to be null", dVar);
        DecisionVariable dSlot = dVar.getByName(slot);
        Assert.assertNotNull("Slot " + slot + " not expected to be null", dSlot);
        Assert.assertNotNull("Slot value not expected to be null", dSlot.getValue());
        return dSlot;
    }
    
    /**
     * Asserts the IVML type of a slot/variable.
     * 
     * @param var the variable
     * @param type the expected type of {@code var}
     */
    private static void assertType(DecisionVariable var, IDatatype type) {
        Assert.assertEquals("Variable/slot type not " + IvmlDatatypeVisitor.getQualifiedType(type), type, 
            DerivedDatatype.resolveToBasis(var.getDecisionVariable().getDeclaration().getType()));
    }
    
    /**
     * Asserts a Real slot value.
     * 
     * @param cfg the VIL configuration (no filter!)
     * @param var the variable name
     * @param slot the slot name
     * @param value the expected value of {@code slot}
     */
    private static void assertDouble(Configuration cfg, String var, String slot, double value) {
        DecisionVariable dSlot = assertSlot(cfg, var, slot);
        assertType(dSlot, RealType.TYPE);
        Assert.assertEquals(value, dSlot.getRealValue(), 1.0);        
    }
    
    /**
     * Asserts a Double value.
     * 
     * @param cfg the VIL configuration (no filter!)
     * @param var the variable name
     * @param value the expected value of {@code var}
     */
    private static void assertDouble(Configuration cfg, String var, double value) {
        DecisionVariable dVar = cfg.getByName(var);
        Assert.assertNotNull("Variable " + var + " not expected to be null", dVar);
        assertType(dVar, RealType.TYPE);
        Assert.assertNotNull("Double value not expected to be null", dVar.getRealValue());
        Assert.assertEquals(value, dVar.getRealValue().doubleValue(), 1.0);
    }
    
    /**
     * Asserts an actual algorithm.
     * 
     * @param cfg the VIL configuration (no filter!)
     * @param var the variable name
     * @param alg the expected actual algorithm (data source, data sink)
     */
    private static void assertActual(Configuration cfg, String var, String alg) {
        DecisionVariable dVar = cfg.getByName(var);
        Assert.assertNotNull("Variable " + var + " not expected to be null", dVar);
        DecisionVariable aVar = dVar.getByName(QmConstants.SLOT_ACTUAL);
        Assert.assertNotNull("Slot " + QmConstants.SLOT_ACTUAL + " not expected to be null", aVar);
        DecisionVariable nVar = aVar.getByName(QmConstants.SLOT_NAME);
        Assert.assertNotNull("Slot " + QmConstants.SLOT_NAME + " not expected to be null", nVar);
        Assert.assertEquals(alg, nVar.getStringValue());
    }

}
