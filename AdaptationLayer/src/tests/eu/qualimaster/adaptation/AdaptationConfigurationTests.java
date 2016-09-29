package tests.eu.qualimaster.adaptation;

import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.qualimaster.adaptation.AdaptationConfiguration;
import tests.eu.qualimaster.monitoring.MonitoringConfigurationTests;

/**
 * Tests the configuration.
 * 
 * @author Holger Eichelberger
 */
public class AdaptationConfigurationTests extends MonitoringConfigurationTests {
    
    /**
     * Initialize tests.
     */
    @BeforeClass
    public static void start() {
        AdaptationConfiguration.getProperties(); // force creation of options
    }
    
    @Override
    protected void testDirect() {
        super.testDirect();
        Assert.assertEquals("qm.sse.uni-hildesheim.de", AdaptationConfiguration.getAdaptationHost());
        Assert.assertEquals(AdaptationConfiguration.DEFAULT_PORT_ADAPTATION, 
            AdaptationConfiguration.getAdaptationPort());
        Assert.assertEquals(AdaptationConfiguration.DEFAULT_ADAPTATION_RTVIL_TRACERFACTORY, 
            AdaptationConfiguration.getAdaptationRtVilTracerFactory());
        Assert.assertTrue(AdaptationConfiguration.enableAdaptationRtVilLogging());
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
