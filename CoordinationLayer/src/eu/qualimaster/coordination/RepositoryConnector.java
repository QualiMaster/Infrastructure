package eu.qualimaster.coordination;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import ch.qos.logback.classic.Level;
import de.uni_hildesheim.sse.easy.loader.ListLoader;
import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.common.logging.QmLogging;
import eu.qualimaster.dataManagement.storage.hdfs.HdfsUtils;
import eu.qualimaster.easy.extension.internal.ConfigurationInitializer;
import eu.qualimaster.infrastructure.InitializationMode;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import net.ssehub.easy.basics.modelManagement.IModel;
import net.ssehub.easy.basics.modelManagement.ModelInitializer;
import net.ssehub.easy.basics.modelManagement.ModelManagement;
import net.ssehub.easy.basics.modelManagement.ModelManagementException;
import net.ssehub.easy.basics.progress.ProgressObserver;
import net.ssehub.easy.instantiation.core.model.buildlangModel.BuildModel;
import net.ssehub.easy.instantiation.core.model.execution.TracerFactory;
import net.ssehub.easy.instantiation.core.model.templateModel.TemplateModel;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Executor;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.RtVilModel;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Script;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.management.VarModel;
import net.ssehub.easy.varModel.model.AbstractVariable;
import net.ssehub.easy.varModel.model.ModelQuery;
import net.ssehub.easy.varModel.model.ModelQueryException;
import net.ssehub.easy.varModel.model.Project;
import net.ssehub.easy.varModel.model.datatypes.Compound;
import net.ssehub.easy.varModel.model.datatypes.IDatatype;
import net.ssehub.easy.varModel.model.datatypes.Reference;
import net.ssehub.easy.varModel.model.values.CompoundValue;
import net.ssehub.easy.varModel.model.values.ContainerValue;
import net.ssehub.easy.varModel.model.values.IntValue;
import net.ssehub.easy.varModel.model.values.ReferenceValue;
import net.ssehub.easy.varModel.model.values.StringValue;
import net.ssehub.easy.varModel.model.values.Value;

/**
 * Represents the access to the pipeline elements repository. Initially, this connector stored exactly one 
 * configuration. However, due to the parallel execution of monitoring and adaptation, we must maintain at least two
 * different instances of the model so that they can be modified independently.
 * 
 * Instead of directly accessing the configuration or the respective scripts, ask for the models instance
 * of an appropriate execution {@link RepositoryConnector.Phase phase} (just ask that there is no parallel overlap
 * with monitoring or adaptation) and ask the obtained instance for the scripts. 
 * 
 * @author Holger Eichelberger
 */
public class RepositoryConnector {

    /**
     * Defines a phase interface, in case that further phases are needed for some reason.
     * 
     * @author Holger Eichelberger
     */
    public interface IPhase {

        /**
         * Returns the name of the phase.
         * 
         * @return the name
         */
        public String name();
        
        /**
         * Returns whether VIL model shall be loaded. Implies that the location to the model cannot be
         * removed due to dynamic linking of VTL.
         * 
         * @return <code>true</code> if VIL shall be loaded, <code>false</code> else
         */
        public boolean doLoadVil();
        
    }
    
    /**
     * Denotes the model usage phases.
     * 
     * @author Holger Eichelberger
     */
    public static enum Phase implements IPhase {
        MONITORING(false),
        ADAPTATION(true); // true shall only be the last one
        
        private boolean loadVil;
        
        /**
         * Creates a new constant.
         * 
         * @param loadVil whether VIL model shall get loaded (see {@link #doLoadVil()}
         */
        private Phase(boolean loadVil) {
            this.loadVil = loadVil;
        }

        @Override
        public boolean doLoadVil() {
            return loadVil;
        }
        
    }
    
    public static final String PREFIX_IVML = "ivml";
    public static final String PREFIX_VIL = "vil";
    public static final String PREFIX_RTVIL = PREFIX_VIL + ".rt";
    public static final String SUFFIX_NAME = ".name";
    public static final String SUFFIX_VERSION = ".version";

