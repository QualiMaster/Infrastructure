/*
 * Copyright 2009-2014 University of Hildesheim, Software Systems Engineering
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

import java.net.InetAddress;

/**
 * Represents a logging message sent through the infrastructure.
 * 
 * @author Holger Eichelberger
 */
public class LoggingMessage extends UsualMessage {

    private static final long serialVersionUID = 2484552247885603753L;
    private String level;
    private String message;
    private String threadName;
    private long timeStamp;
    private InetAddress address;
    
    /**
     * Creates a logging event. This constructor implicitly stores and transmits the local 
     * IP address.
     * 
     * @param timeStamp the logging time stamp
     * @param level the logging level
     * @param message the logging message
     * @param threadName the thread name
     * @param address the host address
     */
    public LoggingMessage(long timeStamp, String level, String message, String threadName, InetAddress address) {
        this.timeStamp = timeStamp;
        this.level = level;
        this.message = message;
        this.threadName = threadName;
        this.address = address;
    }

    /**
     * Returns the logging level.
     * 
     * @return the logging level
     */
    public String getLevel() {
        return level;
    }

    /**
     * Returns the message text.
     * 
     * @return the message text
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the thread name.
     * 
     * @return the thread name.
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Returns the logging time stamp.
     * 
     * @return the time stamp
     */
    public long getTimeStamp() {
        return timeStamp;
    }
    
    /**
     * Returns the host address where logging took place.
     * 
     * @return the address
     */
    public InetAddress getHostAddress() {
        return address;
    }
    
    @Override
    public void dispatch(IDispatcher dispatcher) {
        dispatcher.handleLoggingMessage(this);
    }

    @Override
    public int hashCode() {
        return Utils.hashCode(address) + Utils.hashCode(threadName) + Utils.hashCode(message) + Utils.hashCode(level) 
            + (int) (getTimeStamp() ^ (getTimeStamp() >>> 32));
    }
    
    @Override
    public boolean equals(Object obj) {
        boolean equals = false;
        if (obj instanceof LoggingMessage) {
            LoggingMessage msg = (LoggingMessage) obj;
            equals = getTimeStamp() == msg.getTimeStamp();
            equals &= Utils.equals(getHostAddress(), msg.getHostAddress());
            equals &= Utils.equals(getLevel(), msg.getLevel());
            equals &= Utils.equals(getMessage(), msg.getMessage());
            equals &= Utils.equals(getThreadName(), msg.getThreadName());
        }
        return equals;
    }

    @Override
    public Message toInformation() {
        return null; // do not pass on
    }

}
