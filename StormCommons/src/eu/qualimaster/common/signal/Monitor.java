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
package eu.qualimaster.common.signal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import backtype.storm.hooks.ITaskHook;
import backtype.storm.hooks.info.BoltAckInfo;
import backtype.storm.hooks.info.BoltExecuteInfo;
import backtype.storm.hooks.info.BoltFailInfo;
import backtype.storm.hooks.info.EmitInfo;
import backtype.storm.hooks.info.SpoutAckInfo;
import backtype.storm.hooks.info.SpoutFailInfo;
import backtype.storm.task.TopologyContext;
import de.uni_hildesheim.sse.system.GathererFactory;
import de.uni_hildesheim.sse.system.IMemoryDataGatherer;
import eu.qualimaster.Configuration;
import eu.qualimaster.base.algorithm.IncrementalAverage;
import eu.qualimaster.common.monitoring.MonitoringPluginRegistry;
import eu.qualimaster.events.AbstractTimerEventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.TimerEvent;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.events.PipelineElementMultiObservationMonitoringEvent;
import eu.qualimaster.monitoring.events.PipelineElementObservationMonitoringEvent;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.MonitoringFrequency;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.TimeBehavior;

/**
 * A common monitoring support class form bolts and spouts. No monitoring / event sending will 
 * happen as long as the aggregate method(s) are not called. Caller must take care of 
 * {@link MonitoringPluginRegistry#startMonitoring()}, 
 * {@link MonitoringPluginRegistry#emitted(backtype.storm.hooks.info.EmitInfo)}, 
 * {@link MonitoringPluginRegistry#endMonitoring()}. Calls {@link MonitoringPluginRegistry#collectObservations(Map)}.
 * 
 * Example:
 * <pre>
 *    startMonitoring();
 *    // the specific code that actually causes a tuple to be sent
 *    endMonitoring();
 * </pre>
 * 
 * @author Holger Eichelberger
 */
public class Monitor extends AbstractMonitor implements IMonitoringChangeListener, ITaskHook {
    
    private static final IMemoryDataGatherer MEMGATHERER = GathererFactory.getMemoryDataGatherer();
    private String namespace;
    private String name;
    private IncrementalAverage executionTime;
    private long sendInterval; 
    private ComponentKey key;
    private AtomicLong lastSend = new AtomicLong();
    private AtomicLong itemsSend = new AtomicLong(0);
    private AtomicLong itemsVolume = new AtomicLong(-1);
    private boolean includeItems;
    private TimerEventHandler timerHandler;
    private boolean collectVolume = Configuration.enableVolumeMonitoring();
    private boolean initialized = false;
    private int taskId;
    private Map<IObservable, Double> recordOnce;
    
    /**
     * Creates a monitor and sends once the executors resource usage event. Call {@link #start(boolean)} as soon as the 
     * underlying node is ready for processing. {@link StormSignalConnection#configureEventBus(Map)} shall be called 
     * before so that {@link Configuration#enableVolumeMonitoring()} has been set for this JVM.
     * 
     * @param namespace the namespace (pipeline name)
     * @param name the element name
     * @param includeItems whether the send items shall also be included
     * @param context the topology context for creating the component id
     *     
     * @see #init(boolean)
     */
    public Monitor(String namespace, String name, boolean includeItems, TopologyContext context) {
        this.namespace = namespace;
        this.name = name;
        this.executionTime = new IncrementalAverage();
        this.sendInterval = 500;
        this.taskId = context.getThisTaskId();
        this.key = new ComponentKey(context.getThisWorkerPort(), taskId);
        this.key.setThreadId(Thread.currentThread().getId());
        this.includeItems = includeItems;
    }
    
    /**
     * Records a specific observation once for sending it with the next message.
     * 
     * @param observable the observable to be recorded (ignored if <b>null</b>)
     * @param value the observed value
     */
    public synchronized void recordOnce(IObservable observable, double value) {
        if (null != observable) {
            if (null == recordOnce) {
                recordOnce = new HashMap<IObservable, Double>();
            }
            recordOnce.put(observable, value);
        }
    }

    /**
     * Records a set of observations once for sending them with the next message.
     * 
     * @param observations the observations to be recorded (ignored if <b>null</b>)
     */
    public synchronized void recordOnce(Map<IObservable, Double> observations) {
        if (null != observations) {
            if (null == recordOnce) {
                recordOnce = new HashMap<IObservable, Double>();
            }
            recordOnce.putAll(observations);
        }
    }
    
