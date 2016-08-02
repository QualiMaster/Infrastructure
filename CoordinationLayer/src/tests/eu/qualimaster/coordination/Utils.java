package tests.eu.qualimaster.coordination;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import eu.qualimaster.coordination.ArtifactRegistry;
import eu.qualimaster.coordination.CoordinationConfiguration;

/**
 * Test utilities.
 * 
 * @author Holger Eichelberger
 */
public class Utils {

    public static final String MODEL_ARTIFACTSPEC = "eu.qualimaster:infrastructure:0.0.1";
    public static final IModelProvider INFRASTRUCTURE_TEST_MODEL_PROVIDER = new IModelProvider() {

        private File tmp;
        
        @Override
        public void provideModel(Properties properties) {
            try {
                tmp = File.createTempFile("qmModelArtifact", ".jar");
                tmp.deleteOnExit();
                URL tmpUrl = tmp.toURI().toURL();
                JarUtil.jarModelArtifact(tmp);
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
        
    };

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
        if (null == contents) {
            contents = new HashSet<File>();
        }
        String tmp = System.getProperty("java.io.tmpdir", null);
        if (null != tmp) {
            File tmpDir = new File(tmp);
            File[] files = tmpDir.listFiles();
            if (null != files) {
                for (int f = 0; f < files.length; f++) {
                    File file = files[f];
                    if (delete) {
                        if (!contents.contains(file)) {
                            file.delete();
                        }
                    } else {
                        contents.add(file);
                    }
                }
            }
        }
        return contents;
    }
    
}
