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
package eu.qualimaster.coordination.commands;

import java.util.Date;

import eu.qualimaster.common.QMInternal;
import net.ssehub.easy.instantiation.core.model.vilTypes.Invisible;

/**
 * Controls replay on pipelines.
 * 
 * @author Holger Eichelberger
 */
public class ReplayCommand extends AbstractPipelineElementCommand {

    private static final long serialVersionUID = 8972441311966913205L;
    private boolean startReplay;
    private int ticket;
    private Date start;
    private Date end;
    private float speed;
    private String query;

    /**
     * Creates a replay command. This command will be dispatched to all sinks.
     * 
     * @param pipeline the target pipeline
     * @param pipelineElement the element to affect (sink)
     * @param startReplay start (<code>true</code>) or stop (<code>false</code>) the replay (repeated start messages on 
     *     the same <code>ticket</code> will be ignored). In case of start, call 
     *     {@link #setReplayStartInfo(Date, Date, int, String)} afterwards.
     * @param ticket the ticket number the client will expect data for. Repeated start to already used/stop to disabled
     *     tickets shall be ignored
     */
    public ReplayCommand(String pipeline, String pipelineElement, boolean startReplay, int ticket) {
        super(pipeline, pipelineElement);
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
    @QMInternal
    public void setReplayStartInfo(Date start, Date end, float speed, String query) {
        this.start = start;
        this.end = end;
        this.speed = speed;
        this.query = query;
    }

    /**
     * Sets additional data required to start a replay. This method is required for rt-VIL.
     * 
     * @param start the start point in time (if <b>null</b> start from the first available data)
     * @param end the end point in time (if <b>null</b> start from the last available data)
     * @param speed the replay speed, positive speed up, negative slow down
     * @param query which data to send (format unspecified so far)
     * 
     * @see #toDate(String)
     */
    public void setReplayStartInfo(String start, String end, double speed, String query) {
        setReplayStartInfo(start, end, (float) speed, query);
    }

    /**
     * Sets additional data required to start a replay.
     * 
     * @param start the start point in time (if <b>null</b> start from the first available data)
     * @param end the end point in time (if <b>null</b> start from the last available data)
     * @param speed the replay speed, positive speed up, negative slow down
     * @param query which data to send (format unspecified so far)
     * 
     * @see #toDate(String)
     */
    @Invisible
    public void setReplayStartInfo(String start, String end, float speed, String query) {
        setReplayStartInfo(toDate(start), toDate(end), speed, query);
    }
    
    /**
     * Turns a date into a string (for IVML).
     * 
     * @param date the date
     * @return the related string (<b>null</b> if <code>date</code> is <b>null</b>)
     * @see #toDate(String)
     */
    @QMInternal
    public static String toString(Date date) {
        return null != date ? String.valueOf(date.getTime()) : null;
    }
    
    /**
     * Sturns a string into a date.
     * 
     * @param text the string
     * @return the date (<b>null</b> if <code>text</code> is null or invalid)
     * @see #toString(Date)
     */
    @QMInternal
    public static Date toDate(String text) {
        Date result = null;
        if (text != null) {
            try {
                result = new Date(Long.parseLong(text));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return result;
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
     * Returns the replay speed.
     * 
     * @return the replay speed
     */
    public double getSpeedDbl() {
        return speed;
    }

    /**
     * Returns the replay speed.
     * 
     * @return the replay speed
     */
    @QMInternal
    public float getSpeed() {
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
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitReplayCommand(this);
    }

}
