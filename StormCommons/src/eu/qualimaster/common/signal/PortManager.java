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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.storm.curator.framework.CuratorFramework;
import org.apache.storm.curator.framework.api.transaction.CuratorTransaction;
import org.apache.storm.curator.framework.api.transaction.CuratorTransactionFinal;
import org.apache.storm.curator.framework.imps.CuratorFrameworkState;
import org.apache.storm.zookeeper.WatchedEvent;
import org.apache.storm.zookeeper.Watcher;
import org.apache.storm.zookeeper.Watcher.Event.EventType;

import backtype.storm.utils.Utils;
import eu.qualimaster.Configuration;

import static eu.qualimaster.common.signal.SignalMechanism.PATH_SEPARATOR;

/**
 * Dynamically manages the ports to be used for loose pipeline connections and switches. Assignment ids are intended
 * to separate multiple assignments for one task using a (logical) identifier.
 * 
 * @author Holger Eichelberger
 * @author Cui Qin
 */
public class PortManager {

    private static final String PORTS = PATH_SEPARATOR + "ports";
    private static final String PORTS_PREFIX = PORTS + PATH_SEPARATOR;
    private static final String NODES = PORTS_PREFIX + "nodes";
    private static final String NODES_PREFIX = NODES + PATH_SEPARATOR;
    private static final String HOSTS = PORTS_PREFIX + "hosts";
    private static final String HOSTS_PREFIX = HOSTS + PATH_SEPARATOR;

    private CuratorFramework client;
    private PortRange range;
    
    /**
     * Represents a host-port assignment.
     * 
     * @author Holger Eichelberger
     */
    public static class PortAssignment implements Serializable {
        
        private static final long serialVersionUID = -4270456287996753553L;
        private String host;
        private int port;
        private int taskId;
        private String assignmentId;

        /**
         * Creates a port assignment.
         * 
         * @param host the host name
         * @param port the port number
         * @param taskId the task id
         * @param assignmentId an id identifying this assignment (for returning the port if multiple are assigned 
         *     per task, may be <b>null</b>)
         */
        public PortAssignment(String host, int port, int taskId, String assignmentId) {
            this.host = host;
            this.port = port;
            this.taskId = taskId;
            this.assignmentId = assignmentId;
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

        /**
         * Returns the task id.
         * 
         * @return the task id
         */
        public int getTaskId() {
            return taskId;
        }
        
        /**
         * Returns the assignment id.
         * 
         * @return the assignment id (may be <b>null</b>)
         */
        public String getAssignmentId() {
            return assignmentId;
        }
        
        /**
         * Returns whether <code>assignmentId</code> is equal to the one store in this instance.
         * 
         * @param assignmentId the assignment id to check for
         * @return <code>true</code> for equal, <code>false</code> else
         */
        public boolean equalsAssigmentId(String assignmentId) {
            boolean result;
            if (null == assignmentId) {
                result = this.assignmentId == null;
            } else {
                result = assignmentId.equals(this.assignmentId);
            }
            return result;
        }
        
        @Override
        public int hashCode() {
            return host.hashCode() ^ port ^ taskId;
        }
        
        @Override
        public boolean equals(Object obj) {
            boolean result;
            if (obj instanceof PortAssignment) {
                PortAssignment pa = (PortAssignment) obj;
                result = pa.getHost().equals(host) && pa.getPort() == port && pa.getTaskId() == taskId;
            } else {
                result = false;
            }
            return result;
        }
        
        @Override
        public String toString() {
            return host + "@" + port + " " + taskId + " " + assignmentId;
        }

    }
    
    /**
     * Allows to watch a certain port assignment.
     * 
     * @author Holger Eichelberger
     */
    public interface IPortAssignmentWatcher {

        /**
         * Notifies about a done port assignment.
         * 
         * @param request the port assignment request
         * @param assignment the actual port assignment
         */
        public void notifyPortAssigned(PortAssignmentRequest request, PortAssignment assignment);
        
    }
    
    /**
     * Describes a range of (valid) ports.
     * 
     * @author Holger Eichelberger
     */
    public static class PortRange {
        private int lowPort;
        private int highPort;

