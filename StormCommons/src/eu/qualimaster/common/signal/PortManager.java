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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.storm.curator.framework.CuratorFramework;
import org.apache.storm.curator.framework.api.transaction.CuratorTransaction;
import org.apache.storm.curator.framework.api.transaction.CuratorTransactionFinal;

import backtype.storm.utils.Utils;

/**
 * Dynamically manages the ports to be used for loose pipeline connections and switches. Assignment ids are intended
 * to separate multiple assignments for one task using a (logical) identifier)
 * 
 * @author Holger Eichelberger
 * @author Cui Qin
 */
public class PortManager {

    private static final String PATH_SEPARATOR = "/";
    private static final String PORTS = "qm" + PATH_SEPARATOR + "ports";
    private static final String PORTS_PREFIX = PORTS + PATH_SEPARATOR;
    private static final String NODES = PORTS_PREFIX + "nodes";
    private static final String NODES_PREFIX = NODES + PATH_SEPARATOR;
    private static final String HOSTS = PORTS_PREFIX + "hosts";
    private static final String HOSTS_PREFIX = HOSTS + PATH_SEPARATOR;

    private CuratorFramework client;
    
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
         * Creates a port range.
         * 
         * @param lowPort the low port
         * @param highPort the high port
         */
        public PortRange(int lowPort, int highPort) {
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
         * @param taskId the task id
         * @param assignment the port assignment to register
         */
        private void registerPortAssignment(int taskId, PortAssignment assignment) {
            Map<Integer, PortAssignment> portTable = assignments.get(taskId);
            if (null == portTable) {
                portTable = new HashMap<Integer, PortAssignment>();
                assignments.put(taskId, portTable);
            }
            portTable.put(assignment.getPort(), assignment);
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
                    done = true;
                }
            }
            return done;
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
     * Cleans up all existing port assignments.
     * 
     * @throws SignalException in case of communication problems
     */
    public void clearAllPortAssignments() throws SignalException {
        delete(PORTS);
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
        String path = getNodePath(pipeline, element, assignment.getTaskId());
        PortsTable table = loadSafe(path, PortsTable.class);
        if (null != table) {
            if (table.clearPortAssignment(assignment)) {
                store(path, table, null, false);
            }
        }
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
        return NODES_PREFIX + pipeline + PATH_SEPARATOR + element + PATH_SEPARATOR; // TODO decide about taskId
    }

    /**
     * Returns the zookeeper path for a host.
     * 
     * @param host the host name
     * @return the path
     */
    private String getHostPath(String host) {
        return HOSTS_PREFIX + host + PATH_SEPARATOR;
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
        PortAssignment result = new PortAssignment("localhost", 8999, 0, assignmentId); // TODO remove fallback
        String path = getNodePath(pipeline, element, taskId);
        PortsTable table = loadSafe(path, PortsTable.class);
        if (null != table) {
            result = table.getPortAssignment(taskId, assignmentId);
        }
        return result;
    }
    
    /**
     * Closes this port manager. No port assignments will be available afterwards.
     */
    public void close() {
        client = null;
    }
    
    // checkstyle: stop exception type check

    /**
     * Cleans up the port assignments for a pipeline.
     * 
     * @param pipeline the pipeline name
     * @throws SignalException in case of communication problems
     */
    public void clearPortAssignments(String pipeline) throws SignalException {
        String pipPath = PORTS + pipeline;
        try {
            if (client.checkExists().forPath(pipPath) != null) { 
                CuratorTransaction transaction = client.inTransaction();
                List<String> children = client.getChildren().forPath(pipPath);
                if (null != children) {
                    for (String child : children) {
                        PortsTable table = loadSafe(child, PortsTable.class);
                        delete(child);
                        Map<String, List<PortAssignment>> assng = table.assigmentsByHost();
                        for (Map.Entry<String, List<PortAssignment>> ent : assng.entrySet()) {
                            String hostPath = getHostPath(ent.getKey());
                            HostTable hosts = loadSafe(hostPath, HostTable.class);
                            if (null != hosts) {
                                for (PortAssignment a : ent.getValue()) {
                                    hosts.removeAssignment(a.getPort());
                                }
                            }
                            store(hostPath, hosts, transaction, false);
                        }
                    }
                    delete(pipPath, transaction, true);
                }
            }
        } catch (Exception e) {
            throw new SignalException(e.getMessage());
        }
    }
    
