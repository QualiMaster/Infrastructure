package tests.eu.qualimaster.coordination;

import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.coordination.CoordinationConfiguration;
import tests.eu.qualimaster.dataManagement.DataManagementConfigurationTests;

/**
 * Tests the configuration.
 * 
 * @author Holger Eichelberger
 */
public class CoordinationConfigurationTests extends DataManagementConfigurationTests {
    
    private static final String MODEL_LOC = "/usr/local/model.jar";
    
    @Override
    protected void testDirect() {
        super.testDirect();
        Assert.assertEquals("/null", CoordinationConfiguration.getLocalPipelineElementsRepositoryLocation());
        Assert.assertEquals("/home/storm/artifacts", CoordinationConfiguration.getLocalArtifactsLocation());
        Assert.assertNotNull(CoordinationConfiguration.getPipelineElementsRepository());
        Assert.assertEquals("https://nexus.sse.uni-hildesheim.de/repos/qm/", 
            CoordinationConfiguration.getPipelineElementsRepository().toString());
        Assert.assertEquals("eu.qualimaster:InfrastructureModel:0.0.2", 
            CoordinationConfiguration.getConfigurationModelArtifactSpecification());
        Assert.assertEquals(CoordinationConfiguration.EMPTY_VALUE, 
            CoordinationConfiguration.getLocalConfigModelArtifactLocation());
        Assert.assertEquals(CoordinationConfiguration.EMPTY_VALUE, 
            CoordinationConfiguration.getShutdownProcedure());
        Assert.assertEquals(CoordinationConfiguration.EMPTY_VALUE, 
            CoordinationConfiguration.getShutdownProcedureConfiguration());
        Assert.assertEquals(CoordinationConfiguration.DEFAULT_DELETE_PROFILING_PIPELINES, 
             CoordinationConfiguration.deleteProfilingPipelines());
        Assert.assertEquals(CoordinationConfiguration.DEFAULT_PIPELINE_SETTINGS_LOCATION, 
             CoordinationConfiguration.getPipelineSettingsLocation());
        Assert.assertEquals(CoordinationConfiguration.DEFAULT_DETAILED_PROFILING, 
             CoordinationConfiguration.getProfilingMode());
        Assert.assertEquals(CoordinationConfiguration.DEFAULT_PROFILE_LOCATION, 
             CoordinationConfiguration.getProfileLocation());
    }

    @Override
    protected void testAfterReplay() {
        super.testAfterReplay();
    }
    
    @Override
    protected void buildProperties(Properties prop) {
        super.buildProperties(prop);
        prop.put(CoordinationConfiguration.LOCAL_CONFIG_MODEL_ARTIFACT_LOCATION, MODEL_LOC);
    }
    
    @Override
    protected void testViaProperties() {
        super.testViaProperties();
        Assert.assertEquals(MODEL_LOC, CoordinationConfiguration.getLocalConfigModelArtifactLocation());
    }

    @Override
    @Test
    public void configurationTest() {
        super.configurationTest();
    }
    
}
