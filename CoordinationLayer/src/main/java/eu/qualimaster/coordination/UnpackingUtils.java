package eu.qualimaster.coordination;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;

import eu.qualimaster.file.Utils;

/**
 * Resource unpacking utilities.
 * 
 * @author Holger Eichelberger
 */
public class UnpackingUtils {

    /**
     * Represents a folder. Call {@link #release()} if not needed anymore.
     * 
     * @author Holger Eichelberger
     */
    public interface IFolderAccess {

        /**
         * Whether this folder has the given sub-folder.
         * 
         * @param path the path to the sub-folder
         * @return <code>true</code> if the sub-folder exists, <code>false</code> else
         */
        public boolean hasFolder(String... path);
        
        /**
         * Unpacks the sub-folder <code>path</code> to <code>target</code>.
         * 
         * @param target the target file
         * @param path the path
         * @throws IOException in case of an I/O problem
         */
        public void unpack(File target, String...path) throws IOException;
        
        /**
         * Releases this instance.
         */
        public void release();
        
    }
    
    /**
     * A physical file system folder.
     * 
     * @author Holger Eichelberger
     */
    public static class FolderAccess implements IFolderAccess {
        
        private File path;
        
        /**
         * Creates the file system folder access instance.
         * 
         * @param path the path to the jar file
         */
        public FolderAccess(File path) {
            this.path = path;
        }

        @Override
        public boolean hasFolder(String... path) {
            File tmp = toFile(this.path, path);
            return tmp.exists() && tmp.isDirectory();
        }

        @Override
        public void unpack(File target, String... path) throws IOException {
            File src = toFile(this.path, path);
            copyIfNotExists(src, target);
        }
        
        @Override
        public void release() {
            // nothing to do
        }
        
    }
    
    /**
     * Turns a (relative) path into a file.
     * 
     * @param base the base path
     * @param path the path
     * @return the file
     */
    static File toFile(File base, String[] path) {
        File tmp = base;
        for (String p : path) {
            tmp = new File(tmp, p);
        }
        return tmp;
    }

    /**
     * A folder represented by a JAR file.
     * 
     * @author Holger Eichelberger
     */
    public static class JarFileAccess implements IFolderAccess {

        private JarFile jf;
        
        /**
         * Creates the JAR folder access instance.
         * 
         * @param path the path to the jar file
         */
        public JarFileAccess(File path) {
            try {
                jf = new JarFile(path);
            } catch (IOException e) {
                LogManager.getLogger(getClass()).error("While creating instance: " + e.getMessage());
            }
        }

        @Override
        public boolean hasFolder(String... path) {
            boolean result = false;
            if (null != jf) {
                String entryName = toEntryName(path) + "/";
                JarEntry entry = jf.getJarEntry(entryName);
                if (null != entry) {
                    result = entry.isDirectory();
                }
            }
            return result;
        }

        /**
         * Turns a path into an entry name.
         * 
         * @param path the path
         * @return the name
         */
        private String toEntryName(String[] path) {
            String entryName = "";
            for (String p : path) {
                if (!entryName.isEmpty()) {
                    entryName += "/";
                }
                entryName += p;
            }
            return entryName;
        }

        @Override
        public void unpack(File target, String... path) throws IOException {
            Set<File> allowed = new HashSet<File>();
            if (null != jf) {
                String prefixEntryName = toEntryName(path) + "/";
                Enumeration<JarEntry> iter = jf.entries();
                while (iter.hasMoreElements()) {
                    JarEntry entry = iter.nextElement();
                    String eName = entry.getName();
                    if (!entry.isDirectory() && eName.startsWith(prefixEntryName) 
                        && eName.length() > prefixEntryName.length()) {
                        File tmp = new File(target, eName.substring(prefixEntryName.length()));
                        File tmpPar = tmp.getParentFile();
                        if (!tmpPar.exists()) { 
                            // don't overwrite existing structures, but allow unpacking if structure did not 
                            // exist before
                            allowed.add(tmpPar);
                        }
                        if (allowed.contains(tmpPar)) {
                            tmpPar.mkdirs();
                            Utils.setDefaultPermissions(tmpPar);            
                            InputStream in = jf.getInputStream(entry);
                            FileUtils.copyInputStreamToFile(in, tmp);
                            Utils.setDefaultPermissions(tmp);
                            in.close();
                        }
                    }
                }
            }
        }

        @Override
        public void release() {
            if (null != jf) {
                try {
                    jf.close();
                }  catch (IOException e) {
                    LogManager.getLogger(getClass()).error("While releasing instance: " + e.getMessage());
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
                        copyIfNotExists(f, new File(target, f.getName()));
                    }
                }
            } else {
                FileUtils.copyFile(source, tgt);
            }
            Utils.setDefaultPermissions(tgt);
        }
    }
    
}
