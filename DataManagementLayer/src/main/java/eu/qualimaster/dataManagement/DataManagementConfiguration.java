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
package eu.qualimaster.dataManagement;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import eu.qualimaster.Configuration;
import eu.qualimaster.IOptionSetter;

/**
 * Specific configuration options introduced by the data management layer.
 * 
 * @author Holger Eichelberger
 */
public class DataManagementConfiguration extends Configuration {

    /**
     * Denotes the default HDFS URL (String).
     */
    public static final String URL_HDFS = "hdfs.url";

    /**
     * The default value for {@link #URL_HDFS}.
     */
    public static final String DEFAULT_URL_HDFS = EMPTY_VALUE;
    
    /**
     * Denotes the default HDFS user (String).
     */
    public static final String URL_HDFS_USER = "hdfs.user";

    /**
     * The default value for {@link #URL_HDFS_USER}.
     */
    public static final String DEFAULT_URL_HDFS_USER = EMPTY_VALUE;

    /**
     * Denotes the default HDFS user (String, user=group comma separated for multiple groups, semicolon 
     * separated for user/group combinations).
     */
    public static final String URL_HDFS_GROUPMAPPING = "hdfs.groupMapping";

    /**
     * The default value for {@link #URL_HDFS_GROUPMAPPING}.
     */
    public static final String DEFAULT_URL_HDFS_GROUPMAPPING = EMPTY_VALUE;
    
    /**
     * Denotes the default distributed file system (DFS) path (String).
     */
    public static final String PATH_DFS = "dfs.path";

    /**
     * The default value for {@link #PATH_DFS}.
     */
    public static final String DEFAULT_PATH_DFS = EMPTY_VALUE;

    /**
     * Denotes the base path within HDFS.
     */
    public static final String PATH_HDFS = "hdfs.path";

    /**
     * The default value for {@link #PATH_HDFS}.
     */
    public static final String DEFAULT_PATH_HDFS = EMPTY_VALUE;
    
    /**
     * Denotes the directory for the accounts file. If not given, uses {@link #PATH_DFS} as base path.
     */
    public static final String PATH_ACCOUNTS = "accounts.path";

    /**
     * The default value for {@link #PATH_ACCOUNTS}. Uses {@link #PATH_DFS} if not given explicity.
     */
    public static final String DEFAULT_PATH_ACCOUNTS = EMPTY_VALUE;
    
    /**
     * Returns the pipeline startup delay from initialized to started.
     */
    public static final String PIPELINE_START_DELAY = "pipeline.start.delay.time";
    
    /**
     * The default value for {@link #PIPELINE_START_DELAY}, {@value}.
     */
    public static final int DEFAULT_PIPELINE_START_DELAY = 2000;

    /**
     * Returns the pipeline startup delay from initialized to started.
     */
    public static final String PIPELINE_START_SOURCE_AUTOCONNECT = "pipeline.start.source.autoconnect";
    
    /**
     * The default value for {@link #PIPELINE_START_SOURCE_AUTOCONNECT}, {@value}.
     */
    public static final boolean DEFAULT_PIPELINE_START_SOURCE_AUTOCONNECT = false;

    /**
     * Whether simulated data shall use HDFS for reading input. If false, consider first {@link #SIMULATION_LOCAL_PATH},
     * if not given {@link #PATH_DFS}.
     */
    public static final String SIMULATION_USE_HDFS = "simulation.useHdfs";
    
    /**
     * The default value for {@link #SIMULATION_USE_HDFS}, {@value}.
     */
    public static final boolean DEFAULT_SIMULATION_USE_HDFS = true;

    /**
     * The local path containing simulated data. If not given, consider {@link #PATH_DFS}.
     */
    public static final String SIMULATION_LOCAL_PATH = "simulation.localPath";

    /**
     * The default value for {@link #SIMULATION_LOCAL_PATH}, {@value}.
     */
    public static final String DEFAULT_SIMULATION_LOCAL_PATH = "";
    
    /**
     * The local path containing information about the external service. If not given, consider {@link #PATH_DFS}.
     */
    public static final String EXTERNAL_SERVICE_PATH = "externalService.path";

    /**
     * The default value for {@link #EXTERNAL_SERVICE_PATH}, {@value}.
     */
    public static final String DEFAULT_EXTERNAL_SERVICE_PATH = "";
    
    /**
     * Whether the external service is reachable only via tunneling.
     */
    public static final String EXTERNAL_SERVICE_TUNNELING = "externalService.tunneling";

    /**
     * The default value for {@link #EXTERNAL_SERVICE_PATH}, {@value}.
     */
    public static final boolean DEFAULT_EXTERNAL_SERVICE_TUNNELING = false;
    
    /**
     * Defines the quorum zookeepers for hbase (comma-separated network names).
     */
    public static final String HBASE_ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";

    /**
     * The default value for {@link #HBASE_ZOOKEEPER_QUORUM}, {@value}.
     */
    public static final String DEFAULT_HBASE_ZOOKEEPER_QUORUM = "";
    