        /**
         * Creates a port range from a string in format "<num>-<num>".
         * 
         * @param range the range
         * @throws IllegalArgumentException if range is not given
         * @throws NumberFormatException if the given ports are not numerical
         */
        public PortRange(String range) {
            if (null == range || range.isEmpty()) {
                throw new IllegalArgumentException("range must not be null or empty");
            }
            String[] parts = range.replace(" ", "").split("-");
            if (null != parts) {
                if (1 == parts.length) {
                    int p = Integer.parseInt(parts[0]);
                    setPorts(p, p);
                } else if (2 == parts.length) {
                    setPorts(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                } else {
                    throw new IllegalArgumentException("illegal range format: " + range);    
                }
            } else {
                throw new IllegalArgumentException("illegal range format: " + range);
            }
        }
        
        /**
         * Creates a port range. If not given in ascending order, ports will be re-ordered.
         * 
         * @param lowPort the low port
         * @param highPort the high port
         */
        public PortRange(int lowPort, int highPort) {
            setPorts(lowPort, highPort);
        }
        
        /**
         * Sets the ports range. If not given in ascending order, ports will be re-ordered.
         * 
         * @param lowPort the low port
         * @param highPort the high port
         */        
        private void setPorts(int lowPort, int highPort) {
            this.lowPort = Math.min(lowPort, highPort);
            this.highPort = Math.max(lowPort, highPort);
        }

        /**
         * Returns the low port.
         * 
         * @return the low port (inclusive)
         */
        public int getLowPort() {
            return lowPort;
        }
        
        /**
         * Returns the high port.
         * 
         * @return the high port (inclusive)
         */
        public int getHighPort() {
            return highPort;
        }
        
    }
    
    /**
     * A table of allocated ports per host.
     * 
     * @author Holger Eichelberger
     */
    public static class HostTable implements Serializable {
        
        private static final long serialVersionUID = -4118233476276137785L;
        private Map<Integer, String> assignments = new HashMap<Integer, String>();

        /**
         * Creates an instance.
         */
        public HostTable() {
        }
        
        /**
         * Adds an assignment of a port to a pipeline.
         * 
         * @param port the port
         * @param pipeline the pipeline name
         */
        public void addAssignment(int port, String pipeline) {
            assignments.put(port, pipeline);
        }
        
        /**
         * Removes a potential assignment for the given <code>port</code> .
         * 
         * @param port the port to remove the assignment for
         * @return <code>true</code> if changed, <code>false</code> else
         */
        public boolean removeAssignment(int port) {
            return null != assignments.remove(port);
        }
        
        /**
         * Returns the assignment for <code>port</code>.
         * 
         * @param port the port to look for
         * @return the assigned pipeline or <b>null</b> if there is no assignment
         */
        public String getAssignment(int port) {
            return assignments.get(port);
        }
        
        /**
         * Clears this table.
         */
        public void clear() {
            assignments.clear();
        }

        @Override
        public String toString() {
            return assignments.toString();
        }

    }

    /**
     * The table of ports in use for a certain node.
     * 
     * @author Holger Eichelberger
     */
    public static class PortsTable implements Serializable {

        private static final long serialVersionUID = 5288422389921327479L;
        private Map<Integer, Map<Integer, PortAssignment>> assignments 
            = new HashMap<Integer, Map<Integer, PortAssignment>>();

        /**
         * Creates an instance.
         */
        public PortsTable() {
        }
        
        /**
         * Returns an existing port assignment.
         * 
         * @param taskId the task id of <code>element</code>
         * @param assignmentId the assignmentId to look for (may be <b>null</b>)
         * @return the port assignment or <b>null</b> if there is none
         */
        public PortAssignment getPortAssignment(int taskId, String assignmentId) {
            PortAssignment result = null;
            Map<Integer, PortAssignment> portTable = assignments.get(taskId);
            if (null != portTable) {
                for (PortAssignment p : portTable.values()) {
                    if (p.equalsAssigmentId(assignmentId)) {
                        result = p;
                        break;
                    }
                }
                getLogger().info("Querying port assignment for " + taskId + " " + assignmentId + ": " + result 
                    + " from " + portTable);
            }
            return result;
        }
        
        /**
         * Returns all assignments.
         * 
         * @return all assignments
         */
        public List<PortAssignment> assigments() {
            List<PortAssignment> result = new ArrayList<PortAssignment>();
            for (Map<Integer, PortAssignment> inner : assignments.values()) {
                result.addAll(inner.values());
            }
            return result;
        }

        /**
         * Returns all assignments per host.
         * 
         * @return all assignments per host
         */
        public Map<String, List<PortAssignment>> assigmentsByHost() {
            Map<String, List<PortAssignment>> result = new HashMap<String, List<PortAssignment>>();
            for (Map<Integer, PortAssignment> inner : assignments.values()) {
                for (PortAssignment pa : inner.values()) {
                    String host = pa.getHost();
                    List<PortAssignment> l = result.get(host);
                    if (null == l) {
                        l = new ArrayList<PortAssignment>();
                        result.put(host, l);
                    }
                    l.add(pa);
                }
            }
            return result;
        }

        /**
         * Registers a port assignment. 
         * 
         * @param assignment the port assignment to register
         */
        private void registerPortAssignment(PortAssignment assignment) {
            int taskId = assignment.getTaskId();
            Map<Integer, PortAssignment> portTable = assignments.get(taskId);
            if (null == portTable) {
                portTable = new HashMap<Integer, PortAssignment>();
                assignments.put(taskId, portTable);
            }
            portTable.put(assignment.getPort(), assignment);
            getLogger().info("Registered port assignment " + assignment + " to " + portTable);
        }

