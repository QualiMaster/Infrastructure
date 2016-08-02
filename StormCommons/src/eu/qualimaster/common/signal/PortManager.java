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

import org.apache.storm.curator.framework.CuratorFramework;

/**
 * Dynamically manages the ports to be used for loose pipeline connections and switches.
 * 
 * @author Holger Eichelberger
 * @author Cui Qin
 */
public class PortManager {
    
    private CuratorFramework client;
    
    /**
     * Represents a host-port assignment.
     * 
     * @author Holger Eichelberger
     */
    public static class PortAssignment {
        
        private String host;
        private int port;

        /**
         * Creates a port assignment.
         * 
         * @param host the host name
         * @param port the port number
         */
        public PortAssignment(String host, int port) {
            this.host = host;
            this.port = port;
        }
        
        /**
         * Returns the host name.
         * 
         * @return the host name
         */
        public String getHost() {
            return host;
        }
        
        /**
         * Returns the port number.
         * 
         * @return the port number
         */
        public int getPort() {
            return port;
        }
        
    }
    
    /**
     * Creates a port manager (frontend).
     * 
     * @param client the curator client connection
     */
    public PortManager(CuratorFramework client) {
        this.client = client;
    }
    
    /**
     * Cleans up the port assignments for a pipeline.
     * 
     * @param pipeline the pipeline name
     */
    public void clearPortAssignments(String pipeline) {
        // TODO logical, implementation name?
    }
    
    /**
     * Cleans up all existing port assignments.
     */
    public void clearAllPortAssignments() {
        // TODO implement
    }
    
    /**
     * Registers a port assignment. This method shall only be called by the component which runs the
     * server thread.
     * 
     * @param pipeline the pipeline
     * @param element the element
     * @param taskId the task id
     * @param assignment the port assignment
     */
    public void registerPortAssignment(String pipeline, String element, int taskId, PortAssignment assignment) {
        // TODO implement 
    }
    
    /**
     * Clears the given port assignment, e.g., on shutdown of the specific component.
     * 
     * @param pipeline the pipeline
     * @param element the element
     * @param taskId the task id
     * @param assignment the port assignment
     */
    public void clearPortAssignment(String pipeline, String element, int taskId, PortAssignment assignment) {
        // TODO implement 
    }

    /**
     * Returns an existing port assignment.
     * 
     * @param pipeline the pipeline
     * @param element the element
     * @param taskId the task id
     * @return the port assignment or <b>null</b> if there is none
     */
    public PortAssignment getPortAssignment(String pipeline, String element, int taskId) {
        return new PortAssignment("localhost", 8999);
    }
    
    /**
     * Closes this port manager. No port assignments will be available afterwards.
     */
    public void close() {
        client = null;
    }

}
