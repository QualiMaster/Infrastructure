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
package eu.qualimaster.monitoring;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import net.ssehub.easy.instantiation.rt.core.model.rtVil.AbstractAnalyzerVisitor;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Executor;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Script;
import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.common.monitoring.MonitoringPluginRegistry;
import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.RepositoryConnector.Models;
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.coordination.RuntimeVariableMapping;
import eu.qualimaster.easy.extension.QmConstants;
import eu.qualimaster.easy.extension.internal.PipelineHelper;
import eu.qualimaster.easy.extension.internal.VariableHelper;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.events.ConstraintViolationAdaptationEvent;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.events.ViolatingClause;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.systemState.TypeMapper;
import eu.qualimaster.monitoring.systemState.TypeMapper.TypeCharacterizer;
import eu.qualimaster.observables.AnalysisObservables;
import eu.qualimaster.observables.IObservable;
import net.ssehub.easy.basics.progress.ProgressObserver;
import net.ssehub.easy.reasoning.core.frontend.ReasonerFrontend;
import net.ssehub.easy.reasoning.core.reasoner.ReasonerConfiguration;
import net.ssehub.easy.reasoning.core.reasoner.ReasonerConfiguration.IAdditionalInformationLogger;
import net.ssehub.easy.reasoning.core.reasoner.ReasoningResult;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.cst.ConstraintSyntaxTree;
import net.ssehub.easy.varModel.model.AbstractVariable;
import net.ssehub.easy.varModel.model.Constraint;
import net.ssehub.easy.varModel.model.ModelQuery;
import net.ssehub.easy.varModel.model.ModelQueryException;
import net.ssehub.easy.varModel.model.Project;
import net.ssehub.easy.varModel.model.datatypes.Compound;
import net.ssehub.easy.varModel.model.datatypes.Container;
import net.ssehub.easy.varModel.model.datatypes.IDatatype;
import net.ssehub.easy.varModel.model.datatypes.StringType;
import net.ssehub.easy.varModel.model.values.BooleanValue;
import net.ssehub.easy.varModel.model.values.CompoundValue;
import net.ssehub.easy.varModel.model.values.EnumValue;
import net.ssehub.easy.varModel.model.values.IntValue;
import net.ssehub.easy.varModel.model.values.StringValue;
import net.ssehub.easy.varModel.model.values.Value;
import net.ssehub.easy.varModel.persistency.StringProvider;

/**
 * The reasoning task for detecting SLA violations on the infrastructure configuration.
 * 
 * @author Holger Eichelberger
 */
public class ReasoningTask extends TimerTask {

    private static final String OBSERVATION_SEPARATOR = ".";
    private static final boolean WITH_DEBUG = Boolean.valueOf(System.getProperty("qm.monitoring.debug", "false"));
    private static final ReasonerConfiguration CONFIGURATION = new ReasonerConfiguration();
    private AtomicBoolean inProcessing = new AtomicBoolean(false);
    private Configuration config;
    private Script rtVilModel;
    private File tmp;
    private AnalyzerVisitor analyzerVisitor = new AnalyzerVisitor();
    private int debugFileCount = 0;
    private RuntimeVariableMapping variableMapping;
    private Map<String, Deviation> activeDeviations = new HashMap<String, Deviation>();
    private transient Set<String> currentDeviations = new HashSet<String>();
    private IReasoningListener listener;
    private IReasoningModelProvider provider;
    private double minDevDifference = MonitoringConfiguration.getAnalysisMinDeviationDifference() / 100.0;
    
    private IDatatype typePipeline;
    private IDatatype typePipelineElement;
    private IDatatype typeAlgorithm;
    private IDatatype typeMachine;
    private IDatatype typeHwNode;
    
    private AdaptationEvent event = new AdaptationEvent() {

        private static final long serialVersionUID = 2164881726000323540L;
    };
    
    static {
        CONFIGURATION.setRuntimeMode(true);
        CONFIGURATION.setAdditionalInformationLogger(new IAdditionalInformationLogger() {
            
            @Override
            public void info(String arg0) {
            }
        });
    }
    
    /**
     * Defines a reasoning listener to obtain the actual messages from reasoning, mostly for debugging.
     * 
     * @author Holger Eichelberger
     */
    public interface IReasoningListener {