        /**
         * Clears the given port assignment if it exists.
         * 
         * @param assignment the assignment to be cleared
         * @return <code>true</code> for changed, <code>false</code> else
         */
        public boolean clearPortAssignment(PortAssignment assignment) {
            boolean done = false;
            Map<Integer, PortAssignment> portTable = assignments.get(assignment.getTaskId());
            if (null != portTable) {
                PortAssignment ex = portTable.get(assignment.getPort());
                if (ex.equals(assignment)) {
                    portTable.remove(assignment.getPort());
                    if (0 == portTable.size()) {
                        assignments.remove(assignment.getTaskId());
                    }
                    getLogger().info("Unregistered port assignment " + assignment + " from " + portTable);
                    done = true;
                }
            }
            return done;
        }
        
        @Override
        public String toString() {
            return assignments.toString();
        }

    }
    
    /**
     * A node printer, prints individual nodes and decides whether a node shall be printed.
     * 
     * @author Holger Eichelberger
     */
    private interface INodePrinter {
        
        /**
         * Shall print the data of a zNode.
         *  
         * @param out the output stream
         * @param path the zNode path
         * @param indentation the actual indentation
         * @throws SignalException if reading the data fails
         */
        public void print(PrintStream out, String path, String indentation) throws SignalException;
        
    }
    
    /**
     * A deserializing node printer.
     * 
     * @author Holger Eichelberger
     */
    private class DeserializingNodePrinter implements INodePrinter {

        private int depth;
        private Class<?> dataCls;

        /**
         * Deserializes for <code>dataCls</code>.
         * 
         * @param dataCls the type to serialize for
         */
        private DeserializingNodePrinter(Class<?> dataCls) {
            this(0, dataCls);
        }

        /**
         * Deserializes for <code>dataCls</code> on a given path depth level.
         * 
         * @param depth the path depth level (ignored if not positive), 1 is typically the leading /
         * @param dataCls the type to serialize for
         */
        private DeserializingNodePrinter(int depth, Class<?> dataCls) {
            this.depth = depth;
            this.dataCls = dataCls;
        }
        
        @Override
        public void print(PrintStream out, String path, String indentation) throws SignalException {
            boolean list = true;
            if (depth > 0) {
                int count = 0;
                int pos = path.indexOf(PATH_SEPARATOR);
                while (pos >= 0) {
                    count++;
                    pos = path.indexOf(PATH_SEPARATOR, pos + 1);
                }
                list = count == depth;
            }
            if (list) {
                Object table = loadSafe(path, dataCls);
                if (null != table) {
                    out.println(indentation + "  " + table);
                }
            }
        }
        
    }

    /**
     * Creates a port manager (frontend).
     * 
     * @param client the curator client connection
     */
    public PortManager(CuratorFramework client) {
        this(client, null);
    }
    
    /**
     * Creates a port manager (frontend).
     * 
     * @param client the curator client connection
     * @param range the default port range (may be <b>null</b> if there is none)
     */
    public PortManager(CuratorFramework client, PortRange range) {
        this.client = client;
        this.range = range;
    }
    
    /**
     * Returns whether the curator framework is connected.
     * 
     * @return <code>true</code> for connected, <code>false</code> else
     */
    public boolean isConnected() {
        return null != client && CuratorFrameworkState.STARTED == client.getState();
    }

    /**
     * Creates a port range using logging for exceptions.
     * 
     * @param range the range to parse
     * @return the port range or <b>null</b> if parsing failed
     */
    public static PortRange createPortRangeQuietly(String range) {
        PortRange result;
        String rng = range;
        if (null == rng) {
            rng = Configuration.DEFAULT_PIPELINE_INTERCONN_PORTS;
        }
        try {
            result = new PortRange(rng);
        } catch (IllegalArgumentException e) {
            getLogger().warn("Parsing port range: " + rng + " " + e.getMessage());
            result = null;
        }
        return result;
    }

    /**
     * Cleans up all existing port assignments.
     * 
     * @throws SignalException in case of communication problems
     */
    public void clearAllPortAssignments() throws SignalException {
        if (isConnected()) {
            SignalMechanism.deleteRecursively(client, PORTS);
        }
    }
    
