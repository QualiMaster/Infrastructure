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
package tests.eu.qualimaster.coordination;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Some JAR utilities for using / testing the various QM layers.
 * 
 * @author Holger Eichelberger
 * @author Sascha El-Sharkawy
 */
public class JarUtil {

    /**
     * Creates the Jar model artifact in <code>file</code>.
     * 
     * @param file the file to be created
     * @throws IOException in case that creating the artifact fails
     */
    public static void jarModelArtifact(File file) throws IOException {
        jarModelArtifact("EASy/", file);
    }
    
    /**
     * Creates the Jar model artifact in <code>file</code>.
     * 
     * @param folder Specifies the root folder of the model files, which shall be packed into the JAR.
     * @param file the file to be created
     * @throws IOException in case that creating the artifact fails
     */
    public static void jarModelArtifact(final String folder, File file) throws IOException {
        Manifest mf = new Manifest();
        JarOutputStream jar = new JarOutputStream(new FileOutputStream(file), mf);

        JarEntry entry = createJarEntry(folder, "EASy/");
        jar.putNextEntry(entry);
        jar.closeEntry();
        
        String fileName = folder + "QM.ivml";
        File f = createFileWithFallback(fileName);
        getLogger().info("Including " + f.getAbsolutePath() + " into model jar");
        entry = createJarEntry(fileName, "EASy/");
        jar.putNextEntry(entry);
        putFile(jar, f);
        jar.closeEntry();

        fileName = folder + "QM.rtvil";
        f = createFileWithFallback(fileName);
        getLogger().info("Including " + f.getAbsolutePath() + " into model jar");
        entry  = createJarEntry(fileName, "EASy/");
        jar.putNextEntry(entry);
        putFile(jar, f);
        jar.closeEntry();

        fileName = folder + "model.properties";
        f = createFileWithFallback(fileName);
        getLogger().info("Including " + f.getAbsolutePath() + " into model jar");
        entry = createJarEntry(fileName, "EASy/");
        jar.putNextEntry(entry);
        putFile(jar, f);
        jar.closeEntry();

        jar.close();
    }
    
    /**
     * Normalizes the target and creates a JarEntry for the target. This mean that if the target starts not with the
     * normalization, the beginning part (folder) is exchanged by the normalization. This is done as later steps assume
     * that the path starts with <tt>EASy/</tt>.
     * @param target The filename (path) to be packed into the JAR file.
     * @param normalization The new root folder of all files. If <tt>null</tt> no normalization will be performed.
     * @return The normalized target location within the JAR archive.
     */
    private static JarEntry createJarEntry(String target, String normalization) {
        JarEntry entry = null;
        
        if (null != normalization && null != target) {
            int pos = target.indexOf("/");
            if (!target.contains(normalization) && pos > -1) {
                target = normalization + target.substring(pos + 1);
            }
        }
        entry = new JarEntry(target);
        
        return entry;
    }
    
    /**
     * Puts the given files into a ZIP archive.
     * 
     * @param target the target file
     * @param files the files to put into the archive
     * @throws IOException in case that creating the artifact fails
     */
    public static void zip(File target, File... files) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(target));

        for (File f : files) {
            getLogger().info("Including " + f.getAbsolutePath() + " into zip");
            ZipEntry entry = new ZipEntry(f.getName());
            zip.putNextEntry(entry);
            putFile(zip, f);
            zip.closeEntry();
        }
        zip.close();
    }

    /**
     * Zips all files in <code>file</code>.
     * 
     * @param zos the ZIP output stream
     * @param file the file to zip (including sub-directories)
     * @throws IOException if I/O problems occur
     */
    public static void zipAll(ZipOutputStream zos, File file) throws IOException {
        zipAll(zos, file, file.getCanonicalPath());
    }
    
    /**
     * Zips all files in <code>file</code>.
     * 
     * @param zos the ZIP output stream
     * @param file the file to zip (including sub-directories)
     * @param base the canonical base path 
     * @throws IOException if I/O problems occur
     */
    private static void zipAll(ZipOutputStream zos, File file, String base) throws IOException {
        String name = file.getCanonicalPath();
        boolean add = true;
        if (name.startsWith(base) && name.length() > base.length()) {
            name = name.substring(base.length() + 1);
            name = name.replace("\\", "/"); // ZIP convention
        } else {
            add = false;
        }

        if (file.isDirectory()) {
            if (add) {
                ZipEntry entry = new ZipEntry(name + "/");
                zos.putNextEntry(entry);
                zos.closeEntry();
            }
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    zipAll(zos, f, base);
                }
            }
        } else {
            if (add) {
                ZipEntry entry = new ZipEntry(name);
                zos.putNextEntry(entry);
                putFile(zos, file);
                zos.closeEntry();
            }
        }
    }
    
    /**
     * Creates a file with fallback to coordination layer.
     * 
     * @param fileName the file name / path to use
     * @return the created vile
     */
    private static File createFileWithFallback(String fileName) {
        File f = new File(Utils.getTestdataDir(), fileName);
        if (!f.exists()) {
            f = new File("../CoordinationLayer/testdata", fileName); // TODO make clean
        }
        return f;
    }

    /**
     * Puts the given file at the current position into <code>jar</code> stream.
     * 
     * @param jar the jar stream to put the file into
     * @param source the source file
     * @throws IOException in case that putting the file into <code>jar</code> fails
     */
    public static void putFile(ZipOutputStream jar, File source) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(source));
        putFile(jar, in);
        in.close();
    }

    /**
     * Puts the given stream at the current position into <code>jar</code> stream.
     * 
     * @param jar the jar stream to put the file into
     * @param source the source stream
     * @throws IOException in case that putting the file into <code>jar</code> fails
     */
    private static void putFile(ZipOutputStream jar, InputStream source) throws IOException {
        if (null != source) {
            byte[] buffer = new byte[1024];
            while (true) {
                int count = source.read(buffer);
                if (count == -1) {
                    break;
                }
                jar.write(buffer, 0, count);
            }
            jar.closeEntry();
            source.close();
        }
    }
    
    /**
     * Simple testing.
     * 
     * @param args ignored
     * @throws IOException exception
     */
    public static void main(String[] args) throws IOException {
        jarModelArtifact(new File("modelArtifact.jar"));
    }
    
    /**
     * Returns the logger for this class.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return LogManager.getLogger(JarUtil.class);
    }

}