    public static final String PROPERTY_IVML_NAME = PREFIX_IVML + SUFFIX_NAME;
    public static final String PROPERTY_IVML_VERSION = PREFIX_IVML + SUFFIX_VERSION;
    public static final String PROPERTY_VIL_NAME = PREFIX_VIL + SUFFIX_NAME;
    public static final String PROPERTY_VIL_VERSION = PREFIX_VIL + SUFFIX_VERSION;    
    public static final String PROPERTY_RTVIL_NAME = PREFIX_RTVIL + SUFFIX_NAME;
    public static final String PROPERTY_RTVIL_VERSION = PREFIX_RTVIL + SUFFIX_VERSION;

    private static final String PIPELINES_VAR_NAME = "pipelines";
    private static final String PIPELINE_NAME_VAR_NAME = "name";
    private static final String PIPELINE_ARTIFACT_VAR_NAME = "artifact";
    
    private static Properties modelProperties = new Properties();
    private static ListLoader loader;
    private static Map<IPhase, Models> models = new HashMap<IPhase, Models>();
    private static int updateCount = 0;
    
    /**
     * Stores and caches the core models for a certain phase. {@link #startUsing()} and {@link #endUsing()} shall
     * be used to mark longer uses of instances of this class to prevent/control model updates. Currently, we assume
     * that one instance of this class is mostly used by one handling thread and at most one infrequently (if at all) 
     * occuring model update thread.
     * 
     * @author Holger Eichelberger
     */
    public static class Models {
        
        private IPhase phase;
        private Configuration configuration;
        private Script adaptationScript;
        private net.ssehub.easy.instantiation.core.model.buildlangModel.Script instantiationScript;
        private RuntimeVariableMapping variableMapping;
        private int usageCounter = 0;
        private Models update;
        private File location;
        
        /**
         * Creates a models instance for a certain phase and caches the models.
         * 
         * @param phase the phase
         * @param location the location (for reloading)
         */
        private Models(IPhase phase, File location) {
            this.phase = phase;
            this.location = location;

            Project project = obtainModel(VarModel.INSTANCE, 
                modelProperties.getProperty(PROPERTY_IVML_NAME, "QM"), 
                modelProperties.getProperty(PROPERTY_IVML_VERSION, null));
            if (null != project) {
                configuration = createConfiguration(project, phase);
                try {
                    variableMapping = ConfigurationInitializer.createVariableMapping(configuration);
                } catch (ModelQueryException e) {
                    LogManager.getLogger(getClass()).error(e.getMessage(), e);
                }
            }
            
            adaptationScript = obtainModel(RtVilModel.INSTANCE, 
                modelProperties.getProperty(PROPERTY_RTVIL_NAME, "QM"), 
                modelProperties.getProperty(PROPERTY_RTVIL_VERSION, null));
            
            if (phase.doLoadVil()) { 
                instantiationScript = obtainModel(BuildModel.INSTANCE, 
                    modelProperties.getProperty(PROPERTY_VIL_NAME, "QM"), 
                    modelProperties.getProperty(PROPERTY_VIL_VERSION, null));
            }
        }

        /**
         * Creates a models instance. Overrides any existing models instance. All parameters must be defined!
         * 
         * @param phase the phase
         * @param configuration the variability configuration
         * @param adaptationScript the adaptation script
         * @param instantiationScript the instantiation script
         * @param variableMapping the variable mapping
         */
        public Models(Phase phase, Configuration configuration, Script adaptationScript, 
                net.ssehub.easy.instantiation.core.model.buildlangModel.Script instantiationScript, 
            RuntimeVariableMapping variableMapping) {
            this.phase = phase;
            this.configuration = configuration;
            this.adaptationScript = adaptationScript;
            this.instantiationScript = instantiationScript;
            this.variableMapping = variableMapping;
            models.put(phase, this);
        }

        /**
         * Prepares an update of the contained model instances.
         * 
         * @param location the actual model location
         */
        private void prepareUpdate(File location) {
            update = new Models(phase, location);
        }
        
