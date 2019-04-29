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
package eu.qualimaster.monitoring;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import eu.qualimaster.IOptionSetter;
import eu.qualimaster.coordination.CoordinationConfiguration;
import net.ssehub.easy.reasoning.core.frontend.ReasonerAdapter;

/**
 * Specific configuration options introduced by the monitoring layer.
 * 
 * @author Holger Eichelberger
 */
public class MonitoringConfiguration extends CoordinationConfiguration {

    /**
     * Denotes the adaptations to be disabled, i.e., (simple) observable names of adaptation
     * opportunities that are not considered to be stable and shall not be executed in a stable environment. Multiple 
     * names can be given separated by commas.
     */
    public static final String MONITORING_ANALYSIS_DISABLED = "monitoring.analysis.disabled";

    /**
     * The default value for {@link #MONITORING_ANALYSIS_DISABLED}, {@link #EMPTY_VALUE}.
     */
    public static final String DEFAULT_MONITORING_ANALYSIS_DISABLED = EMPTY_VALUE;

    /**
     * Denotes the minimum difference between two deviations in percent in analysis to cause re-sending an adaptation 
     * event.
     */
    public static final String MONITORING_ANALYSIS_MINDEVDIFF = "monitoring.analysis.minDeviationDifference";

    /**
     * The default value for {@link #MONITORING_ANALYSIS_MINDEVDIFF}, ({@value}).
     */
    public static final Integer DEFAULT_MONITORING_ANALYSIS_MINDEVDIFF = 5;
    
    /**
     * Denotes a filter for hardware resources, i.e., included names / network addresses will considered to be absent 
     * in this instance of the infrastructure. Multiple names can be given separated by commas. * (also as part of a 
     * list) disables all observables.
     */
    public static final String MONITORING_HW_FILTER = "monitoring.hardware.filter";

    /**
     * The default value for {@link #MONITORING_HW_FILTER}, {@link #EMPTY_VALUE}.
     */
    public static final String DEFAULT_MONITORING_HW_FILTER = EMPTY_VALUE;

    /**
     * Denotes the (optional - empty) local location where the monitoring layer may store logs.
     */
    public static final String MONITORING_LOG_LOCATION = "monitoring.log.location";

    /**
     * The default value for {@link #MONITORING_LOG_LOCATION}, {@link #EMPTY_VALUE}.
     */
    public static final String DEFAULT_MONITORING_LOG_LOCATION = EMPTY_VALUE;

    /**
     * Denotes the (optional - empty) local location where the monitoring layer shall store  
     * algorithm profiling data. If not given, use {@link #MONITORING_LOG_LOCATION}, if not
     * given use the temp folder
     */
    public static final String PROFILING_LOG_LOCATION = "profiling.log.location";

    /**
     * The default value for {@link #PROFILING_LOG_LOCATION}, {@link #EMPTY_VALUE}.
     */
    public static final String DEFAULT_PROFILING_LOG_LOCATION = EMPTY_VALUE;

    /**
     * Denotes the (optional - empty) local location where the monitoring layer may store infrastructure logs.
     */
    public static final String MONITORING_LOG_INFRA_LOCATION = "monitoring.logInfra.location";

    /**
     * The default value for {@link #MONITORING_LOG_INFRA_LOCATION}, {@link #EMPTY_VALUE}.
     */
    public static final String DEFAULT_MONITORING_LOG_INFRA_LOCATION = EMPTY_VALUE;

    /**
     * Denotes the monitoring frequency for resource monitoring on compute nodes.
     */
    public static final String MONITORING_NODE_FREQUENCY = "monitoring.node.frequency";

    /**
     * The default value for {@link #MONITORING_NODE_FREQUENCY}, {@value #DEFAULT_MONITORING_NODE_FREQUENCY}.
     */
    public static final int DEFAULT_MONITORING_NODE_FREQUENCY = 1000;
    
    /**
     * Denotes the (initial) cluster monitoring frequency (Integer in ms, lower bound defined by Monitoring Layer).
     */
    public static final String FREQUENCY_MONITORING_CLUSTER = "pipeline.monitoring.cluster";

    /**
     * The default value for {@link #FREQUENCY_MONITORING_CLUSTER} in milliseconds.
     */
    public static final int DEFAULT_FREQUENCY_MONITORING_CLUSTER = 1000;

    /**
     * Denotes the (initial) pipeline monitoring frequency (Integer in ms, lower bound defined by Monitoring Layer).
     */
    public static final String FREQUENCY_MONITORING_PIPELINE = "pipeline.monitoring.frequency";
    
    /**
     * The default value for {@link #FREQUENCY_MONITORING_PIPELINE} in milliseconds.
     */
    public static final int DEFAULT_FREQUENCY_MONITORING_PIPELINE = 1000;
    