    /**
     * Clears the given port assignment, e.g., on shutdown of the specific component.
     * 
     * @param pipeline the pipeline name
     * @param element the pipeline element
     * @param assignment the port assignment
     * @throws SignalException in case of communication problems
     */
    public void clearPortAssignment(String pipeline, String element, PortAssignment assignment) throws SignalException {
        if (isConnected()) {
            String path = getNodePath(pipeline, element, assignment.getTaskId());
            PortsTable portsTable = loadSafe(path, PortsTable.class);
            if (null != portsTable) {
                if (portsTable.clearPortAssignment(assignment)) {
                    String hostPath = getHostPath(assignment.getHost());
                    HostTable hostTable = loadSafe(hostPath, HostTable.class);
                    if (null != hostTable) {
                        hostTable.removeAssignment(assignment.getPort());
                    }
                    CuratorTransaction transaction = client.inTransaction();
                    store(path, portsTable, transaction, false);
                    store(hostPath, hostTable, transaction, true);
                }
            }
        }
    }
    
    /**
     * Returns the zookeeper path for a node represented by a request.
     * 
     * @param request the assignment request
     * @return the path
     */
    private String getNodePath(PortAssignmentRequest request) {
        return getNodePath(request.getPipeline(), request.getElement(), request.getTaskId());
    }
    
    /**
     * Returns the zookeeper path for a node.
     * 
     * @param pipeline the pipeline name
     * @param element the pipeline element
     * @param taskId the task id of <code>element</code>
     * @return the path
     */
    private String getNodePath(String pipeline, String element, int taskId) {
        return NODES_PREFIX + pipeline + PATH_SEPARATOR + element; // TODO decide about taskId
    }

    /**
     * Returns the zookeeper path for a watcher node represented by a request.
     * 
     * @param request the assignment request
     * @return the path
     */
    private String getWatcherPath(PortAssignmentRequest request) {
        return getWatcherPath(request.getPipeline(), request.getElement(), request.getTaskId());
    }
    
    /**
     * Returns the zookeeper path for a request watcher node.
     * 
     * @param pipeline the pipeline name
     * @param element the pipeline element
     * @param taskId the task id of <code>element</code>
     * @return the path
     */
    private String getWatcherPath(String pipeline, String element, int taskId) {
        return NODES_PREFIX + pipeline + PATH_SEPARATOR + element + PATH_SEPARATOR + taskId; // TODO decide about taskId
    }

    /**
     * Returns the zookeeper path for a host.
     * 
     * @param host the host name
     * @return the path
     */
    private String getHostPath(String host) {
        return HOSTS_PREFIX + host;
    }

    /**
     * Returns an existing port assignment.
     * 
     * @param pipeline the pipeline name
     * @param element the pipeline element
     * @param taskId the task id of <code>element</code>
     * @param assignmentId the assignmentId to look for (may be <b>null</b>)
     * @return the port assignment or <b>null</b> if there is none
     * @throws SignalException in case that communication fails
     */
    public PortAssignment getPortAssignment(String pipeline, String element, int taskId, String assignmentId) 
        throws SignalException {
        PortAssignment result = null;

        if (isConnected()) {
            String path = getNodePath(pipeline, element, taskId);
            PortsTable table = loadSafe(path, PortsTable.class);
            if (null != table) {
                result = table.getPortAssignment(taskId, assignmentId);
            }
            getLogger().info("Querying port assignment for " + pipeline + " " + element + " " + taskId + " " 
                + assignmentId + ": " + result + " from " + table);
        } else {
            getLogger().info("Querying port assignment for " + pipeline + " " + element + " " + taskId + " " 
                + assignmentId + ": not connected");
        }
        return result;
    }
    
    /**
     * Closes this port manager. No port assignments will be available afterwards.
     */
    public void close() {
        client = null;
    }
    
    /**
     * Lists all currently assigned ports.
     * 
     * @param out the output stream
     * @throws SignalException in case that reading from zookeeper fails
     */
    public void listPorts(PrintStream out) throws SignalException {
        if (isConnected()) {
            list(System.out, NODES, new DeserializingNodePrinter(4, PortsTable.class));
            list(System.out, HOSTS, new DeserializingNodePrinter(HostTable.class));
        }
    }
    
    // checkstyle: stop exception type check

    /**
     * Cleans up the port assignments for a pipeline.
     * 
     * @param pipeline the pipeline name
     * @throws SignalException in case of communication problems
     */
    public void clearPortAssignments(String pipeline) throws SignalException {
        String pipPath = NODES_PREFIX + pipeline;
        try {
            if (isConnected() && client.checkExists().forPath(pipPath) != null) { 
                List<String> children = client.getChildren().forPath(pipPath);
                if (null != children) {
                    for (String child : children) {
                        String childPath = pipPath + PATH_SEPARATOR + child;
                        PortsTable table = loadSafe(childPath, PortsTable.class);
                        if (null != table) {
                            Map<String, List<PortAssignment>> assng = table.assigmentsByHost();
                            for (Map.Entry<String, List<PortAssignment>> ent : assng.entrySet()) {
                                String hostPath = getHostPath(ent.getKey());
                                HostTable hosts = loadSafe(hostPath, HostTable.class);
                                if (null != hosts) {
                                    for (PortAssignment a : ent.getValue()) {
                                        hosts.removeAssignment(a.getPort());
                                    }
                                }
                                store(hostPath, hosts, null, false);
                            }
                        }
                    }
                }
                delete(pipPath, null, true); // just to be sure
            }
        } catch (Exception e) {
            throw new SignalException(e);
        }
    }
    