        /**
         * Notifies about the actual results.
         * 
         * @param config the configuration after reasoning
         * @param result the reasoning result
         */
        public void notifyReasoningResult(Configuration config, ReasoningResult result);
        
    }

    /**
     * Represents a recorded observation.
     *  
     * @author Holger Eichelberger
     */
    private static class Deviation {
        private double percentage;
        private String frozenPrefix;
        private SystemPart part;
        private String frozenStateKey;
        private IObservable observable;
        private String operation;
        private String variable;

        /**
         * Creates an observation.
         * 
         * @param percentage the observed deviation
         * @param observable the observable for which the deviation was observed
         * @param frozenPrefix the prefix for {@link FrozenSystemState} (may be <b>null</b>)
         * @param frozenStateKey the key for {@link FrozenSystemState} (may be <b>null</b>)
         * @param part the related system part (may be <b>null</b>)
         */
        private Deviation(double percentage, IObservable observable, String frozenPrefix, String frozenStateKey, 
            SystemPart part) {
            setPercentage(percentage);
            this.frozenPrefix = frozenPrefix;
            this.part = part;
            this.frozenStateKey = frozenStateKey;
            this.observable = observable;
        }
        
        /**
         * Defines the causing IVML operation as additional information.
         * 
         * @param operation the operation name
         */
        private void setOperation(String operation) {
            this.operation = operation;
        }
        
        /**
         * Defines the (name of the) causing variable as additional information.
         * 
         * @param variable the causing variable
         */
        private void setVariable(IDecisionVariable variable) {
            this.variable = Configuration.getInstanceName(variable);
        }

        /**
         * Defines the name of the causing IVML operation as additional information.
         * 
         * @return the operation name (may be <b>null</b>)
         */
        private String getOperation() {
            return operation;
        }
        
        /**
         * Returns the name of the causing variable as additional information.
         * 
         * @return the name (may be <b>null</b>)
         */
        private String getVariable() {
            return variable;
        }

        /**
         * Changes the observed deviation percentage.
         * 
         * @param percentage the percentage
         */
        private void setPercentage(double percentage) {
            this.percentage = percentage;
        }
        
        /**
         * Returns the observed deviation percentage.
         * 
         * @return the observed percentage
         */
        private double getPercentage() {
            return percentage;
        }
        
        /**
         * Changes an observation in the related system part or frozen system state.
         * 
         * @param state the state to modify (may be <b>null</b>, then it is not modified, similarly for the prefix
         *   and key given in the constructor)
         * @param value the actual value
         * @param key the aggregation key (may be <b>null</b>)
         */
        private void setObservation(FrozenSystemState state, double value, Object key) {
            if (null != state && null != frozenPrefix && null != frozenStateKey) {
                state.setObservation(frozenPrefix, frozenStateKey, observable, value);
            }
            if (null != part) {
                part.setValue(observable, value, key);
            }
        }
        
        /**
         * Returns the observable.
         * 
         * @return the observable
         */
        private IObservable getObservable() {
            return observable;
        }
        
        @Override
        public String toString() {
            return part + " " + frozenPrefix + " " + frozenStateKey + " " + observable + " " + percentage;
        }
        
    }
    
    /**
     * Defines the interface of a reasoning model provider. This provider may enable / prevent model updates
     * at runtime.
     * 
     * @author Holger Eichelberger
     */
    public interface IReasoningModelProvider {

        /**
         * Returns the IVML configuration.
         * 
         * @return the configuration (may change over time, may be <b>null</b> if loading failed)
         */
        public Configuration getConfiguration();
        
        /**
         * Returns the rt-VIL script.
         * 
         * @return the script (may change over time, may be <b>null</b> if loading failed)
         */
        public Script getScript();
        
        /**
         * Returns the runtime variable mapping.
         * 
         * @return the variable mapping (may change over time, may be <b>null</b> if loading failed)
         */
        public RuntimeVariableMapping getVariableMapping();
        
        /**
         * Indicates the start of a period using the models. During this period no update shall
         * happen.
         */
        public void startUsing();
        
        /**
         * Indicates the start of a period using the models. During this period no update shall
         * happen.
         */
        public void endUsing();
        
    }

