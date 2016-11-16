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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Some file utilities (without linking to more libraries).
 * 
 * @author Holger Eichelberger
 */
public class Utils {

    /**
     * Unjars a JAR file.
     * 
     * @param jarPath the jar file to extract
     * @param destPath the destination directory (created if it does not exist)
     * @throws IOException in case of IO problems
     */
    public static void unjar(Path jarPath, Path destPath) throws IOException {
        unjar(jarPath.toFile(), destPath.toFile());
    }

    /**
     * Unjars a JAR file.
     * 
     * @param jarFile the jar file to extract (no unpack if <b>null</b>)
     * @param destPath the destination directory (created if it does not exist)
     * @throws IOException in case of IO problems
     */
    public static void unjar(File jarFile, Path destPath) throws IOException {
        unjar(jarFile, destPath.toFile());
    }

    /**
     * Extracts a JAR file.
     * 
     * @param jarFile the jar file to extract (no unpack if <b>null</b>)
     * @param destDir the destination directory (created if it does not exist)
     * @throws IOException in case of IO problems
     */
    public static void unjar(File jarFile, File destDir) throws IOException {
        if (null != jarFile) {
            JarFile jar = new JarFile(jarFile);
            Enumeration<JarEntry> enumEntries = jar.entries();
            byte[] buffer = new byte[1024];
            while (enumEntries.hasMoreElements()) {
                JarEntry file = enumEntries.nextElement();
                File f = new File(destDir, file.getName());
                if (file.isDirectory()) { // if its a directory, create it
                    f.mkdirs();
                    continue;
                }
                File fParent = f.getParentFile();
                if (!fParent.exists()) {
                    fParent.mkdirs();
                }
                InputStream is = jar.getInputStream(file); // get the input stream
                FileOutputStream fos = new FileOutputStream(f);
                int readCount = 0;
                do {
                    readCount = is.read(buffer);
                    if (readCount > 0) {
                        fos.write(buffer, 0, readCount);
                    }
                } while (readCount > 0);
                fos.close();
                is.close();
                setDefaultPermissions(f);
            }
            jar.close();
        }
    }
    
    /**
     * Sets the default permissions for unpacked files.
     * 
     * @param file the file to set the permissions for
     */
    public static void setDefaultPermissions(File file) {
        file.setWritable(true); // for TSI installation
        file.setReadable(true); // for TSI installation
    }
    
    /**
     * Returns whether we can delete <code>file</code>, i.e., a single file or an entire directory with 
     * all contained files.
     * 
     * @param file the file to check
     * @return <code>true</code> if deletion is possible, <code>false</code> else
     */
    public static boolean canDelete(File file) {
        boolean canDelete = file.canRead() && file.canWrite();
        if (canDelete && file.isDirectory()) {
            File[] contained = file.listFiles();
            if (null != contained) {
                for (File f : contained) {
                    canDelete = canDelete(f);
                    if (canDelete) {
                        break;
                    }
                }
            }
        }
        return canDelete;
    }
    
    /**
     * Sleeps for the given time.
     * 
     * @param millis the sleep time in milli seconds (ignored if not positive)
     */
    public static final void sleep(int millis) {
        if (millis >= 0) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
            }
        }
    }
    
}
