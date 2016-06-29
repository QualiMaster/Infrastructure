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
package eu.qualimaster.dataManagement.sources.replay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import eu.qualimaster.dataManagement.DataManagementConfiguration;
import eu.qualimaster.dataManagement.storage.hdfs.HdfsUtils;

/**
 * Implements a HDFS source. [preliminary]
 * 
 * @author Holger Eichelberger
 */
public class HdfsSource implements IReplaySource{

    private String defaultFs;
    private Path hdfsPathToData;

    /**
     * Creates a HDFS source based on {@link DataManagementConfiguration#getHdfsUrl()}.
     * 
     * @param hdfsPathToData the HDFS path to data
     */
    public HdfsSource(String hdfsPathToData) {
        this(DataManagementConfiguration.getHdfsUrl(), hdfsPathToData);
    }

    /**
     * Creates a HDFS source.
     * 
     * @param defaultFs the default HDFS URL
     * @param hdfsPathToData the HDFS path to data
     */
    public HdfsSource(String defaultFs, String hdfsPathToData) {
        this(defaultFs, new Path(hdfsPathToData));
    }

    /**
     * Creates a HDFS source based on {@link DataManagementConfiguration#getHdfsUrl()}.
     * 
     * @param hdfsPathToData the HDFS path to data
     */
    public HdfsSource(Path hdfsPathToData) {
        this(DataManagementConfiguration.getHdfsUrl(), hdfsPathToData);
    }

    /**
     * Creates a HDFS source.
     * 
     * @param defaultFs the default HDFS URL
     * @param hdfsPathToData the HDFS path to data
     */
    public HdfsSource(String defaultFs, Path hdfsPathToData) {
        this.defaultFs = defaultFs;
        this.hdfsPathToData = hdfsPathToData;
    }
    
    @Override
    public BufferedReader open() throws IOException {
        if (null == defaultFs || 0 == defaultFs.length()) {
            throw new IOException("No HDFS data source configured! See Configuration." 
                + DataManagementConfiguration.URL_HDFS);
        }
        FileSystem fs = HdfsUtils.getFilesystem(defaultFs);
        return new BufferedReader(new InputStreamReader(fs.open(hdfsPathToData)));
    }

}