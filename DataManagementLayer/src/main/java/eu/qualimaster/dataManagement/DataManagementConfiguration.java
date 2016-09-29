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
     * Please note that an explicit HDFS path {@link #URL_HDFS} takes precendence
     * and uses then {@link #PATH_DFS} as base path.
     */
    public static final String PATH_DFS = "dfs.path";

    /**
     * The default value for {@link #PATH_DFS}.
     */
    public static final String DEFAULT_PATH_DFS = EMPTY_VALUE;
    
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


    private static ConfigurationOption<String> hdfsUrl = createStringOption(URL_HDFS, DEFAULT_URL_HDFS);
    private static ConfigurationOption<String> hdfsUser = createStringOption(URL_HDFS_USER, DEFAULT_URL_HDFS_USER);
    private static ConfigurationOption<String> hdfsGroupMapping = createStringOption(URL_HDFS_GROUPMAPPING, 
        DEFAULT_URL_HDFS_GROUPMAPPING);
    private static ConfigurationOption<String> dfsPath = createStringOption(PATH_DFS, DEFAULT_PATH_DFS);
    private static ConfigurationOption<String> accountsPath = createStringOption(PATH_ACCOUNTS, DEFAULT_PATH_ACCOUNTS);
    private static ConfigurationOption<Boolean> autoConnect = createBooleanOption(
        PIPELINE_START_SOURCE_AUTOCONNECT, DEFAULT_PIPELINE_START_SOURCE_AUTOCONNECT);
    private static ConfigurationOption<Integer> pipelineStartDelay 
        = createIntegerOption(PIPELINE_START_DELAY, DEFAULT_PIPELINE_START_DELAY);

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
     * to a Storm configuration. 
     * 
     * @param config the Storm configuration to be modified as a side effect
     * @see #transferConfigurationFrom(Map)
     */
    @SuppressWarnings("rawtypes")
    public static void transferConfigurationTo(Map config) {
        Configuration.transferConfigurationFrom(config);
    }

    /**
     * Transfers relevant parts of the Storm configuration back into the infrastructure configuration.
     * 
     * @param conf the storm configuration as map
     * @see #transferConfigurationTo(Map)
     */
    @SuppressWarnings("rawtypes")
    public static void transferConfigurationFrom(Map conf) {
        Configuration.transferConfigurationFrom(conf);
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

}
