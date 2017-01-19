package tests.eu.qualimaster;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.junit.Assert;
import org.junit.Test;

import backtype.storm.Config;
import eu.qualimaster.Configuration;
import eu.qualimaster.IOptionSetter;

/**
 * Tests the configuration.
 * 
 * @author Holger Eichelberger
 */
public class ConfigurationTests {
    
    private static final File TESTDATA = new File(System.getProperty("qm.base.dir", "."), "testdata");
    private static final int ZK_PORT = 3000;
    private static final String ZK1 = "zookeeper.sse.uni-hildesheim.de";
    private static final String ZK2 = "zookeeper2.sse.uni-hildesheim.de";
    
    /**
     * Turns a port and a set of hosts into a set. Direct comparison with configuration option is not possible
     * due to host shuffling.
     * 
     * @param port the zookeeper port
     * @param hosts the host names
     * @return the connect set for comparison
     */
    private static Set<String> toConnectSet(int port, String... hosts) {
        Set<String> result = new HashSet<String>();
        for (int i = 0; i < hosts.length; i++) {
            result.add(hosts[i]);
        }
        result.add(Integer.toString(port));
        return result;
    }

    /**
     * Turns a zookeeper connect string into a set for comparison. Direct comparison of the string is not possible
     * due to host shuffling.
     * 
     * @param connectString the connect string
     * @return the connect set for comparison
     */
    private static Set<String> toConnectSet(String connectString) {
        Set<String> result = new HashSet<String>();
        StringTokenizer tokens = new StringTokenizer(connectString, ",");
        while (tokens.hasMoreTokens()) {
            String t = tokens.nextToken();
            String[] hostPort = t.split(":");
            if (2 == hostPort.length) {
                result.add(hostPort[0]);
                result.add(hostPort[1]);
            } else {
                result.add(t);
            }
        }
        return result;
    }
    
    /**
     * Tests directly after reading the configuration.
     */
    protected void testDirect() {
        Assert.assertEquals("nimbus.sse.uni-hildesheim.de", Configuration.getNimbus());
        Assert.assertEquals(ZK1, Configuration.getZookeeper());
        Assert.assertEquals(ZK_PORT, Configuration.getZookeeperPort());
        Assert.assertEquals(toConnectSet(ZK_PORT, ZK1), toConnectSet(Configuration.getZookeeperConnectString()));
        Assert.assertEquals("qm.sse.uni-hildesheim.de", Configuration.getEventHost());
        Assert.assertEquals(2999, Configuration.getEventPort());
        Assert.assertEquals(111, Configuration.getShutdownSignalWaitTime());
        Assert.assertEquals("eu.qualiMaster.MyEvent,eu.qualiMaster.YourEvent", Configuration.getEventDisableLogging());
        Assert.assertEquals(Configuration.DEFAULT_EVENT_RESPONSE_TIMEOUT, 
            Configuration.getEventResponseTimeout()); // default value
        Assert.assertEquals(6627, Configuration.getThriftPort());
        Assert.assertTrue(Configuration.getPipelineSignalsCurator());
        Assert.assertFalse(Configuration.getPipelineSignalsQmEvents());
        Assert.assertEquals(Configuration.DEFAULT_PIPELINE_INTERCONN_PORTS, Configuration.getPipelinePorts());
        Assert.assertEquals(Configuration.DEFAULT_RETRY_INTERVAL_ZOOKEEPER, Configuration.getZookeeperRetryInterval());
        Assert.assertEquals(Configuration.DEFAULT_RETRY_TIMES_ZOOKEEPER, Configuration.getZookeeperRetryTimes());
        Assert.assertEquals(Configuration.DEFAULT_INIT_MODE, Configuration.getInitializationMode());
        Assert.assertEquals(Configuration.DEFAULT_MONITORING_VOLUME_ENABLED, 
            Configuration.enableVolumeMonitoring());
    }

    /**
     * Specific tests after transferring and re-transferring a configuration.
     */
    protected void testAfterReplay() {
        Assert.assertEquals("eu.qualiMaster.MyEvent,eu.qualiMaster.YourEvent", Configuration.getEventDisableLogging());
        Assert.assertEquals("qm.sse.uni-hildesheim.de", Configuration.getEventHost());
        Assert.assertEquals(2999, Configuration.getEventPort());
    }
    
    /**
     * Builds up properties to be set then on the configuration for testing.
     * 
     * @param prop the properties to be modified as a side effect
     */
    protected void buildProperties(Properties prop) {
        prop.put(Configuration.HOST_ZOOKEEPER, ZK1 + "," + ZK2);
    }
    
    /**
     * Tests for the properties set in {@link #buildProperties(Properties)}.
     */
    protected void testViaProperties() {
        Assert.assertEquals(toConnectSet(ZK_PORT, ZK1, ZK2), toConnectSet(Configuration.getZookeeperConnectString()));
    }
    
