package eu.qualimaster;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import backtype.storm.Config;
import backtype.storm.utils.Utils;
import eu.qualimaster.infrastructure.InitializationMode;

/**
 * Some infrastructure configuration information. Configuration options are declared as property key and default value 
 * in terms of constants (for access/documentation) and then turned into internal configuration objects.
 * Subsequent layers may extend the configuration, but already existing configuration options cannot be 
 * overridden.
 * 
 * @author Holger Eichelberger
 */
public class Configuration {
    
    // let's see whether we can unify this transparently with the Storm configuration if possible somehow 
    // (may run on another machine!)
    
    /**
     * An empty return value.
     */
    public static final String EMPTY_VALUE = "";

    public static final String CONFIG_KEY_STORM_ZOOKEEPER_PORT = Config.STORM_ZOOKEEPER_PORT;
    public static final String CONFIG_KEY_STORM_ZOOKEEPER_SERVERS = Config.STORM_ZOOKEEPER_SERVERS;
    public static final String CONFIG_KEY_STORM_NIMBUS_HOST = Config.NIMBUS_HOST;
    public static final String CONFIG_KEY_STORM_NIMBUS_PORT = Config.NIMBUS_THRIFT_PORT;
    
    // don't use the retry time/interval as for startup intensive components this may overlap/cause Netty to fail
    
    /**
     * Denotes the timeout for clearing response messages in the infrastructure (non-negative Integer).
     */
    public static final String EVENT_RESPONSE_TIMEOUT = "event.response.timeout";

    /**
     * The default value for {@link #EVENT_RESPONSE_TIMEOUT}.
     */
    public static final int DEFAULT_EVENT_RESPONSE_TIMEOUT = 5 * 60 * 1000; // 5 minutes due to startup/longer enacts

    /**
     * Denotes the time to wait (in ms) by default until a (worker) curator watcher has been set up. Might be higher on
     * slower clusters.
     */
    public static final String WATCHER_WAITING_TIME = "watcher.waiting.time";

    /**
     * The default value for {@link #WATCHER_WAITING_TIME}.
     */
    public static final int DEFAULT_WATCHER_WAITING_TIME = 100; 
    
    /**
     * Denotes the event bus host setting (String).
     */
    public static final String HOST_EVENT = "eventBus.host";

    /**
     * The default value for {@link #HOST_EVENT}.
     */
    public static final String DEFAULT_HOST_EVENT = "localhost";

    /**
     * Denotes the port setting of the event bus (Integer).
     */
    public static final String PORT_EVENT = "eventBus.port";

    /**
     * The default value for {@link #PORT_EVENT}.
     */
    public static final int DEFAULT_PORT_EVENT = 9998;

    /**
     * Denotes the event classes to disable logging for.
     */
    public static final String EVENT_DISABLE_LOGGING = "eventBus.disableLogging";

    /**
     * The default value for {@link #EVENT_DISABLE_LOGGING}, a comma separated list.
     */
    public static final String DEFAULT_EVENT_DISABLE_LOGGING = EMPTY_VALUE;

    /**
     * Denotes the nimbus host setting (String).
     */
    public static final String HOST_NIMBUS = "nimbus.host";

    /**
     * The default value for {@link #HOST_NIMBUS}.
     */
    public static final String DEFAULT_HOST_NIMBUS = "localhost";
    
    /**
     * Denotes the nimbus thrift port setting (String). Although possible, this value shall not be set in the 
     * QM infrastructure configuration file rather than in the anyway required Storm configuration.
     */
    public static final String PORT_THRIFT = "nimbus.thrift.port";

    /**
     * The default value for {@link #PORT_THRIFT}.
     */
    public static final int DEFAULT_PORT_THRIFT = 6627;
    
    /**
     * Denotes the zookeeper host setting (String). May be multiple zookeepers, each separated by a comma.
     */
    public static final String HOST_ZOOKEEPER = "zookeeper.host";

