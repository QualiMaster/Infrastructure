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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import backtype.storm.daemon.common.Assignment;
import clojure.lang.APersistentMap;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.MapEntry;
import clojure.lang.PersistentArrayMap;
import clojure.lang.PersistentVector;

/**
 * Stores a complete task assignment as a temporary data structure to modify the parallelism. Provides
 * functionality to read from / create a Storm assignment.
 * 
 * @author Holger Eichelberger
 */
public class TaskAssignment {

    /**
     * A comparator for sorting task assignments according to their tasks.
     */
    private static final Comparator<TaskAssignment> TASK_COMPARATOR = new Comparator<TaskAssignment>() {

        @Override
        public int compare(TaskAssignment o1, TaskAssignment o2) {
            return Integer.compare(o1.getTaskStart(), o2.getTaskStart());
        }
        
    };

    private int taskStart = -1;
    private int taskEnd = -1;
    private String hostId;
    private int port = -1;
    private String component;
    private int startTime;
    private boolean disabled;
    
    /**
     * Creates a new executor id.
     * 
     * @param object the object representing the executor
     * @param taskComponents a taskId-component name mapping
     */
    TaskAssignment(Object object, Map<Integer, String> taskComponents) {
        if (object instanceof MapEntry) {
            MapEntry me = (MapEntry) object;
            Object key = me.getKey();
            if (key instanceof PersistentVector) {
                PersistentVector executor = (PersistentVector) key;
                if (2 == executor.size()) {
                    Object start = executor.get(0);
                    Object end = executor.get(1);
                    if (start instanceof Integer && end instanceof Integer) {
                        Integer tmpStart = (Integer) start;
                        this.taskStart = tmpStart;
                        this.taskEnd = (Integer) end;
                        this.component = taskComponents.get(tmpStart);
                    }
                }
            }
            Object val = me.getValue();
            if (val instanceof PersistentVector) {
                PersistentVector hostPort = (PersistentVector) val;
                if (2 == hostPort.size()) {
                    Object host = hostPort.get(0);
                    Object port = hostPort.get(1);
                    if (host instanceof String && port instanceof Integer) {
                        this.hostId = (String) host;
                        this.port = (Integer) port;
                    }
                }
            }
        }
    }
    
    /**
     * Copies a task assignment.
     * 
     * @param assng the task assignment to be copied
     */
    TaskAssignment(TaskAssignment assng) {
        this(assng, assng.startTime);
    }
    
    /**
     * Copies a task assignment with explicit start time.
     * 
     * @param assng the task assignment to be copied (without start time)
     * @param now the start time for the copied assignment
     */
    TaskAssignment(TaskAssignment assng, int now) {
        this(assng, assng.taskStart, assng.taskEnd, now);
    }

    /**
     * Copies a task assignment with explicit start task id, end task id and start time.
     * 
     * @param assng the task assignment to be copied (without start time)
     * @param taskStart the explicit start task id
     * @param taskEnd the explicit end task id
     * @param startTime the start time for the copied assignment
     */
    TaskAssignment(TaskAssignment assng, int taskStart, int taskEnd, int startTime) {
        this(taskStart, taskEnd, assng.hostId, assng.port, assng.component);
        this.startTime = startTime;
    }

    /**
     * Copies a task assignment with explicit start task id, end task id, start time and host information.
     * 
     * @param assng the task assignment to be copied (without start time)
     * @param taskStart the explicit start task id
     * @param taskEnd the explicit end task id
     * @param startTime the start time for the copied assignment
     * @param host the host data
     */
    TaskAssignment(TaskAssignment assng, int taskStart, int taskEnd, int startTime, HostPort host) {
        this(taskStart, taskEnd, 
            null == host ? assng.hostId : host.getHostId(), 
            null == host ? assng.port : host.getPort(), 
            assng.component);
        this.startTime = startTime;
    }

