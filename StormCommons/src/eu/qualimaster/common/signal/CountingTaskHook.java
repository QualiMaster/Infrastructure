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

import java.util.Map;

import backtype.storm.hooks.ITaskHook;
import backtype.storm.hooks.info.BoltAckInfo;
import backtype.storm.hooks.info.BoltExecuteInfo;
import backtype.storm.hooks.info.BoltFailInfo;
import backtype.storm.hooks.info.EmitInfo;
import backtype.storm.hooks.info.SpoutAckInfo;
import backtype.storm.hooks.info.SpoutFailInfo;
import backtype.storm.task.TopologyContext;
//import de.uni_hildesheim.sse.system.GathererFactory;
//import de.uni_hildesheim.sse.system.IMemoryDataGatherer;
import eu.qualimaster.common.monitoring.MonitoringPluginRegistry;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Implements a counting task hook.
 * 
 * @author Holger Eichelberger
 */
class CountingTaskHook implements ITaskHook, IMonitoringChangeListener {

    //private static final IMemoryDataGatherer MEMGATHERER = GathererFactory.getMemoryDataGatherer();
    private int emitCount;
    private long emitVolume;
    private boolean collectVolume = true;

    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map conf, TopologyContext context) {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void emit(EmitInfo info) {
        if (null != info && null != info.values) {
            emitCount += info.values.size();
            /*if (collectVolume) {
                emitVolume += MEMGATHERER.getObjectSize(info.values);
            }*/
            MonitoringPluginRegistry.emitted(info);
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
    void emitted(Object tuple) {
        if (null != tuple) {
            emitCount++;
            /*if (collectVolume) {
                emitVolume += MEMGATHERER.getObjectSize(tuple);
            }*/
            MonitoringPluginRegistry.emitted(tuple);
        }
    }
    
    /**
     * Returns and resets the actual emit count.
     * 
     * @return the emit count before resetting
     */
    int getAndResetEmitCount() {
        int result = emitCount;
        emitCount = 0;
        return result;
    }

    /**
     * Returns and resets the actual emit volume.
     * 
     * @return the emit volume before resetting (may be <b>-1</b> for disabled)
     */
    long getAndResetEmitVolume() {
        long result = emitVolume;
        emitVolume = 0;
        return collectVolume ? result : -1;
    }

    @Override
    public void notifyMonitoringChange(MonitoringChangeSignal signal) {
        Boolean tmp = signal.getEnabled(TimeBehavior.THROUGHPUT_VOLUME);
        if (null != tmp) {
            collectVolume = tmp;
        }
    }

}
