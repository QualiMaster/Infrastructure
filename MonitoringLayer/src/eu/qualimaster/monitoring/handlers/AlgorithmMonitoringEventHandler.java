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
package eu.qualimaster.monitoring.handlers;

import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.INameMapping.Algorithm;
import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.monitoring.MonitoringEventHandler;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.AlgorithmMonitoringEvent;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;

/**
 * Implements the handling of {@link AlgorithmMonitoringEvent}.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmMonitoringEventHandler extends MonitoringEventHandler<AlgorithmMonitoringEvent> {

    public static final AlgorithmMonitoringEventHandler INSTANCE = new AlgorithmMonitoringEventHandler();
    private static final int CAUSE_NONE = 1;
    private static final int CAUSE_NONE_SUBPIPELINE = 2;
    private static final int CAUSE_UNKNOWN = 0;
    private static final int CAUSE_NO_PIPELINE_NAME = -1;
    private static final int CAUSE_NO_MAPPING = -2;
    private static final int CAUSE_NO_ACTIVE_PIPELINE_COMPONENT = -3;
    private static final int CAUSE_NO_ACTIVE_PIPELINE_ALGORITHM = -4;
    private static final int CAUSE_NO_ALGORITHM = -5;
    private static final int CAUSE_NO_PART = -6;
    
    /**
     * Creates an instance.
     */
    private AlgorithmMonitoringEventHandler() {
        super(AlgorithmMonitoringEvent.class);
    }

    @Override
    protected void handle(AlgorithmMonitoringEvent event, SystemState state) {
        String pipelineName = event.getPipeline();
        int cause = doHandle(event, state, pipelineName);
        int subCause = CAUSE_UNKNOWN;
        String subPipeline = findSubPipeline(pipelineName, event.getAlgorithmId());
        if (null != subPipeline) {
            subCause = doHandle(event, state, subPipeline);
            if (CAUSE_NONE == subCause) { // it's ok if it has been handled once...
                cause = CAUSE_NONE_SUBPIPELINE;
            }
        }
        if (CAUSE_UNKNOWN == cause || cause <= CAUSE_NO_ALGORITHM) { // only the most problematic ones... 
            getLogger().error("Cannot handle " + cause + " " + subCause + " " + event + " " 
                + MonitoringManager.getNameMapping(pipelineName));
        }
    }

    /**
     * Handles the given <code>event</code>.
     * 
     * @param event the event
     * @param state the system state
     * @param pipelineName the pipeline name (may be a sub-pipeline name)
     * @return the internal cause code in case that the event cannot be handled, negative or null in case error
     */
    private int doHandle(AlgorithmMonitoringEvent event, SystemState state, String pipelineName) {
        int cause = CAUSE_UNKNOWN;
        INameMapping mapping = null;
        if (null != pipelineName) { // just to be on the safe side
            mapping = MonitoringManager.getNameMapping(pipelineName);
            if (null != mapping) {
                IObservable obs = event.getObservable();
                if (!obs.isInternal()) {
                    cause = handle(mapping, event, state, obs);
                }                
            } else {
                cause = CAUSE_NO_MAPPING;
            }
        } else {
            cause = CAUSE_NO_PIPELINE_NAME;
        }
        return cause;
    }
    
    /**
     * Handles an algorithm monitoring event.
     * 
     * @param mapping the name mapping
     * @param event the event to be handled
     * @param state the system state
     * @param obs the observable identified for event
     * @return an internal error code, successful if <code>1</code> (temporary)
     */
    private int handle(INameMapping mapping, AlgorithmMonitoringEvent event, SystemState state, 
        IObservable obs) {
        int cause = CAUSE_UNKNOWN;
        String algId = event.getAlgorithmId();
        Component component = mapping.getComponentByClassName(algId);
        SystemPart part = null;
        if (null != component) {
            PipelineSystemPart pip = getActivePipeline(state, mapping.getPipelineName());
            if (null != pip) {
                part = pip.obtainPipelineNode(mapName(mapping, component.getName()));
            } else {
                cause = CAUSE_NO_ACTIVE_PIPELINE_COMPONENT;
            }
        } else {
            // fallback
            Algorithm algorithm = mapping.getAlgorithmByClassName(event.getAlgorithmId());
            if (null != algorithm) {
                PipelineSystemPart pip = getActivePipeline(state, mapping.getPipelineName());
                if (null != pip) {
                    part = pip.getAlgorithm(mapName(mapping, algorithm.getName()));
                } else {
                    cause = CAUSE_NO_ACTIVE_PIPELINE_ALGORITHM;
                }
            } else {
                cause = CAUSE_NO_ALGORITHM;
            }
        }
        if (null != part) {
            part.setValue(obs, event.getValue(), event.getComponentKey());
            cause = CAUSE_NONE;
        } else if (CAUSE_UNKNOWN == cause) { // don't override previous cause
            cause = CAUSE_NO_PART;
        }
        return cause;
    }
    
    /**
     * Maps back an implementation name.
     * 
     * @param mapping the name mapping
     * @param implementationName the implementation name
     * @return the mapped name
     */
    private static String mapName(INameMapping mapping, String implementationName) {
        String tmp = mapping.getPipelineNodeByImplName(implementationName);
        return null != tmp ? tmp : implementationName;
    }

}
