/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.adaptation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.coordination.EnactmentCommandCollector;
import eu.qualimaster.coordination.commands.AbstractCoordinationCommandVisitor;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationExecutionResult;
import eu.qualimaster.coordination.commands.LoadSheddingCommand;
import eu.qualimaster.coordination.commands.MonitoringChangeCommand;
import eu.qualimaster.coordination.commands.ParallelismChangeCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.commands.ReplayCommand;
import eu.qualimaster.coordination.commands.ScheduleWavefrontAdaptationCommand;
import eu.qualimaster.coordination.commands.ShutdownCommand;
import eu.qualimaster.coordination.commands.UpdateCommand;
import eu.qualimaster.easy.extension.internal.PipelineElementHelper;
import eu.qualimaster.easy.extension.internal.VariableHelper;
import eu.qualimaster.monitoring.PipelineUtils;
import eu.qualimaster.monitoring.events.ConstraintViolationAdaptationEvent;
import eu.qualimaster.monitoring.events.ViolatingClause;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;

/**
 * Supports filtering and pruning of adaptation events, e.g., if parts of a pipeline are in enactment.
 * 
 * @author Holger Eichelberger
 */
class AdaptationFiltering {

    /**
     * Defines an adaptation event filter.
     * 
     * @param <E> the actual event type
     * @author Holger Eichelberger
     */
    public abstract static class AdaptationEventFilter <E extends AdaptationEvent> {
        
        private Class<E> cls;
        
        /**
         * Creates an adaptation event filter for a certain event class.
         * 
         * @param cls the event class
         */
        AdaptationEventFilter(Class<E> cls) {
            this.cls = cls;
        }
        
        /**
         * Returns the type of event handled by this filter.
         * 
         * @return the type of event
         */
        public Class<E> handles() {
            return cls;
        }
        
        /**
         * Returns whether a certain <code>event</code> on a given <code>configuration</code> is enabled.
         * 
         * @param config the configuration
         * @param event the event
         * @return <code>true</code> if enabled, <code>false</code> else
         */
        public boolean isEnabled(Configuration config, AdaptationEvent event) {
            boolean result = true;
            if (cls.isInstance(event)) {
                result = isEnabledImpl(config, cls.cast(event));
            }
            return result;
        }
        
        /**
         * Returns whether a certain <code>event</code> on a given <code>configuration</code> is enabled.
         * 
         * @param config the configuration
         * @param event the event
         * @return <code>true</code> if enabled, <code>false</code> else
         */
        protected abstract boolean isEnabledImpl(Configuration config, E event);
        
    }

    /**
     * Creates a pipeline element filter.
     * 
     * @param <E> the actual event type
     * @author Holger Eichelberger
     */
    private interface IPipelineElementFilterCreator<E extends AdaptationEvent> {

        /**
         * Creates the filter from a frozen system state path.
         * 
         * @param fPath the path
         * @return the filter
         */
        public PipelineElementFilter<E> createFilter(List<String> fPath);
        
    }
    
    /**
     * Defines a specialized adaptation filter focusing on enacting pipeline elements. This filter can be enacted
     * multiple times but will be stored only once (counter).
     * 
     * @param <E> the actual event type
     * @author Holger Eichelberger
     */
    protected abstract static class PipelineElementFilter<E extends AdaptationEvent> extends AdaptationEventFilter<E> {

        private String pipeline;
        private String pipelineElement;
        private String frozenStatePath;
        private int count = 1;

        /**
         * Creates the filter.
         * 
         * @param fPath the frozen system state path
         * @param cls the event type class
         */
        PipelineElementFilter(List<String> fPath, Class<E> cls) {
            super(cls);
            this.frozenStatePath = PipelineUtils.toFrozenStatePath(fPath);
            if (null == fPath || fPath.size() != 2) {
                throw new IllegalArgumentException(frozenStatePath + " does not contain pipeline and element");
            }
            this.pipeline = fPath.get(0);
            this.pipelineElement = fPath.get(1);
        }

        /**
         * Creates the filter.
         * 
         * @param pipeline the pipeline name
         * @param pipelineElement the pipeline element name
         * @param cls the event type class
         */
        PipelineElementFilter(String pipeline, String pipelineElement, Class<E> cls) {
            super(cls);
            this.pipeline = pipeline;
            this.pipelineElement = pipelineElement;
            this.frozenStatePath = PipelineUtils.toFrozenStatePathString(pipeline, pipelineElement);
        }

