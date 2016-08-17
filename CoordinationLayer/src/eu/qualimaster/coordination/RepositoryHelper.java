/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.coordination;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.coordination.MavenMetaInfo.SnapshotVersion;
import eu.qualimaster.easy.extension.internal.ConfigurationInitializer;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import net.ssehub.easy.basics.modelManagement.IModel;
import net.ssehub.easy.basics.modelManagement.ModelInfo;
import net.ssehub.easy.basics.modelManagement.ModelManagement;
import net.ssehub.easy.basics.modelManagement.ModelManagementException;
import net.ssehub.easy.basics.modelManagement.Version;
import net.ssehub.easy.basics.modelManagement.VersionFormatException;
import net.ssehub.easy.basics.modelManagement.VersionedModelInfos;
import net.ssehub.easy.instantiation.core.model.common.VilException;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Executor;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.Script;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.model.ModelQuery;
import net.ssehub.easy.varModel.model.ModelQueryException;
import net.ssehub.easy.varModel.model.Project;
import net.ssehub.easy.varModel.model.datatypes.Compound;

/**
 * Helper methods for repository handling without calling / initializing the repository connector.
 * 
 * @author Holger Eichelberger
 */
public class RepositoryHelper {

    private static final String MAVEN_SNAPSHOT_SUFFIX = "-SNAPSHOT";
    
    private static boolean overrideIfExists = true;
    
    /**
     * Returns whether the model shall be overridden if it already exists.
     * 
     * @return <code>true</code> for override, <code>false</code> else
     */
    public static boolean overrideIfExists() {
        return overrideIfExists;
    }
    
    /**
     * Changes the override if exists.
     * 
     * @param override override or not
     */
    public static void setOverrideIfExists(boolean override) {
        overrideIfExists = override;
    }
    
    /**
     * Creates and initializes a (runtime) configuration.
     * 
     * @param project the project to obtain the configuration for
     * @param newVariablePrefix the prefix to be used for new variables
     * @return the configuration
     */
    public static Configuration createConfiguration(Project project, String newVariablePrefix) {
        Configuration configuration = new Configuration(project);
        try {
            ConfigurationInitializer.initializeConfiguration(configuration, newVariablePrefix);
        } catch (VilException e) {
            getLogger().error("Cannot initialize runtime model: " + e.getMessage());
        }
        return configuration;
    }
    
    /**
     * Finds a compound type.
     * 
     * @param project the project to start searching
     * @param name the name of the compound
     * @return the compound type or <b>null</b> if none was found
     * @throws ModelQueryException in case of violated project access restrictions
     */
    public static Compound findCompound(Project project, String name) throws ModelQueryException {
        return (Compound) ModelQuery.findType(project, name, Compound.class);
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
        ModelInfo<M> info = null;
        if (null != version) {
            Version ver = null;
            if (version.length() > 0) {
                try {
                    ver = new Version(version);
                } catch (VersionFormatException e) {
                    getLogger().error("Obtaining model (fallback to no version given):" + e.getMessage());
                }
            }
            List<ModelInfo<M>> infos = mgt.availableModels().getModelInfo(name, ver);
            if (null != infos && infos.size() > 0) { // more than one shall not happen
                info = infos.get(0);
            }
        }
        if (null == info) {
            List<ModelInfo<M>> infos = mgt.availableModels().getModelInfo(name);
            info = VersionedModelInfos.maxVersion(infos);
        }
        if (null != info && !info.isResolved()) {
            try {
                getLogger().info("Loading model " + info.getName() + " @ " + info.getLocation() + "...");
                mgt.load(info);
                getLogger().info("Loading model " + info.getName() + " @ " + info.getLocation() + " done");
            } catch (ModelManagementException e) {
                getLogger().error("Obtaining model: " + e.getMessage());
            }
        }
        M result = null;
        if (null != info) {
            result = info.getResolved();
        }
        return result;
    }
    
