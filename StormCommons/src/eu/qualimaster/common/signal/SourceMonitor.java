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
import java.util.Iterator;
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
     * Creates a monitor and sends once the executors resource usage event.
     * 
     * @param namespace the namespace (pipeline name)
     * @param name the element name
     * @param includeItems whether the send items shall also be included
     * @param context the topology context for creating the component id
     */
    public SourceMonitor(String namespace, String name, boolean includeItems, TopologyContext context) {
        super(namespace, name, includeItems, context);
        this.aggregationInterval = 6000;
    }
    
    /**
     * Registers an aggregation key provider.
     * 
     * @param provider the provider instance (ignored if <b>null</b>)
     */
    public void registerAggregationKeyProvider(AggregationKeyProvider<?> provider) {
        if (null != provider) {
            providers.put(provider.handles(), provider);
        }
    }

    /**
     * Unregisters an aggregation key provider.
     * 
     * @param provider the provider instance (ignored if <b>null</b>)
     */
    public void unregisterAggregationKeyProvider(AggregationKeyProvider<?> provider) {
        if (null != provider) {
            providers.remove(provider.handles());
            // clean up lazy inits
            Iterator<Map.Entry<Class<?>, AggregationKeyProvider<?>>> iter = providers.entrySet().iterator();
            while (iter.hasNext()) {
                if (iter.next().getValue() == provider) {
                    iter.remove();
                }
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
            AggregationKeyProvider<?> provider = getAggregationKeyProvider(tuple.getClass());
            if (null != provider) {
                aggregateKey(provider.getKey(tuple));
            }
        }
    }
    
    /**
     * Returns the aggregation key provider for <code>cls</code> and also considers direct super-interfaces and the
     * direct superclass if not found. If found on super types, register the new mapping lazily.
     * 
     * @param cls the class to return the provider for
     * @return the aggregation provider, <b>null</b> if there is none
     */
    private AggregationKeyProvider<?> getAggregationKeyProvider(Class<?> cls) {
        AggregationKeyProvider<?> result = providers.get(cls);
        if (null == result && !providers.isEmpty()) {
            Class<?>[] ifaces = cls.getInterfaces();
            if (null != ifaces) {
                for (int i = 0; null == result && i < ifaces.length; i++) {
                    result = providers.get(ifaces[i]);
                }
            }
            if (null == result) {
                Class<?> su = cls.getSuperclass();
                if (null != su) {
                    result = providers.get(su);
                }
            }
            if (null != result) { // lazy registration for speedup
                providers.put(cls, result);
            }
        }
        return result;
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
                    occurrences.put(key, 1); // the first one has been seen
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
            Map<String, Integer> occ = new HashMap<String, Integer>();
            synchronized (occurrences) {
                occ.putAll(occurrences);
                occurrences.clear();
            }
            SourceVolumeMonitoringEvent evt = new SourceVolumeMonitoringEvent(getNamespace(), getName(), occ);
            EventManager.send(evt);
            lastAggregation.set(now);
        }
    }
        
    @Override
    public void notifyMonitoringChange(MonitoringChangeSignal signal) {
        super.notifyMonitoringChange(signal);
        Integer tmp = signal.getFrequency(MonitoringFrequency.SOURCE_AGGREGATION);
        if (null != tmp) {
            aggregationInterval = tmp; // we just ignore the EventManager.setTimerPeriod here
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
    }
    
}
