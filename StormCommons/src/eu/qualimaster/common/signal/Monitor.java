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

import backtype.storm.task.TopologyContext;
import eu.qualimaster.base.algorithm.IncrementalAverage;
import eu.qualimaster.events.AbstractTimerEventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.TimerEvent;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.events.PipelineElementMultiObservationMonitoringEvent;
import eu.qualimaster.monitoring.events.PipelineElementObservationMonitoringEvent;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.TimeBehavior;

/**
 * A common monitoring support class form bolts and spouts. No monitoring / event sending will 
 * happen as long as the aggregate method(s) are not called.
 * 
 * Example:
 * <pre>
 *    long start = System.currentTimeMillis();
 *    // the specific code that actually causes a tuple to be sent
 *    aggregateExecutionTime(start);
 * </pre>
 * 
 * @author Holger Eichelberger
 */
public class Monitor {
    
    private String namespace;
    private String name;
    private IncrementalAverage executionTime;
    private long sendInterval; 
    private ComponentKey key;
    private AtomicLong lastSend = new AtomicLong();
    private double itemsSend = 0;
    private boolean includeItems;
    private TimerEventHandler timerHandler;

    /**
     * Creates a monitor and sends once the executors resource usage event.
     * 
     * @param namespace the namespace (pipeline name)
     * @param name the element name
     * @param includeItems whether the send items shall also be included
     * @param context the topology context for creating the component id
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public Monitor(String namespace, String name, boolean includeItems, TopologyContext context, boolean sendRegular) {
        this.namespace = namespace;
        this.name = name;
        this.executionTime = new IncrementalAverage();
        this.sendInterval = 500;
        this.key = new ComponentKey(context.getThisWorkerPort(), context.getThisTaskId());
        this.key.setThreadId(Thread.currentThread().getId());
        this.includeItems = includeItems;

        Map<IObservable, Double> data = new HashMap<IObservable, Double>();
        data.put(ResourceUsage.EXECUTORS, 1.0);
        data.put(ResourceUsage.TASKS, 1.0); // key is per task!
        if (sendRegular) {
            // as this is accounted with full thread information but thrift not, this affects the results
            data.put(TimeBehavior.LATENCY, 0.0);
            data.put(TimeBehavior.THROUGHPUT_ITEMS, 0.0);
        }
        EventManager.send(new PipelineElementMultiObservationMonitoringEvent(namespace, name, key, data));
        this.lastSend.set(System.currentTimeMillis());

        if (sendRegular) {
            // works only in cluster mode due to event handler
            timerHandler = new TimerEventHandler();
            EventManager.setTimerPeriod(sendInterval + 100); // allow for tolerances
            EventManager.register(timerHandler);
        }
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
     * Aggregate the execution time and send the recorded value to the monitoring layer.
     * Shall be used only in combination with a corresponding start time measurement.
     * The number of items to that have been sent since <code>start</code> is assumed to 1.
     *
     * @param start the start execution time
     */
    public void aggregateExecutionTime(long start) {
        aggregateExecutionTime(start, 1);
    }
    
    /**
     * Aggregate the execution time and send the recorded value to the monitoring layer.
     * Shall be used only in combination with a corresponding start time measurement.
     *
     * @param start the start execution time
     * @param itemsCount the number of items emitted since <code>start</code>, (negative is turned to <code>0</code>)
     */
    public void aggregateExecutionTime(long start, int itemsCount) {
        itemsCount = Math.max(0, itemsCount);
        long now = System.currentTimeMillis();
        executionTime.addValue(now - start);
        itemsSend += itemsCount;
        checkSend(now);
    }
    
    /**
     * Checks whether the actual measurements have/shall be sent. This method must be thread-safe.
     * 
     * @param now the current time
     */
    private void checkSend(long now) {
        if (now - lastSend.get() > sendInterval) {
            if (includeItems) {
                Map<IObservable, Double> data = new HashMap<IObservable, Double>();
                data.put(TimeBehavior.LATENCY, executionTime.getAverage());
                data.put(TimeBehavior.THROUGHPUT_ITEMS, itemsSend);
                EventManager.send(new PipelineElementMultiObservationMonitoringEvent(namespace, name, key, data));
            } else {
                EventManager.send(new PipelineElementObservationMonitoringEvent(namespace, name, key, 
                    TimeBehavior.LATENCY, executionTime.getAverage()));
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
    
}
