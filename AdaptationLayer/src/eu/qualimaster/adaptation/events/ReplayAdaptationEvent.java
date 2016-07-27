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
package eu.qualimaster.adaptation.events;

import java.util.Date;

import eu.qualimaster.adaptation.external.ReplayMessage;
import eu.qualimaster.common.QMInternal;
import eu.qualimaster.coordination.commands.ReplayCommand;

/**
 * Represents a request to start/stop replay.
 * 
 * @author Holger Eichelberger
 */
public class ReplayAdaptationEvent extends AdaptationEvent implements IPipelineAdaptationEvent {

    private static final long serialVersionUID = -147486520252671932L;
    private String pipeline;
    private String pipelineElement;
    private boolean startReplay;
    private int ticket;
    private Date start;
    private Date end;
    private int speed;
    private String query;

    /**
     * Creates an replay adaptation event.
     * 
     * @param msg the replay message
     */
    @QMInternal
    public ReplayAdaptationEvent(ReplayMessage msg) {
        this.pipeline = msg.getPipeline();
        this.pipelineElement = msg.getPipelineElement();
        this.startReplay = msg.getStartReplay();
        this.ticket = msg.getTicket();
        this.start = msg.getStart();
        this.end = msg.getEnd();
        this.speed = msg.getSpeed();
        this.query = msg.getQuery();
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
     * Returns the pipeline element.
     * 
     * @return the pipeline element
     */
    public String getPipelineElement() {
        return pipelineElement;
    }
    
    /**
     * Returns whether replay shall start or stop.
     * 
     * @return <code>true</code> for start, <code>false</code> for stop
     */
    public boolean getStartReplay() {
        return startReplay;
    }
    
    /**
     * Returns the replay ticket number.
     * 
     * @return the ticket number
     */
    public int getTicket() {
        return ticket;
    }

    /**
     * Returns the start date. 
     * 
     * @return the data start date (may be <b>null</b> for none)
     */
    @QMInternal
    public Date getStart() {
        return start;
    }
    
    /**
     * Returns the end date. 
     * 
     * @return the data end date (may be <b>null</b> for none)
     */
    @QMInternal
    public Date getEnd() {
        return end;
    }
    
    /**
     * Returns the start date as String (for IVML). 
     * 
     * @return the data start date (may be <b>null</b> for none)
     */
    public String getStartString() {
        return ReplayCommand.toString(start);
    }
    
    /**
     * Returns the end date as String (for IVML). 
     * 
     * @return the data end date (may be <b>null</b> for none)
     */
    public String getEndString() {
        return ReplayCommand.toString(end);
    }
    
    /**
     * Returns the replay speed.
     * 
     * @return the replay speed
     */
    public int getSpeed() {
        return speed;
    }
    
    /**
     * The query.
     * 
     * @return the query string
     */
    public String getQuery() {
        return query;
    }

}