    /**
     * The default value for {@link #HOST_ZOOKEEPER}.
     */
    public static final String DEFAULT_HOST_ZOOKEEPER = "localhost";
    
    /**
     * Denotes the zookeeper (curator) port setting (Integer).
     */
    public static final String PORT_ZOOKEEPER = "zookeeper.port";

    /**
     * The default value for {@link #PORT_ZOOKEEPER}.
     */
    public static final int DEFAULT_PORT_ZOOKEEPER = 2181;

    /**
     * Denotes the retry times for connecting to the zookeepers (trials).
     */
    public static final String RETRY_TIMES_ZOOKEEPER = "zookeeper.retry.times";

    /**
     * The default value for {@link #RETRY_TIMES_ZOOKEEPER} ({@value}).
     */
    public static final int DEFAULT_RETRY_TIMES_ZOOKEEPER = 5;
    
    /**
     * Denotes the retry interval for connecting to the zookeepers (ms).
     */
    public static final String RETRY_INTERVAL_ZOOKEEPER = "zookeeper.retry.interval";

    /**
     * The default value for {@link #RETRY_INTERVAL_ZOOKEEPER} ({@value}). This value must be significantly smaller
     * than the retry time of Storm anf shall be faster than the retry time*trials of Netty.
     */
    public static final int DEFAULT_RETRY_INTERVAL_ZOOKEEPER = 100;
    
    /**
     * Defines the ports to be used for (dynamic) data connections among pipeline parts on software level 
     * (hardware ports are communicated differently).
     */
    public static final String PIPELINE_INTERCONN_PORTS = "pipelines.ports";
    
    /**
     * The default value for {@link #PIPELINE_INTERCONN_PORTS}.
     */
    public static final String DEFAULT_PIPELINE_INTERCONN_PORTS = "63000-64000";
    
    // events

    /**
     * Denotes the setting which determines whether Curator or QM events shall be used for pipeline signalling.
     */
    public static final String PIPELINE_SIGNALS_CURATOR = "pipeline.signals.curator";

    /**
     * The default value for {@link #PIPELINE_SIGNALS_CURATOR}, {@value}.
     */
    public static final boolean DEFAULT_PIPELINE_SIGNALS_CURATOR = true;
    
    /**
     * Denotes the waiting time after sending pipeline shutdown events (Integer in ms, ignored if not positive).
     */
    public static final String TIME_SHUTDOWN_EVENTS = "storm.shutdownEvents.time";

    /**
     * The default value for {@link #TIME_SHUTDOWN_EVENTS} in seconds (Value {@value}).
     */
    public static final int DEFAULT_TIME_SHUTDOWN_EVENTS = 300;

    // worker settings

    /**
     * The initialization mode for the configuration model.
     */
    public static final String INIT_MODE = "confModel.initMode";

    /**
     * The default value for {@link #INIT_MODE}.
     */
    public static final InitializationMode DEFAULT_INIT_MODE = InitializationMode.ADAPTIVE;
    
    /**
     * Denotes whether (data item transfer) volume monitoring shall be monitored on workers. 
     * Helps performing experiments.
     */
    public static final String MONITORING_VOLUME_ENABLED = "monitoring.volume.enabled";

    /**
     * The default value for {@link #VOLUME_MONITORING_ENABLED} ({@value}}).
     */
    public static final boolean DEFAULT_MONITORING_VOLUME_ENABLED = true;

    /**
     * Denotes the (optional - empty) local location where the infrastructure plugins reside.
     */
    public static final String PLUGINS_LOCATION = "plugins.location";

    /**
     * The default value for {@link #PLUGINS_LOCATION}, {@link #EMPTY_VALUE}.
     */
    public static final String DEFAULT_PLUGINS_LOCATION = EMPTY_VALUE;
    
