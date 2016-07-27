package tests.eu.qualimaster.adaptation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import net.ssehub.easy.instantiation.rt.core.model.rtVil.ISimulationNotifier;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.RtVilModel;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.RtVilStorage;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Script;
import net.ssehub.easy.instantiation.core.model.buildlangModel.BuildModel;
import net.ssehub.easy.instantiation.core.model.vilTypes.OperationDescriptor;
import eu.qualimaster.adaptation.AdaptationConfiguration;
import eu.qualimaster.adaptation.AdaptationEventQueue;
import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.RepositoryConnector.Models;
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.coordination.RuntimeVariableMapping;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.easy.extension.internal.ConfigurationInitializer;
import eu.qualimaster.easy.extension.internal.VariableHelper;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.ReasoningTask;
import eu.qualimaster.monitoring.ReasoningTask.IReasoningListener;
import eu.qualimaster.monitoring.ReasoningTask.IReasoningModelProvider;
import eu.qualimaster.monitoring.ReasoningTask.SimpleReasoningModelProvider;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.pipeline.AlgorithmChangeParameter;
import net.ssehub.easy.basics.modelManagement.ModelInitializer;
import net.ssehub.easy.basics.modelManagement.ModelManagementException;
import net.ssehub.easy.basics.progress.ProgressObserver;
import net.ssehub.easy.reasoning.core.reasoner.Message;
import net.ssehub.easy.reasoning.core.reasoner.ReasoningResult;
import net.ssehub.easy.varModel.confModel.AssignmentState;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.ConfigurationException;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.management.VarModel;
import net.ssehub.easy.varModel.model.ModelQueryException;
import net.ssehub.easy.varModel.model.Project;
import net.ssehub.easy.varModel.model.values.Value;
import tests.eu.qualimaster.adaptation.TimeMeasurementTracerFactory.Measure;

