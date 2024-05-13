/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.monitoring.profiling;

import java.io.File;
import java.io.IOException;

import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.INameMapping.Component;
import eu.qualimaster.coordination.UnpackingUtils.IFolderAccess;
import eu.qualimaster.coordination.IPipelineResourceUnpackingPlugin;
import eu.qualimaster.coordination.UnpackingUtils;
import eu.qualimaster.monitoring.MonitoringConfiguration;

/**
 * Unpacks profiling information.
 * 
 * @author Holger Eichelberger
 */
public class PipelineProfileUnpackingPlugin implements IPipelineResourceUnpackingPlugin {

    public static final String PROFILES = "profiles";
    private static final long serialVersionUID = 902273717787064665L;

    @Override
    public void unpack(File path, INameMapping mapping) throws IOException {
        String baseFolder = MonitoringConfiguration.getProfileLocation();
        IFolderAccess access;
        if (path.isFile() && path.getName().endsWith(".jar")) {
            access = new UnpackingUtils.JarFileAccess(path);
        } else if (path.isDirectory()) {
            access = new UnpackingUtils.FolderAccess(path);
        } else {
            access = null;
        }
        // just do artifact unpacking
        if (null != mapping && null != access && !MonitoringConfiguration.isEmpty(baseFolder) ) {
            if (access.hasFolder(PROFILES)) {
                for (String pipelineName : mapping.getPipelineNames()) {
                    for (String nodeName : mapping.getPipelineNodeNames()) {
                        Component comp = mapping.getPipelineNodeComponent(nodeName);
                        if (comp.getContainer().equals(pipelineName)) {
                            unpackComponent(access, pipelineName, nodeName, comp, baseFolder);
                        }
                    }
                }
            }
        }
        access.release();
    }
    
    /**
     * Unpacks the given component <code>comp</code>.
     * 
     * @param access the source folder access
     * @param pipelineName the pipeline name
     * @param element the pipeline element name
     * @param comp the component to unpack
     * @param baseFolder the base folder to unpack to
     * @throws IOException in case of an unpacking problem
     */
    private void unpackComponent(IFolderAccess access, String pipelineName, String element, Component comp, 
        String baseFolder) throws IOException {
        IAlgorithmProfileCreator creator = AlgorithmProfilePredictionManager.getCreator();
        for (String alg : comp.getAlternatives()) {            
            File folder = creator.getPredictorPath(pipelineName, element, alg, baseFolder, null);
            if (!folder.exists()) {
                File algFolder = creator.getPredictorPath(alg, "", null);
                String algFolderName = algFolder.getName();
                if (access.hasFolder(PROFILES, algFolderName)) {
                    access.unpack(folder, PROFILES, algFolderName);
                }
            }
        }
    }
 
    @Override
    public String getName() {
        return "Profile Unpacker";
    }

}