    /**
     * Implements a phase-based reasoning model provider, which is runtime-model update-ready.
     * 
     * @author Holger Eichelberger
     */
    public static class PhaseReasoningModelProvider implements IReasoningModelProvider {
        
        private Phase phase;

        /**
         * Creates the provider.
         * 
         * @param phase the phase to be used
         */
        public PhaseReasoningModelProvider(Phase phase) {
            this.phase = phase;
        }
        
        /**
         * Returns the models for the phase of this provider.
         * 
         * @return the models
         */
        private Models getModels() {
            return RepositoryConnector.getModels(phase);
        }

        @Override
        public Configuration getConfiguration() {
            Models models = getModels();
            return null == models ? null : models.getConfiguration();
        }

        @Override
        public Script getScript() {
            Models models = getModels();
            return null == models ? null : models.getAdaptationScript();
        }

        @Override
        public RuntimeVariableMapping getVariableMapping() {
            Models models = getModels();
            return null == models ? null : models.getVariableMapping();
        }

        @Override
        public void startUsing() {
            Models models = getModels();
            if (null != models) {
                models.startUsing();
            }
        }

        @Override
        public void endUsing() {
            Models models = getModels();
            if (null != models) {
                models.endUsing();
            }
        }
        
    }

    /**
     * Implements a simple reasoning model provider based on given models. This provider
     * shall not be used in a context where the given models may update.
     * 
     * @author Holger Eichelberger
     */
    public static class SimpleReasoningModelProvider implements IReasoningModelProvider {

        private Configuration config;
        private Script script;
        private RuntimeVariableMapping mapping;
        
        /**
         * Creates the provider instance.
         * 
         * @param config the configuration to reason on (modified as a side effect)
         * @param script the rt-VIL model used to map the monitoring values to runtime values
         * @param mapping the mapping of the configuration copies representing runtime instances
         */
        public SimpleReasoningModelProvider(Configuration config, Script script, RuntimeVariableMapping mapping) {
            this.config = config;
            this.script = script;
            this.mapping = mapping;
        }
        
        @Override
        public Configuration getConfiguration() {
            return config;
        }

        @Override
        public Script getScript() {
            return script;
        }

        @Override
        public RuntimeVariableMapping getVariableMapping() {
            return mapping;
        }

        @Override
        public void startUsing() {
            // ignore
        }

        @Override
        public void endUsing() {
            // ignore
        }
        
    }
    
    /**
     * Creates a reasoning task.
     * 
     * @param provider the model provider
     */
    public ReasoningTask(IReasoningModelProvider provider) {
        this.provider = provider;
        tmp = RepositoryConnector.createTmpFolder();
        tmp.deleteOnExit();
        checkProviderUpdate();
    }
    
    @Override
    public boolean cancel() {
        boolean result = super.cancel();
        dispose();
        return result;
    }
    
    /**
     * Disposes temporary resources. To be called explicity if this class is not used as a timer task.
     */
    public void dispose() {
        FileUtils.deleteQuietly(tmp);
    }

    /**
     * Checks for updates of the models in the provider.
     */
    private void checkProviderUpdate() {
        provider.startUsing();
        Configuration cfg = provider.getConfiguration();
        if (cfg != config) { // checking for config is sufficient 
            this.config = cfg;
            this.rtVilModel = provider.getScript();
            this.variableMapping = provider.getVariableMapping();
            
            Project prj = this.config.getProject();
            typePipeline = findDatatype(prj, QmConstants.TYPE_PIPELINE);
            typePipelineElement = findDatatype(prj, QmConstants.TYPE_PIPELINE_ELEMENT);
            typeMachine = findDatatype(prj, QmConstants.TYPE_MACHINE);
            typeHwNode = findDatatype(prj, QmConstants.TYPE_HWNODE);
            typeAlgorithm = findDatatype(prj, QmConstants.TYPE_ALGORITHM);            
        }
        provider.endUsing();
    }
        
    /**
     * Finds the given IVML type.
     *  
     * @param prj the project (may be <b>null</b>, return fallback)
     * @param name the type name
     * @return the type, if not use <code>String</code> as fallback (no match at all)
     */
    private static IDatatype findDatatype(Project prj, String name) {
        IDatatype result = StringType.TYPE;
        if (null != prj) {
            try {
                result = ModelQuery.findType(prj, name, null);
            } catch (ModelQueryException e) {
            }
        }
        return result;
    }
    