/**
 * A simple framework for performing adaptation tests which directly use
 * the QM layers, i.e., bypass Storm and the event bus. On the one side,
 * this is faster and less resource consuming, but, on the other side, requires
 * manual setup of the involved layers. The core idea is to provide a framework
 * which does this setup and to specify the tests as well as intermediary asserts
 * in terms of a test specification instance. This class re-initializes the models
 * instances in {@link RepositoryConnector}. This class also takes time measures over
 * the various adaptation phases. Call {@link #printMeasures()} at the end of all tests.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractDirectAdaptationTests {

    private static final Set<String> JENKINS = new HashSet<String>();

    private Configuration monConfig;
    private Script monRtVilModel;
    private RuntimeVariableMapping monCopyMapping;
    private Configuration adaptConfig;
    private Script adaptRtVilModel;
    private File tmp;
    private boolean debug = false;
    
    static {
        JENKINS.add("jenkins.sse.uni-hildesheim.de");
    }
    
    /**
     * Indicates whether also the VIL model for {@link Phase#MONITORING} shall be loaded.
     * 
     * @return <code>false</code> by default
     */
    protected boolean loadVilModel() {
        return false;
    }

    /**
     * Executed before a single test.
     * 
     * @throws ModelManagementException shall not occur
     * @throws ModelQueryException shall not occur
     */
    @Before
    public void setUp() throws ModelManagementException, ModelQueryException {
        RepositoryConnector.initialize();
        
        // model is not loaded as configuration is not set and we want to use the model in this package
        ModelInitializer.registerLoader(ProgressObserver.NO_OBSERVER);
        ModelInitializer.addLocation(getModelLocation(), ProgressObserver.NO_OBSERVER);
        Project project = RepositoryConnector.obtainModel(VarModel.INSTANCE, "QM", null);
        //CopyMappingCreationListener listener = new CopyMappingCreationListener();
        monConfig = RepositoryConnector.createConfiguration(project, Phase.MONITORING);
        monCopyMapping = ConfigurationInitializer.createVariableMapping(monConfig);
        net.ssehub.easy.instantiation.core.model.buildlangModel.Script vil = null;
        if (loadVilModel()) {
            vil = RepositoryConnector.obtainModel(BuildModel.INSTANCE, "QM", null);
        }
        monRtVilModel = RepositoryConnector.obtainModel(RtVilModel.INSTANCE, "QM", null);
        new Models(Phase.MONITORING, monConfig, monRtVilModel, vil, monCopyMapping); // overrides
        ModelInitializer.removeLocation(getModelLocation(), ProgressObserver.NO_OBSERVER);

        ModelInitializer.addLocation(getModelLocation(), ProgressObserver.NO_OBSERVER);
        project = RepositoryConnector.obtainModel(VarModel.INSTANCE, "QM", null);
        adaptConfig = RepositoryConnector.createConfiguration(project, Phase.ADAPTATION);
        adaptRtVilModel = RepositoryConnector.obtainModel(RtVilModel.INSTANCE, "QM", null);
        tmp = RepositoryConnector.createTmpFolder();
        new Models(Phase.ADAPTATION, adaptConfig, adaptRtVilModel, null, null);  // overrides
        ModelInitializer.removeLocation(getModelLocation(), ProgressObserver.NO_OBSERVER);
    }
    
    /**
     * Executed after a single test.
     * 
     * @throws ModelManagementException shall not occur
     */
    @After
    public void tearDown() throws ModelManagementException {
        tmp.delete();
        ModelInitializer.unregisterLoader(ProgressObserver.NO_OBSERVER);
    }
    
    /**
     * Returns the model location.
     * 
     * @return the model location
     */
    protected abstract File getModelLocation();
    
    /**
     * Defines the interface of a test specification so that {@link #performAdaptation(ITestSpec) performing 
     * the adaptation} can be encapsulated in a strategy method. Override 
     * {@link #notifyReasoningResult(ReasoningResult)} for debugging.
     * 
     * @author Holger Eichelberger
     */
    public abstract static class TestSpec implements ISimulationNotifier, IReasoningListener {

        private String[] pipelines;
        private Set<CoordinationCommand> commands = new HashSet<CoordinationCommand>();
        private int stepCount;
        private String timeIdentifier;

        /**
         * Creates a test specification.
         * 
         * @param stepCount the number of steps to simulate
         * @param pipelines the names of the pipelines to test
         */
        protected TestSpec(int stepCount, String... pipelines) {
            this(null, stepCount, pipelines);
        }

        /**
         * Creates a test specification.
         * 
         * @param stepCount the number of steps to simulate
         * @param timeIdentifier identifier for access to the Time measurements (may be empty or <b>null</b> for no 
         *     time measurement)
         * @param pipelines the names of the pipelines to test
         */
        protected TestSpec(String timeIdentifier, int stepCount, String... pipelines) {
            this.pipelines = pipelines;
            this.stepCount = stepCount;
            this.timeIdentifier = timeIdentifier;
        }
        
        /**
         * Returns the identifier for time recording.
         * 
         * @return the identifier, no time recording if empty or <b>null</b>
         */
        protected String getTimeIdentifier() {
            return timeIdentifier;
        }
        
        /**
         * Returns the name of the pipeline used in testing.
         * 
         * @return the names of the pipelines
         */
        protected String[] getPipelineNames() {
            return pipelines;
        }

        @Override
        public void notifyOperationCall(OperationDescriptor operation, Object[] args) {
            if (null != args && args.length > 0 && args[0] instanceof CoordinationCommand) {
                commands.add((CoordinationCommand) args[0]);
            }
        }
        
        @Override
        public boolean doRollbackSimulation() {
            return false; // model is loaded again for each test, do not roll back for multiple steps
        }

        /**
         * Returns all executed commands of type <code>T</code> for the current step.
         * 
         * @param <T> the command type
         * @param cls the command type class
         * @return the executed commands, <b>null</b> if none of type <code>T</code> were executed
         */
        protected <T extends CoordinationCommand> List<T> findExecutedCommands(Class<T> cls) {
            List<T> result = null;
            FlatCommandCollector collector = new FlatCommandCollector();
            for (CoordinationCommand cmd : commands) {
                cmd.accept(collector);
            }
            List<CoordinationCommand> tmp = collector.getResult();
            for (CoordinationCommand cmd : tmp) {
                if (cls.isInstance(cmd)) {
                    if (null == result) {
                        result = new ArrayList<T>();
                    }
                    result.add(cls.cast(cmd));
                }
            }
            return result;
        }

        /**
         * Asserts the existence / sending of algorithm change commands with specific data.
         *   
         * @param pipeline the name of the pipeline
         * @param element the name of the pipeline element
         * @param algorithm the name of the algorithm
         * @param single whether exactly one command (<code>true</code>) or at least one command shall be 
         *   found (<code>false</code>)
         */
        protected void assertAlgorithmChangeCommand(String pipeline, String element, String algorithm, boolean single) {
            assertAlgorithmChangeCommand(pipeline, element, algorithm, null, single);
        }

        /**
         * Asserts the existence / sending of algorithm change commands with specific data.
         *   
         * @param pipeline the name of the pipeline
         * @param element the name of the pipeline element
         * @param algorithm the name of the algorithm
         * @param params required parameters, ignored if <b>null</b>
         * @param single whether exactly one command (<code>true</code>) or at least one command shall be 
         *   found (<code>false</code>)
         */
        protected void assertAlgorithmChangeCommand(String pipeline, String element, String algorithm, 
            Map<AlgorithmChangeParameter, Serializable> params, boolean single) {
            List<AlgorithmChangeCommand> cmds = findExecutedCommands(AlgorithmChangeCommand.class);
            Assert.assertNotNull(cmds);
            int found = 0;
            for (int c = 0; c < cmds.size(); c++) {
                AlgorithmChangeCommand cmd = cmds.get(c);
                if (pipeline.equals(cmd.getPipeline()) && element.equals(cmd.getPipelineElement()) 
                    && algorithm.equals(cmd.getAlgorithm())) {
                    if (matchesParams(cmd, params)) {
                        found++;
                    }
                }
            }
            if (single) {
                Assert.assertTrue("No change command found", 1 == found);
            } else {
                Assert.assertTrue("No change commands found", found > 0);
            }
        }

        /**
         * Returns whether the params of <code>cmd</code> match all given <code>params</code>. The command parameters
         * may be more and different than <code>params</code> as log as all <code>params</code> are present
         * and the values are equal.
         * 
         * @param cmd the command to compare
         * @param params the required (minimum) parameters
         * @return <code>true</code> for a match (also if <code>param</code> is <b>null</b>), <code>false</code> else
         */
        private boolean matchesParams(AlgorithmChangeCommand cmd, Map<AlgorithmChangeParameter, Serializable> params) {
            boolean matches = true;
            if (null != params) {
                Map<AlgorithmChangeParameter, Serializable> cParams = cmd.getParameters();
                for (Map.Entry<AlgorithmChangeParameter, Serializable> ent : params.entrySet()) {
                    Serializable pVal = ent.getValue();
                    Serializable cVal = cParams.get(ent.getKey());
                    if (!((null == pVal && null == cVal) || (null != pVal && pVal.equals(cVal)))) {
                        matches = false;
                        break;
                    }
                }
            }
            return matches;
        }

        /**
         * Sets a reference value on <code>slotName</code> of <code>var</code>, e.g., for initializing a configuration 
         * appropriately.
         * 
         * @param var the variable to modify
         * @param slotName the slot name within <code>var</code> to modify
         * @param availableSlotName the name of the slot in <code>var</code> containing valid resources 
         * @param elementName the name of the element to select, else the first value in <code>availableSlotName</code>
         *   shall be taken
         */
        protected void setReference(IDecisionVariable var, String slotName, String availableSlotName, 
            String elementName) {
            try {
                IDecisionVariable slot = var.getNestedElement(slotName);
                IDecisionVariable availables = var.getNestedElement(availableSlotName);
                Value value = null;
                if (null != slot && null != availables) {
                    if (null == elementName) {
                        if (availables.getNestedElementsCount() > 0) {
                            value = availables.getNestedElement(0).getValue();
                        }
                    } else {
                        for (int n = 0; null == value && n < availables.getNestedElementsCount(); n++) {
                            IDecisionVariable nested = availables.getNestedElement(n);
                            IDecisionVariable deref = Configuration.dereference(nested);
                            if (elementName.equals(VariableHelper.getName(deref))) {
                                value = nested.getValue(); // yes, nested - take the reference
                            }
                        }
                    }
                }
                if (null != value) {
                    slot.setValue(value, AssignmentState.ASSIGNED);
                } else {
                    Assert.fail("cannot set value '" + elementName + "' for " + slotName 
                        + " as related variable does not exist");
                }
            } catch (ConfigurationException e) {
                Assert.fail("cannot set value for " + slotName + ": " + e.getMessage());
            }
        }
        
        /**
         * Returns an iterator on the collected (simulated) commands for the current step.
         * 
         * @return the iterator
         */
        protected Iterator<CoordinationCommand> commands() {
            return commands.iterator();
        }
        
        /**
         * Returns the number of steps to test.
         * 
         * @return the number of steps
         */
        protected int getStepCount() {
            return stepCount;
        }

        /**
         * Allows initializing the configuration before any step takes place.
         * 
         * @param config the configuration
         */
        protected void initialize(Configuration config) {
        }

        /**
         * Initializes the system state, i.e., "simulates" monitoring.
         * 
         * @param step the simulation step (positive integer)
         * @param state the system state to be modified
         * @return the event that shall be used to initiate the adaptation bypassing the analysis (<b>null</b> to 
         *     perform analysis, in this case {@link #assertAnalysis(AdaptationEvent, Configuration) will be called 
         *     next)
         */
        protected abstract AdaptationEvent monitor(int step, SystemState state);
        
        /**
         * Asserts the event-based result of the monitoring-analysis phase.
         * 
         * @param step the simulation step (positive integer)
         * @param event the event (may be <b>null</b> if the analysis phase did not detect constraint violations)
         * @param config the actual runtime configuration after monitoring-analysis
         * @return <code>true</code> the <code>event</code> valid, pass it to adaptation, <code>false</code> consume 
         *    it silently
         */
        protected abstract boolean assertAnalysis(int step, AdaptationEvent event, Configuration config);
        
        /**
         * Asserts the results of the adaptation phase.
         * 
         * @param step the simulation step (positive integer)
         * @param config the configuration used while testing
         */
        protected abstract void assertAdaptation(int step, Configuration config);
        
        /**
         * Allows to stop the simulation process. Called after {@link #assertAdaptation(int, Configuration)}.
         * 
         * @param step the simulation step (positive integer)
         * @return <code>true</code> for stopping, <code>false</code> else
         */
        protected boolean stop(int step) {
            return false;
        }

        /**
         * Returns the mapping file for <code>pipeline</code>.
         * 
         * @param pipeline the pipeline name
         * @return the mapping file
         * @throws IOException in case of I/O problems
         */
        protected abstract File obtainMappingFile(String pipeline) throws IOException;

        @Override
        public void notifyReasoningResult(Configuration config, ReasoningResult result) {
        }

        /**
         * Ends the test. Clean up temporary data.
         */
        protected void end() {
        }

    }
    
    /**
     * Prints reasoning information. Can simplify implementation of 
     * {@link TestSpec#notifyReasoningResult(Configuration, ReasoningResult)}.
     * 
     * @param config the configuration after reasoning
     * @param result the reasoning result
     */
    protected static void printReasoningInformation(Configuration config, ReasoningResult result) {
        if (null != config) {
            Configuration.printConfig(System.out, config);
        }
        if (null != result && result.hasConflict()) {
            for (int m = 0; m < result.getMessageCount(); m++) {
                Message msg = result.getMessage(m);
                System.out.println(msg.getDescription() + " " + msg.getConflictComments());
            }
        }
    }
    
    /**
     * Performs a single adaptation cycle in simulation mode.
     * 
     * @param testSpec the test specification for initializing / asserting
     * @throws IOException shall not occur
     */
    protected void performAdaptation(TestSpec testSpec) throws IOException {
        if (!debug) {
            Properties prop = new Properties();
            prop.put(AdaptationConfiguration.ADAPTATION_RTVIL_TRACERFACTORY, 
                TimeMeasurementTracerFactory.class.getName());
            AdaptationConfiguration.configure(prop);
        }
        
        List<INameMapping> mappings = new ArrayList<INameMapping>();
        // startup
        for (String pipeline : testSpec.getPipelineNames()) {
            File file = testSpec.obtainMappingFile(pipeline);
            FileInputStream fis = new FileInputStream(file);
            INameMapping mapping = new NameMapping(pipeline, fis);
            fis.close();
            CoordinationManager.registerTestMapping(mapping);
            mappings.add(mapping);
        }

        MonitoringManager.clearState();
        IReasoningModelProvider provider = new SimpleReasoningModelProvider(monConfig, monRtVilModel, monCopyMapping);
        ReasoningTask rTask = new ReasoningTask(provider);
        rTask.setReasoningListener(testSpec);
        testSpec.initialize(adaptConfig);
        for (int step = 1; step <= testSpec.getStepCount(); step++) {
            String id = testSpec.getTimeIdentifier();
            if (testSpec.getStepCount() > 1) {
                id = testSpec.getTimeIdentifier() + " step " + step;
            }
            TimeMeasurementTracerFactory.setCurrentIdentifier(id);
            // monitoring
            AdaptationEvent event = testSpec.monitor(step, MonitoringManager.getSystemState());
            // analysis
            if (null == event) {
                TimeMeasurementTracerFactory.measure(true, Measure.ANALYSIS);
                event = rTask.reason(false);
                TimeMeasurementTracerFactory.measure(false, Measure.ANALYSIS);
                if (!testSpec.assertAnalysis(step, event, monConfig)) {
                    event = null; // consume silently
                }
            }
            
            ISimulationNotifier notifier = RtVilStorage.setSimulationNotifier(testSpec);
            // run adaptation
            if (null != event) {
                if (debug) {
                    System.out.println("Adaptation event " + event);
                }
                TimeMeasurementTracerFactory.measure(true, Measure.ADAPT);
                AdaptationEventQueue.adapt(event, adaptConfig, adaptRtVilModel, tmp);
                TimeMeasurementTracerFactory.measure(false, Measure.ADAPT);
            }
            RtVilStorage.setSimulationNotifier(notifier);
            
            testSpec.assertAdaptation(step, adaptConfig);
            
            if (testSpec.stop(step)) {
                break;
            }
            testSpec.commands.clear();
        }

        testSpec.end();
        MonitoringManager.getSystemState().clear();
        
        for (INameMapping mapping : mappings) {
            CoordinationManager.unregisterNameMapping(mapping);
        }
    }
    
    /**
     * Enables the debug mode for adaptation.
     */
    public void enableDebug() {
        Properties prop = new Properties();
        prop.put(AdaptationConfiguration.ADAPTATION_RTVIL_LOGGING, "true");
        prop.put(AdaptationConfiguration.ADAPTATION_RTVIL_TRACERFACTORY, 
            DebugTimeMeasurementTracerFactory.class.getName());
        AdaptationConfiguration.configure(prop);
        debug = true;
    }
    
    /**
     * Enables resource adaptations.
     * 
     * @param observables the observables to enable the adaptation for
     */
    protected static void enableAdaptations(IObservable... observables) {
        changeAdaptations(false, observables);
    }

    /**
     * Disables resource adaptations.
     * 
     * @param observables the observables to enable the adaptation for
     */
    protected static void disableAdaptations(IObservable... observables) {
        changeAdaptations(true, observables);
    }
    
    /**
     * Changes the actual resource adaptations.
     * 
     * @param add whether observables shall be added (<code>true</code>) or removed (<code>false</code>)
     * @param observables the observables to consider
     */
    private static void changeAdaptations(boolean add, IObservable[] observables) {
        Set<String> adaptations = MonitoringConfiguration.getMonitoringAnalysisDisabled();
        for (IObservable obs : observables) {
            String name = obs.name();
            if (add) {
                adaptations.add(name);
            } else {
                adaptations.remove(name);
            }
        }
        StringBuilder tmp = new StringBuilder();
        Iterator<String> iter = adaptations.iterator();
        while (iter.hasNext()) {
            tmp.append(iter.next());
            if (iter.hasNext()) {
                tmp.append(",");
            }
        }
        Properties prop = new Properties();
        prop.setProperty(MonitoringConfiguration.MONITORING_ANALYSIS_DISABLED, tmp.toString());
        AdaptationConfiguration.configure(prop);
    }

    /**
     * Prints the given configuration.
     * 
     * @param config the configuration to print
     */
    protected void printConfig(Configuration config) {
        Iterator<IDecisionVariable> iter = config.iterator();
        while (iter.hasNext()) {
            print(iter.next(), "");
        }
    }
    
    /**
     * Prints the given decision variable at indent.
     * 
     * @param var the variable
     * @param indent the indent
     */
    private void print(IDecisionVariable var, String indent) {
        System.out.println(indent + " " + var.getDeclaration().getName() + " " + var.getState() + " " + var.getValue());
        for (int n = 0; n < var.getNestedElementsCount(); n++) {
            print(var.getNestedElement(n), indent + "  ");
        }
    }

    /**
     * Returns whether the running machine is identified as Jenkins and some tests shall not run.
     * 
     * @return <code>true</code> if the running machine is Jenkins, <code>false</code> else
     */
    public static boolean isJenkins() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostname = "localhost";
        }
        return JENKINS.contains(hostname);
    }
    
    /**
     * Returns the test directory depending on the system property <code>qm.base.dir</code>.
     * 
     * @return the test directory
     */
    public static File getTestDir() {
        return new File(System.getProperty("qm.base.dir", "."));
    }

    /**
     * Sleeps for a given time.
     * 
     * @param ms the time to sleep
     */
    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }
    
    /**
     * Prints all collected measures.
     */
    public static void printMeasures() {
        System.out.println("-------- MEASURES ---------");
        for (String id : TimeMeasurementTracerFactory.getIdentifiers()) {
            System.out.println(id);
            Map<Measure, Long> observations = TimeMeasurementTracerFactory.getObservations(id);
            TreeMap<String, Long> sorted = new TreeMap<String, Long>();
            for (Map.Entry<Measure, Long> ent : observations.entrySet()) {
                sorted.put(ent.getKey().name(), ent.getValue());
            }
            for (Map.Entry<String, Long> ent : sorted.entrySet()) {
                System.out.println(" " + ent.getKey() + " " + ent.getValue());
            }
        }
    }

}
