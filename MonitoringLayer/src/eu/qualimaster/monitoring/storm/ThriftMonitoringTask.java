package eu.qualimaster.monitoring.storm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.storm.curator.framework.CuratorFramework;
import org.apache.thrift7.TException;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.coordination.HostPort;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.TaskAssignment;
import eu.qualimaster.coordination.ZkUtils;
import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.coordination.INameMapping.Component.Type;
import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.AbstractContainerMonitoringTask;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.observations.ObservedValue;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.StateUtils;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.TimeBehavior;
import backtype.storm.event.EventManager;
import backtype.storm.generated.BoltStats;
import backtype.storm.generated.ClusterSummary;
import backtype.storm.generated.ExecutorInfo;
import backtype.storm.generated.ExecutorSpecificStats;
import backtype.storm.generated.ExecutorStats;
import backtype.storm.generated.ExecutorSummary;
import backtype.storm.generated.NotAliveException;
import backtype.storm.generated.TopologyInfo;
import backtype.storm.generated.TopologySummary;

/**
 * A thrift-based monitoring task for Storm Pipelines.
 * 
 * @author Holger Eichelberger
 */
public class ThriftMonitoringTask extends AbstractContainerMonitoringTask {

    static final String ALL_TIME = ":all-time";
    static final String AT_10M = "600";
    static final String AT_3H = "10800";
    static final String AT_1D = "86400";
    private static final Logger LOGGER = LogManager.getLogger(ThriftMonitoringTask.class);
    private static final int EXECUTOR_START_WAITING_TIME = MonitoringConfiguration.getStormExecutorStartupWaitingTime();
    
    private String pipeline;
    private Set<String> topologyNames = new HashSet<String>();
    private StormConnection connection;
    private Class<? extends AdaptationEvent> adaptationFilter;

    /**
     * Creates the monitoring task.
     * 
     * @param pipeline the pipeline name
     * @param connection the Storm connection
     * @param state the system state to be modified due to monitoring
     * @param adaptationFilter the adaptation filter, may be <b>null</b> if there is none
     */
    ThriftMonitoringTask(String pipeline, StormConnection connection, SystemState state, 
        Class<? extends AdaptationEvent> adaptationFilter) {
        super(state);
        this.pipeline = pipeline;
        this.connection = connection;
        this.adaptationFilter = adaptationFilter;
        INameMapping mapping = MonitoringManager.getNameMapping(pipeline);
        if (null != mapping) {
            topologyNames.addAll(mapping.getPipelineNames());
        } else {
            LOGGER.error("no name mapping for pipeline " + pipeline);
        }
    }
    
    @Override
    public void monitor() {
        if (connection.open()) {
            try {
                ClusterSummary summary = connection.getClusterSummary();

                List<TopologySummary> topologies = summary.get_topologies();
                Set<PipelineSystemPart> modified = new HashSet<PipelineSystemPart>(); 
                for (int t = 0; t < topologies.size(); t++) {
                    TopologySummary topologySummary = topologies.get(t);
                    if (topologyNames.contains(topologySummary.get_name())) {
                        try {
                            PipelineSystemPart part = aggregateTopology(
                                connection.getTopologyInfo(topologySummary.get_id()));
                            if (null != part) {
                                modified.add(part);
                            }
                        } catch (NotAliveException e) {
                        }
                    }
                }
                Collection<PipelineSystemPart> pipelines = getState().getPipelines();
                for (PipelineSystemPart pipeline : pipelines) {
                    if (!modified.contains(pipeline) && pipeline.getStatus().wasStarted()) {
                        pipeline.changeStatus(PipelineLifecycleEvent.Status.DISAPPEARED, true, adaptationFilter);
                    }
                }
            } catch (TException e) {
                LOGGER.error("Cannot obtain thrift data " + e.getMessage(), e);
            }  catch (IllegalStateException e) {
                // monitoring runs longer than topology exists... ignore
            }
        }
    }
    
