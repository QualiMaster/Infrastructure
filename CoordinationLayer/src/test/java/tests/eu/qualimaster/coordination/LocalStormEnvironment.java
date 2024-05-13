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
package tests.eu.qualimaster.coordination;

import java.io.File;
import java.util.Map;
import java.util.Set;

import eu.qualimaster.common.signal.ThriftConnection;
import eu.qualimaster.coordination.StormUtils;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import backtype.storm.LocalCluster;

/**
 * A test environment wrapping a Storm local cluster. This class provides useful helper methods
 * and allows cleaning up the temporary directories that a Storm local cluster does not clean up.
 * It fully encapsulates a local Storm environment and configures the QM infrastructure for testing. 
 * An instance is intended for single use in a test.
 * 
 * @author Holger Eichelberger
 */
public class LocalStormEnvironment {

    public static final int WAIT_AT_END = 8000;
    private Set<File> tmpFiles;
    private LocalCluster cluster;
    
    /**
     * Creates a test environment.
     */
    public LocalStormEnvironment() {
        tmpFiles = Utils.trackTemp(null, false);
        cluster = new LocalCluster();
        ThriftConnection.setLocalCluster(cluster);
    }
    
    /**
     * Sets the topologies used in testing.
     * 
     * @param topologies the topologies
     */
    public void setTopologies(Map<String, TopologyTestInfo> topologies) {
        StormUtils.forTesting(cluster, topologies);
    }
    
    /**
     * Shuts down the test environment but does not finally clean
     * the temporary directory in order to support assertions on
     * the temporary data. Call {@link #cleanup()} afterwards.
     */
    public void shutdown() {
        //http://stackoverflow.com/questions/19151897/connection-refused-error-in-storm
        AbstractCoordinationTests.sleep(3000); // wait for zookeeper
        cluster.shutdown();
        AbstractCoordinationTests.sleep(WAIT_AT_END); // wait for shutting down services
        ThriftConnection.setLocalCluster(null);
    }
    
    /**
     * Cleans up temporary files as well as settings done in the infrastructure.
     */
    public void cleanup() {
        StormUtils.forTesting(null, null);
        ThriftConnection.setLocalCluster(null);
        Utils.trackTemp(tmpFiles, true);
        tmpFiles = null;
    }
    
}
