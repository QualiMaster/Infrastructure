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
package eu.qualimaster.base.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.qualimaster.common.signal.StormSignalConnection;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.monitoring.events.SubTopologyMonitoringEvent;
import backtype.storm.topology.BoltDeclarer;
import backtype.storm.topology.ComponentConfigurationDeclarer;
import backtype.storm.topology.IBasicBolt;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.SpoutDeclarer;
import backtype.storm.topology.TopologyBuilder;

/**
 * A recording topology builder to get information about sub-structures such as sub-topologies.
 * If given, this class uses pipeline options in a transparent way. Please call 
 * {@link #close(String, Config)} at the end of (top-level) pipeline/topology creation.
 * 
 * @author Holger Eichelberger
 */
public class RecordingTopologyBuilder extends TopologyBuilder {

    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Logger.getLogger(RecordingTopologyBuilder.class);
    private String currentId;
    private Map<String, List<String>> recording = new HashMap<String, List<String>>();
    private PipelineOptions options;

    /**
     * Creates a recording topology builder without pipeline options.
     */
    public RecordingTopologyBuilder() {
    }
    
    /**
     * Creates a recording topology builder.
     * 
     * @param options the pipeline options to be used to take parallelism information from
     */
    public RecordingTopologyBuilder(PipelineOptions options) {
        this.options = options;
    }
    
    /**
     * Denotes the start of recording of sub-structures for a certain pipeline element.
     * 
     * @param id the id to record
     */
    public void startRecording(String id) {
        currentId = id; 
    }
    
    /**
     * Records a bolt/spout id if {@link #currentId} is set.
     * 
     * @param id the id to record
     * @param processor the actual processor instance
     */
    private void record(String id, Object processor) {
        if (null != currentId && null != processor) {
            List<String> tmp = recording.get(currentId);
            if (null == tmp) {
                tmp = new ArrayList<String>();
                recording.put(currentId, tmp);
            }
            tmp.add(id + SubTopologyMonitoringEvent.SEPARATOR + processor.getClass().getName());
        }
    }
    
    /**
     * Returns the task parallelism from {@link #options}.
     * 
     * @param id the executor id
     * @param dflt the default value, may be <b>null</b>
     * @return the task parallelism if known from {@link #options}, <b>null</b> else
     */
    private Number getExecutorParallelism(String id, Number dflt) {
        Number result = dflt;
        if (null != options) {
            result = options.getExecutorParallelism(id, dflt);
            if (DEBUG) { // later via LOGGER.debug
                LOGGER.info("Executors for " + id + ": " + result);
            }
        }
        return result;
    }
    
    /**
     * Defines the number of tasks for <code>declarer</code> based on {@link #options}.
     * 
     * @param <D> the type of the declarer
     * @param id the executor id
     * @param declarer the executor declarer
     * @param dflt the default value, may be <b>null</b>
     * @return the declarer
     */
    <D extends ComponentConfigurationDeclarer<?>> D setNumTasks(String id, D declarer, Number dflt) {
        Number tasks = dflt;
        if (null != options) {
            tasks = options.getTaskParallelism(id, null);
        }
        if (null != tasks) {
            if (DEBUG) { // later via LOGGER.debug
                LOGGER.info("Tasks for " + id + ": " + tasks);
            }
            declarer.setNumTasks(tasks);
        }
        return declarer;
    }

    /**
     * Defines the number of tasks for <code>declarer</code> based on {@link #options}.
     * 
     * @param id the executor id
     * @param declarer the executor declarer
     * @return the declarer, actually a {@link DelegatingBoltDeclarer} to keep the number of tasks consistent with 
     *   {@link #options}.
     */
    private BoltDeclarer setNumTasks(String id, BoltDeclarer declarer) {
        setNumTasks(id, declarer, null);
        return new MyBoltGetter(id);
    }

    /**
     * Defines the number of tasks for <code>declarer</code> based on {@link #options}.
     * 
     * @param id the executor id
     * @param declarer the executor declarer
     * @return the declarer, actually a {@link DelegatingSpoutDeclarer} to keep the number of tasks consistent with 
     *   {@link #options}.
     */
    private SpoutDeclarer setNumTasks(String id, SpoutDeclarer declarer) {
        setNumTasks(id, declarer, null);
        return new MySpoutGetter(id);
    }
    
    @Override
    public BoltDeclarer setBolt(String id, IRichBolt bolt) {
        record(id, bolt);
        return setNumTasks(id, super.setBolt(id, bolt, getExecutorParallelism(id, null)));
    }

    @Override
    public BoltDeclarer setBolt(String id, IRichBolt bolt, Number parallelism_hint) {
        record(id, bolt);
        return setNumTasks(id, super.setBolt(id, bolt, getExecutorParallelism(id, parallelism_hint)));
    }

    @Override
    public BoltDeclarer setBolt(String id, IBasicBolt bolt) {
        record(id, bolt);
        return setNumTasks(id, super.setBolt(id, bolt, getExecutorParallelism(id, null)));
    }

    @Override
    public BoltDeclarer setBolt(String id, IBasicBolt bolt, Number parallelism_hint) {
        record(id, bolt);
        return setNumTasks(id, super.setBolt(id, bolt, getExecutorParallelism(id, parallelism_hint)));
    }

    @Override
    public SpoutDeclarer setSpout(String id, IRichSpout spout) {
        record(id, spout);
        return setNumTasks(id, super.setSpout(id, spout, getExecutorParallelism(id, null)));
    }

    @Override
    public SpoutDeclarer setSpout(String id, IRichSpout spout, Number parallelism_hint) {
        record(id, spout);
        return setNumTasks(id, super.setSpout(id, spout, getExecutorParallelism(id, parallelism_hint)));
    }
    
    /**
     * Denotes the end of recording of sub-structures for a certain pipeline element.
     */
    public void endRecording() {
        currentId = null;
    }

    /**
     * Closes pipeline creation.
     * 
     * @param pipelineName the name of the currently built pipeline
     * @param config the topology configuration
     */
    public void close(String pipelineName, @SuppressWarnings("rawtypes") Map config) {
        if (!recording.isEmpty()) {
            StormSignalConnection.configureEventBus(config);
            EventManager.send(new SubTopologyMonitoringEvent(pipelineName, recording));
        }
    }

    /**
     * Bolt getter overriding methods to access {@link #options}.
     * 
     * @author Holger Eichelberger
     */
    private class MyBoltGetter extends BoltGetter {

        private String boltId;
        
        /**
         * Creates a bolt getter.
         * 
         * @param boltId the bolt id
         */
        public MyBoltGetter(String boltId) {
            super(boltId);
            this.boltId = boltId;
        }
        
        @Override
        public BoltDeclarer setNumTasks(Number val) {
            Number tasks = val;
            if (null != options) {
                tasks = options.getTaskParallelism(boltId, val);
            }
            return super.setNumTasks(tasks);
        }
        
    }

    /**
     * Spout getter overriding methods to access {@link #options}.
     * 
     * @author Holger Eichelberger
     */
    private class MySpoutGetter extends SpoutGetter {

        private String spoutId;

        /**
         * Creates a spout getter.
         * 
         * @param spoutId the bolt id
         */
        public MySpoutGetter(String spoutId) {
            super(spoutId);
            this.spoutId = spoutId;
        }
        
        @Override
        public SpoutDeclarer setNumTasks(Number val) {
            Number tasks = val;
            if (null != options) {
                tasks = options.getTaskParallelism(spoutId, val);
            }
            return super.setNumTasks(tasks);
        }
        
    }

}