    /**
     * Represents a port assignment request.
     * 
     * @author Holger Eichelberger
     */
    public static class PortAssignmentRequest {
        
        private String pipeline;
        private String element;
        private int taskId;
        private String host;
        private String assignmentId;
        
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
        
    }
    
    /**
     * Registers a port assignment. This method shall only be called by the component which runs the
     * server thread.
     * 
     * @param request the assignment request
     * @param portRange the range of valid ports
     * @return the assigned port (<b>null</b> if none was assigned)
     * @throws SignalException in case that communication fails
     */
    public PortAssignment registerPortAssignment(PortAssignmentRequest request, PortRange portRange) 
        throws SignalException {
        PortAssignment result = null;
        String nodePath = getNodePath(request.getPipeline(), request.getElement(), request.getTaskId());
        String hostPath = getHostPath(request.getHost());
        try {
            CuratorTransaction transaction = client.inTransaction();
            PortsTable portsTable = loadWithInit(nodePath, PortsTable.class, transaction, false);
            HostTable hostTable = loadWithInit(hostPath, HostTable.class, transaction, false);
            int candidate = portRange.getLowPort();
            while (null == result && candidate <= portRange.getHighPort()) {
                if (null == hostTable.getAssignment(candidate)) {
                    result = new PortAssignment(request.getHost(), candidate, request.getTaskId(), 
                        request.getAssignmentId());
                    portsTable.registerPortAssignment(request.getTaskId(), result);
                    hostTable.addAssignment(candidate, request.getPipeline());
                } else {
                    candidate++;
                }
            }
            store(nodePath, portsTable, transaction, false);
            store(hostPath, hostTable, transaction, true);
        } catch (Exception e) {
            throw new SignalException(e.getMessage());
        }
        return result;
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
            T result;
            if (client.checkExists().forPath(path) == null) {
                if (null != transaction) {
                    checkCommit(transaction.create().forPath(path).and(), commit); // unsure whether transitive
                } else {
                    client.create().creatingParentsIfNeeded().forPath(path);
                }
                result = cls.newInstance();
            } else {
                result = load(path, cls);
            }
            return result;
        } catch (Exception e) {
            throw new SignalException(e.getMessage());
        }
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
            throw new SignalException(e.getMessage());
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
            byte[] data = client.getData().forPath(path);
            Object obj = Utils.deserialize(data);
            if (null != obj && !(cls.isInstance(obj))) {
                throw new SignalException("path " + path + " contains " + obj + " as data rather than a " 
                    + cls.getName());
            }
            return cls.cast(obj);
        } catch (Exception e) {
            throw new SignalException(e.getMessage());
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
    private void store(String path, Object obj) throws SignalException {
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
    private void store(String path, Object obj, CuratorTransaction transaction, boolean commit) throws SignalException {
        try {
            byte[] data = Utils.serialize(obj);
            if (null != transaction) {
                checkCommit(transaction.setData().forPath(path, data).and(), commit);
            } else {
                client.setData().forPath(path, data);
            }
        } catch (Exception e) {
            throw new SignalException(e.getMessage());
        }
    }

    /**
     * Deletes a path.
     * 
     * @param path the path to be deleted
     * @throws SignalException in case of communication problems
     */
    private void delete(String path) throws SignalException {
        delete(path, null, false);
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
                    checkCommit(transaction.delete().forPath(path).and(), commit);
                } else {
                    client.delete().forPath(path);
                }
            }
        } catch (Exception e) {
            throw new SignalException(e.getMessage());
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
                throw new SignalException(e.getMessage());
            }
        }
    }

    // checkstyle: resume exception type check
    
}