    /**
     * Defines the reasoning listener.
     * 
     * @param listener the reasoning listener (may be <b>null</b> to disable listening)
     */
    public void setReasoningListener(IReasoningListener listener) {
        this.listener = listener;
    }

    // checkstyle: stop exception type check
    
    @Override
    public void run() {
        if (!inProcessing.getAndSet(true)) { // guard entry
            reason(true);
            inProcessing.set(false);
        }
    }

    /**
     * Performs reasoning and returns the violating clauses. [public for testing]
     * 
     * @param send if <code>true</code> send events, if <code>false</code> be quiet and do not send events
     * @return the adaptation event or <b>null</b> if no violating clauses were detected
     */
    public AdaptationEvent reason(boolean send) {
        checkProviderUpdate();
        provider.startUsing();
        AdaptationEvent resultEvent = null;
        SystemState sysState = MonitoringManager.getSystemState();
        PipelineAnalysis.analyze(config, sysState);
        Map<String, PipelineLifecycleEvent.Status> pipStatus = sysState.getPipelinesStatus();
        FrozenSystemState state = sysState.freeze();

        if (WITH_DEBUG) {
            String logLocation = MonitoringConfiguration.getMonitoringLogInfraLocation();
            if (!MonitoringConfiguration.isEmpty(logLocation)) {
                File f = new File(logLocation, "monitoring_" + debugFileCount++);
                try {
                    state.store(f);
                } catch (IOException e) {
                    getLogger().error(e.getMessage(), e);
                }
            }
        }
        // map monitoring to config
        ReasoningResult result = null;
        Executor exec = RepositoryConnector.createExecutor(rtVilModel, tmp, config, event, state);
        exec.stopAfterBindValues();
        try {
            exec.execute();
            result = ReasonerFrontend.getInstance().check(config.getProject(), config, CONFIGURATION, 
                ProgressObserver.NO_OBSERVER);
        } catch (Exception e) { // be extremely careful
            getLogger().error("During value binding: " + e.getMessage(), e);
        }
        if (null != listener) {
            listener.notifyReasoningResult(config, result);
        }
        List<ViolatingClause> violating = null;
        if (null != result && result.hasConflict()) { //debugPrintReasoningResult(result);
            analyzerVisitor.setState(state, pipStatus);
            violating = analyzerVisitor.analyze(config, result);
        } 
        if (null == violating) {
            violating = new ArrayList<ViolatingClause>();
        }
        purgeActualDeviations(state, violating);
        if (null != violating && !violating.isEmpty()) {
            Set<String> disabledAdaptations = MonitoringConfiguration.getMonitoringAnalysisDisabled();
            for (int v = 0; v < violating.size(); v++) {
                ViolatingClause cl = violating.get(v);
                IObservable obs = cl.getObservable();
                if (null != obs) {
                    if (disabledAdaptations.contains(obs.name()) || disabledAdaptations.contains("*")) {
                        violating.remove(v);
                    }
                }
            }
            if (!violating.isEmpty()) {
                resultEvent = new ConstraintViolationAdaptationEvent(violating, state);
                if (send) {
                    EventManager.handle(resultEvent);
                }
            }
        }
        currentDeviations.clear();
        analyzerVisitor.clearState();
        MonitoringPluginRegistry.analyze(state);
        provider.endUsing();
        return resultEvent;
    }
    
    /**
     * Prints the relevant contents of a reasoning result (for debugging).
     * 
     * @param result the result to be printed
     */
    static void debugPrintReasoningResult(ReasoningResult result) {
        if (null != result) {
            System.out.println("CONFLICT");     
            for (int m = 0; m < result.getMessageCount(); m++) {
                System.out.println(result.getMessage(m).getDescription());
                List<Constraint> csts = result.getMessage(m).getProblemConstraints();
                for (int c = 0; c < csts.size(); c++) {
                    System.out.println("  " + StringProvider.toIvmlString(csts.get(c).getConsSyntax()));
                }
            }
        }
    }

