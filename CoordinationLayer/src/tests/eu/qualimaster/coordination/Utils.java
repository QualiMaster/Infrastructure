package tests.eu.qualimaster.coordination;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.Set;

import eu.qualimaster.coordination.ArtifactRegistry;
import eu.qualimaster.coordination.CoordinationConfiguration;
import tests.eu.qualimaster.TestHelper;

/**
 * Test utilities.
 * 
 * @author Holger Eichelberger
 */
public class Utils {

    /**
     * Defines a model provider for initializing test models.
     * Additional to the interface, this class facilitates the specification of the root folder to be used.
     * 
     * @author Sascha El-Sharkawy
     */
    public static class ModelProvider implements IModelProvider {
        private final String rootFolder;
        private File tmp;
        
        /**
         * sole constructor for this class.
         * @param rootFolder Specifies the root folder to be used, <tt>null</tt> will use the default folder.
         *     The folder must end with a forward slash <tt>/</tt>.
         */
        public ModelProvider(String rootFolder) {
            this.rootFolder = rootFolder;
        }
        
        @Override
        public void provideModel(Properties properties) {
            try {
                tmp = File.createTempFile("qmModelArtifact", ".jar");
                tmp.deleteOnExit();
                URL tmpUrl = tmp.toURI().toURL();
                if (null == rootFolder) {
                    JarUtil.jarModelArtifact(tmp);
                } else {
                    JarUtil.jarModelArtifact(rootFolder, tmp);
                }
                ArtifactRegistry.defineArtifact(MODEL_ARTIFACTSPEC, tmpUrl);
                properties.put(CoordinationConfiguration.CONFIG_MODEL_ARTIFACT_SPEC, MODEL_ARTIFACTSPEC);
            } catch (IOException e) {
                System.err.println("disabling test model artifact due to: " + e.getMessage());
            }
        }
        
        @Override
        public void dispose() {
            ArtifactRegistry.undefineArtifact(MODEL_ARTIFACTSPEC);
            boolean success = tmp.delete();
            System.out.println("deleting " + tmp + " " + success);
        }
        
    }
    
    public static final String MODEL_ARTIFACTSPEC = "eu.qualimaster:infrastructure:0.0.1";
    public static final IModelProvider INFRASTRUCTURE_TEST_MODEL_PROVIDER = new ModelProvider(null);

    private static IModelProvider modelProvider;
    
    /**
     * Defines a model provider for initializing test models (wherever they come from).
     * 
     * @author qin
     */
    public interface IModelProvider {

        /**
         * Provides the model and changes <code>properties</code> accordingly.
         * 
         * @param properties the QM configuration properties (may be changed as a side effect)
         */
        public void provideModel(Properties properties);
        
        /**
         * Disposes this provider.
         */
        public void dispose();
    }
    
    /**
     * Prevents external instantiation.
     */
    private Utils() {
    }
    
    /**
     * Defines the model provider. When changing the provider, please consider {@link #dispose()}.
     * 
     * @param provider the new model provider
     */
    public static void setModelProvider(IModelProvider provider) {
        modelProvider = provider;
    }
    
    /**
     * Disposes the actual model provider.
     */
    public static void dispose() {
        if (null != modelProvider) {
            modelProvider.dispose();
        }
    }

    /**
     * Configures the infrastructure for local execution.
     * 
     * @param zookeeperPort the port to use for zookeeper connections
     */
    public static void configure(int zookeeperPort) {
        Properties prop = CoordinationConfiguration.getDefaultProperties();
        prop.put(CoordinationConfiguration.PORT_ZOOKEEPER, zookeeperPort);
        
        if (null != modelProvider) {
            modelProvider.provideModel(prop);
        }
        CoordinationConfiguration.configure(prop);
    }
    
    /**
     * Configures the infrastructure for local execution.
     */
    public static void configure() {
        Properties prop = CoordinationConfiguration.getDefaultProperties();
        if (null != modelProvider) {
            modelProvider.provideModel(prop);
        }
        CoordinationConfiguration.configure(prop);
    }
    
    /**
     * Returns the testing directory.
     * 
     * @return the testing directory
     */
    public static final File getTestdataDir() {
        return new File(System.getProperty("qm.base.dir", "."), "testdata");
    }
    
    /**
     * Tracks the files / folders or deletes untracked files or folders in the temp directory.
     * This method may help getting rid of logs created but not deleted by the Storm local cluster.
     * 
     * @param contents the contents of the temp dir (top-level only)
     * @param delete if <code>false</code> collect the files, else if <code>true</code> delete undeleted
     * @return <code>contents</code> or a new instance created if <code>contents</code> is <b>null</b>
     */
    public static Set<File> trackTemp(Set<File> contents, boolean delete) {
        return TestHelper.trackTemp(contents, delete);
    }
    
}
