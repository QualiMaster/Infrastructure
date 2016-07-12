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
package eu.qualimaster.coordination;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent.Status;
import eu.qualimaster.coordination.profiling.IProfile;
import eu.qualimaster.coordination.profiling.ParseResult;
import eu.qualimaster.coordination.profiling.ProcessingEntry;
import eu.qualimaster.coordination.profiling.ProfileControlParserFactory;
import eu.qualimaster.dataManagement.storage.hdfs.HdfsUtils;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper.ProfileData;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineOptions;
import net.ssehub.easy.varModel.confModel.Configuration;

/**
 * Class for controlling the profiling of an algorithm via its data/profiling script.
 * 
 * @author Holger Eichelberger
 */
public class ProfileControl implements IProfile {

    public static final int KILL_WAITING_TIME = 3000;
    
    /**
     * Encapsulates the profile execution, in particular for testing.
     * 
     * @author Holger Eichelberger
     */
    public interface IProfileExecution {
       
        /**
         * Starts the given pipeline.
         * 
         * @param mapping the pipeline name mapping
         * @param jarPath the path to the Jar file
         * @param options the pipeline start options
         * @throws IOException in case of start problems
         */
        public void start(INameMapping mapping, File jarPath, PipelineOptions options) throws IOException;
        
        /**
         * Kill the given pipeline.
         * 
         * @param mapping the pipeline name mapping
         * @param options the pipeline kill options
         * @throws IOException in case of kill problems
         */
        public void kill(INameMapping mapping, PipelineOptions options) throws IOException;
        
    }

    public static final IProfileExecution STORM_EXECUTION = new IProfileExecution() {

        @Override
        public void start(INameMapping mapping, File jarPath, PipelineOptions options) throws IOException {
            CoordinationCommandExecutionVisitor.doPipelineStart(mapping, jarPath.getAbsolutePath(), options, null);
            //StormUtils.submitTopology(CoordinationConfiguration.getNimbus(), mapping, 
            //    jarPath.getAbsolutePath(), options);
        }
        
        @Override
        public void kill(INameMapping mapping, PipelineOptions options) throws IOException {
            CoordinationCommandExecutionVisitor.doPipelineStop(mapping, options, null);
            //StormUtils.killTopology(CoordinationConfiguration.getNimbus(), pipelineName, 0, options);            
        }
        
    };

    private static final Map<String, ProfileControl> INSTANCES = Collections.synchronizedMap(
        new HashMap<String, ProfileControl>());

    //private boolean done = false;
    private int maxVariants = 1;
    private Configuration config;
    private String familyName;
    private String algorithmName;
    private ProfileData data;
    private INameMapping mapping;
    private PipelineOptions lastOptions;
    private boolean useHdfs;
    private String dataPath;
    private File dataFile;
    private IProfileExecution execution;
    
    private List<ProcessingEntry> processing;
    private Map<String, List<Serializable>> parameters;
    private int actVariant = 0;
    private List<Position> actPos = new ArrayList<Position>();
    
    /**
     * Denotes an iterator position.
     * 
     * @author Holger Eichelberger
     */
    private class Position {
        
        private String identifier;
        private int position;
        
        /**
         * Creates a common iterator position for tasks, executors and workers.
         */
        private Position() {
            this(null);
        }
        
        /**
         * Creates a specific iterator position for parameters.
         * 
         * @param identifier the parameter name as identifier
         */
        private Position(String identifier) {
            this.identifier = identifier;
            this.position = 0;
        }

        /**
         * Advances the current position.
         * 
         * @return <code>true</code> in case of an overflow, i.e., position is larger than the amount of elements and
         *   is reset to <code>0</code> and the next level shall go on iterating, <code>false</code> if the advance
         *   was done within the number of elements
         */
        private boolean advance() {
            boolean overflow = false;
            position++;
            List<?> base;
            if (null == identifier) {
                base = processing;
            } else {
                base = parameters.get(identifier);
            }
            if (position >= base.size()) {
                position = 0;
                overflow = true;
            }
            return overflow;
        }
    }

    /**
     * Creates the profile control instance. Adds itself to {@link #INSTANCES}.
     * 
     * @param config the configuration containing at least the family / algorithm
     * @param cmd the profile command used to start the profiling
     * @param data the profile data (paths) 
     * @throws IOException if loading the name mapping fails
     */
    public ProfileControl(Configuration config, ProfileAlgorithmCommand cmd, ProfileData data) 
        throws IOException {
        this(config, cmd, data, STORM_EXECUTION);
    }
    