    /**
     * Defines the parent path for hbase on the quorum servers.
     */
    public static final String HBASE_ZNODE_PARENT = "hbase.znode.parent";

    /**
     * The default value for {@link #HBASE_ZNODE_PARENT}, {@value}.
     */
    public static final String DEFAULT_HBASE_ZNODE_PARENT = "";
    
    private static ConfigurationOption<String> hdfsUrl = createStringOption(URL_HDFS, DEFAULT_URL_HDFS);
    private static ConfigurationOption<String> hdfsUser = createStringOption(URL_HDFS_USER, DEFAULT_URL_HDFS_USER);
    private static ConfigurationOption<String> hdfsGroupMapping = createStringOption(URL_HDFS_GROUPMAPPING, 
        DEFAULT_URL_HDFS_GROUPMAPPING);
    private static ConfigurationOption<String> dfsPath = createStringOption(PATH_DFS, DEFAULT_PATH_DFS);
    private static ConfigurationOption<String> hdfsPath = createStringOption(PATH_HDFS, DEFAULT_PATH_HDFS);
    private static ConfigurationOption<String> accountsPath = createStringOption(PATH_ACCOUNTS, DEFAULT_PATH_ACCOUNTS);
    private static ConfigurationOption<Boolean> autoConnect = createBooleanOption(
        PIPELINE_START_SOURCE_AUTOCONNECT, DEFAULT_PIPELINE_START_SOURCE_AUTOCONNECT);
    private static ConfigurationOption<Integer> pipelineStartDelay 
        = createIntegerOption(PIPELINE_START_DELAY, DEFAULT_PIPELINE_START_DELAY);
    private static ConfigurationOption<Boolean> simulationHdfs 
        = createBooleanOption(SIMULATION_USE_HDFS, DEFAULT_SIMULATION_USE_HDFS);
    private static ConfigurationOption<String> simulationLocalPath 
        = createStringOption(SIMULATION_LOCAL_PATH, DEFAULT_SIMULATION_LOCAL_PATH);
    private static ConfigurationOption<String> externalServicePath 
        = createStringOption(EXTERNAL_SERVICE_PATH, DEFAULT_EXTERNAL_SERVICE_PATH);
    private static ConfigurationOption<Boolean> externalServiceTunneling 
        = createBooleanOption(EXTERNAL_SERVICE_TUNNELING, DEFAULT_EXTERNAL_SERVICE_TUNNELING);
    private static ConfigurationOption<String> hbaseZkeeperQuorum 
        = createStringOption(HBASE_ZOOKEEPER_QUORUM, DEFAULT_HBASE_ZOOKEEPER_QUORUM);
    private static ConfigurationOption<String> hbaseZnodeParent
        = createStringOption(HBASE_ZNODE_PARENT, DEFAULT_HBASE_ZNODE_PARENT);
    
    /**
     * Reads the configuration settings from the file.
     * 
     * @param file the file to take the configuration settings from
     */
    public static void configure(File file) {
        Configuration.configure(file);
    }
    
    /**
     * Returns properties for default configuration (and modification).
     * 
     * @return the default properties
     */
    public static Properties getDefaultProperties() {
        return Configuration.getDefaultProperties();
    }
    
    /**
     * Returns properties for the actual configuration (and modification).
     * 
     * @return the actual properties
     */
    public static Properties getProperties() {
        return Configuration.getProperties();
    }
    
    /**
     * Creates a local configuration.
     * 
     * @see #getDefaultProperties()
     */
    public static void configureLocal() {
        Configuration.configureLocal();
    }

    /**
     * Reads the configuration settings from the given properties.
     * 
     * @param properties the properties to take the configuration settings from
     */
    public static void configure(Properties properties) {
        Configuration.configure(properties);
    }

    /**
     * Reads the configuration settings from the given properties.
     * 
     * @param properties the properties to take the configuration settings from
     * @param useDefaults use the default values if undefined or, if <code>false</code> ignore undefined properties
     */
    public static void configure(Properties properties, boolean useDefaults) {
        Configuration.configure(properties, useDefaults);
    }
            
    /**
     * Transfers relevant infrastructure configuration information from this configuration
     * to an options object. 
     * 
     * @param options the options object to be modified as a side effect
     * @see #transferConfigurationFrom(Map)
     */
    public static void transferConfigurationTo(IOptionSetter options) {
        Configuration.transferConfigurationTo(options);
        options.setOption(PATH_DFS, getDfsPath());
        options.setOption(URL_HDFS, getHdfsUrl());
        options.setOption(PATH_HDFS, getHdfsPath());
        options.setOption(SIMULATION_LOCAL_PATH, getSimulationLocalPath());
        options.setOption(SIMULATION_USE_HDFS, useSimulationHdfs());
        options.setOption(PATH_ACCOUNTS, getAccountsPath());
        options.setOption(EXTERNAL_SERVICE_PATH, getExternalServicePath());
        options.setOption(EXTERNAL_SERVICE_TUNNELING, getExternalServicePath());
        options.setOption(HBASE_ZNODE_PARENT, getHbaseZnodeParent());
        options.setOption(HBASE_ZOOKEEPER_QUORUM, getHbaseZkeeperQuorum());
    }

