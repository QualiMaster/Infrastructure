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
package tests.eu.qualimaster.monitoring;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.JarOutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.coordination.IPipelineResourceUnpackingPlugin;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.coordination.PluginRegistry;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.profiling.AlgorithmProfilePredictionManager;
import eu.qualimaster.monitoring.profiling.IAlgorithmProfileCreator;
import eu.qualimaster.monitoring.profiling.PipelineProfileUnpackingPlugin;
import eu.qualimaster.observables.TimeBehavior;
import tests.eu.qualimaster.coordination.JarUtil;
import tests.eu.qualimaster.coordination.NameMappingTest;

/**
 * Tests the resource unpacking for the algorithm profiles.
 * 
 * @author Holger Eichelberger
 */
public class ResourceUnpackingTests {

    private static final String NAME_PIP = "myPip";
    private static final String NAME_ELT = "myElt";
    private static final String NAME_ALG = "myAlg";
    
    /**
     * Tests resource packing.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testResourceUnpacking() throws IOException {
        // initialize
        IPipelineResourceUnpackingPlugin plugin = new PipelineProfileUnpackingPlugin();
        PluginRegistry.registerPipelineResourceUnpackingPlugin(plugin);
        IAlgorithmProfileCreator creator = AlgorithmProfilePredictionManager.getCreator();
        File tmp = new File(FileUtils.getTempDirectory(), "resourceUnpackingTest");
        FileUtils.deleteQuietly(tmp);
        tmp.mkdirs();
        
        // create test artifacts

        File artifact = new File(tmp, "testArtifact.jar");
        File folder = new File(tmp, "testArtifact");
        folder.mkdirs();

        File profiles = new File(folder, PipelineProfileUnpackingPlugin.PROFILES);
        profiles.mkdirs();
        File algFolder = creator.getPredictorPath(NAME_ALG, profiles.getAbsolutePath(), TimeBehavior.LATENCY);
        algFolder.mkdirs();
        // don't care for the contents
        FileUtils.touch(new File(algFolder, "_map"));
        FileUtils.touch(new File(algFolder, "1"));
        FileUtils.touch(new File(algFolder, "2"));
        
        File classes = new File(folder, "eu/qualimaster/pip/topo");
        classes.mkdirs();
        FileUtils.touch(new File(classes, "Test.class"));
        
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(artifact));
        JarUtil.zipAll(jos, folder);
        jos.close();

        // unpack test artifacts

        algFolder = creator.getPredictorPath(NAME_ALG, profiles.getAbsolutePath(), null);
        test(artifact, new File(tmp, "unpackedArtifact"), algFolder);
        test(folder, new File(tmp, "unpackedFolder"), algFolder);
        
        // clean up
        
        FileUtils.deleteQuietly(tmp);
        PluginRegistry.unregisterPipelineResourceUnpackingPlugin(plugin);
    }
    
    /**
     * Tests whether unpacking leads to the desired results.
     * 
     * @param path the actual artifact/folder to unpack
     * @param target the target file/folder to unpack to
     * @param expected the expected artifact structure
     * @throws IOException if I/O problems occur
     */
    private static void test(File path, File target, File expected) throws IOException {
        IAlgorithmProfileCreator creator = AlgorithmProfilePredictionManager.getCreator();
        target.mkdirs();
        Properties prop = new Properties();
        prop.put(MonitoringConfiguration.PROFILE_LOCATION, target.getAbsolutePath());
        MonitoringConfiguration.configure(prop);

        NameMapping mapping = NameMappingTest.readNameMapping("unpacking.xml", NAME_PIP);
        PluginRegistry.executeUnpackingPlugins(path, mapping);
        File uAlgFolder = creator.getPredictorPath(NAME_PIP, NAME_ELT, NAME_ALG, target.getAbsolutePath(), null);
        
        // assert
        assertEqualDirStructure(expected, uAlgFolder);
    }

    /**
     * Asserts that <code>actual</code> contains an equal directory structure to <code>expected</code>.
     * 
     * @param expected the expected folder
     * @param actual the actual folder
     */
    private static void assertEqualDirStructure(File expected, File actual) {
        Assert.assertTrue(actual + " does not exist", actual.exists());
        File[] eFiles = expected.listFiles();
        File[] aFiles = actual.listFiles();
        if (null == eFiles) {
            Assert.assertNull(aFiles);
        } else {
            for (File e : eFiles) {
                File a = new File(actual, e.getName());
                Assert.assertTrue("File " + a + " does not exist", a.exists());
                if (e.isDirectory()) {
                    assertEqualDirStructure(e, a);
                } else {
                    Assert.assertEquals("Files " + e + " and " + a + " are not equal in length",
                        e.length(), a.length()); // contents is not relevant here
                }
            }
        }
    }

}