    /**
     * Denotes whether profiling shall perform approximation of unknown data points.
     */
    public static final String PROFILE_APPROXIMATE = "profiling.approximation";

    /**
     * The default value for {@link #PROFILE_APPROXIMATE} ({@value}).
     */
    public static final boolean DEFAULT_PROFILE_APPROXIMATE = true;

    /**
     * Denotes the maximum time that we wait for accepting that an existing executor has been created and is 
     * active (Integer in s). The actual time can be faster due to sending startup events (executor, task observation).
     * 0 or negative values switch off the detection heuristic. Disabled in ADAPTIVE startup mode.
     */
    public static final String TIME_STORM_EXECUTOR_STARTUP = "storm.executor.startup.time";

    /**
     * The default value for {@link #TIME_STORM_EXECUTOR_STARTUP} in seconds (Value {@value}).
     */
    public static final int DEFAULT_TIME_STORM_EXECUTOR_STARTUP = 1;

    /**
     * Denotes whether parallel executors shall be considered during startup (longer startup time, but correct) or 
     * shall be ignored.
     */
    public static final String PARALLEL_EXECUTOR_STARTUP = "storm.executor.startup.parallel";

    /**
     * The default value for {@link #PARALLEL_EXECUTOR_STARTUP} (Value {@value}).
     */
    public static final boolean DEFAULT_PARALLEL_EXECUTOR_STARTUP = true;
    
    /**
     * Enables debugging of the thrift-based monitoring. [temporary]
     */
    public static final String THRIFT_MONITORING_DEBUG = "thrift.monitoring.debug";

    /**
     * The default value for {@link #THRIFT_MONITORING_DEBUG} (Value {@value}).
     */
    public static final boolean DEFAULT_THRIFT_MONITORING_DEBUG = false;

    /**
     * Denotes the folder where profiling data for prediction is stored.
     */
    public static final String VOLUME_MODEL_LOCATION = "volumePrediction.data.location";

    /**
     * The default value for {@link #VOLUME_MODEL_LOCATION} (temp).
     */
    public static final String DEFAULT_VOLUME_MODEL_LOCATION = FileUtils.getTempDirectoryPath();

    /**
     * Denotes the time to live for a profile without updates.
     */
    public static final String PROFILE_TTL = "profiling.ttl";
    
    /**
     * The default time to live for a profile without updates ({@value}).
     */
    public static final int DEFAULT_PROFILE_TTL = 10 * 60 * 1000;

    /**
     * Denotes whether profiling is enabled at all.
     */
    public static final String PROFILE_ENABLED = "profiling.enabled";

    /**
     * The default value for {@link #PROFILE_APPROXIMATE} ({@value}).
     */
    public static final boolean DEFAULT_PROFILE_ENABLED = true;

    /**
     * Denotes whether instance-based reasoning is enabled at all.
     */
    public static final String REASONING_INSTANCE_BASED = "reasoning.instance-based";

    /**
     * The default value for {@link #REASONING_INSTANCE_BASED} ({@value}).
     */
    public static final boolean DEFAULT_REASONING_INSTANCE_BASED = false;

    
    private static ConfigurationOption<String> monitoringAnalysisDisabled
        = createStringOption(MONITORING_ANALYSIS_DISABLED, DEFAULT_MONITORING_ANALYSIS_DISABLED);
    private static ConfigurationOption<Integer> monitoringAnalysisMinDevDifference
        = createIntegerOption(MONITORING_ANALYSIS_MINDEVDIFF, DEFAULT_MONITORING_ANALYSIS_MINDEVDIFF);
    private static ConfigurationOption<String> monitoringHardwareFilter 
        = createStringOption(MONITORING_HW_FILTER, DEFAULT_MONITORING_HW_FILTER);
    private static ConfigurationOption<String> monitoringLogLocation 
        = createStringOption(MONITORING_LOG_LOCATION, DEFAULT_MONITORING_LOG_LOCATION);
    private static ConfigurationOption<String> profilingLogLocation 
        = createStringOption(PROFILING_LOG_LOCATION, DEFAULT_PROFILING_LOG_LOCATION);
    private static ConfigurationOption<Integer> monitoringNodeFrequency
        = createIntegerOption(MONITORING_NODE_FREQUENCY, DEFAULT_MONITORING_NODE_FREQUENCY);
    private static ConfigurationOption<String> monitoringLogInfraLocation 
        = createStringOption(MONITORING_LOG_INFRA_LOCATION, DEFAULT_MONITORING_LOG_INFRA_LOCATION);
    private static ConfigurationOption<Integer> clusterMonitoringFrequency 
        = createIntegerOption(FREQUENCY_MONITORING_CLUSTER, DEFAULT_FREQUENCY_MONITORING_CLUSTER);
    private static ConfigurationOption<Integer> pipelineMonitoringFrequency 
        = createIntegerOption(FREQUENCY_MONITORING_PIPELINE, DEFAULT_FREQUENCY_MONITORING_PIPELINE);
    private static ConfigurationOption<Integer> stormExecutorStartupWaitingTime
        = createIntegerOption(TIME_STORM_EXECUTOR_STARTUP, DEFAULT_TIME_STORM_EXECUTOR_STARTUP);
    private static ConfigurationOption<Boolean> stormExecutorStartupParallel
        = createBooleanOption(PARALLEL_EXECUTOR_STARTUP, DEFAULT_PARALLEL_EXECUTOR_STARTUP);
    private static ConfigurationOption<Boolean> debugThriftMonitoring
        = createBooleanOption(THRIFT_MONITORING_DEBUG, DEFAULT_THRIFT_MONITORING_DEBUG);
    private static ConfigurationOption<String> volumeModelLocation 
        = createStringOption(VOLUME_MODEL_LOCATION, DEFAULT_VOLUME_MODEL_LOCATION);
    private static ConfigurationOption<Boolean> profileApproximate
        = createBooleanOption(PROFILE_APPROXIMATE, DEFAULT_PROFILE_APPROXIMATE);
    private static ConfigurationOption<Boolean> profileEnable
        = createBooleanOption(PROFILE_ENABLED, DEFAULT_PROFILE_ENABLED);
    private static ConfigurationOption<Integer> profileTtl
        = createIntegerOption(PROFILE_TTL, DEFAULT_PROFILE_TTL);
    private static ConfigurationOption<Boolean> reasoningInstanceBased
        = createBooleanOption(REASONING_INSTANCE_BASED, DEFAULT_REASONING_INSTANCE_BASED);
    
