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
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.permission.FsPermission;

import eu.qualimaster.dataManagement.DataManagementConfiguration;

/**
 * Some HDFS/storage utilities.
 * 
 * @author Holger Eichelberger
 */
public class HdfsUtils {

    /**
     * Returns the default HDFS URL.
     * 
     * @return the default HDFS URL (empty if not configured)
     */
    private static String getHdfsUrl() {
        return DataManagementConfiguration.getHdfsUrl();
    }

    /**
     * Returns the default DFS path.
     * 
     * @return the default DFS path (empty if not configured)
     */
    private static String getHdfsPath() {
        return DataManagementConfiguration.getHdfsPath();
    }
    
    /**
     * Returns the default DFS path.
     * 
     * @return the default DFS path (empty if not configured)
     */
    private static String getDfsPath() {
        return DataManagementConfiguration.getDfsPath();
    }
    
    /**
     * Returns whether <code>value</code> is empty (@link {@link #EMPTY_VALUE}).
     * 
     * @param value the value to be tested
     * @return <code>true</code> if empty, <code>false</code> else
     */
    public static boolean isEmpty(String value) {
        return DataManagementConfiguration.isEmpty(value);
    }
    
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
            fsUrl = getHdfsUrl();
        }
        Configuration c = new Configuration();
        c.set("fs.defaultFS", fsUrl);
        c.set("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
        c.set("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
        String user = DataManagementConfiguration.getHdfsUser();
        if (!isEmpty(user)) {
            c.set("hadoop.security.service.user.name.key", user);
        }
        String groupMapping = DataManagementConfiguration.getHdfsGroupMapping();
        if (!isEmpty(groupMapping)) {
            c.set("hadoop.security.group.mapping", groupMapping);
        }
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
        if (!isEmpty(getHdfsUrl())) {
            String basePath = getHdfsPath() + "/";
            FileSystem fs = getFilesystem();
            Path target = new Path(basePath, dataFile.getName()); 
            createPath(fs, target);
            fs.copyFromLocalFile(new Path(dataFile.getAbsolutePath()), target);
            dataPath = target.toString();
        }
        return dataPath;
    }
    
    /**
     * Creates a path if it does not exist.
     * 
     * @param fs the file system
     * @param target the target path
     * @throws IOException in case that I/O fails
     */
    private static void createPath(FileSystem fs, Path target) throws IOException {
        if (!fs.exists(target)) {
            fs.mkdirs(target);
            fs.setPermission(target, FsPermission.valueOf("drwxrwxrwx"));
        }
    }
    
    /**
     * Stores the data file to the DFS (alternative).
     * 
     * @return the target path if successful, <b>null</b> else
     * @throws IOException in case that I/O fails
     */
    public static String storeToDfs(File dataFile) throws IOException {
        String dataPath = null;
        if (!isEmpty(getDfsPath())) {
            File targetPath = new File(getDfsPath(), dataFile.getName());
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
        if (!isEmpty(getHdfsUrl())) {
            FileSystem fs = getFilesystem();
            Path target = new Path(getHdfsPath() + "/" + folder);
            if (fs.exists(target)) {
                fs.delete(target, recursive);
            }
        } else if (!isEmpty(getDfsPath())) {
            File targetPath = new File(getDfsPath(), folder.getName());
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
        if (!isEmpty(getHdfsUrl())) {
            FileSystem fs = getFilesystem();
            Path target = new Path(getHdfsPath() + "/" + folder);
            createPath(fs, target);
        } else if (!isEmpty(getDfsPath())) {
            File targetPath = new File(getDfsPath(), folder.getName());
            targetPath.mkdirs();
        } else {
            throw new IOException("Cannot create folder. Check HDFS/DFS configuration.");            
        }
    }
    
    /**
     * Deletes all files in <code>folder</code>.
     * 
     * @param folder the folder to delete the files within
     * @throws IOException in case that deleting fails
     */
    public static void clearFolder(File folder) throws IOException {
        if (!isEmpty(getHdfsUrl())) {
            FileSystem fs = getFilesystem();
            Path target = new Path(getHdfsPath() + "/" + folder);
            if (fs.exists(target)) {
                RemoteIterator<LocatedFileStatus> iter = fs.listFiles(target, false);
                while (iter.hasNext()) {
                    LocatedFileStatus file = iter.next();
                    if (!file.isDirectory()) {
                        fs.delete(file.getPath(), false);
                    }
                }
            }
        } else if (!isEmpty(getDfsPath())) {
            File targetPath = new File(getDfsPath(), folder.getName());
            File[] files = targetPath.listFiles();
            if (null != files) {
                for (File f : files) {
                    if (!f.isDirectory()) {
                        f.delete();
                    }
                }
            }
        } else {
            throw new IOException("Cannot cleanup folder. Check HDFS/DFS configuration.");            
        }
    }
    
    /**
     * Transparently copies <code>source</code> to <code>target</code> in HDFS or DFS.
     * 
     * @param source the source file
     * @param target the target file
     * @param targetBase shell target be considered as the base path in HDFS or shall the Dfs path be prefixed
     * @param includeTopLevel include the top folder to be copied, i.e., created
     * @return the target path used 
     * @throws IOException in case that copying fails
     */
    public static String copy(File source, File target, boolean absolute, boolean includeTopLevel) throws IOException {
        String result;
        if (!isEmpty(getHdfsUrl())) {
            String basePath = absolute ? target + "/" : getHdfsPath() + "/" + target + "/";
            FileSystem fs = getFilesystem();
            copy(fs, basePath, source, includeTopLevel);
            result = basePath;
        } else if (!isEmpty(getDfsPath())) {
            File tgt = new File(getDfsPath(), target.toString());
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
     * @param includeTopLevel include the top folder to be copied, i.e., created
     * @throws IOException in case that copying fails
     */
    private static void copy(FileSystem fs, String basePath, File source, boolean includeTopLevel) throws IOException {
        if (source.isDirectory()) {
            String bp = basePath;
            if (includeTopLevel) {
                bp += "/" + source.getName();
            }
            createPath(fs, new Path(bp));
            File[] files = source.listFiles();
            if (null != files) {
                for (File f : files) {
                    copy(fs, bp, f, true);
                }
            }
        } else {
            Path target = new Path(basePath, source.getName()); 
            fs.copyFromLocalFile(new Path(source.getAbsolutePath()), target);
        }
    }

}
