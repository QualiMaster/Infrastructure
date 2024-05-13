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
import eu.qualimaster.monitoring.MonitoringEventHandler;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * Implements the handling of {@link SubTopologyMonitoringEvent}.
 * 
 * @author Holger Eichelberger
 */
public class SubTopologyMonitoringEventHandler extends MonitoringEventHandler<SubTopologyMonitoringEvent> {

    public static final SubTopologyMonitoringEventHandler INSTANCE = new SubTopologyMonitoringEventHandler();
    
    /**
     * Creates an instance.
     */
    private SubTopologyMonitoringEventHandler() {
        super(SubTopologyMonitoringEvent.class);
    }

    @Override
    protected void handle(SubTopologyMonitoringEvent event, SystemState state) {
        String pipelineName = event.getPipeline();
        if (null == state.getPipeline(pipelineName)) { // check whether it is known - if not, defer
            MonitoringManager.addDeferredEvent(pipelineName, event);
        } else {
            INameMapping mapping = getNameMapping(pipelineName);
            if (null != mapping) {
                mapping.considerSubStructures(event);
                PipelineSystemPart pip = getActivePipeline(state, pipelineName);
                if (null != pip) {
                    pip.setTopology(null);
                    getLogger().info("Reseting topology for pipeline '" + pipelineName 
                        + "' due to received sub-topology monitoring event");
                }
            }
        }
    }

}