        /**
         * Returns the phase.
         * 
         * @return the phase
         */
        public IPhase getPhase() {
            return phase;
        }
        
        /**
         * Returns the configuration.
         * 
         * @return the configuration
         */
        public Configuration getConfiguration() {
            return configuration;
        }
        
        /**
         * Returns the adaptation script.
         * 
         * @return the adaptation script
         */
        public Script getAdaptationScript() {
            return adaptationScript;
        }
        
        /**
         * Returns the instantiation script.
         * 
         * @return the instantiation script (may be <b>null</b> if not loaded)
         */
        public net.ssehub.easy.instantiation.core.model.buildlangModel.Script getInstantiationScript() {
            return instantiationScript;
        }
        
        /**
         * Returns the variable copy mapping linking runtime variable instances.
         * 
         * @return the variable mapping
         */
        public RuntimeVariableMapping getVariableMapping() {
            return variableMapping;
        }

        /**
         * Marks the start of a longer use of this instance.
         */
        public void startUsing() {
            usageCounter++;
        }

        /**
         * Marks the end of a longer use of this instance.
         */
        public void endUsing() {
            if (usageCounter > 0) {
                usageCounter--;
            }
            if (0 == usageCounter && null != update) {
                this.configuration = update.configuration;
                this.adaptationScript = update.adaptationScript;
                this.instantiationScript = update.instantiationScript;
                this.variableMapping = update.variableMapping;
                this.location = update.location;
                update = null;
            }
        }

        /**
         * Forces that VIL-related models are reloaded. Call {@link #startUsing()} before.
         */
        public void reloadVil() {
            BuildModel.INSTANCE.outdateAll();
            TemplateModel.INSTANCE.outdateAll();
        }
       
    }
    
    static {
        initialize();
    }
    
    /**
     * Returns the (first) phase with VIL model.
     * 
     * @return the first phase
     */
    public static Phase getPhaseWithVil() {
        Phase result = null;
        Phase[] phases = Phase.values();
        for (int p = 0; null == result && p < phases.length; p++) {
            if (phases[p].doLoadVil()) {
                result = phases[p];
            }
        }
        return result;
    }
    
    /**
     * Reads the model properties from the specified folder if available. If not available, use default names.
     * 
     * @param folder the folder to search the model properties within
     */
    private static void readModelProperties(File folder) {
        File file = new File(folder, "model.properties");
        if (file.exists()) {
            try {
                FileInputStream in = new FileInputStream(file);
                modelProperties.load(in);
                in.close();
            } catch (IOException e) {
                getLogger().error(e.getMessage());
            }
        }
    }

    /**
     * Updates the model.
     *     
     * @param modelPath the path to the model
     * @param artifactSpec the artifact spec
     * @return the actual path to the model 
     * @throws IOException in case of I/O problems
     */
    private static Path updateModel(Path modelPath, String artifactSpec) throws IOException {
        File modelPathF = modelPath.toFile();
        boolean modelExists = modelPathF.exists();
        boolean update;
        if (RepositoryHelper.overrideIfExists()) {
            update = true;
        } else {
            update = !modelExists;
        }
        if (update) {
            boolean unpack = true;
            File artifact = obtainArtifact(artifactSpec, "infrastructure_model", ".jar");
            if (null != artifact) { // delete only if we have something to override
                if (modelExists) {
                    if (Utils.canDelete(modelPathF)) {
                        FileUtils.deleteDirectory(modelPathF);
                    } else {
                        unpack = false;
                        getLogger().info("Cannot delete model in " + modelPathF + " due to file system permissions. "
                            + "Trying to reuse existing model.");
                    }
                }
            }
            if (unpack) {
                modelPathF.mkdirs();
                Utils.setDefaultPermissions(modelPathF);
                if (null == artifact) {
                    String tmp = CoordinationConfiguration.getLocalConfigModelArtifactLocation();
                    if (null != tmp && !CoordinationConfiguration.isEmpty(tmp)) {
                        artifact = new File(tmp);
                    } else {
                        getLogger().info("Local config model artifact location not available as fallback.");
                    }
                }
                if (null != artifact) {
                    if (artifact.isFile()) {
                        Utils.unjar(artifact, modelPath);
                        getLogger().info("Unpacked infrastructure model into " + modelPath);
                    } else {
                        modelPath = artifact.toPath();
                        getLogger().info("Using model in " + artifact);
                    }
                }
            }
        }
        return modelPath;
    }
    