    /**
     * Creates a rt-VIL executor instance.
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
        Executor exec = createExecutor(rtVilModel, folder, event, state);
        exec.addConfiguration(config);
        return exec;
    }

    /**
     * Creates a rt-VIL executor instance.
     * 
     * @param rtVilModel the rt-VIL model
     * @param folder the folder to execute the model within
     * @param config the IVML configuration
     * @param event the adaptation event
     * @param state the system state
     * @return the executor
     */
    public static Executor createExecutor(Script rtVilModel, File folder, 
        net.ssehub.easy.instantiation.core.model.vilTypes.configuration.Configuration config, AdaptationEvent event, 
        FrozenSystemState state) {
        Executor exec = createExecutor(rtVilModel, folder, event, state);
        exec.addConfiguration(config);
        return exec;
    }
    
    /**
     * Creates a rt-VIL executor instance without config.
     * 
     * @param rtVilModel the rt-VIL model
     * @param folder the folder to execute the model within
     * @param event the adaptation event
     * @param state the system state
     * @return the executor
     */
    private static Executor createExecutor(Script rtVilModel, File folder, AdaptationEvent event, 
        FrozenSystemState state) {
        Executor exec = new Executor(rtVilModel);
        exec.addBase(folder);
        exec.addSource(folder);
        exec.addTarget(folder);
        if (rtVilModel.getParameterCount() > 3) {
            exec.addCustomArgument(rtVilModel.getParameter(3).getName(), event);
        }
        if (rtVilModel.getParameterCount() > 4) {
            exec.addCustomArgument(rtVilModel.getParameter(4).getName(), state.getMapping());
        }
        return exec;
    }

    /**
     * Creates a temporary folder for executing the adaptation specification within.
     * 
     * @return the temporary folder
     */
    public static File createTmpFolder() {
        File tmp = null;
        try {
            tmp = File.createTempFile("qmAdapt", "tmp");
            tmp.delete();
            tmp.mkdirs();
            tmp.deleteOnExit();
        } catch (IOException e) {
            getLogger().error("While creating the temp instantiation folder: " + e.getClass().getName()
                + " " + e.getMessage());
        }
        return tmp;
    }

    /**
     * Obtains an artifact from the processing elements repository (Maven).
     * 
     * @param artifactSpec the artifact specification (may be <b>null</b>)
     * @param name the logical name of the local artifact file
     * @param suffix file name extension possibly including a Maven classifier 
     * @param basePath the base path where to locate the target artifact, may be <b>null</b> for the local 
     *   artifact location
     * @return the artifact as a local file, <b>null</b> if not available
     */
    public static File obtainArtifact(String artifactSpec, String name, String suffix, File basePath) {
        return obtainArtifact(artifactSpec, name, null, suffix, basePath);
    }

    /**
     * Obtains an artifact from the processing elements repository (Maven).
     * 
     * @param artifactSpec the artifact specification (may be <b>null</b>)
     * @param name the logical name of the local artifact file
     * @param classifier the optional classifier (may be <b>null</b>) 
     * @param suffix file name extension
     * @param basePath the base path where to locate the target artifact, may be <b>null</b> for the local 
     *   artifact location
     * @return the artifact as a local file, <b>null</b> if not available
     */
    public static File obtainArtifact(String artifactSpec, String name, String classifier, String suffix, 
        File basePath) {
        File result = null;
        if (null != artifactSpec) {
            String outArtifactSpec = artifactSpec;
            if (null != classifier) {
                outArtifactSpec += " (" + classifier + ")";
            }
            try {
                URL url = obtainArtifactUrl(artifactSpec, classifier, suffix);
                try {
                    result = downloadArtifact(outArtifactSpec, url, name, suffix, basePath);
                } catch (IOException e) {
                    getLogger().error("obtain artifact " + outArtifactSpec + " " + url + ": " 
                        + e.getClass().getName() + " " + e.getMessage());
                    result = null;
                }
                if (null == result) {
                    url = obtainArtifactUrl(artifactSpec, classifier, suffix, true);
                    if (null != url) {
                        try {
                            result = downloadArtifact(outArtifactSpec, url, name, suffix, basePath);    
                        } catch (IOException e1) {
                            // ignore e1, already e failed
                            getLogger().error("obtain artifact " + outArtifactSpec + " " + url + ": " 
                                + e1.getClass().getName() + " " + e1.getMessage());
                        }
                    }
                }
            } catch (MalformedURLException e) {
                getLogger().error("obtain artifact " + outArtifactSpec + " " 
                    + artifactSpecToPath(artifactSpec, classifier, suffix) + ": " + e.getMessage());
            }
        }
        return result;
    }

