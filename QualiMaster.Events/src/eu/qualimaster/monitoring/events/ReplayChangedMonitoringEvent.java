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
package eu.qualimaster.monitoring.events;

/**
 * Notifies the infrastructure about a change of replay parameters.
 * 
 * @author Holger Eichelberger
 */
public class ReplayChangedMonitoringEvent extends AbstractPipelineElementEnactmentCompletedMonitoringEvent {

    private static final long serialVersionUID = 2304561331860696977L;

    private int ticket;
    private boolean startReplay;
    
    /**
     * Creates a replay changed event.
     * 
     * @param pipeline the pipeline name
     * @param pipelineElement the pipeline element
     * @param ticket the affected ticket number
     * @param startReplay whether replay was started/stopped
     * @param causeMsgId the causing message (may be <b>null</b>)
     */
    public ReplayChangedMonitoringEvent(String pipeline, String pipelineElement, int ticket, boolean startReplay, 
        String causeMsgId) {
        super(pipeline, pipelineElement, null, causeMsgId);
        this.ticket = ticket;
        this.startReplay = startReplay;
    }
    
    /**
     * The ticket id.
     * 
     * @return the ticket id
     */
    public int getTicket() {
        return ticket;
    }
    
    /**
     * Whether replay started/stopped.
     * 
     * @return whether replay started/stopped 
     */
    public boolean getStartReplay() {
        return startReplay;
    }
    
}