    /**
     * Reads the configuration settings from the file.
     * 
     * @param file the file to take the configuration settings from
     */
    public static void configure(File file) {
        CoordinationConfiguration.configure(file);
    }
    
    /**
     * Returns properties for default configuration (and modification).
     * 
     * @return the default properties
     */
    public static Properties getDefaultProperties() {
        return CoordinationConfiguration.getDefaultProperties();
    }
    
    /**
     * Returns properties for the actual configuration (and modification).
     * 
     * @return the actual properties
     */
    public static Properties getProperties() {
        return CoordinationConfiguration.getProperties();
    }
    
    /**
     * Creates a local configuration.
     * 
     * @see #getDefaultProperties()
     */
    public static void configureLocal() {
        CoordinationConfiguration.configureLocal();
    }

    /**
     * Reads the configuration settings from the given properties.
     * 
     * @param properties the properties to take the configuration settings from
     */
    public static void configure(Properties properties) {
        CoordinationConfiguration.configure(properties);
    }

    /**
     * Reads the configuration settings from the given properties.
     * 
     * @param properties the properties to take the configuration settings from
     * @param useDefaults use the default values if undefined or, if <code>false</code> ignore undefined properties
     */
    public static void configure(Properties properties, boolean useDefaults) {
        CoordinationConfiguration.configure(properties, useDefaults);
    }
            
    /**
     * Transfers relevant infrastructure configuration information from this configuration
     * to an options object. 
     * 
     * @param options the options object to be modified as a side effect
     * @see #transferConfigurationFrom(Map)
     */
    public static void transferConfigurationTo(IOptionSetter options) {
        CoordinationConfiguration.transferConfigurationTo(options);
    }

    /**
     * Transfers relevant parts of the Storm configuration back into the infrastructure configuration.
     * 
     * @param conf the storm configuration as map
     * @see #transferConfigurationTo(Map)
     */
    @SuppressWarnings("rawtypes")
    public static void transferConfigurationFrom(Map conf) {
        CoordinationConfiguration.transferConfigurationFrom(conf);
    }
    
    /**
     * Clears the configuration. Intended for testing to ensure a fresh state.
     */
    public static void clear() {
        CoordinationConfiguration.clear();
    }
    
    /**
     * Returns the observables disabled in monitoring analysis. Analysis events generated for mentioned resources will 
     * be suppressed from adaptation.
     * 
     * @return the names / network addresses to filter out, may be empty
     */
    public static Set<String> getMonitoringAnalysisDisabled() {
        return toSet(monitoringAnalysisDisabled.getValue());
    }

    /**
     * Returns the profiling log location. Just used if an
     * algorithm is being profiled. If not given, use {@link #getMonitoringLogLocation()} else 
     * temporary directory.
     * 
     * @return the profiling log location
     */
    public static String getProfilingLogLocation() {
        String result = profilingLogLocation.getValue();
        if (isEmpty(result)) {
            result = getMonitoringLogLocation();
            if (isEmpty(result)) {
                result = FileUtils.getTempDirectoryPath();
            }
        }
        return result;
    }
    
