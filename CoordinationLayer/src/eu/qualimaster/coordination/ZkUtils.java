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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.storm.curator.framework.CuratorFramework;
import org.apache.storm.curator.framework.CuratorFrameworkFactory;
import org.apache.storm.curator.retry.RetryOneTime;

import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentMap;
import clojure.lang.LazySeq;
import eu.qualimaster.common.signal.SignalMechanism;
import backtype.storm.daemon.common.SupervisorInfo;
import backtype.storm.daemon.common.Assignment;
import backtype.storm.daemon.common.WorkerHeartbeat;
import backtype.storm.generated.ExecutorInfo;
import backtype.storm.generated.ExecutorSummary;
import backtype.storm.generated.TopologyInfo;
import backtype.storm.utils.Utils;

/**
 * Zookeeper access utilities.
 * 
 * @author Holger Eichelberger
 */
public class ZkUtils {

    /**
     * Denotes an unknown (worker beat) time.
     */
    public static final long UNKNOWN_TIME = -1;
    public static final boolean REUSE_CURATOR = false;
    
    private static final String STORM_NS = "storm";
    private static final Logger LOGGER = LogManager.getLogger(ZkUtils.class);
    private static final String PATH_SEPARATOR = "/";
    private static final String ASSIGNMENTS = "assignments";
    private static final String ASSIGNMENTS_PREFIX = ASSIGNMENTS + PATH_SEPARATOR;
    private static final String SUPERVISORS = "supervisors";
    private static final String SUPERVISORS_PREFIX = SUPERVISORS + PATH_SEPARATOR;
    private static final String WORKERBEATS = "workerbeats";
    private static final String WORKERBEATS_PREFIX = WORKERBEATS + PATH_SEPARATOR;
    
    /**
     * Obtains a Curator framework instance for Storm and tries to 
     * start it.
     * 
     * @return the framework instance, use {@link #close(CuratorFramework)} for releasing the resulting instance 
     *   due to {@link #REUSE_CURATOR}; don't define watchers on the resulting instance
     */
    public static CuratorFramework obtainCuratorFramework() {
        CuratorFramework framework;
        if (REUSE_CURATOR) {
            String connectString = CoordinationConfiguration.getZookeeperConnectString();
            framework = CuratorFrameworkFactory.builder().connectString(connectString)
                .namespace(STORM_NS).retryPolicy(new RetryOneTime(500)).build();
            framework.start();
        } else {
            framework = SignalMechanism.obtainFramework(STORM_NS);
        }
        return framework;
    }
    
    /**
     * Close a curator framework returned by {@link #obtainCuratorFramework()}.
     * 
     * @param framework the framework
     */
    public static void close(CuratorFramework framework) {
        if (!REUSE_CURATOR) {
            framework.close();
        }
    }

    // checkstyle: stop exception type check

