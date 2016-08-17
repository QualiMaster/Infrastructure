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
package eu.qualimaster.coordination;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import backtype.storm.Config;
import eu.qualimaster.PropertyReader;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent.DetailMode;
import eu.qualimaster.dataManagement.DataManagementConfiguration;

/**
 * Specific configuration options introduced by the coordination layer.
 * 
 * @author Holger Eichelberger
 */
public class CoordinationConfiguration extends DataManagementConfiguration {

    /**
     * Denotes whether pipeline artifact downloads by the infrastructure are enabled.
     */
    public static final String PIPELINE_ARTIFACT_DOWNLOAD = "repository.artifacts.pipeline.download";

    /**
     * The default value for {@link #PIPELINE_ARTIFACT_DOWNLOAD} - false due to legacy reasons.
     */
    public static final boolean DEFAULT_PIPELINE_ARTIFACT_DOWNLOAD = false; // legacy

    /**
     * Denotes the (optional - empty) local location of the artifact containing configuration model to be used as 
     * a fallback.
     */
    public static final String LOCAL_CONFIG_MODEL_ARTIFACT_LOCATION = "repository.confModel.local";

    /**
     * The default value for {@link #LOCAL_CONFIG_MODEL_ARTFIACT_LOCATION}, {@link #EMPTY_VALUE}.
     */
    public static final String DEFAULT_LOCAL_CONFIG_MODEL_ARTIFACT_LOCATION = "";

    /**
     * Denotes the (optional) local element repository (file path string).
     */
    public static final String LOCAL_ELEMENT_REPOSITORY = "repository.pipelineElements.location.local";

    /**
     * The default value for {@link #DEFAULT_LOCAL_ELEMENT_REPOSITORY} - may be <b>null</b> for unconfigured.
     */
    public static final String DEFAULT_LOCAL_ELEMENT_REPOSITORY = null;
    
    /**
     * Denotes the local artifacts location (file path string).
     */
    public static final String LOCAL_ARTIFACT_LOCATION = "repository.artifacts.location.local";

    /**
     * The default value for {@link #LOCAL_ARTIFACT_LOCATION} - the Java temporary directory.
     */
    public static final String DEFAULT_LOCAL_ARTIFACT_LOCATION = System.getProperty("java.io.tmpdir");
    
    /**
     * Denotes the location of the pipeline elements repository.
     */
    public static final String PIPELINE_ELEMENTS_REPOSITORY = "repository.pipelineElements.url";

    /**
     * The default value for {@link #PIPELINE_ELEMENTS_REPOSITORY}, {@value}.
     */
    public static final String DEFAULT_PIPELINE_ELEMENTS_REPOSITORY = "https://localhost";

    /**
     * Denotes the location of the pipeline elements repository (fallback if {@link #PIPELINE_ELEMENTS_REPOSITORY} is 
     * used for stable explicitly deployed pipelines, but profiling artifacts are still in the development 
     * repository, however).
     */
    public static final String PIPELINE_ELEMENTS_REPOSITORY_FALLBACK = "repository.pipelineElements.fallback.url";

    /**
     * The default value for {@link #PIPELINE_ELEMENTS_REPOSITORY_FALLBACK}, {@value}.
     */
    public static final String DEFAULT_PIPELINE_ELEMENTS_REPOSITORY_FALLBACK = "https://localhost";

    /**
     * Denotes the artifact specification of the configuration model.
     */
    public static final String CONFIG_MODEL_ARTIFACT_SPEC = "repository.confModel.artifact";
    
    /**
     * The default value for {@link #CONFIG_MODEL_ARTIFACT_SPEC}, {@value}.
     */
    public static final String DEFAULT_CONFIG_MODEL_ARTIFACT_SPEC = "eu.qualimaster:InfrastructureModel:0.0.1-SNAPSHOT";
    
    /**
     * Denotes the Storm command waiting time (Integer in s).
     */
    public static final String TIME_STORM = "storm.cmd.time";

