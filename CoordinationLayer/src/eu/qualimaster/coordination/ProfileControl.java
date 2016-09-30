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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfo;
import eu.qualimaster.coordination.StormUtils.TopologyTestInfoException;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.ProfileAlgorithmCommand;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent;
import eu.qualimaster.coordination.events.AlgorithmProfilingEvent.Status;
import eu.qualimaster.coordination.profiling.IProfile;
import eu.qualimaster.coordination.profiling.ParseResult;
import eu.qualimaster.coordination.profiling.ProcessingEntry;
import eu.qualimaster.coordination.profiling.ProfileControlParserFactory;
import eu.qualimaster.dataManagement.storage.hdfs.HdfsUtils;
import eu.qualimaster.easy.extension.QmConstants;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper.ProfileData;
import eu.qualimaster.easy.extension.internal.HardwareRepositoryHelper;
import eu.qualimaster.easy.extension.internal.PipelineHelper;
import eu.qualimaster.easy.extension.internal.VariableHelper;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.pipeline.AlgorithmChangeParameter;
import net.ssehub.easy.instantiation.core.model.common.VilException;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.model.ModelQueryException;
import net.ssehub.easy.varModel.model.datatypes.Compound;

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
    private List<String> dataPaths;
    private List<File> dataFiles;
    private IProfileExecution execution;
    
    private List<ProcessingEntry> processing;
    private Map<String, List<Serializable>> parameters;
    private List<Map<String, Serializable>> settings = new ArrayList<>();

    private transient int actVariant = 0;
    private transient List<Position> actPos = new ArrayList<Position>();
    
    /**
     * Denotes an iterator position.
     * 
     * @author Holger Eichelberger
     */
    private class Position {
        
        private int position;
        private String dataPath;
        private ProcessingEntry pEnt;
        
        /**
         * Creates a specific iterator position for parameters.
         * 
         * @param pEnt the processing entry
         * @param position the position within {@link ProfileControl#settings}
         * @param dataPath the data path
         */
        private Position(ProcessingEntry pEnt, int position, String dataPath) {
            this.pEnt = pEnt;
            this.position = position;
            this.dataPath = dataPath;
        }
        
        @Override
        public String toString() {
            return pEnt + " " + position + " " + dataPath;
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
        this.execution = null == execution ? STORM_EXECUTION : execution;
        mapping = CoordinationUtils.createMapping(data.getPipelineName(), data.getPipeline());
        CoordinationManager.registerNameMapping(mapping);
        File cf = data.getControlFile();
        ParseResult pResult = ProfileControlParserFactory.INSTANCE.getParser(cf).parseControlFile(cf, this);
        dataFiles = pResult.getDataFiles();
        dataPaths = new ArrayList<String>(dataFiles.size());
        processing = pResult.getProcessingEntries();
        parameters = pResult.getParameters();
        for (File dataFile : dataFiles) {
            String dataPath;
            /*String dataPath = HdfsUtils.storeToHdfs(dataFile);
            if (null != dataPath) {
                useHdfs = true;    
            } else {*/
            dataPath = HdfsUtils.storeToDfs(dataFile);
            if (null == dataPath) {
                throw new IOException("Cannot store data files. Check HDFS/DFS configuration.");
            } else {
                useHdfs = false;
            }
            //}
            dataPaths.add(dataPath);
        }
        calcVariants();
        INSTANCES.put(data.getPipelineName(), this);
        getLogger().info("Profile control created/registered for " + data.getPipelineName());
    }
    
    /**
     * Calculates the number of variants to process and initializes the actual position counters.
     */
    private void calcVariants() {
        Map<String, Serializable> tmp = new HashMap<String, Serializable>();
        String[] names = new String[parameters.size()];
        int[] pos = new int[parameters.size()];
        int i = 0;
        for (String name : parameters.keySet()) {
            names[i] = name;
            pos[i] = 0;
            tmp.put(name, parameters.get(name).get(0));
        }
        boolean cont = parameters.size() > 0;
        while (cont) {
            for (int n = 0; n < names.length; n++) {
                List<Serializable> paramVals = parameters.get(names[n]);
                tmp.put(names[n], paramVals.get(pos[n]));
                Map<String, Serializable> instance = new HashMap<String, Serializable>();
                instance.putAll(tmp);
                settings.add(instance);
                pos[n]++;
                if (pos[n] >= paramVals.size()) {
                    if (n < names.length - 1) {
                        pos[n] = 0;
                    } else {
                        cont = false;
                    }
                } 
            }
        }
        for (ProcessingEntry pEnt : processing) {
            for (String dataPath : dataPaths) {
                if (settings.isEmpty()) {
                    actPos.add(new Position(pEnt, -1, dataPath));
                } else {
                    for (int s = 0; s < settings.size(); s++) {
                        actPos.add(new Position(pEnt, s, dataPath));
                    }
                }
            }
        }
        maxVariants = actPos.size();
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
            Position pos = actPos.get(actVariant);
            ProcessingEntry proc = pos.pEnt;
            lastOptions.setNumberOfWorkers(proc.getWorkers() + 1);
            lastOptions.enableProfilingMode();
            lastOptions.setWaitTime(0);
            if (proc.getTasks() > 0) {
                lastOptions.setTaskParallelism(AlgorithmProfileHelper.FAM_NAME, proc.getTasks());
            }
            if (proc.getExecutors() > 0) {
                lastOptions.setExecutorParallelism(AlgorithmProfileHelper.FAM_NAME, proc.getExecutors());
            }
            if (pos.position >= 0) {
                Map<String, Serializable> args = settings.get(pos.position);
                for (Map.Entry<String, Serializable> ent : args.entrySet()) {
                    lastOptions.setExecutorArgument(AlgorithmProfileHelper.FAM_NAME, 
                        ent.getKey(), ent.getValue());
                }
            }

            lastOptions.setExecutorArgument(AlgorithmProfileHelper.SRC_NAME, 
                useHdfs ? AlgorithmProfileHelper.PARAM_HDFS_DATAFILE 
                : AlgorithmProfileHelper.PARAM_DATAFILE, pos.dataPath);

            if (StormUtils.inTesting()) {
                try {
                    Map<String, TopologyTestInfo> topologies = new HashMap<String, TopologyTestInfo>();
                    topologies.put(getPipeline(), new TopologyTestInfo(getPipeline(), data.getPipeline(), 
                         lastOptions.toMap(), data));
                    StormUtils.forTesting(StormUtils.getLocalCluster(), topologies);
                } catch (TopologyTestInfoException e) {
                    getLogger().error("Testing: " + e.getMessage());
                }
            }
            if (0 == actVariant) { // this is the first execution, notify monitoring but defer until pipeline started
                considerDetails(CoordinationManager.deferProfilingStart(getPipeline(), AlgorithmProfileHelper.FAM_NAME, 
                    getAlgorithmName(), lastOptions.toMap()));
            } else {
                sendAlgorithmProfilingEvent(Status.NEXT, lastOptions.toMap());
            }
            execution.start(mapping, data.getPipeline(), lastOptions);

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
        return data.getDataFile(); // just the base data file
    }
    
    /**
     * Returns the logger.
     * 
     * @return the logger
     */
    private Logger getLogger() {
        return LogManager.getLogger(getClass());
    }
    
    /**
     * Sends an initial algorithm change command, in particular for hardware.
     */
    private void sendInitialAlgorithmChangeCommand() {
        String pipelineName = getPipeline();
        String familyName = getFamilyName();
        String algorithmName = getAlgorithmName();
        AlgorithmChangeCommand cmd = new AlgorithmChangeCommand(pipelineName, 
            AlgorithmProfileHelper.FAM_NAME, algorithmName);
        try {
            Compound familyType = eu.qualimaster.easy.extension.internal.Utils.findCompound(
                config.getProject(), QmConstants.TYPE_FAMILY);
            IDecisionVariable family = eu.qualimaster.easy.extension.internal.Utils.findNamedVariable(
                config, familyType, familyName);
            IDecisionVariable algo = PipelineHelper.obtainAlgorithmFromFamilyByName(family, 
                QmConstants.SLOT_FAMILY_MEMBERS, algorithmName);
            if (null != algo) {
                IDecisionVariable hw = algo.getNestedElement(QmConstants.SLOT_HARDWAREALGORITHM_HWNODE);
                if (null != hw) {
                    String artifact = VariableHelper.getString(algo, QmConstants.SLOT_ALGORITHM_ARTIFACT);
                    if (null != artifact) {
                        try {
                            cmd.setStringParameter(AlgorithmChangeParameter.IMPLEMENTING_ARTIFACT, 
                                HardwareRepositoryHelper.obtainHardwareArtifactUrl(artifact));
                        } catch (VilException e) {
                            getLogger().error(e.getMessage());
                        }
                    }
    
                    hw = Configuration.dereference(hw);
                    setStringParameter(cmd, AlgorithmChangeParameter.COPROCESSOR_HOST, hw, "host");
                    setIntParameter(cmd, AlgorithmChangeParameter.CONTROL_RESPONSE_PORT, hw, "commandSendingPort");
                    setIntParameter(cmd, AlgorithmChangeParameter.CONTROL_REQUEST_PORT, hw, "commandReceivingPort");
                }
            }
        } catch (ModelQueryException e) {
            getLogger().error(e.getMessage());
        }
        cmd.execute();
    }

    /**
     * Sets a String parameter for an algorithm change command. Nothing happens if the variable or the slot 
     * do not exist.
     * 
     * @param cmd the command
     * @param param the parameter
     * @param var the variable to take the value from
     * @param slot the slot to take the value from
     */
    private static void setStringParameter(AlgorithmChangeCommand cmd, AlgorithmChangeParameter param, 
        IDecisionVariable var, String slot) {
        String val = VariableHelper.getString(var, slot);
        if (null != val) {
            cmd.setStringParameter(param, val);
        }
    }
    
    /**
     * Sets an int parameter for an algorithm change command. Nothing happens if the variable or the slot do not exist.
     * 
     * @param cmd the command
     * @param param the parameter
     * @param var the variable to take the value from
     * @param slot the slot to take the value from
     */
    private static void setIntParameter(AlgorithmChangeCommand cmd, AlgorithmChangeParameter param, 
        IDecisionVariable var, String slot) {
        Integer val = VariableHelper.getInteger(var, slot);
        if (null != val) {
            cmd.setIntParameter(param, val);
        }
    }
    
    /**
     * Called to inform about a created pipeline.
     * 
     * @param pipelineName the name of the pipeline
     */
    static void created(String pipelineName) {
        ProfileControl ctl = INSTANCES.get(pipelineName);
        if (null != ctl) {
            ctl.sendInitialAlgorithmChangeCommand();
        }
    }

}