    /**
     * Represents a port assignment request.
     * 
     * @author Holger Eichelberger
     */
    public static class PortAssignmentRequest implements Serializable {
        
        private static final long serialVersionUID = -1192936660033952587L;
        private String pipeline;
        private String element;
        private int taskId;
        private String host;
        private String assignmentId;
        private boolean check = true;
        
        /**
         * Creates a port assignment request.
         * 
         * @param pipeline the pipeline name
         * @param element the pipeline element name
         * @param taskId the task id
         * @param host the host name
         * @param assignmentId the assignment id identifying this assignment for retrieval (may be <b>null</b>)
         * @throws IllegalArgumentException in case that a parameter is not given correctly
         */
        public PortAssignmentRequest(String pipeline, String element, int taskId, String host, String assignmentId) {
            if (null == pipeline || pipeline.isEmpty()) {
                throw new IllegalArgumentException("pipeline must not be null");
            }
            if (null == element || element.isEmpty()) {
                throw new IllegalArgumentException("element must not be null");
            }
            if (null == host || host.isEmpty()) {
                throw new IllegalArgumentException("host must not be null");
            }
            this.pipeline = pipeline;
            this.element = element;
            this.taskId = taskId;
            this.host = host;
            this.assignmentId = assignmentId;
        }
        
