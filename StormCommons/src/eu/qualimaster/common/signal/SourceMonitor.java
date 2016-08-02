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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import backtype.storm.hooks.info.EmitInfo;
import backtype.storm.task.TopologyContext;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.SourceVolumeMonitoringEvent;
import eu.qualimaster.observables.MonitoringFrequency;

/**
 * A specific monitor for pipeline sources, in particular to support data aggregation for source 
 * volume prediction. This class provides two ways of performing the aggregation:
 * <ol>
 *   <li>Direct call of {@link #aggregateKey(String)} from the pipeline source code.</li>
 *   <li>Registration of an aggregation key provider, which allows transparent aggregation via the
 *     task hook mechanisms of the parent class.</li>
 * </ol>
 * 
 * @author Holger Eichelberger
 */
public class SourceMonitor extends Monitor {

    private Map<Class<?>, AggregationKeyProvider<?>> providers = new HashMap<Class<?>, AggregationKeyProvider<?>>();
    private Map<String, Integer> occurrences = new HashMap<String, Integer>(); // specific class for performance?
    private AtomicLong lastAggregation = new AtomicLong();
    private long aggregationInterval; 

    /**
     * An aggregation key provider plugin.
     * 
     * @param <T> the tuple type
     * @author Holger Eichelberger
     */
    public abstract class AggregationKeyProvider<T> {

        private Class<T> cls;
        
        /**
         * Creates an aggregation key provider.
         * 
         * @param cls the tuple class
         */
        protected AggregationKeyProvider(Class<T> cls) {
            if (null == cls) {
                throw new IllegalArgumentException("cls must not be null");
            }
            this.cls = cls;
        }
        
        /**
         * Returns the aggregation key for <code>tuple</code>.
         * 
         * @param tuple the tuple
         * @return the aggregation key
         */
        public abstract String getAggregationKey(T tuple);
        
        /**
         * Returns the aggregation key for <code>tuple</code>.
         * 
         * @param tuple the tuple
         * @return the aggregation key
         */
        private String getKey(Object tuple) {
            return getAggregationKey(cls.cast(tuple));
        }
    }
    
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
    public SourceMonitor(String namespace, String name, boolean includeItems, TopologyContext context,
        boolean sendRegular) {
        super(namespace, name, includeItems, context, sendRegular);
        this.aggregationInterval = 6000;
    }
    
    /**
     * Registers an aggregation key provider.
     * 
     * @param <T> the tuple type
     * @param cls the tuple class (nothing happens if <b>null</b>)
     * @param provider the provider instance (deregistered if <b>null</b>)
     */
    public <T> void registerAggregationKeyProvider(Class<T> cls, AggregationKeyProvider<T> provider) {
        if (null != cls) {
            if (null == provider) {
                providers.remove(cls);
            } else {
                providers.put(cls, provider);
            }
        }
    }
    
    /**
     * Changes the source volume aggregation interval, which determines when aggregated information shall be sent next.
     * 
     * @param aggregationInterval the aggregation interval
     */
    public void setAggregationInterval(long aggregationInterval) {
        this.aggregationInterval = aggregationInterval;
    }

    @Override
    public void emitted(Object tuple) {
        super.emitted(tuple);
        aggregate(tuple);
    }
    
    @Override
    public void emit(EmitInfo info) {
        super.emit(info);
        if (null != info && null != info.values && providers.size() > 0) {
            for (int v = 0; v < info.values.size(); v++) {
                aggregate(info.values.get(v));
            }
        }
    }
    
    /**
     * Aggregates the occurrence of <code>tuple</code>.
     * 
     * @param tuple the tuple
     */
    private void aggregate(Object tuple) {
        if (null != tuple) {
            AggregationKeyProvider<?> provider = providers.get(tuple.getClass());
            if (null != provider) {
                aggregateKey(provider.getKey(tuple));
            }
        }
    }
    
    /**
     * Aggregates the <code>key</code> for an individual tuple.
     * 
     * @param key the key
     */
    public void aggregateKey(String key) {
        if (null != key) {
            synchronized (occurrences) {
                Integer count = occurrences.get(key);
                if (null == count) {
                    occurrences.put(key, 0);
                } else {
                    occurrences.put(key, count + 1);
                }
            }
        }
    }
    
    @Override
    protected void checkSend(long now) {
        super.checkSend(now);
        if (occurrences.size() > 0 && aggregationInterval > 0 && now - lastAggregation.get() > aggregationInterval) {
            Map<String, Integer> oldOcc = occurrences;
            Map<String, Integer> newOcc = new HashMap<String, Integer>();
            synchronized (occurrences) {
                occurrences = newOcc;
            }
            SourceVolumeMonitoringEvent evt = new SourceVolumeMonitoringEvent(getNamespace(), getName(), oldOcc);
            EventManager.send(evt);
            lastAggregation.set(now);
        }
    }
        
    @Override
    public void notifyMonitoringChange(MonitoringChangeSignal signal) {
        super.notifyMonitoringChange(signal);
        Integer tmp = signal.getFrequency(MonitoringFrequency.PIPELINE_NODE);
        if (null != tmp) {
            aggregationInterval = tmp; // we just ignore the EventManager.setTimerPeriod here
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
    }
    
}
