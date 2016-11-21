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

import org.apache.commons.io.FileUtils;

import eu.qualimaster.coordination.IPipelineResourceUnpackingPlugin;
import eu.qualimaster.monitoring.MonitoringConfiguration;

/**
 * Unpacks profiling information.
 * 
 * @author Holger Eichelberger
 */
class PipelineProfileUnpackingPlugin implements IPipelineResourceUnpackingPlugin {

    private static final long serialVersionUID = 902273717787064665L;

    @Override
    public String getPath() {
        return "profiles";
    }

    @Override
    public void unpack(File dir) throws IOException {
        String baseFolder = MonitoringConfiguration.getProfileLocation();
        if (!MonitoringConfiguration.isEmpty(baseFolder) && dir.exists()) {
            File[] files = dir.listFiles();
            if (null != files) {
                File base = new File(baseFolder);
                if (!base.exists()) {
                    base.mkdirs();
                    Utils.setDefaultPermissions(base);
                }
                for (File f : files) {
                    copyIfNotExists(f, base);
                }
            }
        }
    }

    /**
     * Copies files and folders if they do not exist in <code>target</code>.
     * 
     * @param source the source file
     * @param target the target file
     * @throws IOException in case of I/O read/write problems
     */
    private static void copyIfNotExists(File source, File target) throws IOException {
        File tgt = new File(target, source.getName());
        if (!tgt.exists()) {
            if (source.isDirectory()) {
                tgt.mkdirs();
                File[] files = source.listFiles();
                if (null != files) {
                    for (File f : files) {
                        copyIfNotExists(f, target);
                    }
                }
            } else {
                FileUtils.copyFile(source, tgt);
            }
            Utils.setDefaultPermissions(tgt);
        }
    }

    @Override
    public String getName() {
        return "Profile Unpacker";
    }

}
