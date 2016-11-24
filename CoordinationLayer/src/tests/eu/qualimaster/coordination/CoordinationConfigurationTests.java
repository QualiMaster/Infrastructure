package tests.eu.qualimaster.coordination;

import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import backtype.storm.Config;
import backtype.storm.utils.Utils;
import eu.qualimaster.Configuration;
import eu.qualimaster.common.signal.Constants;
import eu.qualimaster.coordination.CoordinationConfiguration;
import eu.qualimaster.coordination.StormPipelineOptionsSetter;
import eu.qualimaster.coordination.StormUtils;
import eu.qualimaster.infrastructure.InitializationMode;
import eu.qualimaster.infrastructure.PipelineOptions;
import tests.eu.qualimaster.dataManagement.DataManagementConfigurationTests;

/**
 * Tests the configuration.
 * 
 * @author Holger Eichelberger
 */
public class CoordinationConfigurationTests extends DataManagementConfigurationTests {
    
    private static final String MODEL_LOC = "/usr/local/model.jar";

    /**
     * Initialize tests.
     */
    @BeforeClass
    public static void start() {
        CoordinationConfiguration.getProperties(); // force creation of options
    }
    
    @Override
    protected void testDirect() {
        super.testDirect();
        Assert.assertEquals("/null", CoordinationConfiguration.getLocalPipelineElementsRepositoryLocation());
        Assert.assertEquals("/home/storm/artifacts", CoordinationConfiguration.getLocalArtifactsLocation());
        Assert.assertNotNull(CoordinationConfiguration.getPipelineElementsRepository());
        Assert.assertEquals("https://nexus.sse.uni-hildesheim.de/repos/qm/", 
            CoordinationConfiguration.getPipelineElementsRepository().toString());
        Assert.assertNotNull(CoordinationConfiguration.getPipelineElementsRepositoryFallback());
        Assert.assertEquals("https://nexus.sse.uni-hildesheim.de/repos/qmDevel/", 
            CoordinationConfiguration.getPipelineElementsRepositoryFallback().toString());
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
        Assert.assertEquals(CoordinationConfiguration.DEFAULT_SPECIFICPIPSETTINGS_ARTIFACT_SPEC, 
             CoordinationConfiguration.getSpecificPipelineSettingsArtifactSpecification());
        Assert.assertEquals(CoordinationConfiguration.DEFAULT_INIT_MODE, 
             CoordinationConfiguration.getInitializationMode());
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
    
    /**
     * Tests the pipeline options.
     */
    @Test
    public void pipelineOptionsTest() {
        PipelineOptions opts = new PipelineOptions();
        opts.setNumberOfWorkers(5);
        Properties prop = new Properties();
        prop.put(CoordinationConfiguration.PIPELINE_START_SOURCE_AUTOCONNECT, "true");
        prop.put(CoordinationConfiguration.INIT_MODE, InitializationMode.DYNAMIC.name());
        prop.put(Configuration.HOST_EVENT, "local");
        prop.put(Configuration.PORT_EVENT, 1234);
        prop.put(Configuration.EVENT_DISABLE_LOGGING, "aaa,bbb");
        prop.put(Configuration.PIPELINE_INTERCONN_PORTS, "10-20");
        CoordinationConfiguration.configure(prop, false);
        System.out.println("Configured " + prop);
        
        // during submission
        @SuppressWarnings("rawtypes")
        Map stormConf = Utils.readStormConfig();
        StormPipelineOptionsSetter optSetter = new StormPipelineOptionsSetter(stormConf, opts);
        StormUtils.doCommonConfiguration(optSetter);
        System.out.println("Conf " + stormConf);
        System.out.println("OPTS " + opts);
        String[] args = opts.toArgs("pip");
        System.out.println("ARGS " + java.util.Arrays.toString(args));
        
        // in topology
        PipelineOptions options = new PipelineOptions(args);
        Config config = new Config();
        config.setMessageTimeoutSecs(100);
        config.setDebug(false);
        config.put("windowSize", 1 * 30);  // Window size (in secs)
        config.put("windowAdvance", 1);  // Advance of the window (in secs)
        config.put("SUBPIPELINE.NAME", "pip"); //sub-pipeline namespace
        //The settings to optimize the storm performance.
        config.put(Config.TOPOLOGY_RECEIVER_BUFFER_SIZE, 8);
        config.put(Config.TOPOLOGY_TRANSFER_BUFFER_SIZE, 32);
        config.put(Config.TOPOLOGY_EXECUTOR_RECEIVE_BUFFER_SIZE, 16384);
        config.put(Config.TOPOLOGY_EXECUTOR_SEND_BUFFER_SIZE, 16384);
        config.put(Configuration.HOST_EVENT, Configuration.getEventHost());
        config.put(Configuration.PORT_EVENT, Configuration.getEventPort());
        config.put(Configuration.EVENT_DISABLE_LOGGING, Configuration.getEventDisableLogging());
        config.put(Configuration.PIPELINE_INTERCONN_PORTS, Configuration.getPipelinePorts());
        options.toConf(config);
        System.out.println("Pip Config " + config);
        
        Assert.assertEquals("true", config.get(Constants.CONFIG_KEY_SOURCE_AUTOCONNECT));
        Assert.assertEquals(InitializationMode.DYNAMIC.name(), config.get(Constants.CONFIG_KEY_INIT_MODE));
        Assert.assertEquals("local", config.get(Configuration.HOST_EVENT));
        Assert.assertEquals("1234", config.get(Configuration.PORT_EVENT));
        Assert.assertEquals("aaa,bbb", config.get(Configuration.EVENT_DISABLE_LOGGING));
        Assert.assertEquals("10-20", config.get(Configuration.PIPELINE_INTERCONN_PORTS));
        
        CoordinationConfiguration.clear();
    }

}
