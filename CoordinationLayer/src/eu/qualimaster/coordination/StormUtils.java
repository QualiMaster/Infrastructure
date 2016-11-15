package eu.qualimaster.coordination;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.storm.curator.framework.CuratorFramework;
import org.apache.thrift7.TException;

import eu.qualimaster.Configuration;
import eu.qualimaster.base.algorithm.IMainTopologyCreate;
import eu.qualimaster.base.algorithm.TopologyOutput;
import eu.qualimaster.common.signal.Constants;
import eu.qualimaster.common.signal.ThriftConnection;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper.ProfileData;
import eu.qualimaster.infrastructure.PipelineOptions;
import backtype.storm.Config;
import backtype.storm.ILocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.daemon.common.Assignment;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.ClusterSummary;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.KillOptions;
import backtype.storm.generated.Nimbus;
import backtype.storm.generated.NotAliveException;
import backtype.storm.generated.RebalanceOptions;
import backtype.storm.generated.StormTopology;
import backtype.storm.generated.TopologyInfo;
import backtype.storm.generated.TopologySummary;
import backtype.storm.utils.NimbusClient;
import backtype.storm.utils.Time;
import backtype.storm.utils.Utils;

/**
 * Some utility methods for performing coordination operations on Storm. The methods here shall not work on pipeline
 * (concept) level rather than on lower storm (concept) level!
 * 
 * @author ap0n
 * @author Holger Eichelberger
 */
public class StormUtils {

    private static final Logger LOGGER = LogManager.getLogger(StormUtils.class);
    private static ILocalCluster localCluster;
    private static Map<String, TopologyTestInfo> testTopologies;


    /**
     * A specific exception for failing the creation of topology test infos.
     * 
     * @author Holger Eichelberger
     */
    public static class TopologyTestInfoException extends Exception {

        private static final long serialVersionUID = -4812832799124323947L;

        /**
         * Creates the exception.
         * 
         * @param message the message
         * @param cause the cause
         */
        public TopologyTestInfoException(String message, Throwable cause) {
            super(message, cause);
        }
        
    }
    
    /**
     * Local topology information to be used while testing.
     * 
     * @author Holger Eichelberger
     */
    public static class TopologyTestInfo {

        private StormTopology topology;
        private File mappingFile;
        @SuppressWarnings("rawtypes")
        private Map topologyConfig;
        private ProfileData profileData;

        /**
         * Creates a new topology information.
         * 
         * @param topology
         *            the topology
         * @param mappingFile
         *            the mapping file
         * @param topologyConfig
         *            the topology configuration
         */
        @SuppressWarnings("rawtypes")
        public TopologyTestInfo(StormTopology topology, File mappingFile, Map topologyConfig) {
            this(topology, mappingFile, topologyConfig, null);
        }

        /**
         * Creates a new topology information.
         * 
         * @param topology the topology
         * @param mappingFile the mapping file
         * @param topologyConfig the topology configuration
         * @param profileData optional profiling data for simulating profiling executions (may be <b>null</b>)
         */
        @SuppressWarnings("rawtypes")
        public TopologyTestInfo(StormTopology topology, File mappingFile, Map topologyConfig, ProfileData profileData) {
            this.topology = topology;
            this.mappingFile = mappingFile;
            this.topologyConfig = topologyConfig;
            this.profileData = profileData;
        }
        
        /**
         * Creates a topology test information object from pipeline code.
         * 
         * @param name the name of the pipeline
         * @param path the path to the code (if jar, <code>..\classes</code> will be used for loading the mapping file)
         * @param topologyConfig the topology configuration
         * @param profileData optional profiling data for simulating profiling executions (may be <b>null</b>)
         * @throws TopologyTestInfoException in case that obtaining/instantiating the topology fails
         */
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public TopologyTestInfo(String name, File path, Map topologyConfig, ProfileData profileData) 
            throws TopologyTestInfoException {
            this.topologyConfig = new HashMap();
            if (null != topologyConfig) {
                this.topologyConfig.putAll(topologyConfig);
            }
            this.profileData = profileData;
            File binPath = path;
            if (path.getName().endsWith(".jar")) {
                binPath = new File(path.getParentFile(), "classes");
                String topoClasspath = path.getAbsolutePath();
                // make topology available to workers
                Object tmp = this.topologyConfig.get(Config.TOPOLOGY_CLASSPATH);
                if (null != tmp) {
                    topoClasspath = tmp.toString() + File.pathSeparator + topoClasspath;
                }
                this.topologyConfig.put(Config.TOPOLOGY_CLASSPATH, topoClasspath);
            }
            mappingFile = new File(binPath, "mapping.xml");
            if (!mappingFile.exists()) {
                throw new TopologyTestInfoException("Cannot find mapping file " + mappingFile.getAbsolutePath(), null);
            }
            String topologyClass = "eu.qualimaster." + name + ".topology.Topology$MainTopologyCreator";
            URL[] urls = new URL[1];
            try {
                urls[0] = path.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new TopologyTestInfoException(e.getMessage(), e);
            }
            try (URLClassLoader loader = new URLClassLoader(urls)) {
                Class<?> cls = loader.loadClass(topologyClass);
                Object obj = cls.newInstance();
                if (obj instanceof IMainTopologyCreate) {
                    IMainTopologyCreate tCreator = (IMainTopologyCreate) obj;
                    TopologyOutput out = tCreator.createMainTopology();
                    this.topology = out.getBuilder().createTopology();
                }
            } catch (ClassNotFoundException e) {
                throw new TopologyTestInfoException("cannot find topology class " + topologyClass, e);
            } catch (IllegalAccessException e) {
                throw new TopologyTestInfoException("cannot instantiate topology class " + topologyClass, e);
            } catch (InstantiationException e) {
                throw new TopologyTestInfoException("cannot instantiate topology class " + topologyClass, e);
            } catch (IOException e) {
                LogManager.getLogger(getClass()).error("while closing class loader: " + e.getMessage(), e);
            }
        }