        /**
         * Returns the pipeline name.
         * 
         * @return the pipeline name
         */
        public String getPipeline() {
            return pipeline;
        }

        /**
         * Returns the pipeline element name.
         * 
         * @return the pipeline element name
         */
        public String getPipelineElement() {
            return pipelineElement;
        }

        /**
         * Returns the frozen system state path.
         * 
         * @return the path
         */
        public String getFrozenStatePath() {
            return frozenStatePath;
        }
        
        /**
         * Returns the number of times this filter has been activated while at least one filter of this instance
         * was activated.
         * 
         * @return the number of activation times
         */
        public int getCount() {
            return count;
        }
        
        /**
         * Increases the number of activation times.
         */
        void incCount() {
            count++;
        }

        /**
         * Decreases the number of activation times.
         */
        void decCount() {
            count--;
        }

    }
    
    /**
     * Implements a constraint violation adaptation event filter to prevent adaptation during enactment.
     * 
     * @author Holger Eichelberger
     */
    public static class ConstraintViolationAdaptationEventFilter 
        extends PipelineElementFilter<ConstraintViolationAdaptationEvent> {

        /**
         * Creates a filter from a frozen system state path.
         * 
         * @param fPath the path
         */
        ConstraintViolationAdaptationEventFilter(List<String> fPath) {
            super(fPath, ConstraintViolationAdaptationEvent.class);
        }

        /**
         * Creates a filter from a pipeline / element name.
         * 
         * @param pipeline the pipeline name
         * @param pipelineElement the pipeline element name
         */
        ConstraintViolationAdaptationEventFilter(String pipeline, String pipelineElement) {
            super(pipeline, pipelineElement, ConstraintViolationAdaptationEvent.class);
        }

        @Override
        protected boolean isEnabledImpl(Configuration config, ConstraintViolationAdaptationEvent event) {
            boolean isEnabled = true;
            for (int c = 0; isEnabled && c < event.getViolatingClauseCount(); c++) {
                ViolatingClause clause = event.getViolatingClause(c);
                if (getPipeline().equals(clause.getPipeline())) {
                    IDecisionVariable elt = PipelineElementHelper.findPipelineElement(config, clause.getVariable());
                    isEnabled = VariableHelper.hasName(elt, getPipelineElement());
                }
            }
            return isEnabled;
        }
        
    }
    
    /**
     * Defines the standard pipeline element filter creator for constraint violation events.
     */
    public static final IPipelineElementFilterCreator<ConstraintViolationAdaptationEvent> 
        CONSTRAINT_VIOLATION_FILTER_CREATOR = new IPipelineElementFilterCreator<ConstraintViolationAdaptationEvent>() {

            @Override
            public PipelineElementFilter<ConstraintViolationAdaptationEvent> createFilter(List<String> fPath) {
                return new ConstraintViolationAdaptationEventFilter(fPath);
            }
        
        };
    
    private static List<AdaptationEventFilter<?>> filters 
        = Collections.synchronizedList(new ArrayList<AdaptationEventFilter<?>>()); // linked list vs concurrent modific
    private static Map<String, PipelineElementFilter<?>> pipFilters 
        = Collections.synchronizedMap(new HashMap<String, PipelineElementFilter<?>>());

    /**
     * Returns whether a certain <code>event</code> on a given <code>configuration</code> is enabled considering all
     * defined filters.
     * 
     * @param config the configuration
     * @param event the event
     * @return <code>true</code> if enabled, <code>false</code> else
     */
    static boolean isEnabled(Configuration config, AdaptationEvent event) {
        boolean isEnabled = true;
        for (int f = 0; isEnabled && f < filters.size(); f++) {
            isEnabled = filters.get(f).isEnabled(config, event);
        }
        return isEnabled;
    }

    /**
     * Modifies the actual pipeline element filters.
     * 
     * @param command the (set/sequence) coordination command
     * @param addFilter add or remove the corresponding filter
     */
    static void modifyPipelineElementFilters(CoordinationCommand command, boolean addFilter) {
        if (null != command) {
            EnactmentCommandCollector collector = new EnactmentCommandCollector();
            command.accept(collector);
            List<CoordinationCommand> commands = collector.getResult();
            for (CoordinationCommand cmd : commands) {
                modifyPipelineElementFilter(cmd, addFilter, CONSTRAINT_VIOLATION_FILTER_CREATOR);
            }
        }
    }