    /**
     * Returns the assignment for the given <code>topology</code> from the zookeeper connection 
     * <code>framework</code>.
     * 
     * @param framework the Curator framework connection to the Zookeeper
     * @param topology the topology to set the assignment for
     * @return the assignment for <code>topology</code>
     * @throws IOException in case of writing problems
     */
    public static Assignment getAssignment(CuratorFramework framework, TopologyInfo topology) throws IOException {
        Assignment result = null;
        try {
            byte[] data = framework.getData().forPath(ASSIGNMENTS_PREFIX + topology.get_id());
            Object o = Utils.deserialize(data);
            if (o instanceof Assignment) {
                result = (Assignment) o;
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return result;
    }

    /**
     * Sets the <code>assignment</code> for the given <code>topology</code> in the zookeeper connection 
     * <code>framework</code>.
     * 
     * @param framework the Curator framework connection to the Zookeeper
     * @param topology the topology to set the assignment for
     * @param assignment the assignment to set
     * @throws IOException in case of writing problems
     */
    public static void setAssignment(CuratorFramework framework, TopologyInfo topology, Assignment assignment) 
        throws IOException {
        try {
            byte[] data = Utils.serialize(assignment);
            framework.setData().forPath(ASSIGNMENTS_PREFIX + topology.get_id(), data);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Returns the supervisor information for the given <code>id</code> from the zookeeper connection 
     * <code>framework</code>.
     * 
     * @param framework the Curator framework connection to the Zookeeper
     * @param supervisorId the supervisor identifier
     * @return the supervisor information (may be <b>null</b> if the related ZNode does not exist)
     * @throws IOException in case of writing problems
     */
    public static SupervisorInfo getSupervisor(CuratorFramework framework, String supervisorId) throws IOException {
        SupervisorInfo result = null;
        try {
            byte[] data = framework.getData().forPath(SUPERVISORS_PREFIX + supervisorId);
            Object o = Utils.deserialize(data);
            if (o instanceof SupervisorInfo) {
                result = (SupervisorInfo) o;
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return result;
    }

    /**
     * Returns the supervisor information for all known supervisors of from the zookeeper connection 
     * <code>framework</code>.
     * 
     * @param framework the Curator framework connection to the Zookeeper
     * @return the supervisor information (may be empty if there are no supervisors)
     * @throws IOException in case of writing problems
     */
    public static List<SupervisorInfo> getSupervisors(CuratorFramework framework) throws IOException {
        List<SupervisorInfo> result = new ArrayList<SupervisorInfo>();
        try {
            List<String> ids = framework.getChildren().forPath(SUPERVISORS);
            if (null != ids) {
                for (String id : ids) {
                    try {
                        SupervisorInfo info = getSupervisor(framework, id);
                        if (null != info) {
                            result.add(info);
                        }
                    } catch (IOException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return result;
    }

    // checkstyle: resume exception type check

    /**
     * Returns the available slots of the given supervisor.
     * 
     * @param info the supervisor information object
     * @return the available slots
     */
    public static Set<Integer> getAvailableSlots(SupervisorInfo info) {
        Set<Integer> result = new HashSet<Integer>();
        if (info.meta instanceof LazySeq) {
            Object[] ports = ((LazySeq) info.meta).toArray();
            if (null != ports) {
                for (int p = 0; p < ports.length; p++) {
                    Object portO = ports[p];
                    if (portO instanceof Integer) {
                        result.add((Integer) portO);
                    } else if (portO instanceof Number) {
                        result.add(((Number) portO).intValue());
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the available slots of the given supervisor.
     * 
     * @param framework the Curator framework connection to the Zookeeper
     * @param supervisorId the supervisor identifier
     * @return the available slots
     * @throws IOException in case of writing problems
     */
    public static Set<Integer> getAvailableSlots(CuratorFramework framework, String supervisorId) throws IOException {
        return getAvailableSlots(getSupervisor(framework, supervisorId));
    }

    /**
     * Returns the available slots of the given supervisors.
     * 
     * @param framework the Curator framework connection to the Zookeeper
     * @param supervisorIds the supervisor identifier
     * @return the available slots
     * @throws IOException in case of writing problems
     */
    public static Set<Integer> getAvailableSlots(CuratorFramework framework, Set<String> supervisorIds) 
        throws IOException {
        Set<Integer> result = new HashSet<Integer>();
        for (String id : supervisorIds) {
            result.addAll(ZkUtils.getAvailableSlots(framework, id));
        }
        return result;
    }
    
    /**
     * Returns the amount of worker dependencies in <code>assignment</code>.
     * 
     * @param assignment the assignment to return the amount for
     * @return the amount of worker dependencies
     */
    public static int getWorkerDependenciesCount(Assignment assignment) {
        int result = 0;
        IPersistentCollection coll = getWorkerDependencies(assignment);
        if (null != coll) {
            result = coll.count();
        }
        return result;
    }
    
    /**
     * Returns the worker dependencies from an assignment instance. This method is required to support both, the 
     * original Storm version as well as the enhanced QM version.
     *  
     * @param assignment the assignment instance
     * @return the worker dependencies (may be <b>null</b> if not given or not available)
     */
    public static IPersistentCollection getWorkerDependencies(Assignment assignment) {
        IPersistentCollection result = null;
        if (null != assignment) {
            try {
                Field f = Assignment.class.getField("worker_dependencies");
                Object tmp = f.get(assignment);
                if (tmp instanceof IPersistentCollection) {
                    result = (IPersistentCollection) tmp;
                }
            } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * Creates an assignment instance. This method is required to support both, the original Storm version as 
     * well as the enhanced QM version.
     * 
     * @param masterCodeDir the directory where the pipeline code is located
     * @param nodeHost the node-host mapping (assignmentId - physical host names)
     * @param executorNodePort the executor - node/port mapping, i.e. [start/end task Id] to port and assignmentId
     * @param executorStart the executorId - start time mapping
     * @param workerDependencies the optional worker dependencies for enacting changes in the correct sequence
     * @return the created assignment instance, <b>null</b> if creation was not possible for some reason 
     */
    public static Assignment createAssignment(Object masterCodeDir, IPersistentMap nodeHost, 
        IPersistentMap executorNodePort, IPersistentMap executorStart, IPersistentCollection workerDependencies) {
        Assignment result = null;
        Constructor<?> consEx = null; // the QM extended version
        Constructor<?> consDflt = null; // the default version
        for (Constructor<?> cons : Assignment.class.getConstructors()) {
            int paramCount = cons.getParameterTypes().length;
            if (5 == paramCount) {
                consEx = cons;
            } else if (4 == paramCount) {
                consDflt = cons;
            }
        }

        try {
            if (null != consEx) {
                result = (Assignment) consEx.newInstance(masterCodeDir, nodeHost, executorNodePort, executorStart, 
                    workerDependencies);
            } else if (null != consDflt) {
                LOGGER.warn("This is not the QM Storm version running. Falling back to original assignment");
                result = (Assignment) consDflt.newInstance(masterCodeDir, nodeHost, executorNodePort, executorStart);
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return result;
    }

    /**
     * Returns whether we are running the extended QualiMaster storm version.
     * 
     * @return <code>true</code> in case of the extended version, <code>false</code> else
     */
    public static boolean isQmStormVersion() {
        boolean result = false;
        try {
            Assignment.class.getField("worker_dependencies");
            result = true;
        } catch (NoSuchFieldException | SecurityException e) {
        }
        return result;
        
    }

    /**
     * Returns a textual representation of <code>assignment</code>.
     * 
     * @param assignment the assignment
     * @return the textual version
     */
    public static String toString(Assignment assignment) {
        return null == assignment ? "null" : assignment.executor__GT_node_PLUS_port + " " 
            + assignment.executor__GT_start_time_secs + " " 
            + getWorkerDependencies(assignment);
    }

    // checkstyle: stop exception type check

    /**
     * Returns the alive workers for the given <code>topology</code>. For handling the worker ids, please refer to 
     * {@link HostPort#WORKERBEATS_HOSTPORT_PARSER}.
     * 
     * @param framework the Curator framework connection to the Zookeeper
     * @param topology the topology to set the assignment for
     * @return the alive workers in format node-port (may be <b>null</b> if there are no workers)
     * @throws IOException in case of errors accessing the heartbeats
     */
    public static List<String> getAliveWorkers(CuratorFramework framework, TopologyInfo topology) throws IOException {
        List<String> alive = null;
        try {
            String wpPath = WORKERBEATS_PREFIX + topology.get_id();
            alive = framework.getChildren().forPath(wpPath);
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return alive;
    }

    /**
     * Returns the actual worker beats for the given <code>topology</code>. For handling the worker ids, please refer to
     * {@link HostPort#WORKERBEATS_HOSTPORT_PARSER}.
     * 
     * @param framework the Curator framework connection to the Zookeeper
     * @param topology the topology to set the assignment for
     * @return the alive workers in format node-port with associated alive time stamp (may be <b>null</b> if 
     *     there are no workers)
     * @throws IOException in case of errors accessing the heartbeats
     */
    public static Map<String, Long> getWorkerBeats(CuratorFramework framework, TopologyInfo topology) 
        throws IOException {
        //  format: (str (workerbeat-storm-root storm-id) "/" node "-" port))
        Map<String, Long> result = null;
        try {
            String wpPath = WORKERBEATS_PREFIX + topology.get_id();
            List<String> children = framework.getChildren().forPath(wpPath);
            for (String child : children) {
                byte[] data = framework.getData().forPath(wpPath + "/" + child);
                Object o = Utils.deserialize(data);
                if (o instanceof WorkerHeartbeat) {
                    WorkerHeartbeat beat = (WorkerHeartbeat) o;
                    if (null == result) {
                        result = new HashMap<String, Long>();
                    }
                    if (beat.time_secs instanceof Long)  {
                        result.put(child, (Long) beat.time_secs);
                    } else if (beat.time_secs instanceof Number)  {
                        result.put(child, ((Number) beat.time_secs).longValue());
                    } else {
                        result.put(child, UNKNOWN_TIME);
                    }
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
        return result;
    }

    // checkstyle: resume exception type check

    /**
     * Maps executor tasks from <code>info</code>.
     * 
     * @param info the topology info containing the executor-taskid mapping
     * @return a task-id-to-executors mapping
     */
    public static Map<Integer, String> taskComponentMapping(TopologyInfo info) {
        Map<Integer, String> result = new HashMap<Integer, String>();
        for (ExecutorSummary executor : info.get_executors()) {
            ExecutorInfo eInfo = executor.get_executor_info();
            for (int t = eInfo.get_task_start(); t <= eInfo.get_task_end(); t++) {
                result.put(t, executor.get_component_id());
            }
        }
        return result;
    }

    /**
     * Returns the endpoint id.
     *
     * @param port the port number
     * @param hostId the host id
     * @return the endpoint id
     */
    public static String getEndpointId(int port, String hostId) {
        return port + "/" + hostId;
    }

}