    /**
     * Downloads an artifact.
     * 
     * @param artifactSpec the artifact specification
     * @param url the URL
     * @param name the logical name of the local artifact file
     * @param suffix file name extension
     * @param basePath the base path where to locate the target artifact, may be <b>null</b> for the local 
     *   artifact location
     * @return the artifact as a local file, <b>null</b> if not available
     * @throws IOException in case that downloading failed
     */
    private static File downloadArtifact(String artifactSpec, URL url, String name, String suffix, 
        File basePath) throws IOException {
        InputStream in = url.openStream();
        Path path;
        if (null == basePath) {
            path = createLocalArtifactPath(name, suffix);
        } else {
            File f = new File(basePath, name + suffix);
            f.createNewFile();
            path = f.toPath();
        }
        path.toFile().delete(); // force reload on start
        Files.copy(in, path);
        getLogger().info("obtained artifact " + artifactSpec + " from " + url + " to " + path);
        in.close();
        return path.toFile();
    }
    
    /**
     * Obtains the URL for a specific artifact. In case of snapshot artifacts, the most recently deployed artifact
     * URL is determined.
     * 
     * @param artifactSpec the artifact specification (may be <b>null</b>)
     * @param classifier the optional classifier (may be <b>null</b>) 
     * @param suffix file name extension
     * @return the URL to the artifact
     * @throws MalformedURLException in case that the URL cannot be constructed
     * @see CoordinationConfiguration#getPipelineElementsRepository()
     */
    public static URL obtainArtifactUrl(String artifactSpec, String classifier, String suffix) 
        throws MalformedURLException {
        return obtainArtifactUrl(artifactSpec, classifier, suffix, false);
    }
    
    /**
     * Obtains the URL for a specific artifact. In case of snapshot artifacts, the most recently deployed artifact
     * URL is determined.
     * 
     * @param artifactSpec the artifact specification (may be <b>null</b>)
     * @param classifier the optional classifier (may be <b>null</b>) 
     * @param suffix file name extension
     * @param fallback use the fallback repository
     * @return the URL to the artifact (may be <b>null</b> in case that the fallback respository is not configured)
     * @throws MalformedURLException in case that the URL cannot be constructed
     * @see CoordinationConfiguration#getPipelineElementsRepository()
     */
    private static URL obtainArtifactUrl(String artifactSpec, String classifier, String suffix, boolean fallback) 
        throws MalformedURLException {
        String urlPart = artifactSpecToPath(artifactSpec, classifier, suffix);
        URL url = ArtifactRegistry.getArtifactURL(artifactSpec);
        if (null == url) {
            url = obtainPipelineElementsUrl(urlPart, fallback);
        }
        return url;
    }
    
    /**
     * Creates a local artifact path into {@link CoordinationConfiguration#getLocalArtifactsLocation()}.
     * 
     * @param name the logical name of the artifact
     * @param suffix the optional suffix in case of files (may be <b>null</b>)
     * @return the artifact path
     */
    static Path createLocalArtifactPath(String name, String suffix) {
        if (null == suffix) {
            suffix = "";
        }
        return FileSystems.getDefault().getPath(
            CoordinationConfiguration.getLocalArtifactsLocation(), name + suffix);
    }
    
