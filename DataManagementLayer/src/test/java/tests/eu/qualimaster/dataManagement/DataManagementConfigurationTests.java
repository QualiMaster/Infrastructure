package tests.eu.qualimaster.dataManagement;

import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.qualimaster.dataManagement.DataManagementConfiguration;
import tests.eu.qualimaster.ConfigurationTests;

/**
 * Tests the configuration.
 * 
 * @author Holger Eichelberger
 */
public class DataManagementConfigurationTests extends ConfigurationTests {

    /**
     * Initialize tests.
     */
    @BeforeClass
    public static void start() {
        DataManagementConfiguration.getProperties(); // force creation of options
    }
    
    @Override
    protected void testDirect() {
        super.testDirect();
        Assert.assertEquals(DataManagementConfiguration.DEFAULT_URL_HDFS, DataManagementConfiguration.getHdfsUrl());
        Assert.assertEquals(DataManagementConfiguration.DEFAULT_PATH_DFS, DataManagementConfiguration.getDfsPath());
        Assert.assertEquals(DataManagementConfiguration.DEFAULT_PATH_HDFS, DataManagementConfiguration.getHdfsPath());
        // as not configured explicitly
        Assert.assertEquals(DataManagementConfiguration.DEFAULT_PIPELINE_START_DELAY, 
            DataManagementConfiguration.getPipelineStartNotificationDelay());
        Assert.assertEquals(DataManagementConfiguration.getAccountsPath(), DataManagementConfiguration.getDfsPath());
        Assert.assertEquals(DataManagementConfiguration.DEFAULT_PIPELINE_START_SOURCE_AUTOCONNECT, 
            DataManagementConfiguration.getPipelineStartSourceAutoconnect());
        Assert.assertEquals("storm", DataManagementConfiguration.getHdfsUser());
        Assert.assertEquals("storm=hdfs", DataManagementConfiguration.getHdfsGroupMapping());
        Assert.assertEquals(DataManagementConfiguration.DEFAULT_SIMULATION_USE_HDFS, 
            DataManagementConfiguration.useSimulationHdfs());
        Assert.assertEquals(DataManagementConfiguration.getDfsPath(), 
            DataManagementConfiguration.getSimulationLocalPath());
        Assert.assertEquals(DataManagementConfiguration.getDfsPath(), 
            DataManagementConfiguration.getExternalServicePath());
        Assert.assertEquals(DataManagementConfiguration.DEFAULT_EXTERNAL_SERVICE_TUNNELING, 
            DataManagementConfiguration.getExternalServiceTunneling());
        Assert.assertEquals(DataManagementConfiguration.DEFAULT_HBASE_ZNODE_PARENT, 
            DataManagementConfiguration.getHbaseZnodeParent());
        Assert.assertEquals(DataManagementConfiguration.DEFAULT_HBASE_ZOOKEEPER_QUORUM, 
            DataManagementConfiguration.getHbaseZkeeperQuorum());        
    }

    @Override
    protected void testAfterReplay() {
        super.testAfterReplay();
    }
    
    @Override
    protected void buildProperties(Properties prop) {
        super.buildProperties(prop);
    }
    
    @Override
    protected void testViaProperties() {
        super.testViaProperties();
    }

    @Override
    @Test
    public void configurationTest() {
        super.configurationTest();
    }
    
}
