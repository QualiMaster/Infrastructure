package tests.eu.qualimaster;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import backtype.storm.Config;
import eu.qualimaster.Configuration;

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
     * Turns a port and a set of hosts into a zookeeper connect string.
     * 
     * @param port the zookeeper port
     * @param hosts the host names
     * @return the connect string
     */
    private static String toConnectString(int port, String... hosts) {
        String result = "";
        for (int i = 0; i < hosts.length; i++) {
            if (i > 0) {
                result += ",";
            }
            result += hosts[i];
            result += ":";
            result += port;
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
        Assert.assertEquals(toConnectString(ZK_PORT, ZK1), Configuration.getZookeeperConnectString());
        Assert.assertEquals("qm.sse.uni-hildesheim.de", Configuration.getEventHost());
        Assert.assertEquals(2999, Configuration.getEventPort());
        Assert.assertEquals(111, Configuration.getShutdownSignalWaitTime());
        Assert.assertEquals("eu.qualiMaster.MyEvent,eu.qualiMaster.YourEvent", Configuration.getEventDisableLogging());
        Assert.assertEquals(Configuration.DEFAULT_EVENT_RESPONSE_TIMEOUT, 
            Configuration.getEventResponseTimeout()); // default value
        Assert.assertEquals(6627, Configuration.getThriftPort());
        Assert.assertTrue(Configuration.getPipelineSignalsCurator());
        Assert.assertFalse(Configuration.getPipelineSignalsQmEvents());
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
        Assert.assertEquals(toConnectString(ZK_PORT, ZK1, ZK2), Configuration.getZookeeperConnectString());
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
        System.out.println("Reading " + file);
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
        Config config = new Config();
        Configuration.transferConfigurationTo(config);
        Configuration.transferConfigurationFrom(config);
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
    }
    
}