    /**
     * Initializes monitoring.
     * 
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public void init(boolean sendRegular) {
        if (!initialized) {
            initialized = true;
            Map<IObservable, Double> data = new HashMap<IObservable, Double>();
            considerRecordedOnce(data);
            data.put(ResourceUsage.EXECUTORS, 1.0);
            data.put(ResourceUsage.TASKS, 1.0); // key is per task!
            if (sendRegular) {
                // as this is accounted with full thread information but thrift not, this affects the results
                data.put(TimeBehavior.LATENCY, 0.0);
                data.put(TimeBehavior.THROUGHPUT_ITEMS, 0.0);
            }
            // avoid blocking remainder of initialization
            EventManager.asyncSend(new PipelineElementMultiObservationMonitoringEvent(namespace, name, key, data));
            this.lastSend.set(System.currentTimeMillis());
    
            if (sendRegular) {
                // works only in cluster mode due to event handler
                timerHandler = new TimerEventHandler();
                EventManager.setTimerPeriod(sendInterval + 100); // allow for tolerances
                EventManager.register(timerHandler);
            }
        }
    }

    /**
     * Consider the recorded-once entries and adds them to <code>observations</code>. Clears the recorded-once entries.
     * 
     * @param observations the observations to add the recorded-once entries to
     */
    private void considerRecordedOnce(Map<IObservable, Double> observations) {
        synchronized (this) {
            if (null != recordOnce && !recordOnce.isEmpty()) {
                observations.putAll(recordOnce);
                recordOnce.clear();
            }
        }
    }
    
    /**
     * Returns the namespace.
     * 
     * @return the namespace (pipeline name)
     */
    public String getNamespace() {
        return namespace;
    }
    
    /**
     * Returns the element name.
     * 
     * @return the element name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Reacts on timer events.
     * 
     * @author Holger Eichelberger
     */
    private class TimerEventHandler extends AbstractTimerEventHandler {

        @Override
        protected void handle(TimerEvent event) {
            // works only in cluster mode due to event handler
            checkSend(System.currentTimeMillis());
        }
        
    }

    /**
     * Aggregate the execution time and send the recorded value to the monitoring layer if the send interval
     * is outdated. Shall be used only in combination with a corresponding start time measurement.
     * The number of items to that have been sent since <code>start</code> is by default 1.
     *
     * @param start the start execution time
     */
    public void aggregateExecutionTime(long start) {
        aggregateExecutionTime(start, 1);
    }

    @Override
    public void aggregateExecutionTime(long start, int itemsCount) {
        if (start > 0) {
            long now = System.currentTimeMillis();
            executionTime.addValue(now - start);
            itemsSend.addAndGet(itemsCount);
            checkSend(now);
        }
    }

    /**
     * Checks whether the actual measurements have/shall be sent. This method must be thread-safe.
     * 
     * @param now the current time
     */
    protected void checkSend(long now) {
        if (sendInterval > 0 && now - lastSend.get() > sendInterval) {
            if (includeItems) {
                Map<IObservable, Double> data = new HashMap<IObservable, Double>();
                considerRecordedOnce(data);
                MonitoringPluginRegistry.collectObservations(data);
                data.put(TimeBehavior.LATENCY, executionTime.getAverage());
                data.put(TimeBehavior.THROUGHPUT_ITEMS, Double.valueOf(itemsSend.get()));
                long itemsTmp = itemsVolume.get();
                if (collectVolume && itemsTmp > 0) {
                    data.put(TimeBehavior.THROUGHPUT_VOLUME, Double.valueOf(itemsTmp));
                }
                EventManager.send(new PipelineElementMultiObservationMonitoringEvent(namespace, name, key, data));
            } else {
                boolean hasRecordOnce;
                synchronized (this) {
                    hasRecordOnce = null != recordOnce && !recordOnce.isEmpty();
                }
                if (MonitoringPluginRegistry.getRegisteredPluginCount() > 0 || hasRecordOnce) {
                    Map<IObservable, Double> data = new HashMap<IObservable, Double>();
                    considerRecordedOnce(data);
                    MonitoringPluginRegistry.collectObservations(data);
                    data.put(TimeBehavior.LATENCY, executionTime.getAverage());
                    EventManager.send(new PipelineElementMultiObservationMonitoringEvent(namespace, name, key, data));
                } else {
                    EventManager.send(new PipelineElementObservationMonitoringEvent(namespace, name, key, 
                        TimeBehavior.LATENCY, executionTime.getAverage()));
                }
            }
            lastSend.set(now);
        }
    }
    
