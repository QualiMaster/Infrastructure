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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.events.IEvent;
import eu.qualimaster.monitoring.events.AbstractPipelineElementMonitoringEvent;
import eu.qualimaster.monitoring.events.MonitoringEvent;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * A specific handler for monitoring events.
 * 
 * @param <E> the event type
 * @author Holger Eichelberger
 */
public abstract class MonitoringEventHandler <E extends MonitoringEvent> {

    private Class<E> eventClass;
    
    /**
     * Creates a new event handler.
     * 
     * @param eventClass the handled class of events
     */
    protected MonitoringEventHandler(Class<E> eventClass) {
        this.eventClass = eventClass;
    }
    
    /**
     * Returns the class of events being handled.
     * 
     * @return the handled class of events
     */
    public Class<E> handles() {
        return eventClass;
    }
    
    /**
     * Called internally to handle a generic event.
     * 
     * @param event the event to handle
     * @param state the actual system state to be modified
     */
    void doHandle(IEvent event, SystemState state) {
        handle(handles().cast(event), state);
    }
    
    /**
     * Handles an event of the supported event type.
     * 
     * @param event the event to be handled
     * @param state the actual system state to be modified
     */
    protected abstract void handle(E event, SystemState state);

    /**
     * Determines the system part for aggregation.
     * 
     * @param event the monitoring event
     * @param state the system state to be modified
     * @return the system part (platform by default, may be <b>null</b> if it should be a pipeline/element in 
     *   a dead pipeline)
     */
    protected static SystemPart determineAggregationPart(AbstractPipelineElementMonitoringEvent event, 
        SystemState state) {
        String pipelineName = event.getPipeline();
        SystemPart target = state.getPlatform();
        PipelineSystemPart pPart = null;
        if (null != pipelineName) {
            target = null;
            pPart = getActivePipeline(state, pipelineName);
            if (null != pPart) {
                target = pPart;
                String nodeName = event.getPipelineElement();
                PipelineNodeSystemPart nPart = null;
                if (null != nodeName) {
                    INameMapping mapping = MonitoringManager.getNameMapping(pipelineName);
                    nPart = SystemState.getNodePart(mapping, pPart, nodeName);
                }
                if (null != nPart) {
                    target = nPart;
                }
            }
        } 
        return target;
    }

    /**
     * Returns the pipeline system part representing an active (not shutting down) pipeline.
     * 
     * @param state the system state
     * @param pipelineName the pipeline name
     * @return the active pipeline or <b>null</b>
     */
    protected static PipelineSystemPart getActivePipeline(SystemState state, String pipelineName) {
        PipelineSystemPart pip = state.getPipeline(pipelineName);
        if (null != pip && pip.isShuttingDown()) {
            pip = null;
        }
        return pip;
    }
    
    /**
     * Returns the logger for this class.
     * 
     * @return the logger
     */
    protected Logger getLogger() {
        return LogManager.getLogger(getClass());
    }
    
    /**
     * Returns the name mapping for the given <code>pipelineName</code>.
     * 
     * @param pipelineName the name of the pipeline
     * @return the name mapping (an identity mapping in case of no registered mapping)
     */
    protected static INameMapping getNameMapping(String pipelineName) {
        return MonitoringManager.getNameMapping(pipelineName);
    }

}