    /**
     * Returns the monitoring log location, may be {@link #EMPTY_VALUE}.
     * 
     * @return the monitoring log location, may be {@link #EMPTY_VALUE}
     */
    public static String getMonitoringLogLocation() {
        return monitoringLogLocation.getValue();
    }

    /**
     * Returns the monitoring frequency for resource monitoring on compute nodes.
     * 
     * @return the monitoring frequency
     */
    public static int getMonitoringNodeFrequency() {
        return monitoringNodeFrequency.getValue();
    }
    
    /**
     * Returns the monitoring log location, may be {@link #EMPTY_VALUE}.
     * 
     * @return the monitoring log location, may be {@link #EMPTY_VALUE}
     */
    public static String getMonitoringLogInfraLocation() {
        return monitoringLogInfraLocation.getValue();
    }
    
    /**
     * Returns the filters for hardware in monitoring. Mentioned hardware machines will be considered to be 
     * not available.
     * 
     * @return the names / network addresses to filter out, may be empty
     */
    public static Set<String> getMonitoringHardwareFilter() {
        return toSet(monitoringHardwareFilter.getValue());
    }
    
    /**
     * Returns the maximum executor startup waiting time, i.e., the time the monitoring layer waits at maximum
     * per Storm executor in order to detect that a pipeline is up and running.
     * 
     * @return the maximum waiting time in seconds, 0 switches off the heuristic.
     */
    public static int getStormExecutorStartupWaitingTime() {
        return stormExecutorStartupWaitingTime.getValue();
    }
    
    /**
     * Returns whether the startup detection shall take parallel executors into account.
     * 
     * @return <code>true</code> for parallel executors, <code>false</code> else
     */
    public static boolean getStormExecutorStartupParallel() {
        return stormExecutorStartupParallel.getValue();
    }
    
    /**
     * Returns the (initial) pipeline monitoring frequency for active monitoring.
     * 
     * @return the initial monitoring frequency (in ms)
     */
    public static int getPipelineMonitoringFrequency() {
        return pipelineMonitoringFrequency.getValue();
    }

    /**
     * Returns the (initial) cluster monitoring frequency for active monitoring.
     * 
     * @return the initial monitoring frequency (in ms)
     */
    public static int getClusterMonitoringFrequency() {
        return clusterMonitoringFrequency.getValue();
    }
    
    /**
     * Returns whether reasoning is enabled.
     * 
     * @return <code>true</code> if reasoning is enabled, <code>false</code> else
     */
    public static boolean isReasoningEnabled() {
        return Boolean.valueOf(System.getProperty("qm.reasoning", "true"));
    }
    
    /**
     * Returns whether thrift monitoring shall cause debug output.
     * 
     * @return <code>true</code> for debugging, <code>false</code> else
     */
    public static boolean debugThriftMonitoring() {
        return debugThriftMonitoring.getValue();
    }

    /**
     * The location where the volume prediction model is located.
     * 
     * @return the location
     */
    public static String getVolumeModelLocation() {
        return volumeModelLocation.getValue();
    }

    /**
     * The minimum difference between two deviations during analysis causing re-sending of adaptation events.
     * 
     * @return the difference in percent
     */
    public static int getAnalysisMinDeviationDifference() {
        return monitoringAnalysisMinDevDifference.getValue();
    }
    
    /**
     * Returns whether profile prediction shall enable approximation of unknown data points.
     * 
     * @return <code>true</code> for approximation, <code>false</code> else
     */
    public static boolean enableProfileApproximate() {
        return profileApproximate.getValue();
    }

    /**
     * Returns whether profile prediction shall be enabled enable.
     * 
     * @return <code>true</code> for enabled, <code>false</code> else
     */
    public static boolean enableProfile() {
        return profileEnable.getValue();
    }

    /**
     * Returns the time-to-live for a profile without update.
     * 
     * @return the time to live in milliseconds
     */
    public static int getProfileTtl() {
        return profileTtl.getValue();
    }
    
    /**
     * Returns whether reasoning shall be instance-based, i.e., reuse a reasoner for a given model.
     * 
     * @return {@code true} for instance-based reasoning, {@code false} for creating a new reasoner per 
     *    reasoning action
     */
    public static boolean getReasoningInstanceBased() {
        return reasoningInstanceBased.getValue();
    }
    
    /**
     * Creates a reasoner adapter for storing already known reasoner instances for configurations. 
     * Considers {@link getReasoningInstanceBased()}. [convenience]
     * 
     * @return the reasoner adapter
     */
    public static ReasonerAdapter createReasonerAdapter() {
        // TODO MonLayer/AdaptLayer clear upon model change
        return new ReasonerAdapter(MonitoringConfiguration.getReasoningInstanceBased());
    }

}