    /**
     * Initializes the repository connector. This is done automatically on first access, but shall be done during
     * layer startup.
     */
    public static void initialize() {
        if (null == loader) {
            // start up EASy 
            try {
                QmLogging.setLogLevel(Level.INFO);
                QmLogging.disableUnwantedLogging();
                //QmLogging.setLogLevel("org.eclipse.xtext.service.BindModule", Level.INFO);
                loader = new ListLoader();
                loader.setVerbose(false);
                loader.startup();
            } catch (IOException e) {
                getLogger().error(e.getMessage());
            }
            
            if (readModels()) {
                // set tracing 
                TracerFactory.setDefaultInstance(TracerFactory.DEFAULT);
            }
        }
    }
    
    /**
     * Returns the path to the current IVML/VIL model.
     * 
     * @return the path
     */
    public static Path getCurrentModelPath() {
        String pathName = "infrastructure_model";
        if (updateCount > 0) {
            pathName += updateCount;
        }
        return RepositoryHelper.createLocalArtifactPath(pathName, null);
    }

    /**
     * Reads the models.
     * 
     * @return <code>true</code> for success, <code>false</code> else
     */
    private static boolean readModels() {
        boolean result = false;
        String artifactSpec = CoordinationConfiguration.getConfigurationModelArtifactSpecification();
        if (null != artifactSpec && artifactSpec.length() > 0) { 
            try {
                Path modelPath = getCurrentModelPath();
                modelPath = updateModel(modelPath, artifactSpec);
                File modelPathF = modelPath.toFile();

                File propLocation = modelPathF;
                File location = new File(modelPathF, "qm.xml");
                if (!location.exists()) {
                    location = new File(modelPathF, "EASy");
                    propLocation = location;
                } 
                readModelProperties(propLocation);

                ModelInitializer.registerLoader(ProgressObserver.NO_OBSERVER);
                for (Phase phase : Phase.values()) {
                    getLogger().info("loading models for " + phase);
                    ModelInitializer.addLocation(location, ProgressObserver.NO_OBSERVER);
                    Models mod = models.get(phase);
                    if (null == mod) {
                        models.put(phase, new Models(phase, location));
                    } else {
                        mod.prepareUpdate(location);
                    }
                    if (!phase.doLoadVil()) {
                        ModelInitializer.removeLocation(location, ProgressObserver.NO_OBSERVER);
                    }
                    getLogger().info("loading models for " + phase + " done ");
                }
                String settingsTarget = CoordinationConfiguration.getPipelineSettingsLocation();
                if (null != settingsTarget && !CoordinationConfiguration.isEmpty(settingsTarget)) {
                    File settingsFolderF = new File(modelPathF, "settings");
                    if (settingsFolderF.exists()) {
                        unpackSpecificSettingsArtifact(settingsFolderF);
                        File settingsTargetF = new File(settingsTarget);
                        //HdfsUtils.createFolder(settingsTargetF); // initial, be sure
                        HdfsUtils.clearFolder(settingsTargetF);
                        //HdfsUtils.deleteFolder(settingsTargetF, true);
                        String tgt = HdfsUtils.copy(settingsFolderF, settingsTargetF, false, false);
                        getLogger().info("unpacked settings to (" + tgt + ")");
                    } else {
                        getLogger().info("no settings folder in model (" + settingsFolderF.getAbsolutePath() + ")");
                    }
                }
                executeUnpackingPlugins(modelPathF);
            } catch (IOException e) {
                getLogger().error("Extracting Infrastructure Model: " + e.getMessage());
            } catch (ModelManagementException e) {
                getLogger().error("Extracting Infrastructure Model: " + e.getMessage());
            }
            result = true;
        } else {
            getLogger().warn("No infrastructure configuration artifact specification given");
        }
        return result;
    }
    
