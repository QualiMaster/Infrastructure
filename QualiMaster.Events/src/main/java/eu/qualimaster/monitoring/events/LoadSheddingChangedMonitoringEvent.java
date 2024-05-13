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
 * Notifies the infrastructure about a change in load shedding.
 * 
 * @author Holger Eichelberger
 */
public class LoadSheddingChangedMonitoringEvent extends AbstractPipelineElementEnactmentCompletedMonitoringEvent {

    private static final long serialVersionUID = 3757748201336726478L;
    private String shedder;
    private String actualShedder;
    
    /**
     * Creates a load shedding changed event.
     * 
     * @param pipeline the pipeline name
     * @param pipelineElement the pipeline element
     * @param shedder the requested shedder
     * @param actualShedder the shedder that became active
     * @param causeMsgId the causing message (may be <b>null</b>)
     */
    public LoadSheddingChangedMonitoringEvent(String pipeline, String pipelineElement, String shedder, 
        String actualShedder, String causeMsgId) {
        super(pipeline, pipelineElement, null, causeMsgId);
        this.shedder = shedder;
        this.actualShedder = actualShedder;
    }
    
    /**
     * Returns the identification/class name of the requested shedder.
     * 
     * @return the name of the shedder
     */
    public String getShedder() {
        return shedder;
    }

    /**
     * Returns the identification/class name of the actual shedder.
     * 
     * @return the name of the shedder
     */
    public String getActualShedder() {
        return actualShedder;
    }

}