    /**
     * Creates an URL within the pipeline elements repository.
     * 
     * @param path the path within the repository
     * @param fallback the fallback repository
     * @return the URL (may be <b>null</b> if the repository base url in 
     *   {@link CoordinationConfiguration#getPipelineElementsRepository()} or 
     *   {@link CoordinationConfiguration#getPipelineElementsRepositoryFallback()} is not configured)
     * @throws MalformedURLException in case that creating the URL fails
     */
    private static URL obtainPipelineElementsUrl(String path, boolean fallback) throws MalformedURLException {
        URL repo;
        if (fallback) {
            repo = CoordinationConfiguration.getPipelineElementsRepositoryFallback();
        } else {
            repo = CoordinationConfiguration.getPipelineElementsRepository();
        }
        return null == repo ? null : new URL(repo, path);
    }
    
    /**
     * Returns the actual file for downloading by resolving Maven (snapshot) versions.
     * 
     * @param path URL path within the elements repository
     * @param version the artifact version
     * @param name the artifact name
     * @param classifier the optional classifier (may be <b>null</b>)
     * @param suffix the file suffix (dot plus extension or just extension)
     * @return the resolved file name
     */
    private static String getActualFile(String path, String version, String name, String classifier, String suffix) {
        String result = null;
        if (version.endsWith(MAVEN_SNAPSHOT_SUFFIX)) {
            try {
                version = version.substring(0, version.length() - MAVEN_SNAPSHOT_SUFFIX.length());
                URL meta = obtainPipelineElementsUrl(path + "/maven-metadata.xml", false);
                InputStream is = meta.openStream();
                MavenMetaInfo info = new MavenMetaInfo(is);
                is.close();

                String extension = suffix;
                while (extension.startsWith(".")) {
                    extension = extension.substring(1);
                }
                String verSuffix = null;
                SnapshotVersion sVer = info.getSnapshotVersion(extension, classifier);
                if (sVer != null) {
                    verSuffix = sVer.getValue();
                }
                if (null == verSuffix && null != info) {
                    String sVersion = info.getSnapshotVersion();
                    String sBuild = info.getSnapshotBuild();
                    if (null != sVersion && null != sBuild) {
                        verSuffix = sVersion + "-" + sBuild;  
                    }                    
                }
                result = name + "-" + verSuffix;
                if (null != classifier) {
                    result = result + "-" + classifier;
                }
            } catch (IOException e) {
                getLogger().error("While determining artifact URL: " + e.getClass().getName() + " " + e.getMessage());
            } 
        } 
        if (null == result) {
            result = name + "-" + version; //  fallback and default case for releases
            if (null != classifier) {
                result = result + "-" + classifier;
            }
        }
        if (!suffix.startsWith(".")) {
            suffix = "." + suffix;
        }
        return result + suffix;
    }
    
    /**
     * Turns an artifact specification of path:name:version into path/version/name.
     * 
     * @param artifactSpec the artifact specification
     * @param classifier the optional classifier (may be <b>null</b>)
     * @param suffix the file suffix
     * @return the artifact path
     */
    private static String artifactSpecToPath(String artifactSpec, String classifier, String suffix) {
        String result = null;
        String[] tmp = artifactSpec.split(":");
        if (null == tmp || tmp.length != 3) {
            result = artifactSpec + suffix;
        } else {
            final char separator = '/';
            String groupId = tmp[0];
            String version = tmp[2];
            String name = tmp[1];
            String path = groupId.replace('.', separator) + separator + name + separator + version;
            result = path + separator + getActualFile(path, version, name, classifier, suffix);
        }
        return result;
    }

    /**
     * Returns the logger for this class.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return LogManager.getLogger(RepositoryHelper.class);
    }

}
