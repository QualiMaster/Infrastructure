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
package tests.eu.qualimaster;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarOutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.file.Utils;

/**
 * Tests for the file utilities.
 * 
 * @author Holger Eichelberger
 */
public class FileTests {
    
    /**
     * Tests the file utils.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testFileUtils() throws IOException {
        File tmp = new File(FileUtils.getTempDirectory(), "testFile");
        FileUtils.deleteQuietly(tmp);
        tmp.mkdirs();
        File f = new File(tmp, "testFile.fil");
        FileUtils.deleteQuietly(f);

        FileOutputStream fos = Utils.createFileOutputStream(f);
        fos.write(10);
        fos.close();
        Assert.assertTrue(f.exists());
        Assert.assertTrue(Utils.hasDefaultPermissions(f));
        FileUtils.deleteQuietly(f);

        fos = Utils.createFileOutputStream(f, true);
        fos.write(10);
        fos.close();
        Assert.assertTrue(f.exists());
        Assert.assertTrue(Utils.hasDefaultPermissions(f));
        FileUtils.deleteQuietly(f);

        FileWriter fw = Utils.createFileWriter(f);
        fw.write(10);
        fw.close();
        Assert.assertTrue(f.exists());
        Assert.assertTrue(Utils.hasDefaultPermissions(f));
        FileUtils.deleteQuietly(f);

        fw = Utils.createFileWriter(f, true);
        fw.write(10);
        fw.close();
        Assert.assertTrue(f.exists());
        Assert.assertTrue(Utils.hasDefaultPermissions(f));
        FileUtils.deleteQuietly(f);

        File dir = new File(tmp, "d");
        Assert.assertTrue(Utils.mkdirs(dir));
        Assert.assertTrue(dir.exists());
        Assert.assertTrue(Utils.hasDefaultPermissions(dir));
        Assert.assertFalse(Utils.mkdirs(dir));
        File dirE = new File(dir, "e");
        File dirEF = new File(dirE, "f");
        File dirEFG = new File(dirEF, "g");
        
        Assert.assertFalse(Utils.canDelete(dirEFG));
        
        Assert.assertTrue(Utils.mkdirs(dirEFG));
        assertFile(dirE);
        assertFile(dirEF);
        assertFile(dirEFG);
        File f1 = new File(dirEFG, "1");
        FileUtils.touch(f1);
        Utils.setDefaultPermissions(f1);
        assertFile(f1);
        
        Assert.assertTrue(Utils.canDelete(dirEFG));
        
        FileUtils.deleteQuietly(f);
    }
    
    /**
     * Tests {@link Utils#sleep(int)}.
     */
    @Test
    public void sleepTest() {
        long now = System.currentTimeMillis();
        Utils.sleep(500);
        long diff = System.currentTimeMillis() - now;
        Assert.assertTrue(Math.abs(diff - 500) < 100);
    }
    
    /**
     * Performs tests on zip/jar operations in Utils.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void zipTests() throws IOException {
        File tmp = new File(FileUtils.getTempDirectory(), "testFile");
        FileUtils.deleteQuietly(tmp);
        tmp.mkdirs();

        File artifact = new File(tmp, "testArtifact.jar");
        File folder = new File(tmp, "testArtifact");
        folder.mkdirs();

        // don't care for the contents
        FileUtils.touch(new File(folder, "_map"));
        FileUtils.touch(new File(folder, "1"));
        FileUtils.touch(new File(folder, "2"));
        File f1 = new File(folder, "f");
        f1.mkdir();
        FileUtils.touch(new File(f1, "a.java"));
        
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(artifact));
        Utils.zipAll(jos, folder);
        jos.close();
        
        File unpack = new File(tmp, "testArtifact");
        Path p = Paths.get(unpack.toURI());
        unpack.mkdirs();
        Utils.unjar(artifact, p);
        
        assertFile(new File(unpack, "_map"));
        assertFile(new File(unpack, "1"));
        assertFile(new File(unpack, "2"));
        assertFile(new File(unpack, "f"));
        assertFile(new File(unpack, "f/a.java"));
        
        File unpack2 = new File(tmp, "testArtifact2");
        unpack2.mkdirs();
        Path p1 = Paths.get(artifact.toURI());
        Path p2 = Paths.get(unpack2.toURI());
        Utils.unjar(p1, p2);

        assertFile(new File(unpack, "_map"));
        assertFile(new File(unpack, "1"));
        assertFile(new File(unpack, "2"));
        assertFile(new File(unpack, "f"));
        assertFile(new File(unpack, "f/a.java"));
    }
    
    /**
     * Asserts existence and permissions of <code>file</code>.
     * 
     * @param file the file to assert
     */
    private static void assertFile(File file) {
        Assert.assertTrue(file.exists());
        Assert.assertTrue(Utils.hasDefaultPermissions(file));
    }

}
