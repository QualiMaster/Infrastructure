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
package tests.eu.qualimaster.coordination;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import eu.qualimaster.coordination.ArtifactRegistry;
import eu.qualimaster.coordination.CoordinationConfiguration;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.ProfileControl;
import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.ProfileControl.IProfileExecution;
import eu.qualimaster.coordination.RepositoryConnector.Models;
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.profiling.IProfile;
import eu.qualimaster.coordination.profiling.IProfileControlParser;
import eu.qualimaster.coordination.profiling.ParseResult;
import eu.qualimaster.coordination.profiling.ProcessingEntry;
import eu.qualimaster.coordination.profiling.ProfileControlParserFactory;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper.ProfileData;
import eu.qualimaster.infrastructure.PipelineOptions;
import net.ssehub.easy.varModel.confModel.Configuration;
import tests.eu.qualimaster.storm.Naming;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the profiling control mechanism and the related parser(s).
 * 
 * @author Holger Eichelberger
 */
public class ProfileControlTests {

    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        Utils.setModelProvider(Utils.INFRASTRUCTURE_TEST_MODEL_PROVIDER);
        Utils.configure();
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        Utils.dispose();
    }
    
    /**
     * A simple profile for testing.
     * 
     * @author Holger Eichelberger
     */
    private class TestProfile implements IProfile {

        private String familyName;
        private String algorithmName;
        private Configuration cfg;
        private File dataFile;

        /**
         * Creates the profile.
         * 
         * @param familyName the family name
         * @param algorithmName the algorithm name
         * @param cfg the configuration
         * @param dataFile the (initial) data file
         */
        private TestProfile(String familyName, String algorithmName, Configuration cfg, File dataFile) {
            this.familyName = familyName;
            this.algorithmName = algorithmName;
            this.cfg = cfg;
            this.dataFile = dataFile;
        }
        
        @Override
        public String getFamilyName() {
            return familyName;
        }

        @Override
        public String getAlgorithmName() {
            return algorithmName;
        }

        @Override
        public Configuration getConfiguration() {
            return cfg;
        }

        @Override
        public File getDataFile() {
            return dataFile;
        }
        
    }
    
    /**
     * Tests the simple text parser.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testSimpleParser() throws IOException {
        File testDir = Utils.getTestdataDir();
        File ctlFile = new File(testDir, "profile.ctl");
        File dataFile = new File(testDir, "profile.data"); 
        Models models = RepositoryConnector.getModels(Phase.MONITORING);
        IProfile profile = new TestProfile(Naming.NODE_PROCESS_FAMILY, Naming.NODE_PROCESS_ALG1, 
            models.getConfiguration(), dataFile);

        // assuming this returns the simple parser
        IProfileControlParser parser = ProfileControlParserFactory.INSTANCE.getParser(ctlFile);
        Assert.assertNotNull(parser);
        ParseResult result = parser.parseControlFile(ctlFile, profile);
        
        Assert.assertNotNull(result);
        // no import, shall be the same
        Assert.assertEquals(1, result.getDataFiles().size());
        Assert.assertEquals(dataFile, result.getDataFiles().get(0));
        // from testdata/profile.ctl
        assertEquals(result.getExecutors(), 1, 1, 2, 3);
        assertEquals(result.getTasks(), 1, 2, 2, 2);
        assertEquals(result.getWorkers(), 1, 2, 3, 4);
        List<Serializable> params = result.getParameters().get("window");
        assertEquals(params, 400, 500, 600, 1000);
    }
    
    /**
     * Tests the simple parser with import.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testSimpleParserImport() throws IOException {
        final String artifactSpec = "eu.qualiMaster:testProfile:2.0";
        File testDir = Utils.getTestdataDir();
        File impCtl = new File(testDir, "profileImport/profile.ctl");
        File impData = new File(testDir, "profileImport/profile.data");
        File tmp = File.createTempFile("qmProfileControlTest", ".zip");
        URL tmpUrl = tmp.toURI().toURL();
        JarUtil.zip(tmp, impCtl, impData);
        ArtifactRegistry.defineArtifact(artifactSpec, tmpUrl);
        
        File ctlFile = new File(testDir, "profile2.ctl");
        File dataFile = new File(testDir, "profile.data"); 

        Models models = RepositoryConnector.getModels(Phase.MONITORING);
        IProfile profile = new TestProfile(Naming.NODE_PROCESS_FAMILY, Naming.NODE_PROCESS_ALG1, 
            models.getConfiguration(), dataFile);

        // assuming this returns the simple parser
        IProfileControlParser parser = ProfileControlParserFactory.INSTANCE.getParser(ctlFile);
        Assert.assertNotNull(parser);
        ParseResult result = parser.parseControlFile(ctlFile, profile);

        Assert.assertNotNull(result);

        Assert.assertEquals(1, result.getDataFiles().size());
        Assert.assertEquals(dataFile, result.getDataFiles().get(0));
        // from testdata/profile.ctl, shall not be overridden
        assertEquals(result.getExecutors(), 4, 3, 1, 1, 2, 3);
        assertEquals(result.getTasks(), 4, 3, 1, 2, 2, 2);
        assertEquals(result.getWorkers(), 4, 3, 1, 2, 3, 4);
        List<Serializable> params = result.getParameters().get("window");
        assertEquals(params, 400, 1200, 500, 600, 1000);
        
        ArtifactRegistry.undefineArtifact(artifactSpec);
        tmp.delete();
    }
    
    /**
     * Tests importing data only.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testSimpleParserDataImport() throws IOException {
        final String artifactSpec = "eu.qualiMaster:testProfile:2.0";
        File testDir = Utils.getTestdataDir();
        File impCtl = new File(testDir, "profileImport/profile.ctl");
        File impData = new File(testDir, "profileImport/profile.data");
        File tmp = File.createTempFile("qmProfileControlTest", ".zip");
        URL tmpUrl = tmp.toURI().toURL();
        JarUtil.zip(tmp, impCtl, impData);
        ArtifactRegistry.defineArtifact(artifactSpec, tmpUrl);
        
        File ctlFile = new File(testDir, "profile3.ctl");
        File dataFile = new File(testDir, "noExist/profile.data"); // does not exist 
        
        Models models = RepositoryConnector.getModels(Phase.MONITORING);
        IProfile profile = new TestProfile(Naming.NODE_PROCESS_FAMILY, Naming.NODE_PROCESS_ALG1, 
            models.getConfiguration(), dataFile);

        // assuming this returns the simple parser
        IProfileControlParser parser = ProfileControlParserFactory.INSTANCE.getParser(ctlFile);
        Assert.assertNotNull(parser);
        ParseResult result = parser.parseControlFile(ctlFile, profile);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getDataFiles().size());
        Assert.assertEquals(dataFile, result.getDataFiles().get(0));
        // from testdata/profile.ctl, shall not be overridden
        assertEquals(result.getExecutors(), 1, 1, 2, 3);
        assertEquals(result.getTasks(), 1, 2, 2, 2);
        assertEquals(result.getWorkers(), 1, 2, 3, 4);
        List<Serializable> params = result.getParameters().get("window");
        assertEquals(params, 400, 500, 600, 1000);
        
        ArtifactRegistry.undefineArtifact(artifactSpec);
        tmp.delete();
        dataFile.delete();
    }
    
    /**
     * Tests the simple parser with import and taking over imported data.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testSimpleParserImport2() throws IOException {
        final String artifactSpec = "eu.qualiMaster:testProfile:2.0";
        File testDir = Utils.getTestdataDir();
        File impCtl = new File(testDir, "profileImport/profile.ctl");
        File impData = new File(testDir, "profileImport/profile.data");
        File tmp = File.createTempFile("qmProfileControlTest", ".zip");
        URL tmpUrl = tmp.toURI().toURL();
        JarUtil.zip(tmp, impCtl, impData);
        ArtifactRegistry.defineArtifact(artifactSpec, tmpUrl);
        
        File ctlFile = new File(testDir, "profileImport2/profile2.ctl");
        File dataFile = new File(testDir, "profileImport2/profile.data"); // does not exist 

        Models models = RepositoryConnector.getModels(Phase.MONITORING);
        IProfile profile = new TestProfile(Naming.NODE_PROCESS_FAMILY, Naming.NODE_PROCESS_ALG1, 
            models.getConfiguration(), dataFile);

        // assuming this returns the simple parser
        IProfileControlParser parser = ProfileControlParserFactory.INSTANCE.getParser(ctlFile);
        Assert.assertNotNull(parser);
        ParseResult result = parser.parseControlFile(ctlFile, profile);

        Assert.assertNotNull(result);

        // temporary data file has been copied to expected data file
        Assert.assertTrue(dataFile.exists());
        Assert.assertEquals(1, result.getDataFiles().size());
        File df = result.getDataFiles().get(0);
        Assert.assertEquals(dataFile, df);
        df.delete(); // this is just temporary
        
        // from testdata/profile.ctl, shall not be overridden
        assertEquals(result.getExecutors(), 4, 3, 1, 1, 2, 3);
        assertEquals(result.getTasks(), 4, 3, 1, 2, 2, 2);
        assertEquals(result.getWorkers(), 4, 3, 1, 2, 3, 4);
        List<Serializable> params = result.getParameters().get("window");
        assertEquals(params, 400, 1200, 500, 600, 1000);
        
        ArtifactRegistry.undefineArtifact(artifactSpec);
        tmp.delete();
    }

    
    /**
     * Asserts the contents of <code>actual</code> and <code>expected</code>.
     *
     * @param <T> the element type
     * @param actual the actual list
     * @param expected the expected values
     */
    @SuppressWarnings("unchecked")
    private static <T extends Serializable> void assertEquals(List<T> actual, T... expected) {
        Assert.assertNotNull(actual);
        List<T> ex = new ArrayList<T>();
        for (T t : expected) {
            ex.add(t);
        }
        Assert.assertEquals(ex, actual);
    }

    /**
     * Returns a processing key for generic hashing.
     * 
     * @param tasks the number of tasks
     * @param executors the number of executors
     * @param workers the number of workers
     * @return the key
     */
    private static final String getProcessingKey(int tasks, int executors, int workers) {
        return tasks + ";" + executors + ";" + workers;
    }
    
    /**
     * Implements a test profile execution class for recording completed executions.
     * 
     * @author Holger Eichelberger
     */
    private static class TestProfileExecution implements IProfileExecution {
        
        private Map<String, Map<String, Set<Serializable>>> counter 
            = new HashMap<String, Map<String, Set<Serializable>>>();
        private List<String> parameterNames = new ArrayList<String>();
        private ParseResult result;
        private Set<String> running = new HashSet<String>();
        private final String dataFile = AlgorithmProfileHelper.getDataFile(
            new File(CoordinationConfiguration.getDfsPath())).getAbsolutePath();
        private boolean expectMultiData = false;

        /**
         * Creates an instance for a certain parse <code>result</code>.
         * 
         * @param expectMultiData whether multiple data files are expected
         * @param result the parse result to start with
         */
        private TestProfileExecution(ParseResult result, boolean expectMultiData) {
            this.result = result;
            parameterNames.addAll(result.getParameterNames());
            this.expectMultiData = expectMultiData;
        }

        @Override
        public void start(INameMapping mapping, File jarPath, PipelineOptions options) throws IOException {
            running.add(mapping.getPipelineName());
            String key = getProcessingKey(options.getTaskParallelism(AlgorithmProfileHelper.FAM_NAME, 0),
                options.getExecutorParallelism(AlgorithmProfileHelper.FAM_NAME, 0), options.getNumberOfWorkers(0));
            Map<String, Set<Serializable>> params = counter.get(key);
            if (null == params) {
                params = new HashMap<String, Set<Serializable>>();
                counter.put(key, params);
            }
            for (String name : parameterNames) {
                if (options.hasExecutorArgument(AlgorithmProfileHelper.FAM_NAME, name)) {
                    Serializable val = options.getExecutorArgument(AlgorithmProfileHelper.FAM_NAME, name);
                    Set<Serializable> values = params.get(name);
                    if (null == values) {
                        values = new HashSet<Serializable>();
                        params.put(name, values);
                    }
                    values.add(val);
                }
            }
            
            Assert.assertFalse(options.hasExecutorArgument(AlgorithmProfileHelper.SRC_NAME, 
                AlgorithmProfileHelper.PARAM_HDFS_DATAFILE));
            if (!expectMultiData) {
                Assert.assertEquals(dataFile, options.getExecutorArgument(AlgorithmProfileHelper.SRC_NAME, 
                    AlgorithmProfileHelper.PARAM_DATAFILE));
            }
        }
        
        @Override
        public void kill(INameMapping mapping, PipelineOptions options) throws IOException {
            running.remove(mapping.getPipelineName());
        }

        /**
         * Asserts that the execution is complete.
         */
        private void assertComplete() {
            Assert.assertTrue("still running pipelines: " + running, running.isEmpty());
            Map<String, List<Serializable>> params = result.getParameters();
            Map<String, Set<Serializable>> tmp = new HashMap<String, Set<Serializable>>();
            for (Map.Entry<String, List<Serializable>> ent : params.entrySet()) {
                Set<Serializable> val = new HashSet<Serializable>();
                tmp.put(ent.getKey(), val);
                val.addAll(ent.getValue());
            }
            
            for (ProcessingEntry pEntry : result.getProcessingEntries()) {
                String key = getProcessingKey(pEntry.getTasks(), pEntry.getExecutors(), 
                    ProfileControl.getActualWorkers(pEntry));
                Assert.assertTrue("key for processing entry " + pEntry + " not registered", counter.containsKey(key));
                Map<String, Set<Serializable>> recParams = counter.get(key);
                Assert.assertEquals(tmp, recParams);
            }
        }
        
    }
    
    /**
     * Tests a run over a profile control instance.
     * 
     * @throws IOException shall not occur
     */
    @Test(timeout = 5000 + 16 * ProfileControl.KILL_WAITING_TIME) 
    public void testProfileControl() throws IOException {
        File testDir = Utils.getTestdataDir();
        File ctlFile = new File(testDir, "profile.ctl");
        File dataFile = new File(testDir, "profile.data"); 
        testProfileControl(ctlFile, dataFile, false);
    }

    /**
     * Tests a run over a profile control instance with multiple data files.
     * 
     * @throws IOException shall not occur
     */
    @Test(timeout = 5000 + 48 * ProfileControl.KILL_WAITING_TIME) 
    public void testProfileControl2() throws IOException {
        File testDir = new File(Utils.getTestdataDir(), "multiProfile");
        File ctlFile = new File(testDir, "profile.ctl");
        File dataFile = new File(testDir, "profile.data"); 
        testProfileControl(ctlFile, dataFile, true);
    }

    /**
     * Tests the profile control for given files. Please set timeout correctly.
     * 
     * @param ctlFile the control file
     * @param dataFile the (base) data file
     * @param expectMultiData whether this is a test case with multi data files
     * @throws IOException in case of I/O problems
     */
    private void testProfileControl(File ctlFile, File dataFile, boolean expectMultiData) throws IOException {
        final String pipeline = "TestPip";

        File tmp = FileUtils.getTempDirectory();
        Properties prop = new Properties();
        prop.put(CoordinationConfiguration.PATH_DFS, tmp.getAbsolutePath());
        prop.put(CoordinationConfiguration.URL_HDFS, CoordinationConfiguration.EMPTY_VALUE);
        CoordinationConfiguration.configure(prop);

        ProfileAlgorithmCommand cmd = new ProfileAlgorithmCommand(Naming.NODE_PROCESS_FAMILY, Naming.NODE_PROCESS_ALG1);
        ProfileData data = new ProfileData(pipeline, new File("test.jar"), dataFile, ctlFile);
        Models models = RepositoryConnector.getModels(Phase.MONITORING);
        IProfile profile = new TestProfile(Naming.NODE_PROCESS_FAMILY, Naming.NODE_PROCESS_ALG1, 
            models.getConfiguration(), dataFile);
        IProfileControlParser parser = ProfileControlParserFactory.INSTANCE.getParser(ctlFile);
        ParseResult parseResult = parser.parseControlFile(ctlFile, profile);
        TestProfileExecution execution = new TestProfileExecution(parseResult, expectMultiData);
        
        Assert.assertNull(ProfileControl.getInstance(pipeline));
        
        ProfileControl control = new ProfileControl(models.getConfiguration(), cmd, data, execution);
        Assert.assertEquals(control, ProfileControl.getInstance(pipeline));
        Assert.assertEquals(Naming.NODE_PROCESS_FAMILY, control.getFamilyName());
        Assert.assertEquals(Naming.NODE_PROCESS_ALG1, control.getAlgorithmName());
        Assert.assertEquals(models.getConfiguration(), control.getConfiguration());
        Assert.assertEquals(data.getDataFile(), control.getDataFile());
        
        int count = 0;
        while (control.hasNext()) { // potential endless loop -> timeout
            control.startNext();
            count++;
            control.killActual();
        }
        Assert.assertEquals(parseResult.getNumberOfVariations(), count);
        execution.assertComplete();
        
        // very end
        ProfileControl.releaseInstance(control);
        Assert.assertNull(ProfileControl.getInstance(pipeline));
         
        FileUtils.deleteQuietly(AlgorithmProfileHelper.getControlFile(tmp));
        FileUtils.deleteQuietly(AlgorithmProfileHelper.getDataFile(tmp));
    }
    
}
