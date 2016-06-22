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

import java.io.Serializable;

/**
 * An abstract pipeline enactment completed event.
 * 
 * @author Holger Eichelberger
 */
public class AbstractPipelineElementEnactmentCompletedMonitoringEvent extends AbstractPipelineElementMonitoringEvent 
    implements IEnactmentCompletedMonitoringEvent {

    private static final long serialVersionUID = 4107862510857650861L;
    private String causeMsgId;
    
    /**
     * Creates an instance.
     * 
     * @param pipeline pipeline the pipeline name
     * @param pipelineElement pipeline element the name of the pipeline element
     * @param key the aggregation component key (may be <b>null</b>)
     * @param causeMsgId the causing message id
     */
    protected AbstractPipelineElementEnactmentCompletedMonitoringEvent(String pipeline, String pipelineElement, 
        Serializable key, String causeMsgId) {
        super(pipeline, pipelineElement, key);
        this.causeMsgId = causeMsgId;
    }

    @Override
    public String getCauseMessageId() {
        return causeMsgId;
    }
    
}
