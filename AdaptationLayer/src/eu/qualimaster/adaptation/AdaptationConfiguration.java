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
package eu.qualimaster.adaptation;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import eu.qualimaster.monitoring.MonitoringConfiguration;

/**
 * Specific configuration options introduced by the adaptation layer.
 * 
 * @author Holger Eichelberger
 */
public class AdaptationConfiguration extends MonitoringConfiguration {

    /**
     * Denotes the port setting of the communication with the adaptation layer (Integer).
     */
    public static final String PORT_ADAPTATION = "adaptation.port";

    /**
     * The default value for {@link #PORT_ADAPTATION}.
     */
    public static final int DEFAULT_PORT_ADAPTATION = 7012;
    
    /**
     * Denotes the host name fir the communication with the adaptation layer (String).
     */
    public static final String HOST_ADAPTATION = "adaptation.host";

    /**
     * The default value for {@link #PORT_ADAPTATION} (default {@link #EMPTY_VALUE}, leads finally to 
     * the same value as {@link HOST_EVENT}.
     */
    public static final String DEFAULT_HOST_ADAPTATION = EMPTY_VALUE;
    
    /**
     * Denotes the option of enabling/disabling the rt-VIL adaptation log.
     */
    public static final String ADAPTATION_RTVIL_LOGGING = "adaptation.rtVil.log";

    /**
     * The default value for {@link #ADAPTATION_RTVIL_LOGGING}, {@value}.
     */
    public static final boolean DEFAULT_ADAPTATION_RTVIL_LOGGING = false;

    /**
     * Denotes the option for defining the tracer factory (qualified class name).
     */
    public static final String ADAPTATION_RTVIL_TRACERFACTORY = "adaptation.rtVil.tracerFactory";

    /**
     * The default value for {@link #ADAPTATION_RTVIL_TRACERFACTORY}, {@value}.
     */
    public static final String DEFAULT_ADAPTATION_RTVIL_TRACERFACTORY = EMPTY_VALUE;
    
    private static ConfigurationOption<String> adaptationHost 
        = createStringOption(HOST_ADAPTATION, DEFAULT_HOST_ADAPTATION);
    private static ConfigurationOption<Boolean> adaptationRtVilLogging 
        = createBooleanOption(ADAPTATION_RTVIL_LOGGING, DEFAULT_ADAPTATION_RTVIL_LOGGING);
    private static ConfigurationOption<String> adaptationRtVilTracerFactory
        = createStringOption(ADAPTATION_RTVIL_TRACERFACTORY, DEFAULT_ADAPTATION_RTVIL_TRACERFACTORY);
    private static ConfigurationOption<Integer> adaptationPort 
        = createIntegerOption(PORT_ADAPTATION, DEFAULT_PORT_ADAPTATION);

    /**
     * Reads the configuration settings from the file.
     * 
     * @param file the file to take the configuration settings from
     */
    public static void configure(File file) {
        MonitoringConfiguration.configure(file);
    }
    
    /**
     * Returns properties for default configuration (and modification).
     * 
     * @return the default properties
     */
    public static Properties getDefaultProperties() {
        return MonitoringConfiguration.getDefaultProperties();
    }
    
    /**
     * Returns properties for the actual configuration (and modification).
     * 
     * @return the actual properties
     */
    public static Properties getProperties() {
        return MonitoringConfiguration.getProperties();
    }
    
    /**
     * Creates a local configuration.
     * 
     * @see #getDefaultProperties()
     */
    public static void configureLocal() {
        MonitoringConfiguration.configureLocal();
    }

    /**
     * Reads the configuration settings from the given properties.
     * 
     * @param properties the properties to take the configuration settings from
     */
    public static void configure(Properties properties) {
        MonitoringConfiguration.configure(properties);
    }

    /**
     * Reads the configuration settings from the given properties.
     * 
     * @param properties the properties to take the configuration settings from
     * @param useDefaults use the default values if undefined or, if <code>false</code> ignore undefined properties
     */
    public static void configure(Properties properties, boolean useDefaults) {
        MonitoringConfiguration.configure(properties, useDefaults);
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
        MonitoringConfiguration.transferConfigurationFrom(config);
    }

    /**
     * Transfers relevant parts of the Storm configuration back into the infrastructure configuration.
     * 
     * @param conf the storm configuration as map
     * @see #transferConfigurationTo(Map)
     */
    @SuppressWarnings("rawtypes")
    public static void transferConfigurationFrom(Map conf) {
        MonitoringConfiguration.transferConfigurationFrom(conf);
    }
    
    /**
     * Clears the configuration. Intended for testing to ensure a fresh state.
     */
    public static void clear() {
        MonitoringConfiguration.clear();
    }

    /**
     * Returns the communication port of the adaptation layer.
     * 
     * @return the adaptation port
     */
    public static int getAdaptationPort() {
        return adaptationPort.getValue();
    }
    
    /**
     * Returns the communication port of the adaptation layer.
     * 
     * @return the adaptation port
     */
    public static String getAdaptationHost() {
        String tmp = adaptationHost.getValue();
        if (EMPTY_VALUE.equals(tmp)) {
            tmp = getEventHost();
        }
        return tmp;
    }
    
    /**
     * Returns whether rt-VIL logging during adaptation is enabled.
     * 
     * @return <code>true</code> for enabled, <code>false</code> else
     */
    public static boolean enableAdaptationRtVilLogging() {
        return adaptationRtVilLogging.getValue();
    }
    
    /**
     * Returns the tracer factory if {@link #enableAdaptationRtVilLogging()}.
     * 
     * @return the tracer factory, empty for internal default
     */
    public static String getAdaptationRtVilTracerFactory() {
        return adaptationRtVilTracerFactory.getValue();
    }

}