    /**
     * Tests reading the configuration.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void configurationTest() {
        // expected values see test.properties
        File file = new File(TESTDATA, "test.properties");
        Assert.assertTrue("Configuration test file " + file.getAbsolutePath() + "does not exist", file.exists());
        System.out.println("Reading " + file.getAbsolutePath());
        Configuration.configure(file);
        System.out.println("Direct test");
        testDirect();

        Map cfg = new HashMap();
        cfg.put(Config.NIMBUS_THRIFT_PORT, 1234);
        Assert.assertEquals(1234, Configuration.getThriftPort(cfg));
        
        Properties prop = new Properties();
        buildProperties(prop);
        Configuration.configure(prop, false);
        testViaProperties();
        
        System.out.println("Replay test");
        final Config config = new Config();
        config.put(Configuration.CONFIG_KEY_STORM_ZOOKEEPER_PORT, 1024);
        List<String> zks = new ArrayList<String>();
        zks.add("localhost");
        zks.add("myserver.de");
        config.put(Configuration.CONFIG_KEY_STORM_ZOOKEEPER_SERVERS, zks);
        Configuration.transferConfigurationTo(new IOptionSetter() {
            
            @Override
            public void setOption(String key, Serializable value) {
                config.put(key, value);
            }
        });
        Configuration.transferConfigurationFrom(config);
        Assert.assertEquals(1024, Configuration.getZookeeperPort());
        Assert.assertEquals(Configuration.DEFAULT_RETRY_INTERVAL_ZOOKEEPER, Configuration.getZookeeperRetryInterval());
        Assert.assertEquals(Configuration.DEFAULT_RETRY_TIMES_ZOOKEEPER, Configuration.getZookeeperRetryTimes());
        Assert.assertEquals("localhost,myserver.de", Configuration.getZookeeper());
        testAfterReplay();
        
        // last:
        Configuration.configureLocal();
        Configuration.getDefaultProperties();
        // ensure fresh state
        Configuration.clear();
    }
    
    /**
     * Turns <code>values</code> into a list.
     * 
     * @param <T> the type of elements
     * @param values the values
     * @return the list
     */
    @SafeVarargs
    protected static <T> List<T> toList(T... values) {
        List<T> result = new ArrayList<T>();
        for (T v : values) {
            result.add(v);
        }
        return result;
    }
    
    /**
     * Turns <code>values</code> into a list.
     * 
     * @param <T> the type of elements
     * @param values the values
     * @return the list
     */
    @SafeVarargs
    protected static <T> Set<T> toSet(T... values) {
        Set<T> result = new HashSet<T>();
        for (T v : values) {
            result.add(v);
        }
        return result;
    }    
    
    /**
     * Asserts that all elements in <code>expected</code> are also in <code>actual</code>.
     * 
     * @param <T> the type of elements
     * @param expected the expected elements
     * @param actual the actual elements
     */
    protected static <T> void assertSet(List<T> expected, Set<T> actual) {
        Assert.assertNotNull(actual);
        Set<T> tmp = new HashSet<T>();
        tmp.addAll(actual);
        tmp.removeAll(expected);
        Assert.assertTrue("unmatched: " + tmp, tmp.isEmpty());
    }
    
    /**
     * Tests utility functions.
     * 
     * @throws MalformedURLException shall not occur
     */
    @Test
    public void testUtilities() throws MalformedURLException {
        Assert.assertTrue(Configuration.toSet(null).isEmpty());
        Assert.assertEquals(toSet("here"), Configuration.toSet("here"));
        Assert.assertEquals(toSet("here", "there"), Configuration.toSet("here,there"));
        Assert.assertEquals(toSet("here", "there"), Configuration.toSet("here, there"));
        
        Assert.assertNull(Configuration.toUrl("abba"));
        Assert.assertEquals(new URL("http://www.sse.de"), Configuration.toUrl("http://www.sse.de"));
        
        // cover reusable and reused method
        MyCfg mc = new MyCfg();
        Assert.assertNotNull(mc.createUrl("key", new URL("http://www.sse.de")));
        Assert.assertNotNull(Configuration.getProperties());
        Assert.assertFalse(Configuration.isEmpty(null));
        Assert.assertTrue(Configuration.isEmpty(""));
        Assert.assertFalse(Configuration.isEmpty("here"));
    }
    
    /**
     * An extended configuration for accessing protected parts.
     * 
     * @author Holger Eichelberger
     */
    private class MyCfg extends Configuration {

        /**
         * Creates an Integer configuration option.
         * 
         * @param key the property key
         * @param dflt the default value
         * @return the configuration option instance
         */
        public ConfigurationOption<URL> createUrl(String key, URL dflt) {
            return createUrlOption(key, dflt);
        }
        
        /**
         * Tests the (double) creation of configuration options.
         */
        private void testOpts() {
            ConfigurationOption<Boolean> opt = createBooleanOption("myKey", false);
            opt.toString();
            try {
                createBooleanOption("myKey", false);
                Assert.fail("No exception for double option creation");
            } catch (IllegalArgumentException e) {
                // fine
            }
        }
        
    }

    /**
     * Tests the (double) creation of configuration options.
     */
    @Test
    public void testConfigurationOption() {
        MyCfg cfg = new MyCfg();
        cfg.testOpts();
    }
    
}
