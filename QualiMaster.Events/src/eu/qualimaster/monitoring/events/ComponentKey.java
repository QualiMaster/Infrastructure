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
package eu.qualimaster.monitoring.events;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Implements a Storm-specific component key for monitoring. Keys shall uniquely identify
 * a task in a cluster.
 * 
 * @author Holger Eichelberger
 */
public class ComponentKey implements Serializable, IRemovalSelector {
    
    public static final  String UNKNOWN_HOST = "<unknown>";
    private static final String TEXT_THREAD_PREFIX = " thread ";
    private static final long serialVersionUID = 3497957153762370554L;
    private int taskId;
    private int port;
    private String hostName;
    private long threadId; // not part of object id!
    
    /**
     * Creates a component key of host name, port and taskId.
     * 
     * @param hostName the host name
     * @param port the used port
     * @param taskId the executed task id
     */
    public ComponentKey(String hostName, int port, int taskId) {
        this.hostName = hostName;
        this.port = port;
        this.taskId = taskId;
    }
    
    /**
     * Creates a component key from the actual host name, the given port and taskId.
     * 
     * @param port the used port
     * @param taskId the executed task id
     */
    public ComponentKey(int port, int taskId) {
        this(getLocalHostName(), port, taskId);
    }
    
    /**
     * Returns the host name (best effort).
     * 
     * @return the host name
     */
    public static String getLocalHostName() {
        String result;
        try {
            result = InetAddress.getLocalHost().getCanonicalHostName(); // check supervisor code
        } catch (UnknownHostException e) {
            result = UNKNOWN_HOST;
        }
        return result;
    }
    
    /**
     * Stores as additional information the thread id of the component.
     * The thread id is not considered to be part of the object identity, i.e., not used in {@link #hashCode()} 
     * or {@link #equals(Object)}. 
     * 
     * @param id the thread id
     */
    public void setThreadId(long id) {
        this.threadId = id;
    }
    
    /**
     * Returns the thread id of the component.
     * The thread id is not considered to be part of the object identity, i.e., not used in {@link #hashCode()} 
     * or {@link #equals(Object)}.
     * 
     * @return the thread id, unknown if not positive
     */
    public long getThreadId() {
        return threadId;
    }
    
    /**
     * Returns the task id.
     * 
     * @return the task id
     */
    public int getTaskId() {
        return taskId;
    }
    
    /**
     * Returns the host name.
     * 
     * @return the host name
     */
    public String getHostName() {
        return hostName;
    }
    
    /**
     * Returns the port number.
     * 
     * @return the port number
     */
    public int getPort() {
        return port;
    }

    @Override
    public boolean remove(Object object) {
        boolean remove = false;
        if (object instanceof ComponentKey) {
            ComponentKey other = (ComponentKey) object;
            remove = other.getTaskId() == getTaskId() 
                && (other.getPort() != getPort() || !other.getHostName().equals(getHostName()));
        }
        return remove;
    }
    
    @Override
    public boolean equals(Object object) {
        boolean equals = false;
        if (object instanceof ComponentKey) {
            ComponentKey other = (ComponentKey) object;
            equals = other.getTaskId() == getTaskId() && other.getPort() == getPort() 
                && other.getHostName().equals(getHostName());
            if (threadId > 0) {
                equals &= other.getThreadId() == threadId;
            }
        }
        return equals;
    }
    
    @Override
    public int hashCode() {
        int code = getTaskId() ^ getPort() ^ getHostName().hashCode();
        if (threadId > 0) {
            code ^= threadId;
        }
        return code;
    }

    /**
     * Returns a component key from a text produced by {@link #toString()}.
     * 
     * @param text the text to parse
     * @return the parsed component key (may be <b>null</b> in case of syntax problems)
     * @throws NumberFormatException in case that numbers cannot be parsed
     */
    public static ComponentKey valueOf(String text) throws NumberFormatException {
        ComponentKey result = null;
        int hostEnd = text.indexOf(':');
        if (hostEnd > 0) {
            int portEnd = text.indexOf(' ', hostEnd + 1);
            if (portEnd > 0) {
                int taskEnd = text.indexOf(' ', portEnd + 1);
                if (taskEnd < 0) {
                    taskEnd = text.length();
                }
                if (taskEnd > 0) {
                    String host = text.substring(0, hostEnd);
                    int port = Integer.parseInt(text.substring(hostEnd + 1, portEnd));
                    String tmp = text.substring(portEnd + 1, taskEnd).trim();
                    if (tmp.startsWith("[") && tmp.endsWith("]")) {
                        tmp = tmp.substring(1, tmp.length() - 1);
                    }
                    int taskId = Integer.parseInt(tmp);
                    result = new ComponentKey(host, port, taskId);
                    parseThread(text, hostEnd + 1, result);
                }
            }
        }
        return result;
    }
    
    /**
     * Parses the optional thread part of a textual representation produced by {@link #toString()}.
     * 
     * @param text the text
     * @param pos the position where to start parsing at
     * @param result the component key instance to be modified as a side effect
     * @throws NumberFormatException in case that numbers cannot be parsed
     */
    private static void parseThread(String text, int pos, ComponentKey result) throws NumberFormatException {
        int threadPos = text.indexOf(TEXT_THREAD_PREFIX, pos);
        if (threadPos > 0) { // optional
            threadPos += TEXT_THREAD_PREFIX.length();
            int threadEndPos = threadPos;
            while (threadEndPos < text.length() && Character.isDigit(text.charAt(threadEndPos))) {
                threadEndPos++;
            }
            if (threadEndPos > threadPos) {
                result.setThreadId(Long.parseLong(text.substring(threadPos, threadEndPos)));
            }
        }
    }
    
    @Override
    public String toString() {
        return getHostName() + ":" + getPort() + " [" + getTaskId() + "]" 
            + (threadId > 0 ? TEXT_THREAD_PREFIX + threadId : "");
    }

}