    /**
     * Executes unpacking plugins.
     * 
     * @param modelPathF the path to the model
     */
    private static void executeUnpackingPlugins(File modelPathF) {
        File[] contained = modelPathF.listFiles();
        if (null != contained) {
            for (File f : contained) {
                IPipelineResourceUnpackingPlugin plugin 
                    = PluginRegistry.getPipelineResourceUnpackingPlugin(f.getName());
                if (null != plugin) {
                    try {
                        plugin.unpack(f);
                    } catch (IOException e) {
                        getLogger().error("While unpacking resource " + f.getName() + " with " + plugin.getName() 
                            + ": " + e.getMessage());
                    }
                }
            }
        }
    }

    
    /**
     * Unpacks the specific pipeline settings from their specific artifact.
     * 
     * @param target the target folder to unpack to
     */
    private static void unpackSpecificSettingsArtifact(File target) {
        String specificSettingsSpec = 
            CoordinationConfiguration.getSpecificPipelineSettingsArtifactSpecification();
        if (!CoordinationConfiguration.isEmpty(specificSettingsSpec)) {
            File artifact = RepositoryHelper.obtainArtifact(specificSettingsSpec, "specific_settings", "settingsSpec", 
                ".zip", null);
            if (null != artifact) {
                try {
                    Utils.unjar(artifact, target);
                } catch (IOException e) {
                    getLogger().info("unpacking specific pipeline settings: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Shall be called at the end of a program run.
     */
    public static void shutdown() {
        if (null != loader) {
            loader.shutdown();
            loader = null;
        }
    }
    
    /**
     * Updates the models at runtime.
     */
    public static void updateModels() {
        
    }
    
    /**
     * Obtains a model considering given name and version.
     * 
     * @param <M> the actual model type
     * @param mgt the model management instance to obtain the model from
     * @param name the name of the model
     * @param version the version (<b>null</b> for obtaining the maximum version)
     * @return the obtained model, <b>null</b> if none was found
     */
    public static <M extends IModel> M obtainModel(ModelManagement<M> mgt, String name, String version) {
        return RepositoryHelper.obtainModel(mgt, name, version); // keep class signature
    }
    
    /**
     * Creates a (runtime) configuration for a certain phase.
     * 
     * @param project the project to create the configuration for
     * @param phase the phase
     * @return the configuration
     */
    public static Configuration createConfiguration(Project project, IPhase phase) {
        boolean initActual = true;
        if (Phase.ADAPTATION == phase) {
            initActual = InitializationMode.STATIC == CoordinationConfiguration.getInitializationMode();
        }
        return RepositoryHelper.createConfiguration(project, phase.name(), initActual);
    }

    /**
     * Derferences a value if needed.
     * 
     * @param val the value to be dereferenced
     * @param config the configuration used to dereference
     * @return the derefenced value or just <code>val</code>
     */
    public static Value dereference(Value val, Configuration config) {
        if (val instanceof ReferenceValue) {
            AbstractVariable var = ((ReferenceValue) val).getValue();
            if (null != var) {
                IDecisionVariable decVar = config.getDecision(var);
                if (null != decVar) {
                    val = decVar.getValue();
                }
            }
        }
        return val;
    }
    
    /**
     * Returns the models for a certain execution phase.
     * 
     * @param phase the phase
     * @return the models (may be <b>null</b> if no infrastructure model was loaded for that phase)
     */
    public static Models getModels(IPhase phase) {
        return models.get(phase);
    }
    
    /**
     * Dereferences a variable if needed.
     * 
     * @param var the variable to be dereferenced
     * @param config the configuration used to dereference
     * @return the dereferenced variable or just <code>var</code>
     */
    public static IDecisionVariable dereference(IDecisionVariable var, Configuration config) {
        if (Reference.TYPE.isAssignableFrom(var.getDeclaration().getType())) {
            var = var.getConfiguration().getDecision(((ReferenceValue) var.getValue()).getValue());
        }
        return var;
    }

    /**
     * Returns the pipeline artifact specification.
     * 
     * @param phase execution phase denoting the configuration to use
     * @param pipelineName the name of the pipeline
     * @return the pipeline artifact in Maven notation (groupId:artifactId:version), <b>null</b> if not 
     *     available / configured
     */
    public static String getPipelineArtifact(IPhase phase, String pipelineName) {
        Models models = getModels(phase);
        models.startUsing();
        String result = getPipelineArtifact(models, pipelineName);
        models.endUsing();
        return result;
    }

    /**
     * Returns the pipeline artifact specification.
     * 
     * @param models the models instance holding the configuration
     * @param pipelineName the name of the pipeline
     * @return the pipeline artifact in Maven notation (groupId:artifactId:version), <b>null</b> if not 
     *     available / configured
     */
    public static String getPipelineArtifact(Models models, String pipelineName) {
        String artifact = null;
        if (null != models) {
            Configuration config = models.getConfiguration();
            artifact = getPipelineArtifact(config, pipelineName);
        } else {
            getLogger().error("Configuration model not loaded - cannot obtain pipeline artifact");
        }
        return artifact;
    }
    
    /**
     * Returns the pipeline artifact specification.
     * 
     * @param config configuration the configuration of the model
     * @param pipelineName the name of the pipeline
     * @return the pipeline artifact in Maven notation (groupId:artifactId:version), <b>null</b> if not 
     *     available / configured
     */
    public static String getPipelineArtifact(Configuration config, String pipelineName) {
        String artifact = null;
        if (null != pipelineName && null != config) {
            ContainerValue pipelines = getPipelines(config);
            if (null != pipelines) {
                for (int e = 0, n = pipelines.getElementSize(); null == artifact && e < n; e++) {
                    Value val = dereference(pipelines.getElement(e), config);
                    if (val instanceof CompoundValue) {
                        CompoundValue pipeline = (CompoundValue) val;
                        String name = pipeline.getStringValue(PIPELINE_NAME_VAR_NAME);
                        if (pipelineName.equals(name)) {
                            artifact = pipeline.getStringValue(PIPELINE_ARTIFACT_VAR_NAME);
                        }
                    } else {
                        getLogger().error("Pipeline value is not a compound rather than " + val.getType());
                    }
                }
            }
        }
        return artifact;
    }

    /**
     * Returns the configured pipelines as a container value containing compounds.
     * 
     * @param config the configuration to take the value from
     * @return the container value or <b>null</b> if not avaluable for some reason
     */
    private static ContainerValue getPipelines(Configuration config) {
        ContainerValue result = null;
        Project model = config.getProject();
        try {
            AbstractVariable decl = ModelQuery.findVariable(model, PIPELINES_VAR_NAME, null);
            if (null != decl) {
                IDecisionVariable var = config.getDecision(decl);
                if (null != var) {
                    Value value = var.getValue();
                    if (value instanceof ContainerValue) {
                        result = (ContainerValue) value;
                        IDatatype cType = result.getContainedType();
                        cType = Reference.dereference(cType);
                        if (!Compound.TYPE.isAssignableFrom(cType)) {
                            getLogger().error("Variable " + PIPELINES_VAR_NAME + " does not contain a collection "
                                + "of compounds.");
                            result = null;
                        }
                    } else {
                        getLogger().error("Variable " + PIPELINES_VAR_NAME + " is not configured correclty.");
                    }
                } else {
                    getLogger().error("Variable " + PIPELINES_VAR_NAME + " not found.");
                }
            } else {
                getLogger().error("Declaration for " + PIPELINES_VAR_NAME + " not found.");
            }
        } catch (ModelQueryException e) {
            getLogger().error(e.getMessage());
        }
        return result;
    }

    /**
     * Obtains an artifact from the processing elements repository (Maven).
     * 
     * @param artifactSpec the artifact specification (may be <b>null</b>)
     * @param name the logical name of the local artifact file
     * @param suffix file name extension possibly including a Maven classifier 
     * @return the artifact as a local file, <b>null</b> if not available
     */
    public static File obtainArtifact(String artifactSpec, String name, String suffix) {
        return RepositoryHelper.obtainArtifact(artifactSpec, name, suffix, null);
    }

    /**
     * Obtains the pipeline JAR.
     * 
     * @param phase the execution phase to use the configuration from
     * @param pipelineName the pipeline name to return 
     * @return the pipeline JAR (<b>null</b> if not found)
     * @see #obtainPipelineJar(Models, String)
     */
    public static File obtainPipelineJar(IPhase phase, String pipelineName) {
        File result = null;
        Models models = getModels(phase);
        if (null != models) {
            models.startUsing();
        }
        result = obtainPipelineJar(getModels(phase), pipelineName);
        if (null != models) {
            models.endUsing();
        }
        return result;
    }
    
    /**
     * Obtains the pipeline JAR.
     * 
     * @param models the models instance holding the configuration (may be <b>null</b>)
     * @param pipelineName the pipeline name to return 
     * @return the pipeline JAR (<b>null</b> if not found)
     */
    public static File obtainPipelineJar(Models models, String pipelineName) {
        // -jar-with-dependencies
        File result = null;
        if (null != models && CoordinationConfiguration.enablePipelineArtifactDownload()) {
            result = obtainArtifact(getPipelineArtifact(models, pipelineName), pipelineName, ".jar");
        }
        if (null == result) {
            String local = CoordinationConfiguration.getLocalPipelineElementsRepositoryLocation();
            if (null != local) {
                result = new File(local, pipelineName + ".jar");
            }
        }
        return result;
    }
    
    /**
     * Creates a temporary folder for executing the adaptation specification within.
     * 
     * @return the temporary folder
     */
    public static File createTmpFolder() {
        return RepositoryHelper.createTmpFolder(); // keep interface
    }
    
    /**
     * Returns the logger for this class.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return LogManager.getLogger(RepositoryConnector.class);
    }

    /**
     * Creates an rt-VIL executor instance.
     * 
     * @param rtVilModel the rt-VIL model
     * @param folder the folder to execute the model within
     * @param config the IVML configuration
     * @param event the adaptation event
     * @param state the system state
     * @return the executor
     */
    public static Executor createExecutor(Script rtVilModel, File folder, Configuration config, AdaptationEvent event, 
        FrozenSystemState state) {
        return RepositoryHelper.createExecutor(rtVilModel, folder, config, event, state); // keep interface
    }
    
    /**
     * Prints the given configuration to the console. [debugging]
     * 
     * @param config the configuration to be printed
     */
    public static void printConfiguration(Configuration config) {
        java.util.Iterator<IDecisionVariable> iter = config.iterator();
        while (iter.hasNext()) {
            IDecisionVariable var = iter.next();
            System.out.println(var.getDeclaration().getName() + " = " + var.getValue());
        }
    }

    /**
     * Returns the Integer value of a decision variable.
     * 
     * @param var the variable to return the Integer for
     * @return the value, only if <code>var</code> is not <b>null</b> and of type Integer
     */
    public static Integer getIntegerValue(IDecisionVariable var) {
        Integer result = null;
        if (null != var) {
            Value value = var.getValue();
            if (value instanceof IntValue) {
                result = ((IntValue) value).getValue();
            }
        }
        return result;
    }

    /**
     * Returns the string value of a decision variable.
     * 
     * @param var the variable to return the string for
     * @return the value, only if <code>var</code> is not <b>null</b> and of type String
     */
    public static String getStringValue(IDecisionVariable var) {
        String result = null;
        if (null != var) {
            Value value = var.getValue();
            if (value instanceof StringValue) {
                result = ((StringValue) value).getValue();
            }
        }
        return result;
    }

    /**
     * Just for testing.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        initialize();
    }

}