    /**
     * The default value for {@link #TIME_STORM} in seconds (Value {@value}).
     */
    public static final int DEFAULT_TIME_STORM = 0;
    
    /**
     * Denotes the procedure to use for shutdown (class or logical name).
     */
    public static final String SHUTDOWN_PROCEDURE = "shutdown.procedure";
    
    /**
     * The default value for {@link #SHUTDOWN_PROCEDURE} (empty).
     */
    public static final String DEFAULT_SHUTDOWN_PROCEDURE = EMPTY_VALUE;

    /**
     * Denotes the configuration/settings to use for shutdown, depends on {@link #SHUTDOWN_PROCEDURE}.
     */
    public static final String SHUTDOWN_PROCEDURE_CONFIGURATION = "shutdown.configuration";

    /**
     * The default value for {@link #SHUTDOWN_PROCEDURE_CONFIGURATION} (empty).
     */
    public static final String DEFAULT_SHUTDOWN_PROCEDURE_CONFIGURATION = EMPTY_VALUE;
    
    /**
     * Denotes the option for enabling/disabling deletion of temporary profiling pipelines.
     */
    public static final String DELETE_PROFILING_PIPELINES = "profiles.delete";

    /**
     * The default value for {@link #DELETE_PROFILING_PIPELINES}.
     */
    public static final boolean DEFAULT_DELETE_PROFILING_PIPELINES = true;
    
    /**
     * Denotes the option for enabling/disabling tracing of detailed profiling (sub-algorithm level).
     */
    public static final String DETAILED_PROFILING = "profiles.details";

    /**
     * The default value for {@link #DETAILED_PROFILING}.
     */
    public static final DetailMode DEFAULT_DETAILED_PROFILING = DetailMode.FALSE;
    
    /**
     * The folder where pipeline setting files shall be copied after extracting the model.
     * Relative folder to {@link #getDfsPath()} or {@link #getHdfsUrl()}. 
     */
    public static final String PIPELINE_SETTINGS_LOCATION = "pipelines.settings";
    
    /**
     * The default value for {@link #PIPELINE_SETTINGS_LOCATION}.
     */
    public static final String DEFAULT_PIPELINE_SETTINGS_LOCATION = EMPTY_VALUE;
    
    /**
     * Denotes the folder where profiling data for prediction is stored.
     */
    public static final String PROFILE_LOCATION = "profiling.data.location";