        /**
         * Returns the QualiMaster specific pipeline mapping file.
         * 
         * @return the mapping file
         */
        public File getMappingFile() {
            return mappingFile;
        }

        /**
         * Returns the associated storm topology.
         * 
         * @return the topology
         */
        StormTopology getTopology() {
            return topology;
        }

        /**
         * Returns the topology configuration.
         * 
         * @return the topology configuration
         */
        @SuppressWarnings("rawtypes")
        public Map getTopologyConfig() {
            return topologyConfig;
        }
        
        /**
         * Returns the base folder representing a project, in particular containing the EASy folder.
         * 
         * @return the EASy folder
         */
        public File getBaseFolder() {
            return mappingFile.getParentFile();
        }
        
        /**
         * Returns the profiling data.
         * 
         * @return the profiling data (may be <b>null</b>)
         */
        public ProfileData getProfileData() {
            return profileData;
        }

    }

    /**
     * Returns a local topology information (requires
     * {@link #forTesting(ILocalCluster, Map)} with proper testing information
     * to return something).
     * 
     * @param pipelineName
     *            the name of the pipeline to return the information for
     * @return the topology (or <b>null</b> if not found)
     */
    public static TopologyTestInfo getLocalInfo(String pipelineName) {
        TopologyTestInfo result;
        if (null != testTopologies) {
            result = testTopologies.get(pipelineName);
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Defines data for testing.
     * 
     * @param instance
     *            the local cluster instance (may be <b>null</b> to stop using
     *            the local cluster).
     * @param topologies
     *            the test topologies
     */
    public static void forTesting(ILocalCluster instance, Map<String, TopologyTestInfo> topologies) {
        localCluster = instance;
        testTopologies = topologies;
    }
    
    /**
     * Returns the names of defined testing topologies.
     * 
     * @return the names
     * @see #inTesting()
     */
    public static Set<String> getTestingTopologyNames() {
        return testTopologies.keySet();
    }
    
    /**
     * Returns the (single) test topology information object.
     * 
     * @return the single object if defined, <b>null</b> if there is none or multiple
     */
    public static TopologyTestInfo getTestInfo() {
        TopologyTestInfo result = null;
        Set<String> topos = StormUtils.getTestingTopologyNames();
        if (1 == topos.size()) {
            result = testTopologies.get(topos.toArray()[0].toString());
        }
        return result;
    }
    
    /**
     * Returns the test topology information instance for the given topology.
     * 
     * @param topologyName the topology name
     * @return the test instance
     */
    public static TopologyTestInfo getTestInfo(String topologyName) {
        return testTopologies.get(topologyName);
    }

    /**
     * Returns whether {@link #forTesting(ILocalCluster, Map)} was called with a
     * local cluster.
     * 
     * @return <code>true</code> if we are in testing mode, <code>false</code>
     *         else
     */
    public static boolean inTesting() {
        return null != localCluster;
    }
    
    /**
     * Returns the local cluster in testing.
     * 
     * @return the local cluster, may be <b>null</b> but not if {@link #inTesting()} is <code>true</code>
     */
    public static ILocalCluster getLocalCluster() {
        return localCluster;
    }

    /**
     * Does common configuration steps for local cluster and distributed cluster startup.
     * 
     * @param stormConf the storm configuration map
     * @param options the startup pipeline options
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void doCommonConfiguration(Map stormConf, PipelineOptions options) {
        Configuration.transferConfigurationTo(stormConf);
        stormConf = options.toConf(stormConf);
        if (CoordinationConfiguration.getPipelineStartSourceAutoconnect()) {
            stormConf.put(Constants.CONFIG_KEY_SOURCE_AUTOCONNECT, "true");
        }
        stormConf.put(Constants.CONFIG_KEY_INIT_MODE, CoordinationConfiguration.getInitializationMode().name());
    }

    /**
     * Submits a Storm topology.
     * 
     * @param host
     *            The nimbus host
     * @param mapping
     *            the topology mapping
     * @param jarPath
     *            The path of the topology jar
     * @param options
     *            pipeline startup options (if none are required, pass just a
     *            freshly created object)
     * 
     * @throws IOException
     *             in case of thrift problems or an invalid topology
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void submitTopology(String host, INameMapping mapping,
            String jarPath, PipelineOptions options) throws IOException {
        String topologyName = mapping.getPipelineName();
        if (null != localCluster) {
            LOGGER.info("Submitting in local cluster mode " + jarPath + " " + options);
            StormTopology topology = null;
            Map stormConf = null;
            if (null != testTopologies) {
                TopologyTestInfo info = testTopologies.get(topologyName);
                if (null != info) {
                    topology = info.getTopology();
                    stormConf = info.getTopologyConfig();
                    // otherwise we run into trouble with multiple system bolts
                    if (null != stormConf) {
                        stormConf.put(Config.STORM_CLUSTER_MODE, "local");
                    }
                }
            }
            if (null == topology) {
                throw new IOException("topology '" + topologyName + "' not found");
            }
            doCommonConfiguration(stormConf, options);
            try {
                localCluster.submitTopology(topologyName, stormConf, topology);
            } catch (InvalidTopologyException e) {
                throw new IOException("Invalid topology " + e.getMessage());
            } catch (AlreadyAliveException e) {
                // just ignore this
                LOGGER.info(e.getMessage());
            }
        } else {
            Map stormConf = Utils.readStormConfig();
            stormConf.put(Config.NIMBUS_HOST, host);
            doCommonConfiguration(stormConf, options);
            try {
                // upload topology jar to Cluster using StormSubmitter
                clearSubmitter();
                LOGGER.info("Submitting " + jarPath);
                String uploadedJarLocation = StormSubmitter.submitJar(stormConf, jarPath);
                System.setProperty("storm.jar", uploadedJarLocation);
                File file = new File(jarPath);
                URLClassLoader loader = URLClassLoader.newInstance(
                        new URL[] {file.toURI().toURL()},
                        StormUtils.class.getClassLoader());
                String topologyClassName = mapping.getContainerName();
                if (null == topologyClassName || 0 == topologyClassName.length()) {
                    throw new IOException("Topology class name is empty in mapping " + mapping + ". Cannot start "
                        + "pipeline " + mapping.getPipelineName() + ". If you try to start it manually, please ensure "
                        + "that the pipeline name in the configuration is also the name of the Jar an in the package "
                        + "name of the topology.");
                } else {
                    Class<?> clazz = loader.loadClass(topologyClassName);
                    Method method = clazz.getMethod("main", String[].class);
                    Object[] arguments = options.toArgs(topologyName);
                    LOGGER.info("Calling main in " + topologyClassName + " with " 
                        + java.util.Arrays.toString(arguments));
                    method.invoke(null, new Object[] {arguments});
                    loader.close();
                }
            } catch (ClassNotFoundException e) {
                throw new IOException("Class not found: " + e.getMessage(), e);
            } catch (NoSuchMethodException e) {
                throw new IOException("No such method: " + e.getMessage(), e);
            } catch (InvocationTargetException e) {
                throw new IOException("Execution problem: " + toString(e.getCause(), true), e);
            } catch (IllegalAccessException e) {
                throw new IOException("Illegal access: " + e.getMessage(), e);
            }
        }
    }
    
    /**
     * Turns a throwable into a printable string.
     * 
     * @param ex the exception / throwable
     * @param addSimpleName if the simple name of the exception / throwable shall be added
     * @return the printable string
     */
    private static String toString(Throwable ex, boolean addSimpleName) {
        String result = "";
        if (addSimpleName) {
            result = ex.getClass().getSimpleName() + " ";
        }
        result += ex.getMessage() + "\n";
        StackTraceElement[] trace = ex.getStackTrace();
        for (int s = 0; s < trace.length; s++) {
            result += trace[s] + "\n";
        }
        return result;
    }

    /**
     * Clears the relevant static Storm submitter fields. Actually, this is a
     * hack, as there is no Storm interface for that and Storm, however, assumes
     * that this class is just called from a script or a temporary JVM.
     */
    public static void clearSubmitter() {
        try {
            Field submittedJarField = StormSubmitter.class
                    .getDeclaredField("submittedJar");
            submittedJarField.setAccessible(true);
            submittedJarField.set(null, null);
        } catch (NoSuchFieldException | SecurityException
                | IllegalArgumentException | IllegalAccessException e) {
            LOGGER.error("Clearing Storm submitter failed: " + e.getMessage());
        }
    }

    // checkstyle: stop exception type check

    /**
     * Kills a storm topology.
     * 
     * @param host
     *            The nimbus host
     * @param topologyName
     *            The name of the topology to kill
     * @param waitTime
     *            Wait time (seconds)
     * @param options
     *            pipeline kill options (if none are required, pass just a
     *            freshly created object)
     * @param waitForNotAlive
     *            wait until the pipeline is considered to be not alive (<code>true</code>) or just fire and 
     *            forget (<code>false</code>)
     * @throws IOException
     *             in case of thrift problems
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void killTopology(String host, String topologyName, int waitTime, PipelineOptions options, 
        boolean waitForNotAlive) throws IOException {
        IClusterAccess access;
        KillOptions killOpts = new KillOptions();
        if (null != localCluster) {
            access = new LocalClusterAccess(localCluster);
            killOpts.set_wait_secs(0);
        } else {
            Map<Object, Object> stormConf = Utils.readStormConfig();
            Map<Object, Object> conf = new HashMap();
            conf.put(Config.NIMBUS_HOST, host);
            conf.put(Config.NIMBUS_THRIFT_PORT,
                    CoordinationConfiguration.getThriftPort(stormConf));
            conf.put(Config.STORM_THRIFT_TRANSPORT_PLUGIN,
                    stormConf.get(Config.STORM_THRIFT_TRANSPORT_PLUGIN));
            // so far only needed in the cluster case
            conf.put(CoordinationConfiguration.HOST_EVENT, CoordinationConfiguration.getEventHost());
            conf.put(CoordinationConfiguration.PORT_EVENT, CoordinationConfiguration.getEventPort());

            Nimbus.Client client = NimbusClient.getConfiguredClient(conf).getClient();
            access = new NimbusClientClusterAccess(client);
            killOpts.set_wait_secs(options.getWaitTime(waitTime));
        }
        LOGGER.info("Killing pipeline " + topologyName + " on " + access.getName() + " with " + killOpts);
        try {
            access.killTopologyWithOpts(topologyName, killOpts);
        } catch (NotAliveException e) {
            throw new IOException("Not alive: " + e.getMessage(), e);
        } catch (TException e) {
            throw new IOException("Transport problem: " + e.getMessage(), e);
        }
        while (waitForNotAlive) {
            // similar to the related method in StormSubmitter
            try {
                if (!topologyExists(access.getClusterInfo(), topologyName)) {
                    break;
                }
            } catch (TException e) {
                break; // end gracefully, in particular for local tests where thrift may have gone
            }
            // client.getTopologyInfo disappears too early
            sleep(100);
        }
    }

    /**
     * Returns whether a given topology exists within <code>summary</code>.
     * 
     * @param summary the summary object
     * @param topologyName the topology name
     * @return <code>true</code> if the topology exists, <code>false</code> else
     */
    private static boolean topologyExists(ClusterSummary summary, String topologyName) {
        boolean result = false;
        for (TopologySummary s : summary.get_topologies()) {
            if (s.get_name().equals(topologyName)) {  
                result = true;
                break;
            } 
        }
        return result;
    }
    
    /**
     * Access to a Storm client with the same interface as a nimbus client.
     * 
     * @author Holger Eichelberger
     */
    private interface IClusterAccess {

        /**
         * Returns the actual cluster summary.
         * 
         * @return the cluster summary
         * @throws TException in case that accessing the summary fails
         */
        public ClusterSummary getClusterInfo() throws TException;
        
        /**
         * Returns the actual information about the topology.
         * 
         * @param topologyName the name / id of the topology
         * @return the topology information
         * @throws TException in case that accessing the summary fails
         * @throws NotAliveException in case that the topology is not alive
         */
        public TopologyInfo getTopologyInfo(String topologyName) throws TException, NotAliveException;
        
        /**
         * Kills a topology with given options.
         * 
         * @param name the name of the topology
         * @param options the options
         * @throws TException in case that accessing the summary fails
         * @throws NotAliveException in case that the topology is not alive
         */
        public void killTopologyWithOpts(String name, KillOptions options) throws NotAliveException, TException;

        /**
         * The name of this access instance(for logging).
         * 
         * @return the name
         */
        public String getName();
        
    }
    
    /**
     * Implements client access for the local cluster.
     * 
     * @author Holger Eichelberger
     */
    private static class LocalClusterAccess implements IClusterAccess {

        private ILocalCluster cluster;

        /**
         * Creates an instance.
         * 
         * @param cluster the cluster instance
         */
        private LocalClusterAccess(ILocalCluster cluster) {
            this.cluster = cluster;
        }
        
        @Override
        public ClusterSummary getClusterInfo() throws TException {
            try {
                return cluster.getClusterInfo();
            } catch (Throwable e) {
                throw new TException(e.getMessage(), e.getCause());
            }
        }

        @Override
        public TopologyInfo getTopologyInfo(String topologyName) throws TException, NotAliveException {
            try {
                return cluster.getTopologyInfo(topologyName);
            } catch (Throwable e) { // interface does not declare exceptions
                if (e instanceof NotAliveException || e.getCause() instanceof NotAliveException) {
                    throw e;
                } else {
                    throw new TException(e.getMessage(), e.getCause());
                }
            }
        }
        
        @Override
        public void killTopologyWithOpts(String name, KillOptions options) throws NotAliveException, TException {
            cluster.killTopologyWithOpts(name, options);
        }
        
        /**
         * Returns the name.
         * 
         * @return the name
         */
        public String getName() {
            return "local cluster";
        }

    }
    
    /**
     * Cluster access via a nimbus client.
     * 
     * @author Holger Eichelberger
     */
    private static class NimbusClientClusterAccess implements IClusterAccess {

        private Nimbus.Client client;
        
        /**
         * Creates a new instance.
         * 
         * @param client the client instance
         */
        private NimbusClientClusterAccess(Nimbus.Client client) {
            this.client = client;
        }
        
        @Override
        public ClusterSummary getClusterInfo() throws TException {
            return client.getClusterInfo();
        }

        @Override
        public TopologyInfo getTopologyInfo(String topologyName) throws TException, NotAliveException {
            return client.getTopologyInfo(topologyName);
        }

        @Override
        public void killTopologyWithOpts(String name, KillOptions options) throws NotAliveException, TException {
            client.killTopologyWithOpts(name, options);
        }

        /**
         * Returns the name.
         * 
         * @return the name
         */
        public String getName() {
            return "cluster";
        }

    }

    /**
     * Rebalances the given topology.
     * 
     * @param host
     *            The nimbus host
     * @param topology
     *            The name of the topology to modify
     * @param numWorkers
     *            the overall number of workers
     * @param executors
     *            the spout/bolt name - executor number mapping
     * @param waitTime
     *            the waiting time in seconds
     * @throws IOException
     *             in case that rebalancing fails
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void rebalance(String host, String topology, int numWorkers,
            Map<String, Integer> executors, int waitTime) throws IOException {
        RebalanceOptions opts = new RebalanceOptions();
        if (numWorkers > 0) {
            opts.set_num_workers(numWorkers);
        }
        if (null != executors) {
            opts.set_num_executors(executors);
        }
        if (waitTime >= 0) {
            opts.set_wait_secs(waitTime);
        }
        if (null != localCluster) {
            try {
                localCluster.rebalance(topology, opts);
            } catch (NotAliveException e) {
                throw new IOException(e.getMessage());
            }
        } else {
            Map<Object, Object> stormConf = Utils.readStormConfig();
            Map<Object, Object> conf = new HashMap();
            conf.put(Config.NIMBUS_HOST, host);
            conf.put(Config.NIMBUS_THRIFT_PORT,
                    CoordinationConfiguration.getThriftPort(stormConf));
            conf.put(Config.STORM_THRIFT_TRANSPORT_PLUGIN,
                    stormConf.get(Config.STORM_THRIFT_TRANSPORT_PLUGIN));
            Nimbus.Client client = NimbusClient.getConfiguredClient(conf)
                    .getClient();
            try {
                client.rebalance(topology, opts);
            } catch (NotAliveException e) {
                throw new IOException("Not alive: " + e.getMessage(), e);
            } catch (TException e) {
                throw new IOException("Transport problem: " + e.getMessage(), e);
            } catch (InvalidTopologyException e) {
                throw new IOException("Invalid topology: " + e.getMessage(), e);
            } catch (RuntimeException e) { // may occur on local cluster
                throw new IOException("Runtime problem: " + e.getMessage(), e);
            }
        }
    }

    // checkstyle: resume exception type check


    /**
     * Topology support data, here as interface to support testing.
     * 
     * @author Holger Eichelberger
     */
    public interface ITopologySupport {
        
        /**
         * Returns a host assignment for the existing <code>assignment</code> (host / port used as fallback) and
         * based on the given <code>request</code>. 
         * 
         * @param assignment the actual assignment
         * @param request the change request
         * @return a host / port combination
         */
        public HostPort getHostAssignment(TaskAssignment assignment, ParallelismChangeRequest request);

        /**
         * Returns the hostId-hostname mapping of the cluster.
         * 
         * @return the hostId-hostname mapping
         */
        public Map<String, String> getHostIdMapping();
        
        /**
         * Returns the actual timestamp for new executors.
         * 
         * @return the actual timestamp
         */
        public int getTimestamp();

    }

    /**
     * Topology support data implementation.
     * 
     * @author Holger Eichelberger
     */
    private static class TopologySupport implements ITopologySupport {
        
        private TopologyInfo topology;
        private CuratorFramework framework;
        // Time.currentTimeSecs();
        private Map<String, List<String>> hostIdMapping;
        private Map<String, String> hostIdName;
        private int timestamp = Time.currentTimeSecs();
        
        /**
         * Creates a topology supporting object.
         * 
         * @param topology the topology under consideration
         * @param thrift an open thrift connection
         * @param framework an open Curator connect to the zookeepers
         * @throws IOException in case that retrieving data fails
         */
        private TopologySupport(TopologyInfo topology, ThriftConnection thrift, CuratorFramework framework) 
            throws IOException {
            this.topology = topology;
            this.framework = framework;
            try {
                ClusterSummary summary = thrift.getClusterSummary();
                this.hostIdMapping = ThriftConnection.getSupervisorHostIdMapping(summary);
                this.hostIdName = ThriftConnection.getSupervisorIdHostMapping(summary);
            } catch (TException e) {
                throw new IOException(e);
            }
        }

        @Override
        public Map<String, String> getHostIdMapping() {
            return hostIdName;
        }

        /**
         * Returns a host assignment for the existing <code>assignment</code> (host / port used as fallback) and
         * based on the given <code>request</code>. 
         * 
         * @param assignment the actual assignment
         * @param request the change request
         * @return a host / port combination, if possible using a free port with priority to port numbers on the 
         *   specified host that are already used by the topology, free ports on the specified host or the host / port
         *   in <code>assignment</code> as fallback  
         */
        public HostPort getHostAssignment(TaskAssignment assignment, ParallelismChangeRequest request) {
            HostPort result = null;
            String reqHost = request.getHost();
            if (null != reqHost) {
                Map<String, List<Integer>> candidates = new HashMap<String, List<Integer>>();
                List<String> ids = hostIdMapping.get(reqHost);
                if (null != ids && !ids.isEmpty()) {
                    Boolean other = request.otherHostThenAssignment();
                    for (int i = 0; i < ids.size(); i++) {
                        String id = ids.get(i);
                        boolean isCandidate = isCandidate(other, id, assignment.getHostId());
                        if (isCandidate) {
                            addCandidate(reqHost, id, candidates);
                        }
                    }
                } else {
                    LOGGER.info("Cannot identify requested host " + reqHost + " as Storm node. Use the same host "
                        + "naming as Storm.");
                }
                if (!candidates.isEmpty()) {
                    result = findCandidateWithUsedPort(candidates, ThriftConnection.getUsedPorts(topology));
                    if (null == result) {
                        result = findMaxFreePorts(candidates, assignment, request);
                    } // else fallback - executor level parallelization
                }
            }
            // fallback - executor level parallelization
            if (null == result) {
                result = new HostPort(assignment.getHostId(), assignment.getPort());
            }
            return result;
        }
        
        /**
         * Returns whether <code>id</code> is a candidate considering <code>hostId</code> as the requested one
         * and <code>other</code> whether other an other host than <code>hostId</code> is requested.
         * 
         * @param other the other flag (may be <b>null</b>)
         * @param id the id to be checked
         * @param hostId the assigned host id
         * @return <code>true</code> if <code>id</code> is a candidate, <code>false</code> else
         */
        private boolean isCandidate(Boolean other, String id, String hostId) {
            boolean isCandidate;
            if (null == other) {
                isCandidate = true;
            } else {
                isCandidate = (other && !id.equals(hostId)) // requires other
                    || (!other && id.equals(hostId));       // requires same
            }
            return isCandidate; 
        }


        /**
         * Adds a candidate given by host name and host id to <code>candidates</code> if there are free ports.
         * 
         * @param hostName the host name
         * @param hostId the host id
         * @param candidates the candidates (host-id to free ports mapping)
         */
        private void addCandidate(String hostName, String hostId, Map<String, List<Integer>> candidates) {
            try {
                Set<Integer> available = ZkUtils.getAvailableSlots(framework, hostId);
                //Set<Integer> used = ThriftConnection.getUsedPort(topology, hostName);
                //available.removeAll(used);
                if (!available.isEmpty()) {
                    List<Integer> ports = new ArrayList<Integer>();
                    ports.addAll(available);
                    candidates.put(hostId, ports);
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        
        /**
         * Finds a candidate with maximum free ports.
         * 
         * @param candidates the actual candidates 
         * @param assignment the actual assignment
         * @param request the change request
         * @return the selected candidate, may be <b>null</b> if none was selected
         */
        private HostPort findMaxFreePorts(Map<String, List<Integer>> candidates, TaskAssignment assignment, 
            ParallelismChangeRequest request) {
            HostPort result;
            String hostId = null;
            List<Integer> maxPorts = null;
            Boolean other = request.otherHostThenAssignment();
            for (Map.Entry<String, List<Integer>> candidate : candidates.entrySet()) {
                String id = candidate.getKey();
                boolean isCandidate = isCandidate(other, id, assignment.getHostId());
                if (isCandidate) {
                    List<Integer> ports = candidate.getValue();
                    if (null == maxPorts || maxPorts.size() < ports.size()) {
                        hostId = id;
                        maxPorts = ports;
                    }
                }
            }
            
            if (null != hostId) {
                result = new HostPort(hostId, maxPorts.get(0));
            } else {
                result = null;
            }
            return result;
        }

        /**
         * Finds a candidate with a used port in <code>usedPorts</code> (topology ports).
         * 
         * @param candidates the candidates
         * @param usedPorts the limiting set of ports
         * @return the selected candidate, may be <b>null</b> if none was selected
         */
        private HostPort findCandidateWithUsedPort(Map<String, List<Integer>> candidates, Set<Integer> usedPorts) {
            HostPort result;
            String hostId = null;
            int port = -1;
            for (Map.Entry<String, List<Integer>> candidate : candidates.entrySet()) {
                String id = candidate.getKey();
                List<Integer> ports = candidate.getValue();
                for (int p = 0; null == hostId && p < ports.size(); p++) {
                    int pt = ports.get(p);
                    if (usedPorts.contains(pt)) {
                        hostId = id;
                        port = pt; 
                    }
                }
            }
            
            if (null != hostId) {
                result = new HostPort(hostId, port);
            } else {
                result = null;
            }
            return result;
        }

        @Override
        public int getTimestamp() {
            return timestamp;
        }

    }
    
    // checkstyle: stop exception type check

    /**
     * Changes the parallelism of a pipeline. 
     * 
     * @param topology the name of the topology
     * @param changes a mapping of components for which a certain amount of executors shall be 
     *   parallelized (positive) or run in sequence (negative). Will be modified as a side
     *   effect in order to indicate requests that were not fulfilled based on the current 
     *   assignment.
     * @throws IOException if the execution fails for some communication or I/O reason
     */
    public static void changeParallelism(String topology, Map<String, ParallelismChangeRequest> changes) 
        throws IOException {
        if (!ZkUtils.isQmStormVersion()) {
            throw new IOException("Only the QM-specific version of Storm supports this kind of change");
        }
        if (purgeTaskChanges(changes)) {
            try {
                ThriftConnection connection = new ThriftConnection();
                connection.open();
                TopologyInfo tInfo = connection.getTopologyInfoByName(topology);
                Map<Integer, String> taskComponents = ZkUtils.taskComponentMapping(tInfo);
                CuratorFramework framework = ZkUtils.obtainCuratorFramework();
                TopologySupport tData = new TopologySupport(tInfo, connection, framework);
                Assignment stormAssng = ZkUtils.getAssignment(framework, tInfo);
                if (ZkUtils.getWorkerDependenciesCount(stormAssng) > 0) {
                    throw new IOException("Reconfiguring " + topology + ". Change not possible at the moment.");
                }
                if (null != stormAssng) {
                    Assignment newAssng = changeParallelism(stormAssng, changes, taskComponents, tData);
                    if (null != newAssng) {
                        LOGGER.info("Parallelism change request: " + changes);
                        LOGGER.info("Old assignment: " + ZkUtils.toString(stormAssng));
                        LOGGER.info("New assignment: " + ZkUtils.toString(newAssng));
                        ZkUtils.setAssignment(framework, tInfo, newAssng);
                    }
                }
                framework.close();
                connection.close();
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }
        } else {
            LOGGER.info("No actual changes to the parallelism requested: " + changes);
        }
    }

    // checkstyle: resume exception type check

    /**
     * Changes the given assignment. This method is public for testing.
     *
     * @param stormAssng the assignment to change
     * @param changes a mapping of components for which a certain amount of executors shall be 
     *   parallelized (positive) or run in sequence (negative). The individual amount of executors 
     *   must not be negative or exceed the defined number of executors. Will be modified as a side
     *   effect in order to indicate requests that were not fulfilled based on the current 
     *   assignment.
     * @param taskComponents an assignment of task ids to the implementing name of the storm component
     * @param tData topology support information
     * @return the new assignment or <b>null</b> in case of no changes
     * @throws IOException if the execution fails for some communication or I/O reason
     */
    public static Assignment changeParallelism(Assignment stormAssng, Map<String, ParallelismChangeRequest> changes, 
        Map<Integer, String> taskComponents, ITopologySupport tData) throws IOException {
        Assignment result = null;
        Map<String, List<TaskAssignment>> componentAssignments 
            = TaskAssignment.readTaskAssignments(stormAssng, taskComponents);
        Iterator<Map.Entry<String, ParallelismChangeRequest>> iter = changes.entrySet().iterator();
        List<String> workerSequence = new ArrayList<String>();
        boolean modified = false;
        while (iter.hasNext()) {
            Map.Entry<String, ParallelismChangeRequest> change = iter.next();
            String elementName = change.getKey();
            ParallelismChangeRequest request = change.getValue();
            int chg = request.getExecutorDiff();
            List<TaskAssignment> assignments = componentAssignments.get(elementName);
            ChangeResult chgResult = null;
            if (null != assignments) {
                if (chg > 0) {
                    chgResult = increaseParallelism(assignments, request, tData);
                } else if (chg < 0) {
                    chgResult = decreaseParallelism(assignments, request, tData);
                } else if (0 == chg) {
                    chgResult = migrateWorker(assignments, request, tData);
                }
                if (ParallelismChangeRequest.FULFILLED == request.getExecutorDiff()) {
                    iter.remove(); // fulfilled
                }
                if (null != chgResult) {
                    modified = true;
                    assignments.addAll(chgResult.newAssignments);
                    mergeWorkerSequence(workerSequence, chgResult.workerDependencies);
                }
            }
        }
        
        if (modified) {
            result = TaskAssignment.createTaskAssignments(stormAssng, 
                TaskAssignment.createNodeHost(tData.getHostIdMapping(), componentAssignments), 
                componentAssignments, workerSequence);
        }
        return result;
    }
    
    /**
     * Represents a directed worker dependency in terms of their endpoints.
     * 
     * @author Holger Eichelberger
     */
    private static class WorkerDependency {
        private String before;
        private String after;

        /**
         * Creates a worker dependency.
         * 
         * @param before the origin
         * @param after the depending worker
         */
        private WorkerDependency(String before, String after) {
            this.before = before;
            this.after = after;
        }
        
        /**
         * Returns the origin endpoint of the dependency (the one that must be processed before).
         * 
         * @return the origin
         */
        public String getBefore() {
            return before;
        }

        /**
         * Returns the dependent endpoint (the one that must be processed after).
         * 
         * @return the dependent endpoint
         */
        public String getAfter() {
            return after;
        }
        
        @Override
        public int hashCode() {
            return before.hashCode() + after.hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
            boolean result;
            if (obj instanceof WorkerDependency) {
                WorkerDependency other = (WorkerDependency) obj;
                result = before.equals(other.before) && after.equals(other.after);
            } else {
                result = false;
            }
            return result;
        }
        
        @Override
        public String toString() {
            return "<" + before + ";" + after + ">";
        }
    }
    
    /**
     * A parallelism change result.
     * 
     * @author Holger Eichelberger
     */
    private static class ChangeResult {
        private List<TaskAssignment> newAssignments = new ArrayList<TaskAssignment>();
        private List<WorkerDependency> workerDependencies = new ArrayList<WorkerDependency>();

        /**
         * Adds a dependency in terms of worker endpoints.
         * 
         * @param before the origin (the endpoint that must be processed before)
         * @param after the dependent endpoint (the endpoint that must be processed before)
         */
        private void addDependency(String before, String after) {
            WorkerDependency dep = new WorkerDependency(before, after);
            if (!workerDependencies.contains(dep)) {
                workerDependencies.add(dep);
            }
        }
    }
    
    /**
     * Merges worker dependencies into a worker sequence.
     * 
     * @param workerSequence the worker sequence
     * @param dependencies the dependencies
     * @throws IOException in case that the dependencies cannot be fulfilled
     */
    private static void mergeWorkerSequence(List<String> workerSequence, List<WorkerDependency> dependencies) 
        throws IOException {
        for (int d = 0; d < dependencies.size(); d++) {
            WorkerDependency dep = dependencies.get(d);
            String before = dep.getBefore();
            String after = dep.getAfter();
            int afterPos = workerSequence.indexOf(after);
            if (after.equals(before)) { // in-worker migration
                if (afterPos < 0) {
                    workerSequence.add(before);
                }
            } else {
                int beforePos = workerSequence.indexOf(before);
                if (beforePos < 0 && afterPos < 0) { // none is known
                    workerSequence.add(before);
                    workerSequence.add(after);
                } else if (beforePos < 0) { // after exists - insert before behind after
                    workerSequence.add(afterPos + 1, before);
                } else if (afterPos < 0) { // before exists - insert after before
                    workerSequence.add(beforePos, after);
                } else { // both exist, may lead to a conflict
                    if (beforePos < afterPos) {
                        throw new IOException("Cannot fulfill changes as executor dependencies contains cycle with " 
                            + before + " << " + after);
                    }
                }
            }
        }
    }
    
    /**
     * Migrates a worker from one supervisor to another.
     * 
     * @param assignments the assignments
     * @param request the change request
     * @param tData the topology support data
     * @return the new assignments created by this method as well as the related worker dependencies (may be 
     *   <b>null</b> in case of no change)
     */
    private static ChangeResult migrateWorker(List<TaskAssignment> assignments, ParallelismChangeRequest request, 
        ITopologySupport tData) {
        // TODO more detailed spec needed
        ChangeResult result;
        TaskAssignment assng = assignments.get(0); // TODO 0???
        HostPort hostPort = tData.getHostAssignment(assng, request);
        if (null != hostPort && !assng.isSame(hostPort)) {
            result = new ChangeResult();
            String origEndpointId = assng.getEndpointId();
            assignments.get(0).setHostPort(hostPort);
            assignments.get(0).setStartTime(tData.getTimestamp());
            // migration - creation before deletion
            result.addDependency(hostPort.getEndpointId(), origEndpointId);
            request.setRemainingExecutorDiff(ParallelismChangeRequest.FULFILLED);
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Increases the parallelism according to <code>taskChange</code>.
     * 
     * @param assignments the original assignments (existing assignments may be disabled)
     * @param request the causing change request
     * @param tData topology support data
     * @return the new assignments created by this method as well as the related worker dependencies (may be 
     *   <b>null</b> in case of no change)
     */
    private static ChangeResult increaseParallelism(List<TaskAssignment> assignments, 
        ParallelismChangeRequest request, ITopologySupport tData) {
        ChangeResult result = new ChangeResult();
        int change = request.getExecutorDiff();
        HostPort host = null;
        // in decreasing order [max task count first]
        for (int a = assignments.size() - 1; change > 0 && a >= 0; a--) {
            TaskAssignment assng = assignments.get(a);
            String assngEndpointId = assng.getEndpointId();
            if (null == host) {
                host = tData.getHostAssignment(assng, request);
            }
            change = assng.split(change, result.newAssignments, host, tData.getTimestamp());
            result.addDependency(host.getEndpointId(), assngEndpointId);
        }
        if (0 == change) {
            change = ParallelismChangeRequest.FULFILLED;
        }
        request.setRemainingExecutorDiff(change); // feedback / purge
        return result;
    }
    
    /**
     * Decreases the parallelism according to <code>taskChange</code>.
     * 
     * @param assignments the original assignments (existing assignments may be disabled)
     * @param request the causing change request
     * @param tData topology support data
     * @return the new assignments created by this method as well as the related worker dependencies (may be 
     *   <b>null</b> in case of no change)
     */
    private static ChangeResult decreaseParallelism(List<TaskAssignment> assignments, 
        ParallelismChangeRequest request, ITopologySupport tData) {
        ChangeResult result = new ChangeResult();
        int change = request.getExecutorDiff();
        if (change == ParallelismChangeRequest.DELETE) {
            change = 0;
            for (int a = 0; a < assignments.size(); a++) {
                change -= assignments.get(a).getTaskCount();
            }
        }
        HostPort host = null;
        if (assignments.size() > 1 && change < 0) { // otherwise nothing to merge
            int mergePos = assignments.size() - 1;
            int initTaskChange = change;
            TaskAssignment mergeIntoOrig = assignments.get(mergePos);
            String mergeIntoOrigEndpointId = mergeIntoOrig.getEndpointId();
            if (null == host) {
                host = tData.getHostAssignment(mergeIntoOrig, request);
            }
            // copy it - merge is a new assignment
            TaskAssignment mergeInto = new TaskAssignment(mergeIntoOrig, tData.getTimestamp());
            // merge the next ones until change is fulfilled
            for (int a = mergePos - 1; change < 0 && a >= 0; a--) {
                change = mergeInto.merge(assignments.get(a), change, host); // try to merge
            }
            if (change != initTaskChange) {
                mergeIntoOrig.disable();
                result.newAssignments.add(mergeInto);
                result.addDependency(host.getEndpointId(), mergeIntoOrigEndpointId);
            }
        }
        if (0 == change) {
            change = ParallelismChangeRequest.FULFILLED;
        }
        request.setRemainingExecutorDiff(change); // feedback / purge
        return result;
    }

    /**
     * Purges task change entries that cause no change.
     * 
     * @param taskChanges the task changes (to be modified as a side effect)
     * @return <code>true</code> if there are task changes left over, <code>false</code> else
     */
    private static boolean purgeTaskChanges(Map<String, ParallelismChangeRequest> taskChanges) {
        Iterator<Map.Entry<String, ParallelismChangeRequest>> iter = taskChanges.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, ParallelismChangeRequest> entry = iter.next();
            ParallelismChangeRequest change = entry.getValue();
            int diff = change.getExecutorDiff();
            if (null == change || (ParallelismChangeRequest.FULFILLED == diff) 
                || (0 == diff && null == change.getHost())) {
                iter.remove();
            }
        }
        return !taskChanges.isEmpty();
    }

    /**
     * Sleep for <code>ms</code>.
     *
     * @param ms the milliseconds to sleep (ignores negative values)
     */
    public static void sleep(int ms) {
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
            }
        }
    }
    
}
