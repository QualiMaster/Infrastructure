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
package eu.qualimaster.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
     * Creates the directory named by this abstract pathname, including any necessary but nonexistent parent 
     * directories. Sets {@link #setDefaultPermissions(File) the default file permissions} to all created folders. 
     * Note that if this operation fails it may have succeeded in creating some of the necessary parent directories.
     *
     * @param dir the directory/directories to be created
     * @return <code>true</code> if and only if the directory was created,
     *     along with all necessary parent directories; <code>false</code> otherwise
     */
    public static boolean mkdirs(File dir) {
        boolean result = false;
        if (!dir.exists()) {
            try {
                result = recMkdirNoTopPermissions(dir.getCanonicalFile());
            } catch (IOException e) {
                // not possible
            }
        } else {
            setDefaultPermissions(dir);
        }
        return result;
    }
    
    /**
     * Creates folders recursively and sets the permissions on all created folders except for the topmost existing one.
     *  
     * @param dir the folder to create
     * @return <code>true</code> if and only if the directory was created,
     *     along with all necessary parent directories; <code>false</code> otherwise
     */
    private static boolean recMkdirNoTopPermissions(File dir) {
        boolean result = false;
        if (!dir.exists()) {
            File par = dir.getParentFile();
            if (null != par) {
                result = recMkdirNoTopPermissions(par);
            }
            result |= dir.mkdir();
            setDefaultPermissions(dir);
        }
        return result;
    }
    
    /**
     * Creates a file output stream to write to the file represented by the specified <code>File</code> object. 
     * File <code>File</code> will be set to the {@link #setDefaultPermissions(File) default permissions} upon closing. 
     * A new <code>FileDescriptor</code> object is created to represent this file connection. <br/>
     * If the file exists but is a directory rather than a regular file, does not exist but cannot be created, or cannot
     * be opened for any other reason then a <code>FileNotFoundException</code> is thrown.
     *
     * @param  file the file to be opened for writing.
     * @return the file output stream
     * @throws FileNotFoundException  if the file exists but is a directory rather than a regular file, does not exist 
     *     but cannot be created, or cannot be opened for any other reason
     * @throws  SecurityException  if a security manager exists and its <code>checkWrite</code> method denies write 
     *     access to the file.
     * @see        java.io.File#getPath()
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkWrite(java.lang.String)
     */
    public static FileOutputStream createFileOutputStream(File file) throws FileNotFoundException {
        return new FileOutputStreamWithDefaultPermissions(file);
    }

    /**
     * Creates a file output stream to write to the file represented by the specified <code>File</code> object. 
     * File <code>File</code> will be set to the {@link #setDefaultPermissions(File) default permissions} upon closing. 
     * A new <code>FileDescriptor</code> object is created to represent this file connection. <br/>
     * If the file exists but is a directory rather than a regular file, does not exist but cannot be created, or cannot
     * be opened for any other reason then a <code>FileNotFoundException</code> is thrown.
     *
     * @param  file the file to be opened for writing.
     * @param  append if <code>true</code>, then bytes will be written to the end of the file rather than the beginning
     * @return the file output stream
     * @throws FileNotFoundException  if the file exists but is a directory rather than a regular file, does not exist 
     *     but cannot be created, or cannot be opened for any other reason
     * @throws  SecurityException  if a security manager exists and its <code>checkWrite</code> method denies write 
     *     access to the file.
     * @see        java.io.File#getPath()
     * @see        java.lang.SecurityException
     * @see        java.lang.SecurityManager#checkWrite(java.lang.String)
     */
    public static FileOutputStream createFileOutputStream(File file, boolean append) throws FileNotFoundException {
        return new FileOutputStreamWithDefaultPermissions(file, append);
    }

    /**
     * Constructs a FileWriter object given a File object. Upon closing, the 
     * {@link FileOutputStreamWithDefaultPermissions#setDefaultPermissions(File) default permissions} will be set. 
     * [convenience]
     *
     * @param file a File object to write to.
     * @return the file writer
     * @throws IOException  if the file exists but is a directory rather than
     *         a regular file, does not exist but cannot be created,
     *         or cannot be opened for any other reason
     */
    public static FileWriter createFileWriter(File file) throws IOException {
        return new FileWriterWithDefaultPermissions(file);
    }

    /**
     * Constructs a FileWriter object given a File object. If the second
     * argument is <code>true</code>, then bytes will be written to the end
     * of the file rather than the beginning. Upon closing, the 
     * {@link FileOutputStreamWithDefaultPermissions#setDefaultPermissions(File) default permissions} will be set. 
     * [convenience]
     *
     * @param file  a File object to write to
     * @param append    if <code>true</code>, then bytes will be written
     *        to the end of the file rather than the beginning
     * @return the file writer
     * @throws IOException  if the file exists but is a directory rather than
     *         a regular file, does not exist but cannot be created,
     *         or cannot be opened for any other reason
     */
    public static FileWriter createFileWriter(File file, boolean append) throws IOException {
        return new FileWriter(file, append);
    }
    
    /**
     * Sets the default permissions for unpacked files.
     * 
     * @param file the file to set the permissions for
     */
    public static void setDefaultPermissions(File file) {
        FileOutputStreamWithDefaultPermissions.setDefaultPermissions(file);
    }
    
    /**
     * Returns whether the given file has the default file permissions (as far as available).
     * 
     * @param file the file to test
     * @return <code>true</code> whether the default permissions are given, <code>false</code> else
     */
    public static boolean hasDefaultPermissions(File file) {
        return FileOutputStreamWithDefaultPermissions.hasDefaultPermissions(file);
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