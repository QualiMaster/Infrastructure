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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import static eu.qualimaster.file.Utils.*;

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
        
        writeFile(jar, folder + "QM.ivml");
        writeFile(jar, folder + "Meta.ivml");
        writeFile(jar, folder + "InfraCfg.ivml");
        writeFile(jar, folder + "PipCfg.ivml");
        writeFile(jar, folder + "QM.rtvil");
        writeFile(jar, folder + "model.properties");

        jar.close();
    }
    
    /**
     * Writes the specified file into the given {@code jar} if the file exists.
     * 
     * @param jar the jar to modify/append
     * @param fileName the name of the file to append
     * @throws IOException in case of read/write problems
     */
    private static void writeFile(JarOutputStream jar, String fileName) throws IOException {
        File f = createFileWithFallback(fileName);
        if (f.exists()) {
            getLogger().info("Including " + f.getAbsolutePath() + " into model jar");
            JarEntry entry = createJarEntry(fileName, "EASy/");
            jar.putNextEntry(entry);
            putFile(jar, f);
            jar.closeEntry();
        }
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
     * Creates a file with fallback to coordination layer.
     * 
     * @param fileName the file name / path to use
     * @return the created vile
     */
    private static File createFileWithFallback(String fileName) {
        File f = new File(Utils.getTestdataDir(), fileName);
        if (!f.exists()) {
            f = new File("../CoordinationLayer/testdata", fileName);
        }
        return f;
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