    /**
     * The default value for {@link #PROFILE_LOCATION}.
     */
    public static final String DEFAULT_PROFILE_LOCATION = FileUtils.getTempDirectoryPath();

    
    static final PropertyReader<DetailMode> DETAIL_MODE_READER = new PropertyReader<DetailMode>() {

        @Override
        public DetailMode read(Properties properties, String key, DetailMode deflt) {
            DetailMode result = deflt;
            String tmp = properties.getProperty(key, deflt.name());
            if (null != tmp) {
                try {
                    result = DetailMode.valueOf(tmp.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // use deflt
                }
            }
            return result;
        }
        
    };
    
    private static ConfigurationOption<Boolean> enablePipelineArtifactDownload 
        = createBooleanOption(PIPELINE_ARTIFACT_DOWNLOAD, DEFAULT_PIPELINE_ARTIFACT_DOWNLOAD);
    private static ConfigurationOption<String> localConfigModelArtifactLocation
        = createStringOption(LOCAL_CONFIG_MODEL_ARTIFACT_LOCATION, DEFAULT_LOCAL_CONFIG_MODEL_ARTIFACT_LOCATION);
    private static ConfigurationOption<String> localPipelineElementsRepository 
        = createStringOption(LOCAL_ELEMENT_REPOSITORY, DEFAULT_LOCAL_ELEMENT_REPOSITORY);
    private static ConfigurationOption<String> localArtifactsLocation 
        = createStringOption(LOCAL_ARTIFACT_LOCATION, DEFAULT_LOCAL_ARTIFACT_LOCATION);
    private static ConfigurationOption<URL> pipelineElementsRepository 
        = createUrlOption(PIPELINE_ELEMENTS_REPOSITORY, toUrl(DEFAULT_PIPELINE_ELEMENTS_REPOSITORY));
    private static ConfigurationOption<URL> pipelineElementsRepositoryFallback 
        = createUrlOption(PIPELINE_ELEMENTS_REPOSITORY_FALLBACK, toUrl(DEFAULT_PIPELINE_ELEMENTS_REPOSITORY_FALLBACK));
    private static ConfigurationOption<String> configModelArtifactSpecification 
        = createStringOption(CONFIG_MODEL_ARTIFACT_SPEC, DEFAULT_CONFIG_MODEL_ARTIFACT_SPEC);
    private static ConfigurationOption<Integer> stormCmdTime = createIntegerOption(TIME_STORM, DEFAULT_TIME_STORM);
    private static ConfigurationOption<String> shutdownProcedure 
        = createStringOption(SHUTDOWN_PROCEDURE, DEFAULT_SHUTDOWN_PROCEDURE);
    private static ConfigurationOption<String> shutdownProcedureConfiguration 
        = createStringOption(SHUTDOWN_PROCEDURE_CONFIGURATION, DEFAULT_SHUTDOWN_PROCEDURE_CONFIGURATION);
    private static ConfigurationOption<Boolean> deleteProfilingPipelines 
        = createBooleanOption(DELETE_PROFILING_PIPELINES, DEFAULT_DELETE_PROFILING_PIPELINES);
    private static ConfigurationOption<String> pipelineSettingsLocation 
        = createStringOption(PIPELINE_SETTINGS_LOCATION, DEFAULT_PIPELINE_SETTINGS_LOCATION);
    private static ConfigurationOption<DetailMode> detailedProfiling 
        = new ConfigurationOption<DetailMode>(DETAILED_PROFILING, DEFAULT_DETAILED_PROFILING, DETAIL_MODE_READER);
    private static ConfigurationOption<String> profileLocation 
        = createStringOption(PROFILE_LOCATION, DEFAULT_PROFILE_LOCATION);

    /**
     * Reads the configuration settings from the file.
     * 
     * @param file the file to take the configuration settings from
     */
    public static void configure(File file) {
        DataManagementConfiguration.configure(file);
    }
    
    /**
     * Returns properties for default configuration (and modification).
     * 
     * @return the default properties
     */
    public static Properties getDefaultProperties() {
        return DataManagementConfiguration.getDefaultProperties();
    }
    
    /**
     * Returns properties for the actual configuration (and modification).
     * 
     * @return the actual properties
     */
    public static Properties getProperties() {
        return DataManagementConfiguration.getProperties();
    }
    
    /**
     * Creates a local configuration.
     * 
     * @see #getDefaultProperties()
     */
    public static void configureLocal() {
        DataManagementConfiguration.configureLocal();
    }

    /**
     * Reads the configuration settings from the given properties.
     * 
     * @param properties the properties to take the configuration settings from
     */
    public static void configure(Properties properties) {
        DataManagementConfiguration.configure(properties);
    }

    /**
     * Reads the configuration settings from the given properties.
     * 
     * @param properties the properties to take the configuration settings from
     * @param useDefaults use the default values if undefined or, if <code>false</code> ignore undefined properties
     */
    public static void configure(Properties properties, boolean useDefaults) {
        DataManagementConfiguration.configure(properties, useDefaults);
    }
            
    /**
     * Transfers relevant infrastructure configuration information from this configuration
     * to a Storm configuration. 
     * 
     * @param config the Storm configuration to be modified as a side effect
     * @see #transferConfigurationFrom(Map)
     */
    public static void transferConfigurationTo(Config config) {
        DataManagementConfiguration.transferConfigurationFrom(config);
    }

    /**
     * Transfers relevant parts of the Storm configuration back into the infrastructure configuration.
     * 
     * @param conf the storm configuration as map
     * @see #transferConfigurationTo(Map)
     */
    @SuppressWarnings("rawtypes")
    public static void transferConfigurationFrom(Map conf) {
        DataManagementConfiguration.transferConfigurationFrom(conf);
    }
    
    /**
     * Clears the configuration. Intended for testing to ensure a fresh state.
     */
    public static void clear() {
        DataManagementConfiguration.clear();
    }
    
    /**
     * Returns the (maximum) time waiting for the execution of storm commands.
     * 
     * @return the maximum time in seconds
     */
    public static int getStormCmdWaitingTime() {
        return stormCmdTime.getValue();
    }
    
    /**
     * Returns the optional local pipeline elements repository location as a fallback for 
     * {@link #getPipelineElementsRepository()} for manual deployment (testing).
     * 
     * @return the optional location (may be <b>null</b>)
     */
    public static String getLocalPipelineElementsRepositoryLocation() {
        return localPipelineElementsRepository.getValue();
    }

    /**
     * Returns the pipeline elements repository from where to obtain pipeline artifacts for execution.
     * 
     * @return the pipeline elements repository as URL
     */
    public static URL getPipelineElementsRepository() {
        return pipelineElementsRepository.getValue();
    }
    
    /**
     * Returns the pipeline elements fallback repository from where to obtain pipeline artifacts for execution.
     * 
     * @return the pipeline elements fallback repository as URL
     */
    public static URL getPipelineElementsRepositoryFallback() {
        return pipelineElementsRepositoryFallback.getValue();
    }
    
    /**
     * Returns the local artifacts location where to store pipeline element artifcts after obtaining 
     * them from {@link #getPipelineElementsRepository()}. May be the same as 
     * {@link #getLocalPipelineElementsRepositoryLocation()}.
     * 
     * @return the artifact location
     */
    public static String getLocalArtifactsLocation() {
        return localArtifactsLocation.getValue();
    }
    
    /**
     * Returns the configuration model artifact specification. [preliminary]
     * 
     * @return the configuration model artifact specification (may be <b>null</b>)
     */
    public static String getConfigurationModelArtifactSpecification() {
        return configModelArtifactSpecification.getValue();
    }
    
    /**
     * Returns the location of a local fallback of the config model artifact (for testing).
     * 
     * @return the location of the local fallback of the config model (may be empty if unset)
     */
    public static String getLocalConfigModelArtifactLocation() {
        return localConfigModelArtifactLocation.getValue();
    }
    
    /**
     * Returns whether automatic pipeline artifact download from the Maven repository shall be enabled.
     * This allows to use the most recent model, while still working with experimental topologies.
     * 
     * @return <code>true</code> if the pipeline artifact download is enabled, <code>false</code> else
     */
    public static boolean enablePipelineArtifactDownload() {
        return enablePipelineArtifactDownload.getValue();
    }
    
    /**
     * Returns the logical or class name of the shutdown procedure. 
     * 
     * @return the name (may be empty)
     */
    public static String getShutdownProcedure() {
        return shutdownProcedure.getValue();
    }

    /**
     * Returns the configuration/setting of the {@link #getShutdownProcedure() actual shutdown procedure}. 
     * 
     * @return the name (may be empty)
     */
    public static String getShutdownProcedureConfiguration() {
        return shutdownProcedureConfiguration.getValue();
    }
    
    /**
     * Returns whether (temporary) profiling pipelines shall be deleted after execution.
     * 
     * @return <code>true</code> for deletion (default), <code>false</code> else
     */
    public static boolean deleteProfilingPipelines() {
        return deleteProfilingPipelines.getValue();
    }
    
    /**
     * Returns the pipeline settings location where pipeline settings files shall be copied to.
     *  
     * @return the location, may be empty if no copying shall happen
     */
    public static String getPipelineSettingsLocation() {
        return pipelineSettingsLocation.getValue();
    }
    
    /**
     * Returns the detailed (sub-algorithm) profiling shall mode.
     * 
     * @return the profiling mode
     */
    public static DetailMode getProfilingMode() {
        return detailedProfiling.getValue();
    }

    /**
     * The location where the profile data is located.
     * 
     * @return the location
     */
    public static String getProfileLocation() {
        return profileLocation.getValue();
    }
    
}