    /**
     * Purges actual deviations and sets back {@link AnalysisObservables#IS_VALID} if needed.
     * 
     * @param state the actual system state
     * @param violating the violating clauses (may be modified to reflect released conditions)
     */
    private void purgeActualDeviations(FrozenSystemState state, List<ViolatingClause> violating) {
        Iterator<Map.Entry<String, Deviation>> iter = activeDeviations.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Deviation> entry = iter.next();
            String key = entry.getKey();
            if (!currentDeviations.contains(key)) {
                iter.remove();
                Deviation deviation = entry.getValue();
                IObservable observable = deviation.getObservable();
                if (AnalysisObservables.IS_VALID == deviation.getObservable()) {
                    deviation.setObservation(state, 1.0, null); // reset to valid
                } else if (null != deviation) {
                    violating.add(new ViolatingClause(observable, deviation.getVariable(), deviation.getOperation(), 
                         ViolatingClause.CLEARED, ViolatingClause.CLEARED));
                }
            }
        }
    }

    // checkstyle: resume exception type check

    /**
     * Characterizes an actual violation.
     * 
     * @author Holger Eichelberger
     */
    private static class ActualViolation {
        
        private IObservable observable;
        private IDecisionVariable variable;
        
        /**
         * Creates an actual violation instance.
         * 
         * @param observable the actual observable
         * @param variable the actual variable
         */
        private ActualViolation(IObservable observable, IDecisionVariable variable) {
            this.observable = observable;
            this.variable = variable;
        }
    }

    /**
     * Extends the evaluation visitor to do some simple constraint deviation analysis.
     * 
     * @author Holger Eichelberger
     */
    private class AnalyzerVisitor extends AbstractAnalyzerVisitor<ViolatingClause> {

        private FrozenSystemState state;
        private Constraint currentConstraint; 
        private Map<String, PipelineLifecycleEvent.Status> pipStatus;
        
        /**
         * Sets the state before analyzing. Please call this method along with 
         * {@link #init(de.uni_hildesheim.sse.model.confModel.IConfiguration, 
         * de.uni_hildesheim.sse.model.confModel.IAssignmentState, boolean, 
         * de.uni_hildesheim.sse.model.cstEvaluation.IValueChangeListener)}.
         * 
         * @param state the state to set
         * @param pipStatus a pipeline - pipeline status mapping
         */
        public void setState(FrozenSystemState state, Map<String, PipelineLifecycleEvent.Status> pipStatus) {
            this.state = state;
            this.pipStatus = pipStatus;
        }
        
        /**
         * Clears the state after analyzing. Please call this method along with {@link #clear()}.
         */
        public void clearState() {
            this.state = null;
            this.pipStatus = null;
        }
        
        @Override
        public List<ViolatingClause> analyze(Configuration config, ConstraintSyntaxTree cst, Constraint constraint) {
            currentConstraint = constraint;
            List<ViolatingClause> result = super.analyze(config, cst, constraint);
            currentConstraint = null;
            return result;
        }

        /**
         * Decides whether a detected deviation is an actual deviation (not already occurred/active) and disables
         * model elements in case of user constraints.
         * 
         * @param var the variable involved in the actual deviation
         * @param operation the constraint operation causing the violation
         * @param deviationPercentage the actual deviation in percent
         * @return the affected observable of the violation, <b>null</b> for no violation
         */
        private ActualViolation isActualViolation(IDecisionVariable var, String operation, Double deviationPercentage) {
            boolean isConstraint = false;
            IDecisionVariable top = Configuration.getTopLevelDecision(var);
            IObservable obs = ObservableMapper.getObservable(var);
            if (null != currentConstraint && currentConstraint.getParent() instanceof AbstractVariable) {
                AbstractVariable parent = (AbstractVariable) currentConstraint.getParent();
                if (Container.TYPE.isAssignableFrom(parent.getType())) {
                    // we cannot go on with currentConstraint as this is on type rather than on model level
                    isConstraint = true;
                    obs = AnalysisObservables.IS_VALID;
                }
            } 
            if (null != top) {
                List<String> path = PipelineUtils.determineFrozenStatePath(top, variableMapping);
                IDatatype topType = top.getDeclaration().getType();
                IDecisionVariable pipElt = variableMapping.getReferencedBy(top);
                if (path.size() > 0) {
                    Deviation deviation = getActualDeviation(deviationPercentage, topType, obs, path, isConstraint);
                    if (null == deviation) {
                        obs = null; // already known and sent - no further event needed
                    } else {
                        if (isConstraint) {
                            deviation.setObservation(state, 0.0, null);
                            // -> take common slot
                            var = (null != pipElt) ? pipElt.getNestedElement(QmConstants.SLOT_NAME) : var; 
                        }
                        deviation.setOperation(operation);
                        deviation.setVariable(var);
                    }
                }
            }
            ActualViolation result;
            if (null == obs) {
                result = null;
            } else {
                result = new ActualViolation(obs, var);
            }
            return result;
        }
        
        /**
         * Returns whether the <code>newDevPercient</code> is an actual deviation.
         * 
         * @param newDevPercent the new deviation in percent
         * @param eltType the type of the related IVML element
         * @param observable the observable for which the potential deviation was recorded
         * @param path the system state path corresponding to <code>eltType</code>
         * @param isUserConstraint is the deviation caused by a user constraint
         * @return the deviation information structure, <b>null</b> for none
         */
        private Deviation getActualDeviation(Double newDevPercent, IDatatype eltType, IObservable observable, 
            List<String> path, boolean isUserConstraint) {
            Deviation deviation = null;
            String fPath = PipelineUtils.toFrozenStatePath(path);
            if (!EnactingPipelineElements.INSTANCE.isEnacting(fPath)) {
                String devKey = fPath + OBSERVATION_SEPARATOR + observable;
                Deviation oldDeviation = activeDeviations.get(devKey);
                if (null == newDevPercent) {
                    newDevPercent = 0.0; // just as a marker
                } 
                if (null == oldDeviation) {
                    TypeCharacterizer characterizer = TypeMapper.findCharacterizer(eltType);
                    String frozenPrefix = null;
                    SystemPart part = null;
                    if (null != characterizer) {
                        frozenPrefix = characterizer.getFrozenStatePrefix();
                        part = characterizer.getSystemPart(path);
                    }
                    deviation = new Deviation(newDevPercent, observable, frozenPrefix, fPath, part);
                    activeDeviations.put(devKey, deviation);
                    currentDeviations.add(devKey);
                } else {
                    if (!isUserConstraint && Math.abs(newDevPercent - oldDeviation.getPercentage()) 
                        > minDevDifference) { 
                        oldDeviation.setPercentage(newDevPercent);
                        deviation = oldDeviation;
                    } else {
                        deviation = null; // don't send event, but we are active
                    }
                    currentDeviations.add(devKey);
                }
            }
            return deviation;
        }
        
        @Override
        protected ViolatingClause createViolationInstance(IDecisionVariable var, String operation, Double deviation, 
            Double deviationPercentage) {
            ViolatingClause clause = null;
            ActualViolation act = isActualViolation(var, operation, deviation); 
            if (null != act) {
                clause = new ViolatingClause(act.observable, Configuration.getInstanceName(act.variable, true), 
                    operation, deviation, deviationPercentage);
                try {
                    IDecisionVariable pVar = PipelineHelper.obtainPipeline(config, act.variable);
                    if (null != pVar) {
                        Value pValue = pVar.getValue();
                        if (pValue instanceof CompoundValue) {
                            Value nValue = ((CompoundValue) pValue).getNestedValue(QmConstants.SLOT_NAME);
                            if (nValue instanceof StringValue) {
                                clause.setPipeline(((StringValue) nValue).getValue());
                            }
                        }
                    }
                } catch (ModelQueryException e) {
                }
            }
            return clause;
        }

        @Override
        protected boolean isRelevantVariable(IDecisionVariable var) {
            boolean found = false;
            if (null != var) {
                for (int a = 0; !found && a < var.getAttributesCount(); a++) {
                    IDecisionVariable attribute = var.getAttribute(a);
                    AbstractVariable decl = attribute.getDeclaration();
                    if (QmConstants.ANNOTATION_BINDING_TIME.equals(decl.getName())) {
                        Value val = attribute.getValue();
                        if (val instanceof EnumValue) {
                            EnumValue eVal = (EnumValue) val;
                            found = (eVal.getValue().getName().startsWith("runtime"));
                        }
                    }
                }
                found = found && isActive(var, state, pipStatus);
            }
            return found;
        }
        
    }
    
    /**
     * Determines whether <code>var</code>, its parent or its context is considered 
     * to be currently active.
     * 
     * @param var the variable to be tested
     * @param state the frozen monitoring state
     * @param pipStatus a pipeline - pipeline status mapping
     * @return <code>true</code> if <code>var</code> is considered to be active, 
     *   <code>false</code> else
     */
    protected boolean isActive(IDecisionVariable var, FrozenSystemState state, Map<String, 
        PipelineLifecycleEvent.Status> pipStatus) {
        boolean result = true; // fallback
        IDecisionVariable actual = var;
        // determine parent variable and its base type
        while (actual.getParent() instanceof IDecisionVariable) {
            actual = (IDecisionVariable) actual.getParent();
        }
        // is finding type and assignable
        IDatatype type = actual.getDeclaration().getType();
        while (type instanceof Compound) {
            Compound ref = ((Compound) type).getRefines();
            if (null != ref) {
                type = ref;
            } else {
                break;
            }
        }
        if (typePipelineElement.isAssignableFrom(type)) {
            try {
                result = isPipelineActive(PipelineHelper.obtainPipeline(actual.getConfiguration(), actual), pipStatus);
            } catch (ModelQueryException e) {
                getLogger().error(e.getMessage(), e);
            }
        } else if (typeAlgorithm.isAssignableFrom(type)) {
            IDecisionVariable top = Configuration.getTopLevelDecision(var);
            IDecisionVariable pipElt = variableMapping.getReferencedBy(top);
            if (null != pipElt) {
                try {
                    result = isPipelineActive(PipelineHelper.obtainPipeline(actual.getConfiguration(), pipElt), 
                        pipStatus);
                } catch (ModelQueryException e) {
                    getLogger().error(e.getMessage(), e);
                }
            } else {
                result = false;
            }
        } else if (typePipeline.isAssignableFrom(type)) {
            result = isPipelineActive(actual, pipStatus);
        } else if (typeMachine.isAssignableFrom(type)) {
            result = isResourceActive(actual);
        } else if (typeHwNode.isAssignableFrom(type)) {
            result = isResourceActive(actual);
        }
        return result;
    }

    /**
     * Returns whether the pipeline in <code>var</code> is active.
     * 
     * @param var the variable to treat as pipeline
     * @param pipStatus a pipeline - pipeline status mapping
     * @return <code>true</code> if the resource is active, <code>false</code> else
     */
    private static boolean isPipelineActive(IDecisionVariable var, 
        Map<String, PipelineLifecycleEvent.Status> pipStatus) {
        boolean result = false;
        String name = VariableHelper.getName(var);
        if (null != name) {
            PipelineLifecycleEvent.Status status = pipStatus.get(name);
            if (PipelineLifecycleEvent.Status.STARTED == status) {
                Value val = getCompoundValue(var, "executors");
                if (val instanceof IntValue) {
                    Integer iVal = ((IntValue) val).getValue();
                    if (null != iVal) {
                        result = iVal.intValue() > 0;
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Returns whether the resource in <code>var</code> is active.
     * 
     * @param var the variable to treat as resource
     * @return <code>true</code> if the resource is active, <code>false</code> else
     */
    private static boolean isResourceActive(IDecisionVariable var) {
        boolean result = false;
        Value val = getCompoundValue(var, "available");
        if (val instanceof BooleanValue) {
            Boolean bVal = ((BooleanValue) val).getValue();
            if (null != bVal) {
                result = bVal.booleanValue();
            }
        }
        return result;
    }

    /**
     * Returns the value of a compound slot if it exists.
     * 
     * @param var the variable to take the compound value from
     * @param slot the slot name
     * @return the value of <code>slot</code> in <code>var</code>, <b>null</b> else
     */
    private static Value getCompoundValue(IDecisionVariable var, String slot) {
        Value result = null;
        if (null != var) {
            Value val = var.getValue();
            if (val instanceof CompoundValue) {
                CompoundValue cValue = (CompoundValue) val;
                result = cValue.getNestedValue(slot);
            }
        }
        return result;
    }
    
    /**
     * Returns the logger for this class.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return LogManager.getLogger(ReasoningTask.class);
    }
    
}