    /**
     * Returns the component key.
     * 
     * @return the component key
     */
    public ComponentKey getComponentKey() {
        return key;
    }
    
    /**
     * Informs this monitor about a shutdown currently being processed by the hosting pipeline element.
     */
    void shutdown() {
        if (null != timerHandler) {
            EventManager.unregister(timerHandler);
        }
        timerHandler = null;
    }
    
    /**
     * Explicitly sets the volume of emitted tuples.
     * 
     * @param volume the volume (negative disables collection)
     */
    void setVolume(long volume) {
        this.itemsVolume.set(volume);
    }

    @Override
    public void notifyMonitoringChange(MonitoringChangeSignal signal) {
        Integer tmp = signal.getFrequency(MonitoringFrequency.PIPELINE_NODE);
        if (null != tmp) {
            sendInterval = tmp;
            if (null != timerHandler) {
                if (0 == tmp) {
                    EventManager.setTimerPeriod(0); // disable
                } else {
                    EventManager.setTimerPeriod(tmp + 100); // allow for tolerances
                }
            }
        }
        tmp = signal.getFrequency(MonitoringFrequency.PIPELINE_NODE_RESOURCES);
        if (null != tmp) {
            System.setProperty("qm.spass.frequency", tmp.toString()); // no constant to avoid dependency
        }
        Boolean b = signal.getEnabled(TimeBehavior.THROUGHPUT_VOLUME);
        if (null != b) {
            collectVolume = b;
        }
    }
    
    // --------------------- ITaskHook ---------------------------
    
    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map conf, TopologyContext context) {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void emit(EmitInfo info) {
        if (null != info && null != info.values && info.taskId == taskId) {
            String stream = info.stream;
            if (null != stream && !isInternalStream(stream)) { // ignore the internal streams
                itemsSend.addAndGet(info.values.size());
                if (collectVolume) {
                    itemsVolume.addAndGet(MEMGATHERER.getObjectSize(info.values));
                }
                MonitoringPluginRegistry.emitted(info);
            }
        }
    }

    @Override
    public void spoutAck(SpoutAckInfo info) {
    }

    @Override
    public void spoutFail(SpoutFailInfo info) {
    }

    @Override
    public void boltExecute(BoltExecuteInfo info) {
        if (null != info && null != info.executeLatencyMs && info.executingTaskId == taskId) {
            if (!isMonitoring()) { // if we are within start/endMonitoring, we already have the time
                executionTime.addValue(info.executeLatencyMs);
                checkSend(System.currentTimeMillis());
            }
        }
    }

    @Override
    public void boltAck(BoltAckInfo info) {
    }

    @Override
    public void boltFail(BoltFailInfo info) {
    }
    
    /**
     * Counts emitting for a sink execution method.
     * 
     * @param tuple the tuple emitted
     */
    public void emitted(Object tuple) {
        if (null != tuple) {
            itemsSend.incrementAndGet();
            if (collectVolume) {
                itemsVolume.addAndGet(MEMGATHERER.getObjectSize(tuple));
            }
            MonitoringPluginRegistry.emitted(tuple);
        }
    }
    
    /**
     * Returns a new monitor for a parallel thread.
     * 
     * @return the monitor
     */
    public ThreadMonitor createThreadMonitor() {
        return new ThreadMonitor(this);
    }
    
    /**
     * Returns whether an executor name is considered to be Storm internal.
     * 
     * @param name the executor name
     * @return <code>true</code> if internal, <code>false</code> else
     */
    public static boolean isInternalExecutor(String name) {
        return name.startsWith("__"); // internal: ackers, system etc.
    }

    /**
     * Returns whether a stream name is considered to be Storm internal.
     * 
     * @param name the executor name
     * @return <code>true</code> if internal, <code>false</code> else
     */
    public static boolean isInternalStream(String name) {
        return name.startsWith("__"); // internal: ackers, system etc.
    }

}