        /**
         * Defines whether the port manager shall check whether the port to return for this
         * request is free.
         * 
         * @param check <code>true</code> for checking (default), <code>false</code> else
         */
        public void setCheck(boolean check) {
            this.check = check;
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
         * Returns the pipeline element name.
         * 
         * @return the pipeline element name
         */
        public String getElement() {
            return element;
        }

        /**
         * Returns the task id.
         * 
         * @return the task id.
         */
        public int getTaskId() {
            return taskId;
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
         * Returns the assignment id.
         * 
         * @return the assignment id (may be <b>null</b>)
         */
        public String getAssignmentId() {
            return assignmentId;
        }
        
        /**
         * Returns whether the port manager shall check the port candidate whether it is free.
         * 
         * @return <code>true</code> for checking, <code>false</code> else
         */
        public boolean doCheck() {
            return check;
        }
        
        @Override
        public String toString() {
            return "port request pip:" + pipeline + " elt: " + element + " taskId: " 
                    + taskId + " host: " + host + " id: " + assignmentId + " check: " + check;
        }
        
    }
    
    /**
     * Stores a combination of request and assignment to inform the watcher.
     * 
     * @author Holger Eichelberger
     */
    public static class WatcherInfo implements Serializable {
        
        private static final long serialVersionUID = 6895673208849791252L;
        private PortAssignmentRequest request;
        private PortAssignment assignment;

        /**
         * Creates a watcher information object.
         */
        public WatcherInfo() {
        }

        /**
         * Sets the request.
         * 
         * @param request the request
         */
        private void setRequest(PortAssignmentRequest request) {
            this.request = request;
        }

        /**
         * Sets the assignment.
         * 
         * @param assignment the assignment
         */
        private void setAssignment(PortAssignment assignment) {
            this.assignment = assignment;
        }
        
        /**
         * Returns the assignment.
         * 
         * @return the assignment (may be <b>null</b> if not set)
         */
        public PortAssignment getAssignment() {
            return assignment;
        }
        
        /**
         * Returns the request.
         * 
         * @return the request (may be <b>null</b> if not set)
         */
        public PortAssignmentRequest getRequest() {
            return request;
        }
        
    }

    /**
     * Registers a port assignment using the default range registered in the constructor. 
     * This method shall only be called by the component which runs the server thread.
     * 
     * @param request the assignment request
     * @return the assigned port (<b>null</b> if none was assigned)
     * @throws SignalException in case that communication fails
     * @throws IllegalArgumentException if no default range was given in the constructor
     */
    public PortAssignment registerPortAssignment(PortAssignmentRequest request) throws SignalException {
        return registerPortAssignment(request, range);
    }
    
    /**
     * Registers a port assignment. This method shall only be called by the component which runs the
     * server thread.
     * 
     * @param request the assignment request
     * @param portRange the range of valid ports
     * @return the assigned port (<b>null</b> if none was assigned)
     * @throws SignalException in case that communication fails
     * @throws IllegalArgumentException if port range or request are <b>null</b>
     */
    public PortAssignment registerPortAssignment(PortAssignmentRequest request, PortRange portRange) 
        throws SignalException {
        if (null == portRange) {
            throw new IllegalArgumentException("no port range given");
        }
        if (null == request) {
            throw new IllegalArgumentException("no request given");
        }
        PortAssignment result = null;
        if (isConnected()) {
            String nodePath = getNodePath(request);
            String watcherPath = getWatcherPath(request);
            String hostPath = getHostPath(request.getHost());
            try {
                assertExists(nodePath, null, false);
                assertExists(hostPath, null, false);
                assertExists(watcherPath, null, false);
                CuratorTransaction transaction = client.inTransaction();
                PortsTable portsTable = loadWithInit(nodePath, PortsTable.class, transaction, false);
                HostTable hostTable = loadWithInit(hostPath, HostTable.class, transaction, false);
                WatcherInfo watcherInfo = loadWithInit(watcherPath, WatcherInfo.class, transaction, false);
                if (null == watcherInfo.getRequest()) {
                    watcherInfo.setRequest(request);
                }
                int candidate = portRange.getLowPort();
                boolean check = request.doCheck();
                while (null == result && candidate <= portRange.getHighPort()) {
                    if (null == hostTable.getAssignment(candidate)) {
                        if (!check || (check && isPortFree(request.getHost(), candidate))) {
                            result = new PortAssignment(request.getHost(), candidate, request.getTaskId(), 
                                request.getAssignmentId());
                            portsTable.registerPortAssignment(result);
                            hostTable.addAssignment(candidate, request.getPipeline());
                        }
                    }
                    if (null == result) {
                        candidate++;
                    }
                }
                store(nodePath, portsTable, transaction, false);
                store(hostPath, hostTable, transaction, false);
                watcherInfo.setAssignment(result);
                store(watcherPath, watcherInfo, transaction, true);
            } catch (Exception e) {
                throw new SignalException(e);
            }
        }
        getLogger().info("Registered port assignment " + result + " for " + request);
        return result;
    }
    
    /**
     * An internal curator watcher instance for informing an external watcher.
     * 
     * @author Holger Eichelberger
     */
    private class PortAssignmentWatcher implements Watcher {

        private IPortAssignmentWatcher watcher;

        /**
         * Creates the instance.
         * 
         * @param watcher the external watcher
         */
        private PortAssignmentWatcher(IPortAssignmentWatcher watcher) {
            this.watcher = watcher;
        }
        
        @Override
        public void process(WatchedEvent event) {
            if (EventType.NodeDataChanged == event.getType()) {
                try {
                    WatcherInfo info = load(event.getPath(), WatcherInfo.class);
                    if (null != info) {
                        watcher.notifyPortAssigned(info.getRequest(), info.getAssignment());
                    }
                } catch (SignalException e) {
                    getLogger().error("Processing watcher: " + e.getMessage(), e);
                }
            }
        }
        
    }
    
    /**
     * Tries notifying a watcher.
     * 
     * @param path the path of the zkNode to watch
     * @param watcher the watcher
     * @return <code>true</code> if the notification has been done, <code>false</code> else
     * @throws SignalException in case that loading data failed
     */
    private boolean tryNotifyWatcher(String path, IPortAssignmentWatcher watcher) throws SignalException {
        boolean done = false;
        WatcherInfo info = load(path, WatcherInfo.class);
        if (null != info && null != info.getAssignment()) {
            watcher.notifyPortAssigned(info.getRequest(), info.getAssignment());
            done = true;
        }
        return done;
    }
    
    /**
     * Sets a one-time watcher for a certain port assignment, i.e., the watcher is informed if the port 
     * assignment request is fulfilled. This may happen immediately if the port already has been assigned or may
     * take a while until the port is requested.
     * 
     * @param pipeline the pipeline name
     * @param element the pipeline element
     * @param taskId the task id of <code>element</code>
     * @param assignmentId the assignmentId to look for (may be <b>null</b>)
     * @param watcher the watcher
     * @throws SignalException in case that creating the watcher or requesting data fails
     */
    public void setPortAssigmentWatcher(String pipeline, String element, int taskId, String assignmentId, 
        IPortAssignmentWatcher watcher) throws SignalException  {
        // assignmentId is ignored for now
        if (null != watcher) {
            String watcherPath = getWatcherPath(pipeline, element, taskId);
            assertExists(watcherPath, null, false);
            if (!tryNotifyWatcher(watcherPath, watcher)) {
                try {
                    client.checkExists().usingWatcher(new PortAssignmentWatcher(watcher)).forPath(watcherPath);
                } catch (Exception e) {
                    throw new SignalException(e);
                }
            }
        }
    }
    
    // from here we assume that we are connected!!!
    
    /**
     * Returns whether the specified port is free.
     * 
     * @param host the host
     * @param port the port
     * @return <code>true</code> for free, <code>false</code> else
     */
    private boolean isPortFree(String host, int port) {
        // https://git.eclipse.org/c/jdt/eclipse.jdt.debug.git/tree/org.eclipse.jdt.launching/launching/
        //         org/eclipse/jdt/launching/SocketUtil.java
        boolean free = false;
        Socket s = null;
        try {
            s = new Socket(host, port);
        } catch (ConnectException e) {
            free = true;
        } catch (IOException e) {
        }
        if (null != s) {
            try {
                s.close();
            } catch (IOException e) {
            }
        }
        return free;
    }
    
    /**
     * Loads an instance of <code>class</code> from <code>path</code> if possible. If <code>path</code> does not exist,
     * initialize it with a default instance of <code>class</code> using the accessible no-argument constructor. 
     * 
     * @param <T> the type of object to load
     * @param path the path to save to
     * @param cls the type of the object to load
     * @param transaction use the transaction and add to it, may be <b>null</b> for no transaction
     * @param commit commit the transaction, ignored if no transaction 
     * @return the instance or <b>null</b>
     * @throws SignalException in case of I/O problems or if the object in <code>path</code>
     * @see #load(String, Class)
     */
    private <T> T loadWithInit(String path, Class<T> cls, CuratorTransaction transaction, boolean commit) 
        throws SignalException {
        try {
            T result = load(path, cls);
            if (null == result) {
                result = cls.newInstance();
            }
            return result;
        } catch (Exception e) {
            throw new SignalException(e);
        }
    }
    
    /**
     * Asserts the existence of <code>path</code>.
     * 
     * @param path the path to check/create
     * @param transaction use the transaction and add to it, may be <b>null</b> for no transaction
     * @param commit commit the transaction, ignored if no transaction 
     * @throws SignalException in case of I/O problems
     */
    private void assertExists(String path, CuratorTransaction transaction, boolean commit) throws SignalException {
        try {
            if (client.checkExists().forPath(path) == null) {
                int pos = path.indexOf(PATH_SEPARATOR);
                if (0 == pos) {
                    pos = path.indexOf(PATH_SEPARATOR, 1);
                }
                CuratorTransactionFinal fin = null;
                while (pos > 0) {
                    int next = path.indexOf(PATH_SEPARATOR, pos + 1);
                    String sub = path.substring(0, pos);
                    if (null == client.checkExists().forPath(sub)) {
                        fin = createPath(transaction, fin, sub);
                    }
                    pos = next;
                }
                fin = createPath(transaction, fin, path);
                if (null != transaction) {
                    checkCommit(fin, commit);
                }
            }
        } catch (Exception e) {
            throw new SignalException(e);
        }
    }

    /**
     * Creates a path.
     * 
     * @param transaction use the transaction and add to it, may be <b>null</b> for no transaction
     * @param fin append to this transaction if not <b>null</b> (start of transaction)
     * @param path the path to create
     * @return the created transaction (may be <b>null</b> if <code>transaction</code> is <b>null</b>)
     * @throws SignalException in case of I/O problems
     */
    private CuratorTransactionFinal createPath(CuratorTransaction transaction, CuratorTransactionFinal fin, String path)
        throws SignalException {
        CuratorTransactionFinal result = null;
        try {
            if (null != transaction) {
                if (null == fin) {
                    result = transaction.create().forPath(path, null).and();    
                } else {
                    result = fin.create().forPath(path, null).and();
                }
            } else {
                client.create().forPath(path, null);
            }
        } catch (Exception e) {
            throw new SignalException(e);
        }
        return result;
    }

    /**
     * Loads an instance of <code>class</code> from <code>path</code> if possible and <code>path</code> exists. 
     * 
     * @param <T> the type of object to load
     * @param path the path to save to
     * @param cls the type of the object to load
     * @return the instance or <b>null</b>
     * @throws SignalException in case of I/O problems or if the object in <code>path</code>
     * @see #load(String, Class)
     */
    private <T> T loadSafe(String path, Class<T> cls) throws SignalException {
        T result;
        try {
            if (client.checkExists().forPath(path) != null) {
                result = load(path, cls);
            } else {
                result = null;
            }
        } catch (Exception e) {
            throw new SignalException(e);
        }
        return result;
    }

    /**
     * Loads an instance of <code>class</code> from <code>path</code> if possible. Does not check whether
     * <code>path</code> exists.
     * 
     * @param <T> the type of object to load
     * @param path the path to save to
     * @param cls the type of the object to load
     * @return the instance or <b>null</b>
     * @throws SignalException in case of I/O problems or if the object in <code>path</code> is not of type 
     *   <code>cls</code>
     */
    private <T> T load(String path, Class<T> cls) throws SignalException {
        try {
            T result;
            byte[] data = client.getData().forPath(path);
            if (null != data) {
                Object obj = Utils.deserialize(data);
                if (null != obj && !(cls.isInstance(obj))) {
                    throw new SignalException("path " + path + " contains " + obj + " as data rather than a " 
                        + cls.getName());
                }
                result = cls.cast(obj);
            } else {
                result = null;
            }
            return result;
        } catch (Exception e) {
            throw new SignalException(e);
        }
    }
    
    /**
     * Stores <code>obj</code> into <code>path</code>.
     * 
     * @param path the path to save to
     * @param obj the object to save
     * @throws SignalException in case of I/O problems
     */
    @SuppressWarnings("unused")
    private void store(String path, Serializable obj) throws SignalException {
        store(path, obj, null, false);
    }

    /**
     * Stores <code>obj</code> into <code>path</code>.
     * 
     * @param path the path to save to
     * @param obj the object to save
     * @param transaction use the transaction and add to it, may be <b>null</b> for no transaction
     * @param commit commit the transaction, ignored if no transaction 
     * @throws SignalException in case of I/O problems
     */
    private void store(String path, Serializable obj, CuratorTransaction transaction, boolean commit) 
        throws SignalException {
        try {
            byte[] data = Utils.serialize(obj);
            assertExists(path, transaction, commit);
            if (null != transaction) {
                checkCommit(transaction.setData().forPath(path, data).and(), commit);
            } else {
                client.setData().forPath(path, data);
            }
        } catch (Exception e) {
            throw new SignalException(e);
        }
    }

    /**
     * Deletes a path.
     * 
     * @param path the path to be deleted
     * @param transaction use the transaction and add to it, may be <b>null</b> for no transaction
     * @param commit commit the transaction, ignored if no transaction 
     * @throws SignalException in case of communication problems
     */
    private void delete(String path, CuratorTransaction transaction, boolean commit) throws SignalException {
        try {
            if (client.checkExists().forPath(path) != null) {
                if (null != transaction) {
                    deleteRecursively(path, transaction, commit);
                } else {
                    SignalMechanism.deleteRecursively(client, path);
                }
            }
        } catch (Exception e) {
            throw new SignalException(e);
        }
    }

    /**
     * Deletes a path recursively within <code>transaction</code>.
     * 
     * @param path the path to be deleted
     * @param transaction the curator transaction
     * @param commit commit the transaction or leave it open
     * @throws SignalException if problems occur during deletion
     */
    private void deleteRecursively(String path, CuratorTransaction transaction, boolean commit) 
        throws SignalException {
        try {
            if (client.checkExists().forPath(path) != null) {
                List<String> children = client.getChildren().forPath(path);
                if (null != children && children.size() > 0) {
                    for (String c : children) {
                        deleteRecursively(path + PATH_SEPARATOR + c, transaction, false);
                    }
                }
                checkCommit(transaction.delete().forPath(path).and(), commit);
            }
        } catch (Exception e) {
            throw new SignalException(e);
        }
    }
    
    /**
     * Checks whether a commit shall be done.
     * 
     * @param transaction the transaction to be committed
     * @param commit commit the transaction or not
     * @throws SignalException in case of communication problems
     */
    private void checkCommit(CuratorTransactionFinal transaction, boolean commit) throws SignalException {
        if (commit) {
            try {
                transaction.commit();
            } catch (Exception e) {
                throw new SignalException(e);
            }
        }
    }
     
    /**
     * Lists zNodes in recursive fashion.
     * 
     * @param out the output stream
     * @param path the path to visit/list
     * @param indentation the current indentation
     * @param printer the printer
     * @throws SignalException in case that reading from zookeeper fails
     */
    private void listRec(PrintStream out, String path, String indentation, INodePrinter printer) 
        throws SignalException {
        try {
            List<String> children = client.getChildren().forPath(path);
            for (String c : children) {
                String cPath = path + PATH_SEPARATOR + c;
                out.println(indentation + "- " + c);
                if (null != printer) {
                    printer.print(out, cPath, indentation);
                }
                listRec(out, cPath, indentation + " ", printer);
            }
        } catch (Exception e) {
            throw new SignalException(e);
        }
    }

    /**
     * Lists zNodes in recursive fashion.
     * 
     * @param out the output stream
     * @param basePath the path to visit/list
     * @param printer the printer
     * @throws SignalException in case that reading from zookeeper fails
     */
    private void list(PrintStream out, String basePath, INodePrinter printer) throws SignalException {
        out.println(basePath);
        try {
            if (client.checkExists().forPath(basePath) != null) {
                listRec(out, basePath, " ", printer);
            }
        } catch (Exception e) {
            throw new SignalException(e);
        }
    }

    // checkstyle: resume exception type check
    
    /**
     * Returns the logger.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return LogManager.getLogger(PortManager.class);
    }

}
