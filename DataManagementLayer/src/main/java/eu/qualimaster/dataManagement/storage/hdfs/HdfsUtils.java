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
package eu.qualimaster.dataManagement.storage.hdfs;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import eu.qualimaster.dataManagement.DataManagementConfiguration;

/**
 * Some HDFS utilities.
 * 
 * @author Holger Eichelberger
 */
public class HdfsUtils {

    /**
     * Returns the default file system according to {@link DataManagementConfiguration#getHdfsUrl()}.
     * 
     * @return the file system
     * @throws IOException if creating the filesystem fails for some I/O reason
     */
    public static FileSystem getFilesystem() throws IOException {
        return getFilesystem(null);
    }

    /**
     * Returns the default file system.
     * 
     * @param defaultFs the default fs URL (if not given, we rely on {@link DataManagementConfiguration#getHdfsUrl()})
     * @return the file system
     * @throws IOException if creating the filesystem fails for some I/O reason
     */
    public static FileSystem getFilesystem(String defaultFs) throws IOException {
        String fsUrl = defaultFs;
        if (null == fsUrl || 0 == fsUrl.length()) {
            fsUrl = DataManagementConfiguration.getHdfsUrl();
        }
        Configuration c = new Configuration();
        c.set("fs.defaultFS", defaultFs);
        c.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        c.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
        return FileSystem.get(c);
    }

}