    /**
     * Transfers relevant parts of the Storm configuration back into the infrastructure configuration.
     * 
     * @param conf the storm configuration as map
     * @see #transferConfigurationTo(Map)
     */
    @SuppressWarnings("rawtypes")
    public static void transferConfigurationFrom(Map conf) {
        Properties prop = new Properties();
        transfer(conf, prop, PATH_DFS, false);
        transfer(conf, prop, SIMULATION_USE_HDFS, false);
        transfer(conf, prop, SIMULATION_LOCAL_PATH, false);
        transfer(conf, prop, URL_HDFS, false);
        transfer(conf, prop, PATH_HDFS, false);
        transfer(conf, prop, PATH_ACCOUNTS, false);
        transfer(conf, prop, EXTERNAL_SERVICE_PATH, false);
        transfer(conf, prop, EXTERNAL_SERVICE_TUNNELING, false);
        transfer(conf, prop, HBASE_ZNODE_PARENT, false);
        transfer(conf, prop, HBASE_ZOOKEEPER_QUORUM, false);
        transferConfigurationFrom(conf, prop);
    }
    
    /**
     * Clears the configuration. Intended for testing to ensure a fresh state.
     */
    public static void clear() {
        Configuration.clear();
    }
    
    /**
     * Returns the default HDFS URL.
     * 
     * @return the default HDFS URL ({@link #EMPTY_VALUE} if not configured)
     */
    public static String getHdfsUrl() {
        return hdfsUrl.getValue();
    }

    /**
     * Returns the default DFS path.
     * 
     * @return the default DFS path ({@link #EMPTY_VALUE} if not configured)
     */
    public static String getDfsPath() {
        return dfsPath.getValue();
    }
    
    /**
     * Returns the default HDFS path.
     * 
     * @return the default HDFS path
     */
    public static String getHdfsPath() {
        return hdfsPath.getValue();
    }
    
    /**
     * Returns the default path for the accounts file. If not configured, resorts to {@link #getDfsPath()}.
     * 
     * @return the DFS path if not configured
     */
    public static String getAccountsPath() {
        String path = accountsPath.getValue();
        if (isEmpty(path)) {
            path = getDfsPath();
        }
        return path;
    }
    
    /**
     * Returns the pipeline startup delay from initialized to started.
     * 
     * @return the delay
     */
    public static int getPipelineStartNotificationDelay() {
        return pipelineStartDelay.getValue();
    }
    
    /**
     * Returns whether data sources shall do auto-connect at startup.
     * 
     * @return <code>true</code> for auto-connect, <code>false</code> else
     */
    public static boolean getPipelineStartSourceAutoconnect() {
        return autoConnect.getValue();
    }

    /**
     * Returns the HDFS user.
     * 
     * @return the HDFS use (may be empty if none is defined, may relate to {@link #getHdfsGroupMapping()})
     */
    public static String getHdfsUser() {
        return hdfsUser.getValue(); //"storm"
    }

    /**
     * Returns the HDFS group mapping.
     * 
     * @return the HDFS group mapping (may be empty if none is defined)
     */
    public static String getHdfsGroupMapping() {
        return hdfsGroupMapping.getValue();       //"storm=hdfs" 
    }

    /**
     * In data simulations, use HDFS or {@link #getSimulationLocalPath()}.
     * 
     * @return <code>true</code> for HDFS, <code>false</code> for simulation
     */
    public static boolean useSimulationHdfs() {
        return simulationHdfs.getValue();
    }
    
    /**
     * Returns the local path for data simulations. Defaults to {@link #getDfsPath()} if not defined.
     * 
     * @return the local path for data simulations
     */
    public static String getSimulationLocalPath() {
        String result = simulationLocalPath.getValue();
        if (isEmpty(result)) {
            result = getDfsPath();
        }
        return result;
    }

    /**
     * Returns the path to files used by the external service performing communication with data consuming applications.
     * Defaults to {@link #getDfsPath()} if not defined.
     * 
     * @return the path to the external service files
     */
    public static String getExternalServicePath() {
        String result = externalServicePath.getValue();
        if (isEmpty(result)) {
            result = getDfsPath();
        }
        return result;
    }
    
    /**
     * Returns whether the external service(s) of a pipeline are reachable by tunneling only.
     * 
     * @return <code>true</code> for tunneling, <code>false</code> else
     */
    public static boolean getExternalServiceTunneling() {
        return externalServiceTunneling.getValue();
    }

    /**
     * Returns the Zookeeper quorum for HBase.
     * 
     * @return the Zookeeper quorum as comma separated network names/ips (may be empty if not configured)
     */
    public static String getHbaseZkeeperQuorum() {
        return hbaseZkeeperQuorum.getValue();
    }

    /**
     * Returns the Znode parent for HBase.
     * 
     * @return the Znode parent as path (may be empty if not configured)
     */
    public static String getHbaseZnodeParent() {
        return hbaseZnodeParent.getValue();
    }

}