    public static final PropertyReader<InitializationMode> INIT_MODE_READER = new PropertyReader<InitializationMode>() {

        @Override
        public InitializationMode read(Properties properties, String key, InitializationMode deflt) {
            InitializationMode result = deflt;
            String tmp = properties.getProperty(key, deflt.name());
            if (null != tmp) {
                try {
                    result = InitializationMode.valueOf(tmp.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // use deflt
                }
            }
            return result;
        }
        
    };

    private static final Logger LOGGER = Logger.getLogger(Configuration.class);
    
    /**
     * Implements a configuration option.
     * 
     * @param <T> the type of the value
     * @author Holger Eichelberger
     */
    public static class ConfigurationOption<T> {
        private T value;
        private T dflt;
        private String key;
        private PropertyReader<T> reader;
        
        /**
         * Creates a new configuration option.
         * 
         * @param key the key (for the configuration file)
         * @param dflt the default value
         * @param reader the respective property reader
         */
        public ConfigurationOption(String key, T dflt, PropertyReader<T> reader) {
            this.key = key;
            this.dflt = dflt;
            this.value = dflt;
            this.reader = reader;
            
            if (OPTIONS.containsKey(key)) {
                throw new IllegalArgumentException("There is already a configuration option " + key 
                    + ". Cannot override.");
            } else {
                OPTIONS.put(key, this);
            }
        }
        
        /**
         * Returns the value of this configuration option.
         * 
         * @return the value
         */
        public T getValue() {
            return value;
        }
        
        /**
         * Returns the default value of this configuration option.
         * 
         * @return the default value
         */
        public T getDefault() {
            return dflt;
        }
        
        /**
         * Defines the value of this configuration option.
         * 
         * @param value the new value
         */
        protected void setValue(T value) {
            this.value = value;
        }
        
        /**
         * Returns the property key of this option.
         * 
         * @return the property key
         */
        public String getKey() {
            return key;
        }
        
        /**
         * Turns this option into an entry of a properties set.
         * 
         * @param properties the properties set to be modified as a side effect
         * @param dflt put the default value or the actual value
         */
        protected void toProperties(Properties properties, boolean dflt) {
            T val = dflt ? this.dflt : value;
            if (null != val) {
                properties.put(key, val);
            }
        }
        
        /**
         * Reads this option from a properties file.
         * 
         * @param properties the properties set to be modified as a side effect
         */
        protected void fromProperties(Properties properties) {
            value = reader.read(properties, key, value);
        }
        
        /**
         * Clears this option by setting it to its default value.
         */
        protected void clear() {
            setValue(getDefault());
        }
        
        @Override
        public String toString() {
            return key + " = " + value;
        }
        
    }
    
    private static final Map<String, ConfigurationOption<?>> OPTIONS = new HashMap<String, ConfigurationOption<?>>();

    private static ConfigurationOption<Integer> eventResponseTimeout 
        = createIntegerOption(EVENT_RESPONSE_TIMEOUT, DEFAULT_EVENT_RESPONSE_TIMEOUT);
    private static ConfigurationOption<String> eventHost 
        = createStringOption(HOST_EVENT, DEFAULT_HOST_EVENT);
    private static ConfigurationOption<Integer> eventPort 
        = createIntegerOption(PORT_EVENT, DEFAULT_PORT_EVENT);
    private static ConfigurationOption<String> eventDisableLogging 
        = createStringOption(EVENT_DISABLE_LOGGING, DEFAULT_EVENT_DISABLE_LOGGING);
    private static ConfigurationOption<String> pluginsLocation
        = createStringOption(PLUGINS_LOCATION, DEFAULT_PLUGINS_LOCATION);

    // storm commons
    
    private static ConfigurationOption<String> nimbus = createStringOption(HOST_NIMBUS, DEFAULT_HOST_NIMBUS);
    private static ConfigurationOption<Integer> thriftPort 
        = createIntegerOption(PORT_THRIFT, DEFAULT_PORT_THRIFT);
    private static ConfigurationOption<String> zookeeper = createStringOption(HOST_ZOOKEEPER, DEFAULT_HOST_ZOOKEEPER);
    private static ConfigurationOption<Integer> zookeeperPort 
        = createIntegerOption(PORT_ZOOKEEPER, DEFAULT_PORT_ZOOKEEPER);
    private static ConfigurationOption<Integer> zookeeperRetryTimes 
        = createIntegerOption(RETRY_TIMES_ZOOKEEPER, DEFAULT_RETRY_TIMES_ZOOKEEPER);
    private static ConfigurationOption<Integer> zookeeperRetryInterval 
        = createIntegerOption(RETRY_INTERVAL_ZOOKEEPER, DEFAULT_RETRY_INTERVAL_ZOOKEEPER);
    private static ConfigurationOption<Boolean> pipelineSignalsCurator 
        = createBooleanOption(PIPELINE_SIGNALS_CURATOR, DEFAULT_PIPELINE_SIGNALS_CURATOR);
    private static ConfigurationOption<Integer> shutdownEventWaitingTime 
        = createIntegerOption(TIME_SHUTDOWN_EVENTS, DEFAULT_TIME_SHUTDOWN_EVENTS);
    private static ConfigurationOption<String> pipelinePorts
        = createStringOption(PIPELINE_INTERCONN_PORTS, DEFAULT_PIPELINE_INTERCONN_PORTS);
    private static ConfigurationOption<InitializationMode> initMode 
        = new ConfigurationOption<InitializationMode>(INIT_MODE, DEFAULT_INIT_MODE, INIT_MODE_READER);
    private static ConfigurationOption<Boolean> enableVolumeMonitoring
        = createBooleanOption(MONITORING_VOLUME_ENABLED, DEFAULT_MONITORING_VOLUME_ENABLED);
    private static ConfigurationOption<Integer> watcherWaitingTime
        = createIntegerOption(WATCHER_WAITING_TIME, DEFAULT_WATCHER_WAITING_TIME);
    
    /**
     * Prevents external creation / static class.
     */
    protected Configuration() {
    }
    
    /**
     * Turns a String into a URL.
     * 
     * @param url the URL
     * @return the URL instance for <code>url</code>, <b>null</b> in case of a malformed <code>url</code>
     */
    public static URL toUrl(String url) {
        URL result;
        try {
            result = new URL(url);
        } catch (MalformedURLException e) {
            LogManager.getLogger(Configuration.class).info("Turning " + url + " into URL : " + e.getMessage());
            result = null;
        }
        return result;
    }
    
    /**
     * Creates a String configuration option.
     * 
     * @param key the property key
     * @param dflt the default value
     * @return the configuration option instance
     */
    protected static ConfigurationOption<String> createStringOption(String key, String dflt) {
        return new ConfigurationOption<String>(key, dflt, PropertyReader.STRING_READER);
    }

    /**
     * Creates a Boolean configuration option.
     * 
     * @param key the property key
     * @param dflt the default value
     * @return the configuration option instance
     */
    protected static ConfigurationOption<Boolean> createBooleanOption(String key, Boolean dflt) {
        return new ConfigurationOption<Boolean>(key, dflt, PropertyReader.BOOLEAN_READER);
    }

    /**
     * Creates an Integer configuration option.
     * 
     * @param key the property key
     * @param dflt the default value
     * @return the configuration option instance
     */
    protected static ConfigurationOption<Integer> createIntegerOption(String key, Integer dflt) {
        return new ConfigurationOption<Integer>(key, dflt, PropertyReader.INTEGER_READER);
    }
    
    /**
     * Creates an Integer configuration option.
     * 
     * @param key the property key
     * @param dflt the default value
     * @return the configuration option instance
     */
    protected static ConfigurationOption<URL> createUrlOption(String key, URL dflt) {
        return new ConfigurationOption<URL>(key, dflt, PropertyReader.URL_READER);
    }
    
    /**
     * Reads the configuration settings from the file.
     * 
     * @param file the file to take the configuration settings from
     */
    public static void configure(File file) {
        Properties prop = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            prop.load(in);
            in.close();
            Configuration.configure(prop);
        } catch (IOException e) {
            LogManager.getLogger(Configuration.class).error("While reading configuration file " 
                + file.getAbsolutePath() + ": " + e.getMessage());
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e1) {
                    // ignore
                }
            }
        }
    }

    /**
     * Returns properties for default configuration (and modification).
     * 
     * @return the default properties
     */
    public static Properties getDefaultProperties() {
        return getProperties(true);
    }
    
    /**
     * Returns properties for the actual configuration (and modification).
     * 
     * @return the actual properties
     */
    public static Properties getProperties() {
        return getProperties(false);
    }
    
    /**
     * Returns properties for default configuration (and modification).
     * 
     * @param dflt return the default values or the actual values
     * @return the local properties
     */
    protected static Properties getProperties(boolean dflt) {
        Properties prop = new Properties();
        for (ConfigurationOption<?> option : OPTIONS.values()) {
            option.toProperties(prop, dflt);
        }
        return prop;
    }
    
    /**
     * Creates a local configuration.
     * 
     * @see #getDefaultProperties()
     */
    public static void configureLocal() {
        configure(getProperties(true));
    }

    /**
     * Reads the configuration settings from the given properties.
     * 
     * @param properties the properties to take the configuration settings from
     */
    public static void configure(Properties properties) {
        configure(properties, true);
    }
    
    /**
     * Reads the configuration settings from the given properties.
     * 
     * @param properties the properties to take the configuration settings from
     * @param useDefaults use the default values if undefined or, if <code>false</code> ignore undefined properties
     */
    public static void configure(Properties properties, boolean useDefaults) {
        for (ConfigurationOption<?> option : OPTIONS.values()) {
            if (doSetProperty(properties, option.getKey(), useDefaults)) {
                option.fromProperties(properties);
            }
        }
    }

    /**
     * Returns whether a property shall be taken from the given properties object.
     * 
     * @param properties the properties to take the configuration settings from
     * @param key the configuration key to check for
     * @param useDefaults use the default values if undefined or, if <code>false</code> ignore undefined properties
     * @return <code>true</code> if the property shall be set, <code>false</code> else
     */
    private static boolean doSetProperty(Properties properties, String key, boolean useDefaults) {
        return useDefaults || (!useDefaults && null != properties.get(key));
    }
    
    /**
     * Returns the address of the nimbus host.
     * 
     * @return the address of the nimbus host
     */
    public static String getNimbus() {
        return nimbus.getValue();
    }
    
    /**
     * Returns the address of the zookeeper server(s).
     * 
     * @return the address of the zookeeper server, in case of multiple ones separated by comma.
     */
    public static String getZookeeper() {
        return zookeeper.getValue();
    }

    /**
     * Returns the zookeeper connect string from the given parameters.
     * 
     * @param zookeepers the zookeepers
     * @param zkPort the zookeeper port number
     * @return the zookeeper connect string, host name and port separated by ":", multiple zookeepers by ","
     */
    public static String getZookeeperConnectString(String zookeepers, int zkPort) {
        StringTokenizer tokens = new StringTokenizer(zookeepers, ",");
        List<String> hosts = new ArrayList<String>();
        while (tokens.hasMoreTokens()) {
            hosts.add(tokens.nextToken());
        }
        Collections.shuffle(hosts); // try addressing different zookeepers as first
        StringBuffer result = new StringBuffer();
        Iterator<String> iter = hosts.iterator();
        while (iter.hasNext()) {
            result.append(iter.next());
            result.append(":");
            result.append(zkPort);
            if (iter.hasNext()) {
                result.append(",");
            }
        }
        return result.toString();
    }
    
    /**
     * Returns the zookeeper connect string from the currently configured {@link #getZookeeper() zookeeper(s)}
     * and the {@link #getZookeeperPort() zookeeper port}.
     * 
     * @return the zookeeper connect string, host name and port separated by ":", multiple zookeepers by ","
     */
    public static String getZookeeperConnectString() {
        return getZookeeperConnectString(getZookeeper(), getZookeeperPort());
    }
    
    /**
     * Returns the IP port to be used for zookeeper communications (curator).
     * 
     * @return the IP port
     */
    public static int getZookeeperPort() {
        return zookeeperPort.getValue();
    }

    /**
     * Returns the Zookeeper retry times.
     * 
     * @return the Zookeeper retry times
     */
    public static int getZookeeperRetryTimes() {
        return zookeeperRetryTimes.getValue();
    }

    /**
     * Returns the Zookeeper retry interval.
     * 
     * @return the Zookeeper retry interval
     */
    public static int getZookeeperRetryInterval() {
        return zookeeperRetryInterval.getValue();
    }
    
    /**
     * Returns the thrift port.
     * 
     * @param stormConf the storm configuration (may be <b>null</b>)
     * @return the thrift port
     */
    @SuppressWarnings("rawtypes")
    public static int getThriftPort(Map stormConf) {
        int result = thriftPort.getValue(); // may be set explicitly, may be the default
        if (null != stormConf) { // on Nimbus, take over the one from Storm, ignore config
            Object cfg = stormConf.get(Config.NIMBUS_THRIFT_PORT);
            if (cfg instanceof Integer) {
                result = (Integer) cfg;
            }
        }
        return result;
    }
    
    /**
     * Returns the configured thrift port.
     * 
     * @return the thrift port
     */
    public static int getThriftPort() {
        @SuppressWarnings("rawtypes")
        Map stormConf = Utils.readStormConfig();
        return (Integer) stormConf.get(Config.NIMBUS_THRIFT_PORT);
    }
    
    /**
     * Returns the port of the event bus.
     * 
     * @return the event port
     */
    public static int getEventPort() {
        return eventPort.getValue();
    }

    /**
     * Returns the event response timeout.
     * 
     * @return the timeout for collecting/forwarding responses in the infrastructure
     */
    public static int getEventResponseTimeout() {
        return eventResponseTimeout.getValue();
    }

    /**
     * Returns the host of the event bus server.
     * 
     * @return the event bus server host
     */
    public static String getEventHost() {
        return eventHost.getValue();
    }

    /**
     * Returns whether Curator shall be used for pipeline signals.
     * 
     * @return <code>true</code> for Curator, <code>false</code> for QM events
     */
    public static boolean getPipelineSignalsCurator() {
        return pipelineSignalsCurator.getValue();
    }

    /**
     * Returns whether the QM event bus shall be used for pipeline signals.
     * 
     * @return <code>true</code> for QM events, <code>false</code> for Curator (the inverse 
     *     of {@link #getPipelineSignalsCurator()})
     */
    public static boolean getPipelineSignalsQmEvents() {
        return !pipelineSignalsCurator.getValue();
    }
    
    /**
     * Returns whether <code>value</code> is empty (@link {@link #EMPTY_VALUE}).
     * 
     * @param value the value to be tested
     * @return <code>true</code> if empty, <code>false</code> else
     */
    public static boolean isEmpty(String value) {
        return EMPTY_VALUE.equals(value);
    }
    
    /**
     * Returns the waiting time after sending the shutdown signals.
     * 
     * @return the waiting time
     */
    public static int getShutdownSignalWaitTime() {
        return shutdownEventWaitingTime.getValue();
    }
    
    /**
     * Returns the (comma separated) classes for which to disable logging.
     * 
     * @return the comma separated classes, may be {@link #EMPTY_VALUE}
     */
    public static String getEventDisableLogging() {
        return eventDisableLogging.getValue();
    }
    
    /**
     * Turns the given text into a set of strings by splitting it using "," as delimiter.
     * 
     * @param text the text to be splitted (may be <b>null</b> leading to an empty result set)
     * @return the splitted text as set
     */
    public static Set<String> toSet(String text) {
        Set<String> result = new HashSet<String>();
        if (null != text) {
            text = text.trim();
            StringTokenizer names = new StringTokenizer(text, ",");
            while (names.hasMoreTokens()) {
                String tmp = names.nextToken();
                tmp = tmp.trim();
                if (tmp.length() > 0) {
                    result.add(tmp);
                }
            }
        }
        return result;
    }
    
    /**
     * Transfers relevant infrastructure configuration information from this configuration
     * to an options object. 
     * 
     * @param options the options object to be modified as a side effect
     * @see #transferConfigurationFrom(Map)
     */
    public static void transferConfigurationTo(IOptionSetter options) {
        options.setOption(HOST_EVENT, getEventHost());
        options.setOption(PORT_EVENT, getEventPort());
        options.setOption(CONFIG_KEY_STORM_NIMBUS_HOST, getNimbus());
        options.setOption(CONFIG_KEY_STORM_NIMBUS_PORT, getThriftPort());
        options.setOption(EVENT_DISABLE_LOGGING, getEventDisableLogging());
        options.setOption(PIPELINE_INTERCONN_PORTS, getPipelinePorts());
        options.setOption(MONITORING_VOLUME_ENABLED, enableVolumeMonitoring());
        options.setOption(RETRY_INTERVAL_ZOOKEEPER, getZookeeperRetryInterval());
        options.setOption(RETRY_TIMES_ZOOKEEPER, getZookeeperRetryTimes());
        options.setOption(WATCHER_WAITING_TIME, getWatcherWaitingTime());
    }

    /**
     * Transfers relevant parts of the Storm configuration back into the infrastructure configuration.
     * 
     * @param conf the storm configuration as map
     * @see #transferConfigurationTo(Map)
     * @see #transferConfigurationFrom(Map, Properties)
     */
    @SuppressWarnings({ "rawtypes" })
    public static void transferConfigurationFrom(Map conf) {
        Properties prop = new Properties();
        transferConfigurationFrom(conf, prop);
    }

    /**
     * Transfers relevant parts of the Storm configuration back into the infrastructure configuration.
     *
     * @param conf the storm configuration as map
     * @param prop the properties to transfer (may be initialized)
     * @see #transferConfigurationTo(Map)
     * @see #transferConfigurationFrom(Map)
     */
    @SuppressWarnings({ "rawtypes" })
    protected static void transferConfigurationFrom(Map conf, Properties prop) {
        // transfer zookeeper information so that SignalMechanism can create Zookeeper frameworks
        if (null != conf.get(CONFIG_KEY_STORM_ZOOKEEPER_PORT)) {
            prop.put(PORT_ZOOKEEPER, conf.get(CONFIG_KEY_STORM_ZOOKEEPER_PORT).toString());
        }
        if (null != conf.get(CONFIG_KEY_STORM_NIMBUS_PORT)) {
            prop.put(PORT_THRIFT, conf.get(CONFIG_KEY_STORM_NIMBUS_PORT).toString());
        }
        if (null != conf.get(CONFIG_KEY_STORM_NIMBUS_HOST)) {
            prop.put(HOST_NIMBUS, conf.get(CONFIG_KEY_STORM_NIMBUS_HOST).toString());
        }
        if (null != conf.get(CONFIG_KEY_STORM_ZOOKEEPER_SERVERS)) {
            Object zks = conf.get(CONFIG_KEY_STORM_ZOOKEEPER_SERVERS);
            String tmp = zks.toString();
            if (zks instanceof Collection) {
                tmp = "";
                for (Object o : (Collection) zks) {
                    if (tmp.length() > 0) {
                        tmp += ",";
                    }
                    tmp += o.toString();
                }
            }
            prop.put(HOST_ZOOKEEPER, tmp);
        }
        if (null != conf.get(RETRY_TIMES_ZOOKEEPER)) {
            prop.put(RETRY_TIMES_ZOOKEEPER, conf.get(RETRY_TIMES_ZOOKEEPER).toString());
        }
        if (null != conf.get(RETRY_INTERVAL_ZOOKEEPER)) {
            prop.put(RETRY_INTERVAL_ZOOKEEPER, conf.get(RETRY_INTERVAL_ZOOKEEPER).toString());
        }

        // if storm has a configuration value and the actual configuration is not already
        // changed, than change the event bus configuration
        if (null != conf.get(HOST_EVENT)) {
            prop.put(HOST_EVENT, conf.get(HOST_EVENT));
        }
        if (null != conf.get(Configuration.PORT_EVENT)) {
            prop.put(PORT_EVENT, conf.get(PORT_EVENT));
        }
        if (null != conf.get(EVENT_DISABLE_LOGGING)) {
            prop.put(EVENT_DISABLE_LOGGING, conf.get(EVENT_DISABLE_LOGGING));
        }
        if (null != conf.get(PIPELINE_INTERCONN_PORTS)) {
            prop.put(PIPELINE_INTERCONN_PORTS, conf.get(PIPELINE_INTERCONN_PORTS));
        }
        // TODO use transfer above
        transfer(conf, prop, MONITORING_VOLUME_ENABLED, false);
        transfer(conf, prop, WATCHER_WAITING_TIME, false);
        if (prop.size() > 0) {
            System.out.println("Reconfiguring infrastructure settings: " + prop + " from " + conf);
            configure(prop, false);
        }
    }
    
    /**
     * Transfers the setting <code>key</code> from <code>conf</code> to <code>prop</code>. Applies <code>toString</code>
     * if requested.
     * 
     * @param conf the storm configuration as map
     * @param prop the property object
     * @param key the configuration key
     * @param toString apply <code>toString</code> to the value stored in <code>conf</code>
     */
    @SuppressWarnings({ "rawtypes" })
    protected static void transfer(Map conf, Properties prop, String key, boolean toString) {
        if (null != conf.get(key)) {
            Object val = conf.get(key);
            if (toString) {
                val = val.toString();
            }
            prop.put(key, val);
        }
    }
    
    /**
     * Clears the configuration. Intended for testing to ensure a fresh state.
     */
    public static void clear() {
        for (ConfigurationOption<?> opt : OPTIONS.values()) {
            opt.clear();
        }
    }

    /**
     * Returns the configured pipeline interconnection ports.
     * 
     * @return the pipeline interconnection ports
     */
    public static String getPipelinePorts() {
        LOGGER.info("The configured pipeline ports: " + pipelinePorts.getValue());        
        return pipelinePorts.getValue();
    }

    
    /**
     * Returns the initialization mode for the configuration model.
     * 
     * @return the initialization mode
     */
    public static InitializationMode getInitializationMode() {
        return initMode.getValue();
    }


    /**
     * Returns whether (data item transfer) volume shall be monitored on workers. Intended to enable experiments.
     * 
     * @return <code>true</code> for enabled, <code>false</code> else
     */
    public static boolean enableVolumeMonitoring() {
        return enableVolumeMonitoring.getValue();
    }

    /**
     * Returns the watcher waiting time.
     * 
     * @return the watcher waiting time in milliseconds
     */
    public static int getWatcherWaitingTime() {
        return watcherWaitingTime.getValue();        
    }
    
    /**
     * Returns the plugins location.
     * 
     * @return the plugins location
     */
    public static String getPluginsLocation() {
        return pluginsLocation.getValue();
    }

}
