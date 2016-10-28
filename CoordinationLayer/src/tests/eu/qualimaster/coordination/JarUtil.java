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

        JarEntry entry = new JarEntry(folder);
        jar.putNextEntry(entry);
        jar.closeEntry();
        
        String fileName = folder + "QM.ivml";
        File f = createFileWithFallback(fileName);
        getLogger().info("Including " + f.getAbsolutePath() + " into model jar");
        entry = new JarEntry(fileName);
        jar.putNextEntry(entry);
        putFile(jar, f);
        jar.closeEntry();

        fileName = folder + "QM.rtvil";
        f = createFileWithFallback(fileName);
        getLogger().info("Including " + f.getAbsolutePath() + " into model jar");
        entry = new JarEntry(fileName);
        jar.putNextEntry(entry);
        putFile(jar, f);
        jar.closeEntry();

        fileName = folder + "model.properties";
        f = createFileWithFallback(fileName);
        getLogger().info("Including " + f.getAbsolutePath() + " into model jar");
        entry = new JarEntry(fileName);
        jar.putNextEntry(entry);
        putFile(jar, f);
        jar.closeEntry();

        jar.close();
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
    private static void putFile(ZipOutputStream jar, File source) throws IOException {
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
