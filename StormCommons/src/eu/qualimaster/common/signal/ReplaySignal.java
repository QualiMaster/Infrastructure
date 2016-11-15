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
package eu.qualimaster.common.signal;

import java.util.Date;

import eu.qualimaster.common.QMInternal;

/**
 * Implements a replay signal to be sent to replay sinks.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class ReplaySignal extends AbstractTopologyExecutorSignal {

    private static final String IDENTIFIER = "replay";
    private static final long serialVersionUID = 3605057427020136764L;
    private boolean startReplay;
    private int ticket;
    private Date start;
    private Date end;
    private float speed;
    private String query;

    /**
     * Creates a replay signal.
     * 
     * @param topology the topology
     * @param executor the executor name
     * @param startReplay whether replay shall start or stop
     * @param ticket the ticket number characterizing the replay
     * @param causeMsgId the message id of the causing message (may be <b>null</b> or empty if there is none)
     */
    public ReplaySignal(String topology, String executor, boolean startReplay, int ticket, String causeMsgId) {
        super(topology, executor, causeMsgId);
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
    public void setReplayStartInfo(Date start, Date end, float speed, String query) {
        this.start = start;
        this.end = end;
        this.speed = speed;
        this.query = query;
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
    public byte[] createPayload() {
        return defaultSerialize(IDENTIFIER);
    }
    
    /**
     * Interprets the payload and sends it to the given listener if appropriate. [public for testing]
     * 
     * @param payload the signal payload
     * @param topology the name of the target topology (irrelevant)
     * @param executor the name of the target executor (irrelevant)
     * @param listener the listener
     * @return <code>true</code> if done, <code>false</code> else
     */
    public static boolean notify(byte[] payload, String topology, String executor, IReplayListener listener) {
        boolean done = false;
        ReplaySignal sig = defaultDeserialize(payload, IDENTIFIER, ReplaySignal.class);
        if (null != sig) {
            listener.notifyReplay(sig);
            done = true;
        }
        return done;
    }

    @Override
    public String toString() {
        return "ReplaySignal " + super.toString() + " #" + getTicket() + " " + startReplay + " " + start + " -> " + end 
            + " @" + speed + " " + query;
    }

}
