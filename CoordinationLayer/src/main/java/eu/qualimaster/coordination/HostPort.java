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
package eu.qualimaster.coordination;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents an explicit host assignment. A host may be a network name or a Storm host id.
 * 
 * @author Holger Eichelberger
 */
public class HostPort {

    /**
     * A host-port text-to-instance parser.
     * 
     * @author Holger Eichelberger
     */
    public interface IHostPortParser {

        /**
         * Turns a text into a worker id.
         * 
         * @param text the text to be turned into a worker id (may be <b>null</b>)
         * @return the worker id or {@link #UNKONWN_WORKER} if the format cannot be parsed
         */
        public HostPort parse(String text);
        
    }

    /**
     * Implements a default workerbeats format parser.
     */
    public static final IHostPortParser WORKERBEATS_HOSTPORT_PARSER = new IHostPortParser() {
        
        @Override
        public HostPort parse(String text) {
            HostPort result = HostPort.UNKONWN;
            if (null != text) {
                int pos = text.indexOf(WORKERBEAT_SEPARATOR);
                if (pos > 0 && pos < text.length()) { // within
                    String node = text.substring(0, pos);
                    String tmp = text.substring(pos + 1, text.length());
                    try {
                        int port = Integer.parseInt(tmp);
                        result = new HostPort(node, port);
                    } catch (NumberFormatException e) {
                        // -> result = null
                    }
                } 
            }
            return result;
        }
    };

    /**
     * Denotes the separator between hostId and port used in workerbeats.
     */
    public static final String WORKERBEAT_SEPARATOR = "-";
    
    /**
     * Denotes the unknown (invalid) host assignment.
     */
    public static final HostPort UNKONWN = new HostPort("", -1);

    private String hostId;
    private int port;
    
    /**
     * Creates a host assignment.
     * 
     * @param hostId the host id
     * @param port the port
     */
    public HostPort(String hostId, int port) {
        this.hostId = hostId;
        this.port = port;
    }
    
    /**
     * Returns the host id.
     * 
     * @return the host id
     */
    public String getHostId() {
        return hostId;
    }
    
    /**
     * Returns the port.
     * 
     * @return the port
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Returns the endpoint id.
     * 
     * @return the endpoint id
     */
    public String getEndpointId() {
        return ZkUtils.getEndpointId(port, hostId);
    }

    /**
     * Returns whether this worker id is the unknown worker id.
     * 
     * @return <code>true</code> if this is the unknown worker id, <code>false</code> else
     */
    public boolean isUnknown() {
        return UNKONWN == this;
    }
    
    /**
     * Returns whether this worker id is invalid (no hostId, illegal port number).
     * 
     * @return <code>true</code> if invalid, <code>false</code> else
     */
    public boolean isInvalid() {
        return null == hostId || hostId.length() == 0 || port <= 0;
    }

    @Override
    public String toString() {
        return "[hostId " + hostId + " @ " + port + "]";
    }
    
    /**
     * Turns a text of format node-port into a worker id.
     * 
     * @param text the text to be turned into a worker id (may be <b>null</b>)
     * @return the worker id or {@link #UNKONWN_WORKER} if the format cannot be parsed
     */
    public static HostPort toHostPort(String text) {
        HostPort result = HostPort.UNKONWN;
        if (null != text) {
            int pos = text.indexOf("-");
            if (pos > 0 && pos < text.length()) { // within
                String node = text.substring(0, pos);
                String tmp = text.substring(pos + 1, text.length());
                try {
                    int port = Integer.parseInt(tmp);
                    result = new HostPort(node, port);
                } catch (NumberFormatException e) {
                    // -> result = null
                }
            } 
        }
        return result;
    }
    
    /**
     * Turns a list of textual worker ids to worker id objects.
     * 
     * @param workers the workers to be converted (may be <b>null</b>)
     * @param parser the host-port parser to be used
     * @return the converted workers (may be <b>null</b> if <code>workers</code> is <b>null</b>). Individual entries
     *   may be {@link #UNKONWN_WORKER} if the respective textual id cannot be converted.
     */
    public static List<HostPort> toHostPort(List<String> workers, IHostPortParser parser) {
        assert null != parser;
        List<HostPort> result = null;
        if (null != workers) {
            result = new ArrayList<HostPort>(workers.size());
            for (int w = 0; w < workers.size(); w++) {
                result.add(parser.parse(workers.get(w)));
            }
        }
        return result;
    }
    
    /**
     * Turns a map of textual worker ids with associated information to a map with worker ids.
     * 
     * @param <T> the value type
     * @param workers the workers to be converted (may be <b>null</b>)
     * @param parser the host-port parser to be used
     * @return the corresponding mapping from worker ids (may be {@link #UNKONWN_WORKER} to the same information, 
     *    may be <b>null</b> if <code>workers</code> is <b>null</b>
     */
    public static <T> Map<HostPort, T> toHostPort(Map<String, T> workers, IHostPortParser parser) {
        assert null != parser;
        Map<HostPort, T> result = null;
        if (null != workers) {
            result = new HashMap<HostPort, T>();
            for (Map.Entry<String, T> ent : workers.entrySet()) {
                result.put(parser.parse(ent.getKey()), ent.getValue());
            }
        }
        return result;
    }

}