    /**
     * Creates the profile control instance. Adds itself to {@link #INSTANCES}.
     * 
     * @param config the configuration containing at least the family / algorithm
     * @param cmd the profile command used to start the profiling
     * @param data the profile data (paths)
     * @param execution the execution instance (uses {@link #STORM_EXECUTION} as default if not given)
     * @throws IOException if loading the name mapping fails
     */
    public ProfileControl(Configuration config, ProfileAlgorithmCommand cmd, ProfileData data, 
        IProfileExecution execution) throws IOException {
        this.config = config;
        this.familyName = cmd.getFamily();
        this.algorithmName = cmd.getAlgorithm();
        this.data = data;
        this.dataFile = data.getDataFile();
        this.execution = null == execution ? STORM_EXECUTION : execution;
        mapping = CoordinationUtils.createMapping(data.getPipelineName(), data.getPipeline());
        CoordinationManager.registerNameMapping(mapping);
        File cf = data.getControlFile();
        ParseResult pResult = ProfileControlParserFactory.INSTANCE.getParser(cf).parseControlFile(cf, this);
        dataFile = pResult.getDataFile();
        processing = pResult.getProcessingEntries();
        parameters = pResult.getParameters();
        useHdfs = storeToHdfs();
        if (!useHdfs) {
            if (!storeToDfs()) {
                throw new IOException("Cannot store data files. Check HDFS/DFS configuration.");
            }
        }
        calcVariants();
        INSTANCES.put(data.getPipelineName(), this);
        getLogger().info("Profile control created/registered for " + data.getPipelineName());
    }
    
    /**
     * Calculates the number of variants to process and initializes the actual position counters.
     */
    private void calcVariants() {
        int maxExec = processing.size();
        
        maxVariants = Math.max(1, maxExec);
        actPos.add(new Position());
        for (Map.Entry<String, List<Serializable>> ent : parameters.entrySet()) {
            maxVariants *= ent.getValue().size();
            actPos.add(new Position(ent.getKey()));
        }
    }

    /**
     * Stores the data file to the HDFS (alternative).
     * 
     * @return <code>true</code> if successful, <code>false</code> else
     */
    private boolean storeToHdfs() {
        boolean done = false;
        if (!CoordinationConfiguration.isEmpty(CoordinationConfiguration.getHdfsUrl())) {
            try {
                String basePath = CoordinationConfiguration.getDfsPath() + "/"; // + family + algorithm
                FileSystem fs = HdfsUtils.getFilesystem();
                Path target = new Path(basePath, dataFile.getName()); 
                fs.copyFromLocalFile(new Path(dataFile.getAbsolutePath()), target);
                dataPath = target.toString();
                done = true;
            } catch (IOException e) {
                getLogger().error(e.getMessage());
            }
        }
        return done;
    }
    
    /**
     * Stores the data file to the DFS (alternative).
     * 
     * @return <code>true</code> if successful, <code>false</code> else
     */
    private boolean storeToDfs() {
        boolean done = false;
        if (!CoordinationConfiguration.isEmpty(CoordinationConfiguration.getDfsPath())) {
            File targetPath = new File(CoordinationConfiguration.getDfsPath(), dataFile.getName());
            try {
                FileUtils.copyFile(dataFile, targetPath);
                dataPath = targetPath.getAbsolutePath().toString();
                done = true;
            } catch (IOException e) {
                getLogger().error(e.getMessage());
            }
        }
        return done;
    }
    
    /**
     * Returns a profile control instance via it's name.
     * 
     * @param pipelineName the pipeline name
     * @return the control instance (may be <b>null</b> if there is none)
     */
    public static ProfileControl getInstance(String pipelineName) {
        ProfileControl result;
        if (null != pipelineName) {
            result = INSTANCES.get(pipelineName);
        } else {
            result = null;
        }
        return result;
    }
    
    /**
     * Releases the given <code>instance</code>, i.e., removes it from {@link #INSTANCES}.
     * Unregisters also the name mapping. Deletes temporary files from {@link #data}.
     * 
     * @param instance the instance to release
     */
    public static void releaseInstance(ProfileControl instance) {
        if (null != instance) {
            File tmp = FileUtils.getTempDirectory();
            File iter = instance.data.getPipeline();
            File parent;
            do {
                parent = iter.getParentFile();
                if (null != parent) {
                    if (parent.equals(tmp)) {
                        if (CoordinationConfiguration.deleteProfilingPipelines()) {
                            org.apache.commons.io.FileUtils.deleteQuietly(iter);
                        }
                        break;
                    } else {
                        iter = parent;
                    }
                }
            } while (null != parent);
            if (null != instance.mapping) {
                CoordinationManager.unregisterNameMapping(instance.mapping);
            }
            INSTANCES.remove(instance.data.getPipelineName());
        }
    }

    /**
     * Returns whether there shall be a next pipeline started.
     * 
     * @return <code>true</code> if there is a next, <code>false</code> else
     */
    public boolean hasNext() {
        return actVariant < maxVariants;
    }
    