    /**
     * Modifies the actual pipeline element filters for primitive coordination commands.
     * 
     * @param command the (set/sequence) coordination command
     * @param addFilter add or remove the corresponding filter
     * @param creator the creator for creating new filter instances
     */
    static void modifyPipelineElementFilter(CoordinationCommand command, boolean addFilter, 
        IPipelineElementFilterCreator<?> creator) {
        FPathCoordinationCommandVisitor vis = new FPathCoordinationCommandVisitor();
        command.accept(vis);
        List<String> fPath = vis.getFrozenStatePath();
        if (null != fPath) {
            PipelineElementFilter<?> filter = pipFilters.get(fPath);
            if (addFilter) {
                if (null == filter) {
                    filter = creator.createFilter(fPath);
                    pipFilters.put(PipelineUtils.toFrozenStatePath(fPath), filter);
                    addFilter(filter);
                } else {
                    filter.incCount();
                }
            } else { // remove
                if (null != filter) {
                    if (filter.getCount() > 1) {
                        filter.decCount();
                    } else {
                        pipFilters.remove(fPath);
                        removeFilter(filter);
                    }
                }
            }
        }
    }

    /**
     * Adds a filter.
     * 
     * @param filter the filter
     */
    static void addFilter(AdaptationEventFilter<?> filter) {
        if (null != filter) {
            filters.add(filter);
        }
    }

    /**
     * Removes a filter.
     * 
     * @param filter the filter
     */
    static void removeFilter(AdaptationEventFilter<?> filter) {
        if (null != filter) {
            filters.remove(filter);
        }
    }

    /**
     * A coordination command visitor for obtaining a frozen system state path.
     * 
     * @author Holger Eichelberger
     */
    private static class FPathCoordinationCommandVisitor extends AbstractCoordinationCommandVisitor {

        private List<String> fPath;

        /**
         * Returns the frozen system state path after visiting.
         * 
         * @return the path, may be <b>null</b>
         */
        private List<String> getFrozenStatePath() {
            return fPath;
        }
        
        @Override
        public CoordinationExecutionResult visitAlgorithmChangeCommand(AlgorithmChangeCommand command) {
            fPath = PipelineUtils.constructPath(command.getPipeline(), command.getPipelineElement());
            return null;
        }

        @Override
        public CoordinationExecutionResult visitParameterChangeCommand(ParameterChangeCommand<?> command) {
            fPath = PipelineUtils.constructPath(command.getPipeline(), command.getPipelineElement());
            return null;
        }

        @Override
        public CoordinationExecutionResult visitPipelineCommand(PipelineCommand command) {
            return null;
        }

        @Override
        public CoordinationExecutionResult visitScheduleWavefrontAdaptationCommand(
            ScheduleWavefrontAdaptationCommand command) {
            return null;
        }

        @Override
        public CoordinationExecutionResult visitMonitoringChangeCommand(MonitoringChangeCommand command) {
            return null;
        }

        @Override
        public CoordinationExecutionResult visitParallelismChangeCommand(ParallelismChangeCommand command) {
            return null;
        }

        @Override
        public CoordinationExecutionResult visitProfileAlgorithmCommand(ProfileAlgorithmCommand command) {
            return null;
        }

        @Override
        public CoordinationExecutionResult visitShutdownCommand(ShutdownCommand command) {
            return null;
        }

        @Override
        public CoordinationExecutionResult visitUpdateCommand(UpdateCommand command) {
            return null;
        }

        @Override
        public CoordinationExecutionResult visitReplayCommand(ReplayCommand command) {
            fPath = PipelineUtils.constructPath(command.getPipeline(), command.getPipelineElement());
            return null;
        }

        @Override
        public CoordinationExecutionResult visitLoadScheddingCommand(LoadSheddingCommand command) {
            fPath = PipelineUtils.constructPath(command.getPipeline(), command.getPipelineElement());
            return null;
        }

        @Override
        public CoordinationExecutionResult visitCloudExecutionCommand(CoordinationCommand command) {
            return null;
        }
        
    }

}
