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

import eu.qualimaster.Configuration;
import eu.qualimaster.dataManagement.DataManagementConfiguration;

/**
 * Some common constants.
 * 
 * @author Holger Eichelberger
 */
public class Constants {
    
    public static final String CONFIG_KEY_STORM_ZOOKEEPER_PORT = Configuration.CONFIG_KEY_STORM_ZOOKEEPER_PORT;
    public static final String CONFIG_KEY_STORM_ZOOKEEPER_SERVERS = Configuration.CONFIG_KEY_STORM_ZOOKEEPER_SERVERS;

    public static final String CONFIG_KEY_SUBPIPELINE_NAME = "SUBPIPELINE.NAME";
    
    // requires startMonitoring()/endMonitoring() rather than aggregateExecutionTime in pipeline code
    public static final boolean MEASURE_BY_TASK_HOOKS = true;
    
    /**
     * Configuration key set by the infrastructure into the storm pipeline conf if data sources shall do autoconnects.
     */
    public static final String CONFIG_KEY_SOURCE_AUTOCONNECT = 
        DataManagementConfiguration.PIPELINE_START_SOURCE_AUTOCONNECT;
    
    /**
     * Configuration key set by the infrastructure passing the actual initialization mode.
     */    
    public static final String CONFIG_KEY_INIT_MODE = Configuration.INIT_MODE;

    /**
     * Whether the starting pipeline is the initial algorithm or a subsequent one (boolean value). 
     */
    public static final String CONFIG_KEY_INITIAL_SUBPIPELINE = "SUBPIPELINE.INITIAL";
    
}