    /**
     * Creates a task assignment with explicit settings. Please use {@link #setStartTime(int)} for 
     * setting the time (0 by default).
     * 
     * @param taskStart the start task id
     * @param taskEnd the end task id
     * @param hostId the host id
     * @param port the port number on <code>hostId</code>
     * @param component the Storm component name
     */
    public TaskAssignment(int taskStart, int taskEnd, String hostId, int port, String component) {
        this.taskStart = taskStart;
        this.taskEnd = taskEnd;
        this.hostId = hostId;
        this.port = port;
        this.component = component;
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
     * Disables this assignment, i.e., in cases of changes the original assignment shall not be written back as active 
     * assignment.
     */
    public void disable() {
        this.disabled = true;
    }

    /**
     * Returns whether this assignment has been {@link #disable() disabled}.
     * 
     * @return <code>true</code> if disabled, <code>false</code> else
     */
    public boolean isDisabled() {
        return disabled;
    }
    
    /**
     * Returns whether this assignment is active and has not been {@link #disable() disabled}.
     * 
     * @return <code>true</code> if active, <code>false</code> else (negation of {@link #isDisabled()} for convenience)
     */
    public boolean isActive() {
        return !disabled;
    }
    
    /**
     * Returns the string representation of the stored executor id.
     * 
     * @return the string representation
     */
    public String getExecutorIdString() {
        return "[" + taskStart + " " + taskEnd + "]";
    }
    
    /**
     * Returns the string representation of the host/port assignment.
     * 
     * @return the string representation
     */
    public String getHostPortString() {
        return "[\"" + hostId + "\" " + port + "]";
    }
    
    /**
     * Turns this executor id into a persistent vector for storing it in a Storm assignment.
     * 
     * @return the persistent vector
     */
    private PersistentVector getExecutorId() {
        Object[] lst = new Object[2];
        lst[0] = taskStart;
        lst[1] = taskEnd;
        return PersistentVector.create(lst);
    }

    /**
     * Turns the host-port mapping into a persistent vector for storing it in a Storm assignment.
     * 
     * @return the persistent vector
     */
    private PersistentVector getHostPort() {
        Object[] lst = new Object[2];
        lst[0] = hostId;
        lst[1] = port;
        return PersistentVector.create(lst);
    }
    
    /**
     * Returns host and port as workerbeat id.
     * 
     * @return workerbeat ide
     */
    public String getWorkerbeatId() {
        return hostId + HostPort.WORKERBEAT_SEPARATOR + port;
    }
    
    /**
     * Returns whether the data is valid.
     * 
     * @return <code>true</code> if valid, <code>false</code> end
     */
    public boolean isValid() {
        boolean valid = taskStart >= 0 && taskEnd >= 0 && taskStart <= taskEnd;
        valid &= hostId != null && port > 0;
        valid &= component != null;
        return valid;
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
     * Returns the TCP port on {@link #getHostId()}.
     * 
     * @return the TCP port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns whether this assignment has the same host.
     * 
     * @param hostPort the host and port
     * @return <code>true</code> if hosts are the same, <code>false</code> else
     */
    public boolean isSameHost(HostPort hostPort) {
        return hostPort.getHostId().equals(getHostId());
    }
    
    /**
     * Returns whether this assignment has the same host and port.
     * 
     * @param hostPort the host and port
     * @return <code>true</code> if host and port are the same, <code>false</code> else
     */
    public boolean isSame(HostPort hostPort) {
        return isSameHost(hostPort) && hostPort.getPort() == getPort();
    }
    
    /**
     * Changes the connection settings.
     * 
     * @param hostPort defines the new host and port
     */
    public void setHostPort(HostPort hostPort) {
        this.hostId = hostPort.getHostId();
        this.port = hostPort.getPort();
    }
    
    /**
     * Returns the name of the topology component being executed.
     * 
     * @return the name of the topology component
     */
    public String getComponent() {
        return component;
    }
    
    /**
     * Defines the start time of this assignment.
     * 
     * @param startTime the start time
     */
    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }
    
    /**
     * Returns the start time of this assignment.
     * 
     * @return the start time
     */
    public int getStartTime() {
        return startTime;
    }
    
    /**
     * Returns the taskRange.
     * 
     * @return the task range
     */
    public int getNumberOfTasks() {
        return isValid() ? taskEnd - taskStart + 1 : 0;
    }
    
    /**
     * Returns the start task id.
     * 
     * @return the start task id
     */
    public int getTaskStart() {
        return taskStart;
    }

    /**
     * Returns the end task id.
     * 
     * @return the end task id
     */
    public int getTaskEnd() {
        return taskEnd;
    }
    
    /**
     * Returns the number of tasks covered by this assignment.
     * 
     * @return the number of tasks
     */
    public int getTaskCount() {
        return taskEnd - taskStart + 1;
    }
    
    /**
     * Tries to split this task assignment as long as the number of tasks is greater 1 and <code>change</code> is 
     * greater than zero.
     * 
     * @param change the number of splits (parallelizations) requested (positive)
     * @param newExecutorIds the list of new executor ids to be assigned (to be modified as a side effect)
     * @param host optional host assignment information (may be <b>null</b>)
     * @param now the current timestamp for new executors
     * @return the remaining changes (non-negative)
     */
    int split(int change, List<TaskAssignment> newExecutorIds, HostPort host, int now) {
        int start = taskStart;
        while (start < taskEnd && change > 0) {
            newExecutorIds.add(new TaskAssignment(this, start, start, now, host));
            start++;
            change--;
        }
        // handle leftover if there are any
        if (start <= taskEnd) {
            newExecutorIds.add(new TaskAssignment(this, start, taskEnd, now, host));
        }
        // if something happened, disable this
        if (start != taskStart) {
            disable();
        }
        return change;
    }

    /**
     * Tries to merge <code>toMerge</code> with this task assignment. This only possible, if the merged task range
     * is continuous without gaps in task ids.
     * 
     * @param toMerge the task assignment to be merged. If successful, <code>toMerge</code> will be disabled.
     * @param change the number of changes (sequentializations) to be done (negative)
     * @param host optional host assignment information (may be <b>null</b>)
     * @return <code>changes</code> if not successful, <code>changes + {@link #getNumberOfTasks()}</code> else
     */
    int merge(TaskAssignment toMerge, int change, HostPort host) {
        if (change < 0) {
            // is the final taskId range continuous?
            boolean done = false;
            if (toMerge.taskEnd + 1 == taskStart) { // [toMerge][this]
                this.taskStart = toMerge.taskStart;
                done = true;
            } else if (toMerge.taskStart - 1 == taskEnd) { // [this][toMerge]
                this.taskEnd = toMerge.taskEnd;
                done = true;
            }
            if (done) {
                if (null != host) {
                    this.hostId = host.getHostId();
                    this.port = host.getPort();
                }
                toMerge.disable();
                change++;
            }
        }
        return change;
    }
    
    /**
     * Returns a textual representation (for debugging).
     * 
     * @return the textual representation
     */
    public String toString() {
        return "task assignment for " + component + " " + getExecutorIdString() + " @ " + getHostPortString(); 
    }
    
    /**
     * Reads task assignments from the Storm assignment structure and returns a component-taskAssignment mapping for
     * further processing.
     * 
     * @param assng the Storm assignment structure
     * @param taskComponents the task-component mapping
     * @return a component-taskAssignment mapping, the task assignments are sorted according to the number of tasks in 
     *     ascending fashion 
     */
    public static Map<String, List<TaskAssignment>> readTaskAssignments(Assignment assng, 
        Map<Integer, String> taskComponents) {
        Map<String, List<TaskAssignment>> result = null;
        if (assng.executor__GT_node_PLUS_port instanceof APersistentMap 
            && assng.executor__GT_start_time_secs instanceof APersistentMap) {
            result = new HashMap<String, List<TaskAssignment>>();

            // turn structure into compound-listOfAssignment structure, idTmp for collecting start times 
            Map<String, TaskAssignment> idTmp = new HashMap<String, TaskAssignment>();
            APersistentMap executorNodePort = (APersistentMap) assng.executor__GT_node_PLUS_port;
            for (Object entry : executorNodePort.entrySet()) {
                TaskAssignment taskAssng = new TaskAssignment(entry, taskComponents);
                if (taskAssng.isValid()) {
                    idTmp.put(taskAssng.getExecutorIdString(), taskAssng);
                    String key = taskAssng.getComponent();
                    List<TaskAssignment> compAssignments = result.get(key);
                    if (null == compAssignments) {
                        compAssignments = new LinkedList<TaskAssignment>();
                        result.put(key, compAssignments);
                    }
                    compAssignments.add(taskAssng);
                }
            }
            // sort
            for (List<TaskAssignment> compAssignments : result.values()) {
                Collections.sort(compAssignments, TASK_COMPARATOR);
            }
            // add start times
            APersistentMap executorStartTimeSecs = (APersistentMap) assng.executor__GT_start_time_secs;
            for (Object entry : executorStartTimeSecs.entrySet()) {
                if (entry instanceof MapEntry) {
                    MapEntry ent = (MapEntry) entry;
                    if (ent.getKey() instanceof PersistentVector && ent.getValue() instanceof Integer) {
                        TaskAssignment taskAssng = idTmp.get(ent.getKey().toString());
                        if (null != taskAssng) {
                            taskAssng.setStartTime((Integer) ent.getValue());
                        }
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Reads the node-host mapping from <code>assng</code>.
     * 
     * @param assng the assignment
     * @return the node-host mapping
     */
    public static Map<String, String> readNodeHost(Assignment assng) {
        Map<String, String> result = new HashMap<String, String>();
        if (assng.node__GT_host instanceof APersistentMap) {
            APersistentMap nodeHost = (APersistentMap) assng.node__GT_host;
            for (Object entry : nodeHost.entrySet()) {
                if (entry instanceof MapEntry) {
                    MapEntry ent = (MapEntry) entry;
                    result.put(ent.getKey().toString(), ent.getValue().toString());
                }
            }
        }
        return result;
    }
    
    /**
     * Creates the node-host mapping.
     * 
     * @param hostIdName the hostId-hostname mapping
     * @param assignments the actual assignments
     * @return the node-host mapping
     */
    public static Map<String, String> createNodeHost(Map<String, String> hostIdName,
        Map<String, List<TaskAssignment>> assignments) {
        Map<String, String> result = new HashMap<String, String>();
        for (List<TaskAssignment> assngs : assignments.values()) {
            for (TaskAssignment assng : assngs) {
                String hostId = assng.getHostId();
                if (!result.containsKey(hostId)) {
                    String hostName = hostIdName.get(hostId);
                    if (null != hostName) {
                        result.put(hostId, hostName);
                    } else {
                        result = null;
                        break;
                    }
                }
            }
            if (null == result) {
                break;
            }
        }
        return result;
    }

    /**
     * Creates a new Storm assignment from the original assignment and the executor mapping.
     * 
     * @param nodeHost the node-host mapping to be written (use the one from <code>original</code> if <b>null</b>)
     * @param original the original storm assignment
     * @param componentAssignments the new assignments
     * @param workerSequence the sequence of workers to be affected (use the value from <code>original</code> 
     *   if <b>null</b>)
     * @return the created Storm assignment (may be <b>null</b> if the creation fails)
     */
    public static Assignment createTaskAssignments(Assignment original, Map<String, String> nodeHost, Map<String, 
        List<TaskAssignment>> componentAssignments, List<String> workerSequence) {
        Map<PersistentVector, PersistentVector> executorNodePort = 
            new HashMap<PersistentVector, PersistentVector>();
        Map<PersistentVector, Integer> executorStartTimeSecs = 
            new HashMap<PersistentVector, Integer>();
        
        for (List<TaskAssignment> assignments : componentAssignments.values()) {
            for (TaskAssignment assignment : assignments) {
                PersistentVector executorId = assignment.getExecutorId();
                if (assignment.isActive()) {
                    executorNodePort.put(executorId, assignment.getHostPort());
                }
                if (assignment.getStartTime() >= 0) {
                    executorStartTimeSecs.put(executorId, assignment.getStartTime());
                }
            }
        }
        
        IPersistentMap nodeHostMap;
        if (null == nodeHost) {
            nodeHostMap = (IPersistentMap) original.node__GT_host;
        } else {
            nodeHostMap = PersistentArrayMap.create(nodeHost);
        }
        
        IPersistentCollection seq;
        if (null == workerSequence) {
            seq = ZkUtils.getWorkerDependencies(original);
        } else {
            seq = PersistentVector.create(workerSequence);
        }
        return ZkUtils.createAssignment(original.master_code_dir, nodeHostMap, 
            PersistentArrayMap.create(executorNodePort), 
            PersistentArrayMap.create(executorStartTimeSecs), seq);
                        
//                        new Assignment(original.master_code_dir, nodeHostMap, 
//            PersistentArrayMap.create(executorNodePort), 
//            PersistentArrayMap.create(executorStartTimeSecs), seq);
    }

}