    /**
     * Testing.
     * 
     * @param topology the topology
     */
    @SuppressWarnings("unused")
    private void checkAssignment(TopologyInfo topology) {
        CuratorFramework curator = connection.getCurator();
        if (null != curator) {
            try {
                List<HostPort> hosts = HostPort.toHostPort(ZkUtils.getAliveWorkers(curator, topology), 
                    HostPort.WORKERBEATS_HOSTPORT_PARSER);
                System.out.println("ALIVE " + hosts);
                Map<Integer, String> taskComponents = ZkUtils.taskComponentMapping(topology);
                Map<String, List<TaskAssignment>> componentAssignments 
                    = TaskAssignment.readTaskAssignments(ZkUtils.getAssignment(curator, topology), taskComponents);
                System.out.println("ASSNG " + componentAssignments);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Returns whether the component assigned to <code>nodeName</code> is a sink.
     * 
     * @param mapping the name mapping
     * @param nodeName the node name
     * @return <code>true</code> if it is a sink, <code>false</code> else
     */
    @SuppressWarnings("unused")
    private boolean isSink(INameMapping mapping, String nodeName) {
        Component comp = mapping.getPipelineNodeComponent(nodeName);
        return null != comp && Type.SINK == comp.getType();
    }

    /**
     * Prepares a topology for aggregation.
     * 
     * @param topology the topology
     * @param mapping the mapping
     * @return the pipeline system part representing the pipeline
     * @throws NotAliveException in case that the requested topology is not alive
     * @throws TException in case of problems accessing the remote topology info
     */
    private PipelineSystemPart preparePipelineAggregation(TopologyInfo topology, INameMapping mapping) 
        throws TException, NotAliveException {
        SystemState state = getState();
        String pipelineName = mapping.getPipelineName();
        PipelineSystemPart part = state.obtainPipeline(pipelineName); // exists or creates
        if (null == part.getTopology()) {
            PipelineTopology topo = Utils.buildPipelineTopology(connection.getTopology(topology), topology, mapping);
            part.setTopology(topo);
            if (null != topo) {
                LOGGER.info("TOPOLOGY for " + mapping.getPipelineName() + " " + topo);
            }
        }
        if (PipelineLifecycleEvent.Status.INITIALIZED == part.getStatus() && !DataManager.isStarted()) {
            if (System.currentTimeMillis() - part.getLastStateChange() // TODO WORKAROUND FOR DML 
                > MonitoringConfiguration.getPipelineStartNotificationDelay()) {
                part.changeStatus(PipelineLifecycleEvent.Status.STARTED, true, adaptationFilter);
            }
        }
        return part;
    }
    
    /**
     * Aggregates the values for the topology.
     * 
     * @param topology the topology information to be aggregated
     * @return the affected / modified pipeline system part, may be <b>null</b> if the pipeline / topology yet does 
     *     not exist 
     * @throws NotAliveException in case that the requested topology is not alive
     * @throws TException in case of problems accessing the remote topology info
     */
    private PipelineSystemPart aggregateTopology(TopologyInfo topology) throws TException, NotAliveException {
        PipelineSystemPart part = null;
        INameMapping mapping = MonitoringManager.getNameMapping(pipeline);
        if (null != mapping) {
            part = preparePipelineAggregation(topology, mapping);
            List<ExecutorSummary> executors = topology.get_executors();
            PipelineStatistics pStat = new PipelineStatistics(part);
            List<String> uptime = new ArrayList<String>();
            List<String> eventsReceived = new ArrayList<String>();
            int executorRunningCount = 0; // first heuristics... uptime of executors - does not work in every case
            int nonInternalCount = 0; // second heuristics... non-legacy pipelines sending proper events
            int nonInternalRunningCount = 0;
            for (int e = 0; e < executors.size(); e++) {
                ExecutorSummary executor = executors.get(e);
                String nodeName = executor.get_component_id();
                if (executor.get_uptime_secs() > EXECUTOR_START_WAITING_TIME) { 
                    executorRunningCount++;
                    uptime.add(nodeName);
                }
                boolean isInternal = Utils.isInternal(executor); 
                ExecutorStats stats = executor.get_stats();
                PipelineNodeSystemPart nodePart = SystemState.getNodePart(mapping, part, nodeName);
                if (!isInternal) {
                    nonInternalCount++;
                    if (nodePart.getObservedValue(ResourceUsage.TASKS) > 0) {
                        nonInternalRunningCount++;
                        eventsReceived.add(nodeName);
                    }
                }
                if (null != stats) {
                    if (isInternal) {
                        nodeName = "_SYSTEM_"; // TODO check whether a special node is better
                    }
                    if (doThrift(executor, nodePart, isInternal)) { // non-thrift happens along the events
                        aggregateExecutor(executor, nodePart, isInternal);
                    }
                    if (!isInternal) {
                        sendSummaryEvent(nodePart, part.getName(), MonitoringManager.DEMO_MSG_PROCESSING_ELEMENT);
                    }
                    pStat.collect(nodePart);
                } // no stats... in particular if
            }
            debugExecutors(executors, mapping, part);
            
            boolean allInitialized = pStat.commit();
            sendSummaryEvent(part, null, MonitoringManager.DEMO_MSG_PIPELINE);
            boolean createdChanged = false;
            if ((PipelineLifecycleEvent.Status.UNKNOWN == part.getStatus() // shall not happen 
                || PipelineLifecycleEvent.Status.STARTING == part.getStatus())) {
                // consider pipeline creation finished as soon as all executors are running and ready to work
                LOGGER.info("Trying to elevate '" + part.getName() + "' to CREATED: uptime " + uptime + " " 
                    + executors.size() + " " + executorRunningCount + " event received " + eventsReceived + " " 
                    + nonInternalCount + " " + nonInternalRunningCount);
                if (executors.size() == executorRunningCount || nonInternalCount == nonInternalRunningCount) {
                    part.changeStatus(PipelineLifecycleEvent.Status.CREATED, true, adaptationFilter);
                    createdChanged = true;
                }
            } 
            if (!createdChanged && allInitialized && PipelineLifecycleEvent.Status.CREATED == part.getStatus()) {
                part.changeStatus(PipelineLifecycleEvent.Status.INITIALIZED, true, adaptationFilter);
            }
        } else {
            LOGGER.error("no mapping for " + topology.get_name());
        }
        return part;
    }
    
    /**
     * Debug-print the non-thrift executor states.
     * 
     * @param executors the executors
     * @param mapping the name mapping
     * @param part the pipeline parts
     */
    private void debugExecutors(List<ExecutorSummary> executors, INameMapping mapping, PipelineSystemPart part) {
        if (MonitoringConfiguration.debugThriftMonitoring()) {
            for (int e = 0; e < executors.size(); e++) {
                ExecutorSummary executor = executors.get(e);
                boolean isInternal = Utils.isInternal(executor); 
                String nodeName = executor.get_component_id();
                PipelineNodeSystemPart nodePart = SystemState.getNodePart(mapping, part, nodeName);
                if (!doThrift(executor, nodePart, isInternal)) {
                    LogManager.getLogger(EventManager.class).info("from events " + nodePart);
                }
            }                
        }
    }
    
    /**
     * Returns the number of tasks handled by an executor.
     * 
     * @param executor the executor (summary)
     * @return the number of tasks
     */
    private static int getTaskCount(ExecutorSummary executor) {
        ExecutorInfo info = executor.get_executor_info();
        return info.get_task_end() - info.get_task_start() + 1;
    }
    
    /**
     * Aggregates an executor.
     * 
     * @param executor the excutor
     * @param nodePart the target node part
     * @param isInternal whether it is considered as an internal Storm node
     */
    private void aggregateExecutor(ExecutorSummary executor, PipelineNodeSystemPart nodePart, 
        boolean isInternal) {
        List<ComponentKey> keys = toKeys(executor);
        ExecutorStats stats = executor.get_stats();
        int tasks = keys.size();
        for (int k = 0; k < tasks; k++) {
            ComponentKey key = keys.get(k);
            if (stats.get_specific().is_set_bolt()) {
                aggregateBolt(executor, nodePart, isInternal, key, tasks);
            } else if (stats.get_specific().is_set_spout()) {
                aggregateSpout(executor, nodePart, isInternal, key, tasks);
            } else {
                aggregateOther(executor, nodePart, isInternal, key, tasks);
            }
        }
        setValueToKeysAndClear(nodePart, keys, ResourceUsage.EXECUTORS, 1);
        setValueToKeysAndClear(nodePart, keys, ResourceUsage.TASKS, getTaskCount(executor));
    }
    
    /**
     * Returns whether thrift monitoring shall be done for the given executor / node part.
     * 
     * @param executor the executor
     * @param nodePart the node part
     * @param isInternal whether the executor is considered to be an internal node
     * @return <code>true</code> for thrift monitoring, <code>false</code> else
     */
    private boolean doThrift(ExecutorSummary executor, PipelineNodeSystemPart nodePart, boolean isInternal) {
        boolean doThrift = nodePart.useThrift();
        if (doThrift) {
            ExecutorStats stats = executor.get_stats();
            if (null != stats && null != stats.get_specific() 
                && (!stats.get_specific().is_set_bolt() && !stats.get_specific().is_set_spout())) {
                doThrift = isInternal;
            }
        }
        return doThrift;
    }

    /**
     * Aggregates a spout.
     * 
     * @param executor the executor summary
     * @param part the system part representing the node
     * @param isInternal whether <code>executor</code> is internal
     * @param key the component to modify
     * @param keyCount the number of keys/tasks of <code>executor</code>
     */
    private void aggregateSpout(ExecutorSummary executor, PipelineNodeSystemPart part, 
        boolean isInternal, ComponentKey key, int keyCount) {
        // ack in a Spout corresponds to the totally acked items, not to the actual ones as in a Bolt
        // get_complete_ms_avg() measures the entire tree
        // relying on monitoring messages that deliver LATENCY and THROUGHPUT - updated there
        
        // :complete-latencies get_complete_ms_avg / get_acked
        if (MonitoringConfiguration.debugThriftMonitoring()) {
            LogManager.getLogger(EventManager.class).info("from thrift " + part.getName() 
                + " capacity " 
                + part.getObservedValue(ResourceUsage.CAPACITY, key) + " throughput " 
                + part.getObservedValue(TimeBehavior.THROUGHPUT_ITEMS, key) + " key " + key + " -> " + part);
        }
    }
    
    /**
     * Aggregates an other node.
     * 
     * @param executor the executor summary
     * @param part the system part representing the node
     * @param isInternal whether <code>executor</code> is internal
     * @param key the component to modify
     * @param keyCount the number of keys/tasks of <code>executor</code>
     */
    private void aggregateOther(ExecutorSummary executor, PipelineNodeSystemPart part, 
        boolean isInternal, ComponentKey key, int keyCount) {
        ExecutorStats stats = executor.get_stats();
        ObservedValue executeLatencyValue = part.getObservedValue(TimeBehavior.LATENCY, key);
        double executeLatency = null == executeLatencyValue ? 0 : executeLatencyValue.get(); 
        if (!isInternal) {
            long executedAll = getLongStatValue(stats.get_emitted(), ALL_TIME);
            StateUtils.setValue(part, TimeBehavior.THROUGHPUT_ITEMS, executedAll / keyCount, key);
            StateUtils.updateCapacity(part, key, true);

            if (MonitoringConfiguration.debugThriftMonitoring()) {
                LogManager.getLogger(EventManager.class).info("from thrift " + part.getName()  
                    + " executedAll " + executedAll + " execLatency " + executeLatency 
                    + " capacity " 
                    + part.getObservedValue(ResourceUsage.CAPACITY, key) + " throughput " 
                    + part.getObservedValue(TimeBehavior.THROUGHPUT_ITEMS, key) + " key " + key + " -> " + part);
            }
        }
    }

    /**
     * Handles a bolt.
     * 
     * @param executor the executor summary
     * @param part the system part representing the node
     * @param isInternal whether <code>executor</code> is internal
     * @param key the component to modify
     * @param keyCount the number of keys/tasks of <code>executor</code>
     */
    private void aggregateBolt(ExecutorSummary executor, PipelineNodeSystemPart part, 
        boolean isInternal, ComponentKey key, int keyCount) {
        ExecutorStats stats = executor.get_stats();
        ExecutorSpecificStats specificStats = stats.get_specific();
        BoltStats boltStats = specificStats.get_bolt();
        //executor_throughput_items = getBoltStatLongValueFromMap(boltStats.get_executed(), ALL_TIME);
        if (!isInternal) {
            double executeLatency;
            double executed;
            // :process-latencies process_ms_avg / get_acked; :execute-latencies execute_ms_avg / get_executed
            if (Type.SINK == part.getComponentType()) {
                // storm includes all outgoing streams
                executeLatency = getDoubleMinStatValue(boltStats.get_process_ms_avg(), AT_10M);
                executed = getLongStatValue(boltStats.get_executed(), AT_10M);
            } else {
                // storm includes all outgoing streams
                executeLatency = getDoubleMinStatValue(boltStats.get_process_ms_avg(), AT_10M);
                executed = getLongStatValue(stats.get_emitted(), AT_10M);
            }
            StateUtils.setValue(part, TimeBehavior.LATENCY, executeLatency, key); // average over all tasks
            // sum of all tasks
            StateUtils.setValue(part, TimeBehavior.THROUGHPUT_ITEMS, executed / keyCount, key); 
            StateUtils.updateCapacity(part, key, true);
            
            if (MonitoringConfiguration.debugThriftMonitoring()) {
                LogManager.getLogger(EventManager.class).info("from thrift " + part.getName()  
                    + " executed " + executed + boltStats.get_executed() + " execLatency " + executeLatency + " " 
                    + boltStats.get_execute_ms_avg()
                    + " processLatency " + getDoubleStatValue(boltStats.get_process_ms_avg(), AT_10M) + " " 
                    + boltStats.get_process_ms_avg()
                    + " capacity " 
                    + part.getObservedValue(ResourceUsage.CAPACITY, key) + " throughput " 
                    + part.getObservedValue(TimeBehavior.THROUGHPUT_ITEMS, key) + " key " + key + " -> " + part);
            }
        }
    }
    
    /**
     * Sets the given <code>value</code> to all components in the observation of <code>observable</code> in 
     * <code>part</code> and clears unnamed components.
     * 
     * @param part the system part
     * @param keys the keys to set the value for (and to clear the remaining ones)
     * @param observable the observable to operate on
     * @param value the value to set
     */
    private void setValueToKeysAndClear(PipelineNodeSystemPart part, List<ComponentKey> keys, IObservable observable, 
        double value) {
        Set<Object> executorKeys = new HashSet<Object>(); 
        executorKeys.addAll(part.getComponentKeys(observable));
        setValue(part, observable, 1, keys);
        executorKeys.remove(keys);
        part.clearComponents(observable, executorKeys);
    }

    /**
     * Turns an executor summary into component keys.
     * 
     * @param executor the executor summary
     * @return the component keys
     */
    private List<ComponentKey> toKeys(ExecutorSummary executor) {
        List<ComponentKey> keys = new ArrayList<ComponentKey>();
        ExecutorInfo info = executor.get_executor_info();
        for (int t = info.get_task_start(); t <= info.get_task_end(); t++) {
            keys.add(new ComponentKey(executor.get_host(), executor.get_port(), t));
        }
        return keys;
    }
    
    /**
     * Sets an observation value for multiple component keys.
     * 
     * @param part the target system part
     * @param observable the observable
     * @param value the observed value
     * @param keys the component keys
     */
    private void setValue(SystemPart part, IObservable observable, double value, List<ComponentKey> keys) {
        for (int k = 0; k < keys.size(); k++) {
            StateUtils.setValue(part, observable, value, keys.get(k));
        }
    }
    
    // further conversions e.g. from long to double if required shall be added in the style of the value methods above

    /**
     * Returns a (sum) stat value from a value map.
     * 
     * @param <T> the inner key type
     * @param map the value map
     * @param statName the stat name
     * @return the stat value
     */
    public static <T> double getDoubleStatValue(Map<String, Map<T, Double>> map, String statName) {
        double result = 0.0;
        Map<?, Double> intermediateMap = map.get(statName);
        if (null != intermediateMap) {
            for (Map.Entry<?, Double> ent : intermediateMap.entrySet()) {
                Object streamId = ent.getKey();
                if (null != streamId && !streamId.toString().startsWith("__")) {
                    Double dbl = ent.getValue();
                    if (null != dbl) {
                        result += dbl;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns a (min) stat value from a value map.
     * 
     * @param <T> the inner key type
     * @param map the value map
     * @param statName the stat name
     * @return the stat value
     */
    public static <T> double getDoubleMinStatValue(Map<String, Map<T, Double>> map, String statName) {
        double result = 0.0;
        int count = 0;
        Map<?, Double> intermediateMap = map.get(statName);
        if (null != intermediateMap) {
            for (Map.Entry<?, Double> ent : intermediateMap.entrySet()) {
                Object streamId = ent.getKey();
                if (null != streamId && !streamId.toString().startsWith("__")) {
                    Double dbl = ent.getValue();
                    if (null != dbl) {
                        if (0 == count) {
                            result = dbl;
                        } else {
                            result = Math.min(dbl, result);
                        }
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Returns a stat value from a value map.
     * 
     * @param <T> the inner key type
     * @param map the value map
     * @param statName the stat name
     * @return the stat value
     */
    public static <T> long getLongStatValue(Map<String, Map<T, Long>> map, String statName) {
        long result = 0;
        Map<?, Long> intermediateMap = map.get(statName);
        if (null != intermediateMap) {
            for (Map.Entry<?, Long> ent : intermediateMap.entrySet()) {
                Object streamId = ent.getKey();
                if (null != streamId && !streamId.toString().startsWith("__")) {
                    Long dbl = ent.getValue();
                    if (null != dbl) {
                        result += dbl;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public boolean cancel() {
        return super.cancel();
    }

    @Override
    public int getFrequency() {
        return MonitoringConfiguration.getPipelineMonitoringFrequency();
    }
    
    @Override
    protected void failover(Throwable th) {
        connection.failover(th);
    }

}