    /**
     * Sends an {@link AlgorithmProfilingEvent} with the given <code>status</code>.
     * 
     * @param status the profiling status of the actual profiling pipeline / family / algorithm
     * @param settings the actual profiling settings for information, may be <b>null</b>
     */
    private void sendAlgorithmProfilingEvent(Status status, Map<String, Serializable> settings) {
        // the family name changes due to the profiling pipeline generation, the pipeline name corresponds to 
        // getPipeline(), the algorithm name is as in the original configuration (not changed / adapted 
        // during generation)
        AlgorithmProfilingEvent evt = new AlgorithmProfilingEvent(getPipeline(), AlgorithmProfileHelper.FAM_NAME, 
            getAlgorithmName(), status, settings);
        considerDetails(evt);
        EventManager.send(evt);
    }
    
    /**
     * Returns the actual number of workers to be scheduled.
     * 
     * @param proc the processing entry
     * @return the number of workers
     */
    public static int getActualWorkers(ProcessingEntry proc) {
        return proc.getWorkers() + 1;
    }
    
    /**
     * Returns the next pipeline options for starting.
     * 
     * @throws IOException in case that starting the pipeline fails
     */
    public void startNext() throws IOException {
        if (hasNext()) {
            lastOptions = new PipelineOptions(AdaptationEvent.class);
            ProcessingEntry proc = processing.get(actPos.get(0).position);
            lastOptions.setNumberOfWorkers(proc.getWorkers() + 1);
            lastOptions.enableProfilingMode();
            lastOptions.setWaitTime(0);
            if (proc.getTasks() > 0) {
                lastOptions.setTaskParallelism(AlgorithmProfileHelper.FAM_NAME, proc.getTasks());
            }
            if (proc.getExecutors() > 0) {
                lastOptions.setExecutorParallelism(AlgorithmProfileHelper.FAM_NAME, proc.getExecutors());
            }
            
            for (int p = 1; p < actPos.size(); p++) { // 0 is processing entry!
                Position pos = actPos.get(p);
                List<Serializable> params = parameters.get(pos.identifier);
                lastOptions.setExecutorArgument(AlgorithmProfileHelper.FAM_NAME, 
                    pos.identifier, params.get(pos.position));
            }

            lastOptions.setExecutorArgument(AlgorithmProfileHelper.SRC_NAME, 
                useHdfs ? AlgorithmProfileHelper.PARAM_HDFS_DATAFILE 
                : AlgorithmProfileHelper.PARAM_DATAFILE, dataPath);

            
            if (0 == actVariant) { // this is the first execution, notify monitoring but defer until pipeline started
                considerDetails(CoordinationManager.deferProfilingStart(getPipeline(), AlgorithmProfileHelper.FAM_NAME, 
                    getAlgorithmName(), lastOptions.toMap()));
            } else {
                sendAlgorithmProfilingEvent(Status.NEXT, lastOptions.toMap());
            }
            execution.start(mapping, data.getPipeline(), lastOptions);
            
            int pos = actPos.size() - 1;
            while (pos >= 0) {
                if (actPos.get(pos).advance()) {
                    pos--;
                    if (pos < 0) {
                        break; // this is the very end - overflow in [0]
                    }
                } else {
                    break;
                }
            }
            actVariant++;
        }
    }
    
    /**
     * Considers the infrastructure settings whether detailed profiling of sub-algorithms
     * shall be enabled or not and adjusts <code>evt</code> accordingly.
     * 
     * @param evt the event to be adjusted if needed
     */
    private static void considerDetails(AlgorithmProfilingEvent evt) {
        evt.setDetailMode(CoordinationConfiguration.getProfilingMode());
    }
    
    /**
     * Kills the actual pipeline.
     * 
     * @throws IOException in case that killing fails
     */
    public void killActual() throws IOException {
        getLogger().info("Profile control killing " + data.getPipelineName());
        execution.kill(mapping, lastOptions);
        lastOptions = null;
        if (hasNext()) {
            try { // sometimes Storm does not get rid of the pipeline -> already exists on cluster
                Thread.sleep(KILL_WAITING_TIME);
            } catch (InterruptedException e) {
            }
        } else {
            sendAlgorithmProfilingEvent(Status.END, null); // null may be lastOptions.toMap() but unused
        }
    }

    /**
     * Returns the actual name of the profiling pipeline. In contrast to {@link #getFamilyName()} and 
     * {@link #getAlgorithmName()} this refers to the actual running pipeline.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return data.getPipelineName();
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
        return config;
    }

    @Override
    public File getDataFile() {
        return dataFile;
    }
    
    /**
     * Returns the logger.
     * 
     * @return the logger
     */
    private Logger getLogger() {
        return LogManager.getLogger(getClass());
    }

}
