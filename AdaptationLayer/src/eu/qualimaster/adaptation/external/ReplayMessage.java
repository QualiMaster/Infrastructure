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
package eu.qualimaster.adaptation.external;

import java.util.Date;

/**
 * Starts or stops data replay.
 * 
 * @author Holger Eichelberger
 */
public class ReplayMessage extends RequestMessage {

    private static final long serialVersionUID = -8425698624352080034L;
    private String pipeline;
    private String pipelineElement;
    private boolean startReplay;
    private int ticket;
    private Date start;
    private Date end;
    private int speed;
    private String query;
    
    /**
     * Creates a replay message.
     * 
     * @param pipeline the target pipeline
     * @param pipelineElement the pipeline element to change the replay for
     * @param startReplay start (<code>true</code>) or stop (<code>false</code>) the replay (repeated start messages on 
     *     the same <code>ticket</code> will be ignored). In case of start, call 
     *     {@link #setReplayStartInfo(Date, Date, int, String)} afterwards.
     * @param ticket the ticket number the client will expect data for. Repeated start to already used/stop to disabled
     *     tickets shall be ignored
     */
    public ReplayMessage(String pipeline, String pipelineElement, boolean startReplay, int ticket) {
        this.pipeline = pipeline;
        this.pipelineElement = pipelineElement;
        this.startReplay = startReplay;
        this.ticket = ticket;
    }
    
    /**
     * Sets additional data required to start a replay.
     * 
     * @param start the start point in time (if <b>null</b> start from the first available data)
     * @param end the end point in time (if <b>null</b> start from the last available data)
     * @param speed the replay speed, positive speed up, negative slow down
     * @param query which data to send (format unspecified so far)
     */
    public void setReplayStartInfo(Date start, Date end, int speed, String query) {
        this.start = start;
        this.end = end;
        this.speed = speed;
        this.query = query;
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
     * Returns the pipeline element to change the replaying for.
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
    public Date getStart() {
        return start;
    }
    
    /**
     * Returns the end date. 
     * 
     * @return the data end date (may be <b>null</b> for none)
     */
    public Date getEnd() {
        return end;
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
    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleReplayMessage(this);
    }

    @Override
    public Message toInformation() {
        String text;
        if (startReplay) {
            text = "start replay # " + ticket + (null != start ? " from " + start : "") 
                + (null != end ? " to " + end : "") + " speed " + speed  + " query " + query;
        } else {
            text = "stop replay # " + ticket;
        }
        return new InformationMessage(pipeline, null, text, null);
    }

    @Override
    public String toString() {
        String text = "ReplayMessage " + pipeline + " " + pipelineElement + " ";
        if (startReplay) {
            text = "start replay # " + ticket + (null != start ? " from " + start : "") 
                + (null != end ? " to " + end : "") + " speed " + speed  + " query " + query;
        } else {
            text = "stop replay # " + ticket;
        }
        return text;
    }

}
