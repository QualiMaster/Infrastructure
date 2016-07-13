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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import eu.qualimaster.dataManagement.DataManagementConfiguration;

/**
 * Some HDFS/storage utilities.
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


    /**
     * Stores <code>dataFile</code> either to HDFS (precedence) or to DFS.
     * 
     * @param dataFile the data file
     * @return the target path if successful, <b>null</b> else
     * @throws IOException in case that I/O fails
     */
    public static String store(File dataFile) throws IOException {
        String dataPath = storeToHdfs(dataFile);
        if (null == dataPath) {
            if (null == storeToDfs(dataFile)) {
                throw new IOException("Cannot store file '" + dataFile + "', neither to HDFS nor DFS. "
                    + "Check HDFS/DFS configuration.");
            }
        }
        return dataPath;
    }
    
    /**
     * Stores the data file to the HDFS (alternative) using the Dfs path as prefix.
     * 
     * @return the target path if successful, <b>null</b> else
     * @throws IOException in case that I/O fails
     */
    public static String storeToHdfs(File dataFile) throws IOException {
        String dataPath = null;
        if (!DataManagementConfiguration.isEmpty(DataManagementConfiguration.getHdfsUrl())) {
            String basePath = DataManagementConfiguration.getDfsPath() + "/";
            FileSystem fs = HdfsUtils.getFilesystem();
            Path target = new Path(basePath, dataFile.getName()); 
            fs.copyFromLocalFile(new Path(dataFile.getAbsolutePath()), target);
            dataPath = target.toString();
        }
        return dataPath;
    }
    
    /**
     * Stores the data file to the DFS (alternative).
     * 
     * @return the target path if successful, <b>null</b> else
     * @throws IOException in case that I/O fails
     */
    public static String storeToDfs(File dataFile) throws IOException {
        String dataPath = null;
        if (!DataManagementConfiguration.isEmpty(DataManagementConfiguration.getDfsPath())) {
            File targetPath = new File(DataManagementConfiguration.getDfsPath(), dataFile.getName());
            FileUtils.copyFile(dataFile, targetPath);
            dataPath = targetPath.getAbsolutePath().toString();
        }
        return dataPath;
    }

    /**
     * Deletes a folder, either transparently on DFS or the file system.
     *  
     * @param folder the folder to create (relative)
     * @throws IOException in case that the creation fails
     */
    public static void deleteFolder(File folder, boolean recursive) throws IOException {
        if (!DataManagementConfiguration.isEmpty(DataManagementConfiguration.getHdfsUrl())) {
            FileSystem fs = HdfsUtils.getFilesystem();
            Path target = new Path(DataManagementConfiguration.getDfsPath() + "/" + folder);
            fs.delete(target, recursive);
        } else if (!DataManagementConfiguration.isEmpty(DataManagementConfiguration.getDfsPath())) {
            File targetPath = new File(DataManagementConfiguration.getDfsPath(), folder.getName());
            if (recursive) {
                FileUtils.deleteDirectory(targetPath);
            } else {
                targetPath.delete();
            }
        } else {
            throw new IOException("Delete folder. Check HDFS/DFS configuration.");            
        }
    }
    
    /**
     * Creates a folder, either transparently on DFS or the file system.
     *  
     * @param folder the folder to create (relative)
     * @throws IOException in case that the creation fails
     */
    public static void createFolder(File folder) throws IOException {
        if (!DataManagementConfiguration.isEmpty(DataManagementConfiguration.getHdfsUrl())) {
            FileSystem fs = HdfsUtils.getFilesystem();
            Path target = new Path(DataManagementConfiguration.getDfsPath() + "/" + folder);
            fs.create(target);
        } else if (!DataManagementConfiguration.isEmpty(DataManagementConfiguration.getDfsPath())) {
            File targetPath = new File(DataManagementConfiguration.getDfsPath(), folder.getName());
System.out.println("deleting " + targetPath);
            targetPath.mkdirs();
        } else {
            throw new IOException("Cannot crete folder. Check HDFS/DFS configuration.");            
        }
    }
    
    /**
     * Transparently copies <code>source</code> to <code>target</code> in HDFS or DFS.
     * 
     * @param source the source file
     * @param target the target file
     * @param targetBase shell target be considered as the base path in HDFS or shall the Dfs path be prefixed
     * @return the target path used 
     * @throws IOException in case that copying fails
     */
    public static String copy(File source, File target, boolean absolute) throws IOException {
        String result;
        if (!DataManagementConfiguration.isEmpty(DataManagementConfiguration.getHdfsUrl())) {
            String basePath = absolute ? target + "/" : 
                DataManagementConfiguration.getDfsPath() + "/" + target + "/";
            FileSystem fs = HdfsUtils.getFilesystem();
            copy(fs, basePath, source);
            result = basePath;
        } else if (!DataManagementConfiguration.isEmpty(DataManagementConfiguration.getDfsPath())) {
            File tgt = new File(DataManagementConfiguration.getDfsPath(), target.toString());
System.out.println("copying " + source+" -> "+tgt);
            FileUtils.copyDirectory(source, tgt);
            result = tgt.getAbsolutePath();
        } else {
            throw new IOException("Cannot copy directory. Check HDFS/DFS configuration.");            
        }
        return result;
    }
    
    /**
     * Copies <code>source</code> to <code>fs</code> and <code>basePath</code>.
     * 
     * @param fs the file system
     * @param basePath the actual base path
     * @param source the source file/directory
     * @throws IOException in case that copying fails
     */
    private static void copy(FileSystem fs, String basePath, File source) throws IOException {
        if (source.isDirectory()) {
            String bp = basePath + "/" + source.getName();
            fs.create(new Path(bp));
            File[] files = source.listFiles();
            if (null != files) {
                for (File f : files) {
                    copy(fs, bp, f);
                }
            }
        } else {
            Path target = new Path(basePath, source.getName()); 
            fs.copyFromLocalFile(new Path(source.getAbsolutePath()), target);
        }
    }

}
