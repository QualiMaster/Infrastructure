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
package eu.qualimaster.monitoring.handlers;

import java.util.Set;

import eu.qualimaster.common.signal.ConnectTaskMonitoringEvent;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.monitoring.MonitoringEventHandler;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.ResourceUsage;

/**
 * A handler for connect task monitoring events.
 * 
 * @author Holger Eichelberger
 */
public class ConnectTaskMonitoringEventHandler extends MonitoringEventHandler<ConnectTaskMonitoringEvent> {

    public static final ConnectTaskMonitoringEventHandler INSTANCE = new ConnectTaskMonitoringEventHandler();
    
    /**
     * Creates an instance.
     */
    private ConnectTaskMonitoringEventHandler() {
        super(ConnectTaskMonitoringEvent.class);
    }

    @Override
    protected void handle(ConnectTaskMonitoringEvent event, SystemState state) {
        // do not run checkTaskAndExecutors in here, this is too early - curator connection on worker not guaranteed
        // can be used to clean up old task entries where it makes sense to avoid multi-aggregation of the same value
        // just try to cause a re-send of information if a worker dies unexpectedly
        
        handle(event, state, true);
        handle(event, state, false);
    }
    
    /**
     * Handles the event.
     * 
     * @param event the event
     * @param state the system state
     * @param forMainPipeline handle for main/sub-pipeline
     */
    private void handle(ConnectTaskMonitoringEvent event, SystemState state, boolean forMainPipeline) {
        SystemPart target = determineAggregationPart(event, state, forMainPipeline);
        if (null != target && event.getKey() instanceof ComponentKey) {
            ComponentKey eventKey = (ComponentKey) event.getKey();
            Set<Object> keys = target.getComponentKeys(ResourceUsage.EXECUTORS);
            if (null != keys && !keys.contains(eventKey)) {
                ComponentKey found = null;
                for (Object k : keys) {
                    if (k instanceof ComponentKey) {
                        ComponentKey knownKey = (ComponentKey) k;
                        if (knownKey.getTaskId() == eventKey.getTaskId() && !knownKey.equals(eventKey)) {
                            found = knownKey;
                            break;
                        }
                    }
                }
                if (null != found) { // this seems to be a worker restart
                    handleRestartedWorker(found, event, target);
                }
            }
        }
    }
    
    /**
     * Handles a re-starting worker.
     * 
     * @param found the already known component key
     * @param event the causing event
     * @param target the target system part to aggregate for
     */
    private void handleRestartedWorker(ComponentKey found, ConnectTaskMonitoringEvent event, SystemPart target) {
        getLogger().info("Detected worker restart/migration for " + found + " -> " + event.getKey());
        target.replaceComponentKeys(found, event.getKey(), ResourceUsage.EXECUTORS, ResourceUsage.TASKS);
        if (target instanceof PipelineNodeSystemPart) {
            PipelineNodeSystemPart node = (PipelineNodeSystemPart) target;
            NodeImplementationSystemPart alg = node.getCurrent();
            if (null != alg) {
                getLogger().info("Detected worker had assigned algorithm. Re-sending algorithm change");
                new AlgorithmChangeCommand(event.getPipeline(), event.getPipelineElement(), 
                    alg.getName()).execute();
            }
        }
    }

}
