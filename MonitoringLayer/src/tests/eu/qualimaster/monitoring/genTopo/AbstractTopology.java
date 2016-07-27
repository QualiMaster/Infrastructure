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
package tests.eu.qualimaster.monitoring.genTopo;

import java.util.Map;

import org.junit.Assert;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;

/**
 * Reusable topology base class.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractTopology {

    /**
     * Creates the topology.
     * 
     * @param config the pipeline configuration
     * @param builder the topology builder
     */
    public void createTopology(@SuppressWarnings("rawtypes") Map config, RecordingTopologyBuilder builder) {
        Config cfg = new Config();
        for (Object o : config.entrySet()) {
            if (o instanceof Map.Entry) {
                @SuppressWarnings("rawtypes")
                Map.Entry ent = (Map.Entry) o;
                cfg.put(ent.getKey().toString(), ent.getValue());
            }
        }
        createTopology(cfg, builder);
    }
    
    /**
     * Creates the topology.
     * 
     * @param config the pipeline configuration
     * @param builder the topology builder
     */
    public abstract void createTopology(Config config, RecordingTopologyBuilder builder);

    /**
     * Returns the name of the topology / pipeline.
     * 
     * @return the name
     */
    public abstract String getName();
    
    /**
     * Asserts the results of the test.
     * 
     * @param state the (preserved) system state to assert
     * @param mapping the (preserved) name mapping
     * @param pipRunTime the (rough) runtime in ms of the executed pipeline
     */
    public abstract void assertState(SystemState state, INameMapping mapping, long pipRunTime);
    
    // checkstyle: stop exception type check

    /**
     * Creates a standalone topology.
     * 
     * @param args the topology arguments
     * @param topo the topology instance
     * @throws Exception in case of creation problems
     */
    public static void main(String[] args, AbstractTopology topo) throws Exception {
        Config config = new Config();
        config.setMessageTimeoutSecs(100);
        PipelineOptions options = new PipelineOptions(args);
        RecordingTopologyBuilder b = new RecordingTopologyBuilder(options);
        topo.createTopology(config, b);
        
        // main topology: int numWorkers = options.getNumberOfWorkers(2);
        options.toConf(config);
        
        if (args != null && args.length > 0) {
            config.setNumWorkers(2);
            StormSubmitter.submitTopology(args[0], config, b.createTopology());
        } else {
            config.setMaxTaskParallelism(2);
            final LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("testGenPip", config, b.createTopology());
        }
    }

    // checkstyle: resume exception type check

    /**
     * Returns the mapping file name.
     * 
     * @return the mapping file
     */
    public abstract String getMappingFileName();

    /**
     * Asserts that the observed value is greater than <code>expected</code>.
     * 
     * @param expected the expected value
     * @param part the part to get the actual value from
     * @param obs the observable denoting the actual value
     */
    protected static void assertGreater(double expected, PipelineNodeSystemPart part, IObservable obs) {
        Assert.assertTrue("no value for " + obs + " on " + part.getName(), part.hasValue(obs));
        Assert.assertTrue("not greater ", expected < part.getObservedValue(obs));
    }
    
}
