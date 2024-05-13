package eu.qualimaster.coordination;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.coordination.RepositoryConnector.Models;
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.events.EventManager;
import net.ssehub.easy.varModel.confModel.Configuration;

/**
 * Utility and helper methods.
 * 
 * @author Holger Eichelberger
 */
public class CoordinationUtils {

    /**
     * Prevents (external) instantiation.
     */
    private CoordinationUtils() {
    }
    
    /**
     * Returns the receiver component or <b>null</b>.
     * 
     * @param component the component to be considered
     * @return the receiver component or <b>null</b> if none was found
     */
    public static Component getReceiverComponent(Component component) {
        Component result = null;
        if (null != component && component.isReceiver()) {
            result = component;
        }
        return result;
    }
    
    /**
     * Obtains the parameter receiver component, either from the mapped node or component implementation name.
     * 
     * @param mapping the name mapping
     * @param pipelineElement the pipeline element of the original receiver
     * @param parameter the parameter name
     * @return the component representing the actual (possibly mapped) receiver (may be <b>null</b> if there is none)
     */
    public static Component getParameterReceiverComponent(INameMapping mapping, String pipelineElement, 
        String parameter) {
        String mappedElt = mapping.getParameterMapping(pipelineElement, parameter);
        Component result = mapping.getPipelineNodeComponent(mappedElt);
        if (null == result) {
            result = mapping.getComponentByImplName(mappedElt);
        }
        return result;
    }
    
    /**
     * Creates a name mapping for a given pipeline with implementing <code>file</code> JAR artifact.
     * 
     * @param pipelineName the name of the pipeline
     * @param file the mapping file (may be <b>null</b>)
     * @return the name mapping, an identity mapping if reading is obviously not possible
     * @throws IOException in case of problems while reading <code>file</code>
     */
    static final INameMapping createMapping(String pipelineName, File file) throws IOException {
        INameMapping result = null;
        TopologyTestInfo info = StormUtils.getLocalInfo(pipelineName);
        if (null != info) {
            File tmp = info.getMappingFile();
            if (null != tmp && tmp.exists() && tmp.canRead()) {
                FileInputStream in = new FileInputStream(tmp);
                result = new NameMapping(pipelineName, in);
                in.close();
            }
        }
        if (null == result && null != file && file.exists() && file.canRead()) {
            JarFile jf = new JarFile(file);
            ZipEntry entry = jf.getEntry(NameMapping.MAPPING_FILE_NAME);
            if (null != entry) {
                result = new NameMapping(pipelineName, jf.getInputStream(entry));
            }
            jf.close();
        }
        if (null == result) {
            result = new IdentityMapping(pipelineName);
        }        
        return result;
    }
    
    /**
     * Loads the mapping of the given pipeline. If successful, the mapping is afterwards available through 
     * {@link CoordinationManager#getNameMapping(String)}.
     * 
     * @param pipelineName the name of the pipeline
     * @return the absolute name of the pipeline jar for submission, may be empty if no jar was obtained
     * @throws IOException in case that obtaining the pipeline jar failed and we are not in testing or that reading the 
     *     mapping failed
     */
    public static final String loadMapping(String pipelineName) throws IOException {
        File topologyJar = obtainPipelineJar(pipelineName);
        String absPath;
        if (null == topologyJar || !topologyJar.exists()) {
            if (!StormUtils.inTesting()) {
                throw new IOException("Topology JAR for pipeline '" + pipelineName + "' not found");
            }
            absPath = ""; // we are in testing, the topologyJar is ignored anyway
        } else {
            absPath = topologyJar.getAbsolutePath();
        }
        INameMapping mapping = CoordinationUtils.createMapping(pipelineName, topologyJar);
        CoordinationManager.registerNameMapping(mapping);
        TopologyTestInfo info = StormUtils.getLocalInfo(pipelineName);
        if (null != info && null != info.getSubTopologyEvent()) {
            EventManager.send(info.getSubTopologyEvent());
        }
        return absPath;
    }
    
    /**
     * Returns the execution phase to be used in the coordination layer.
     * 
     * @return the phase
     */
    public static final Phase getCoordinationPhase() {
        return Phase.MONITORING; // just reading access to frozen variables
    }
    
    /**
     * Obtains the pipeline JAR.
     * 
     * @param pipelineName the pipeline name to return 
     * @return the pipeline JAR (<b>null</b> if not found)
     * @see #getCoordinationPhase()
     * @see RepositoryConnector#getPipelineArtifact(Phase, String)
     */
    public static File obtainPipelineJar(String pipelineName) {
        return RepositoryConnector.obtainPipelineJar(getCoordinationPhase(), pipelineName);
    }
    
    /**
     * Returns the models instance to be used in the coordination layer.
     * 
     * @return the models instance
     */
    public static final Models getCoordinationModels() {
        return RepositoryConnector.getModels(getCoordinationPhase()); 
    }

    /**
     * Returns the IVML configuration to be used in the coordination layer.
     * 
     * @return the IVML configuration instance
     */
    public static final Configuration getCoordinationConfiguration() {
        return getCoordinationModels().getConfiguration();
    }
    
    /**
     * Returns the namespace of the given <code>mapping</code>.
     * 
     * @param mapping the mapping to return the namespace for
     * @return the namespace
     */
    static String getNamespace(INameMapping mapping) {
        return mapping.getPipelineName();
    }

}
