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
package tests.eu.qualimaster.monitoring.profiling;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.NameMapping;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.observations.ObservationFactory;
import eu.qualimaster.monitoring.observations.ObservationFactory.IObservationCreator;
import eu.qualimaster.monitoring.profiling.AlgorithmProfilePredictionManager;
import eu.qualimaster.monitoring.profiling.Constants;
import eu.qualimaster.monitoring.profiling.ProfilingRegistry;
import eu.qualimaster.monitoring.systemState.NodeImplementationSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.StateUtils;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.monitoring.topology.PipelineTopology;
import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;
import tests.eu.qualimaster.coordination.Utils;
import tests.eu.qualimaster.monitoring.genTopo.TestProcessor;

/**
 * Tests the profiling for the selection of algorithms with tradeoffs. This test uses a different configuration model
 * and shall run after all other model-based tests.
 * 
 * @author Holger Eichelberger
 */
public class SelectionTests {
    
    private static final String PIP_NAME = "pip";
    private static final String SOURCE_ELT = "Src";
    private static final String FAM_ELT = "Fam";
    private static final String HW_ALG = "HW";
    private static final String SW_ALG = "SW";
    private static final String SW_OSC_ALG = "SW-osc";
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.YYYY HH:mm:ss");
    private static final int LAST_TIME = 180;
    private static final int TIME_STEP = 1000;
    private static final double INPUT_RATE = 1000;

    /**
     * The measured observables. Algorithm descriptors must have an assigned trace for each observable.
     */
    private static final IObservable[] MEASURED = new IObservable[] {
        TimeBehavior.THROUGHPUT_ITEMS, TimeBehavior.LATENCY};

    private static final IObservable[] RELEVANT = new IObservable[] {
        TimeBehavior.THROUGHPUT_ITEMS, TimeBehavior.LATENCY, ResourceUsage.CAPACITY, Scalability.ITEMS};

    private File testDataFolder;
    private Map<String, AlgorithmDescriptor> algorithms = new HashMap<String, AlgorithmDescriptor>(); 
    
    /**
     * Registers an algorithm descriptor.
     * 
     * @param name the name of the algorithm
     * @param profilingTime the date/time when profiling took place
     * @return the created/registered descriptor
     */
    private AlgorithmDescriptor registerDescriptor(String name, String profilingTime) {
        AlgorithmDescriptor result = new AlgorithmDescriptor(name, profilingTime); 
        algorithms.put(name, result);
        return result;
    }
    
    /**
     * Implements a log entry.
     * 
     * @author Holger Eichelberger
     */
    private static class Entry {
        private long timestamp;
        private Map<IObservable, Double> measurements = new HashMap<IObservable, Double>();
        private Map<IObservable, Double> predictions = new HashMap<IObservable, Double>();

        /**
         * Creates a log entry for a given timestamp.
         * 
         * @param timestamp the timestamp
         */
        private Entry(long timestamp) {
            this.timestamp = timestamp;
        }

        /**
         * Records the predicted value for an observable.
         * 
         * @param observable the observable
         * @param value the value
         */
        private void setObserved(IObservable observable, double value) {
            measurements.put(observable, value);
        }
        
        /**
         * Records the predicted value for an observable.
         * 
         * @param observable the observable
         * @param value the value
         */
        private void setPredicted(IObservable observable, double value) {
            predictions.put(observable, value);
        }

        /**
         * Returns the logged measured value for <code>observable</code>.
         * 
         * @param observable the observable
         * @return the logged measured value, {@link Constants#NO_PREDICTION} if no value is available
         */
        private double getMeasuredValue(IObservable observable) {
            double result = Constants.NO_PREDICTION;
            Double tmp = measurements.get(observable);
            if (null != tmp) {
                result = tmp.doubleValue();
            }
            return result;
        }

        /**
         * Returns the logged predicted value for <code>observable</code>.
         * 
         * @param observable the observable
         * @return the logged predicted value, {@link Constants#NO_PREDICTION} if no value is available
         */
        private double getPredictedValue(IObservable observable) {
            double result = Constants.NO_PREDICTION;
            Double tmp = predictions.get(observable);
            if (null != tmp) {
                result = tmp.doubleValue();
            }
            return result;
        }

        
        @Override
        public String toString() {
            String result = timestamp + "\t";
            for (IObservable obs : RELEVANT) {
                result += SelectionTests.toString(measurements.get(obs)) + "\t";
            }
            for (IObservable obs : RELEVANT) {
                result += SelectionTests.toString(predictions.get(obs)) + "\t";
            }
            return result;
        }

    }

    /**
     * Returns a header line.
     * 
     * @param algorithm the name of the algorithm
     * @return the header
     */
    private String logHeader(String algorithm) {
        String result = "timestamp." + algorithm + "\t";
        for (IObservable obs : RELEVANT) {
            result += "measured." + algorithm + "." + obs.name() + "\t";
        }
        for (IObservable obs : RELEVANT) {
            result += "predicted." + algorithm + "." + obs.name() + "\t";
        }
        return result;
    }

    /**
     * Turns a double into an Excel string.
     * 
     * @param value the value
     * @return the string representation
     */
    private static String toString(Double value) {
        String tmp = String.format("%.15f", value);
        return tmp.replace(".", ","); // for excel
    }
    
    /**
     * Implements an algorithm descriptor which holds the trace functions for various observables.
     * 
     * @author Holger Eichelberger
     */
    private static class AlgorithmDescriptor {
        
        private String name;
        private long profilingTime;
        private Map<IObservable, Trace> traces = new HashMap<IObservable, Trace>();
        private List<Entry> log = new ArrayList<Entry>();

        /**
         * Creates an algorithm descriptor.
         * 
         * @param name the name of the algorithm
         * @param profilingTime the date/time when profiling took place
         */
        private AlgorithmDescriptor(String name, String profilingTime) {
            this.name = name;
            try {
                this.profilingTime = DATE_FORMAT.parse(profilingTime).getTime();
            } catch (ParseException e) {
                System.out.println(e.getMessage());
                this.profilingTime = System.currentTimeMillis();
            }
        }
        
        /**
         * Adds a trace.
         * 
         * @param observable the observable
         * @param trace the associated trace
         */
        private void addTrace(IObservable observable, Trace trace) {
            traces.put(observable, trace);
        }
        
        /**
         * Returns the name of the algorithm.
         * 
         * @return the name 
         */
        private String getName() {
            return name;
        }
        
        /**
         * Returns the profiling time stamp.
         * 
         * @return the profiling time stamp
         */
        private long getProfilingTime() {
            return profilingTime;
        }
        
        /**
         * Returns an observation for a given observable and a point in time from the underlying trace.
         * 
         * @param observable the observable
         * @param time the point in time
         * @return the observation, {@link Constants#NO_PREDICTION} for no value 
         */
        private double getObservation(IObservable observable, double time) {
            double result;
            Trace trace = traces.get(observable);
            if (null == trace) {
                result = Constants.NO_PREDICTION;
            } else {
                result = trace.value(time);
            }
            return result;
        }
        
        /**
         * Returns whether this descriptor has a trace for <code>observable</code>.
         * 
         * @param observable the observable to look for
         * @return <code>true</code> if there is a trace and {@link #getObservation(IObservable, double)} will
         *     deliver a value from the trace, <code>false</code> else
         */
        private boolean hasTrace(IObservable observable) {
            return traces.containsKey(observable);
        }

        /**
         * Returns the last logged measured value for <code>observable</code>.
         * 
         * @param observable the observable
         * @return the last logged measured value, {@link Constants#NO_PREDICTION} if no value is available
         */
        private double getLastLoggedMeasuredValue(IObservable observable) {
            double result = Constants.NO_PREDICTION;
            int size = log.size();
            if (size > 0) {
                result = log.get(size - 1).getMeasuredValue(observable);
            }
            return result;
        }

        /**
         * Returns the last logged predicted value for <code>observable</code>.
         * 
         * @param observable the observable
         * @return the last logged predicted value, {@link Constants#NO_PREDICTION} if no value is available
         */
        @SuppressWarnings("unused")
        private double getLastLoggedPredictedValue(IObservable observable) {
            double result = Constants.NO_PREDICTION;
            int size = log.size();
            if (size > 0) {
                result = log.get(size - 1).getPredictedValue(observable);
            }
            return result;
        }

        /**
         * Creates a new log entry.
         * 
         * @param timestamp the actual timestamp
         * @return the create entry
         */
        private Entry createLogEntry(long timestamp) {
            Entry entry = new Entry(timestamp);
            log.add(entry);
            return entry;
        }
        
        /**
         * Returns the log entries.
         * 
         * @return the log entries
         */
        private List<Entry> getLog() {
            return log;
        }
        
    }
    
    /**
     * Represents a trace function.
     * 
     * @author Holger Eichelberger
     */
    private abstract static class Trace {

        private double shift;
        
        /**
         * Creates a trace instance.
         * 
         * @param shift zero or a positive value shifting the trace with the time axis
         */
        protected Trace(double shift) {
            this.shift = Math.max(0, shift);
        }

        /**
         * The start time so that the trace values are defined.
         * 
         * @return the start time
         */
        abstract int startTime();
        
        /**
         * Returns the trace value for a given point in <code>time&gt;{@link #startTime}</code>.
         * 
         * @param time the time
         * @return the trace value
         */
        abstract double value(double time);
        
        /**
         * Returns whether <code>time</code> is in the valid range considering {@link #shift}.
         * 
         * @param time the time
         * @return <code>true</code> if in range, <code>false</code> else
         */
        protected boolean inRange(double time) {
            return time > shift;
        }
        
        /**
         * Returns the shift value.
         * 
         * @return the shift value
         */
        protected double getShift() {
            return shift;
        }
        
    }
    
    /**
     * Implements a constant trace function.
     * 
     * @author Holger Eichelberger
     *
     */
    private static class ConstrantTrace extends Trace {
        
        private double value;
       
        /**
         * Creates the constant trace function without shift.
         * 
         * @param value the constant value
         */
        private ConstrantTrace(double value) {
            this(value, 0);
        }
        
        /**
         * Creates the constant trace function.
         * 
         * @param value the constant value
         * @param shift zero or a positive value shifting the trace with the time axis
         */
        private ConstrantTrace(double value, double shift) {
            super(shift);
            this.value = value;
        }

        @Override
        protected int startTime() {
            return 0;
        }

        @Override
        protected double value(double time) {
            return value;
        }
        
    }
    
    /**
     * Creates a converting trace (from 0 against the limit).
     * 
     * @author Holger Eichelberger
     */
    private static class ConvergingTrace extends Trace {

        private double limit;

        /**
         * Creates the trace function without shift.
         * 
         * @param limit the limit to converge against
         */
        private ConvergingTrace(double limit) {
            this(limit, 0);
        }

        /**
         * Creates the trace function.
         * 
         * @param limit the limit to converge against
         * @param shift zero or a positive value shifting the trace with the time axis
         */
        public ConvergingTrace(double limit, double shift) {
            super(shift);
            this.limit = limit;
        }
        
        @Override
        int startTime() {
            return 1;
        }
        
        /**
         * Returns the unscaled value in case that refining traces need later scaling.
         * 
         * @param time the time
         * @return the trace value
         */
        protected double unscaledValue(double time) {
            double result;
            if (inRange(time)) {
                result = (1 - 1 / Math.sqrt(time - getShift())); 
            } else {
                result = 0;
            }
            return result; 
        }

        @Override
        double value(double time) {
            return limit * unscaledValue(time); 
        }
        
        /**
         * Returns the limit.
         * 
         * @return the limit
         */
        protected double getLimit() {
            return limit;
        }
        
    }
    
    /**
     * Creates a converging trace from 0 against the limit with oscillation around the original convergence, i.e., also 
     * around the limit.
     * 
     * @author Holger Eichelberger
     */
    private static class ConvergingOsciallatingTrace extends ConvergingTrace {

        private double amplitude;
        
        /**
         * Creates the trace function.
         * 
         * @param limit the limit to converge against
         * @param shift zero or a positive value shifting the trace with the time axis
         * @param amplitude the amplitude of the oscillation
         */
        private ConvergingOsciallatingTrace(double limit, double shift, double amplitude) {
            super(limit, shift);
            this.amplitude = amplitude;
        }

        @Override
        double value(double time) {
            // considers inRange implicitly
            return getLimit() * (super.unscaledValue(time) + amplitude * Math.sin(time - getShift()));
        }

    }
    
    /**
     * Simulates the creation of a profile, i.e., profiling.
     * 
     * @param algorithm the algorithm to profile
     * @param maxTime the maximum time, positive
     * @param inputRate the input rate to profile at
     * @throws IOException in case that the mapping file cannot be read
     */
    private static void simulateProfile(AlgorithmDescriptor algorithm, int maxTime, double inputRate) 
        throws IOException {
        File f = new File(Utils.getTestdataDir(), "selectionPip.xml");
        FileInputStream fis = new FileInputStream(f);
        NameMapping mapping = new NameMapping(PIP_NAME, fis);
        fis.close();
        CoordinationManager.registerTestMapping(mapping);
        // tweak the observations to have the "right" value <-> items/s
        IObservationCreator creator = ObservationFactory.getCreator(Scalability.ITEMS, null);
        ObservationFactory.registerCreator(Scalability.ITEMS, null, ObservationFactory.CREATOR_SINGLE);

        SystemState state = new SystemState();
        PipelineSystemPart pip = state.obtainPipeline(PIP_NAME);
        PipelineNodeSystemPart src = pip.obtainPipelineNode(SOURCE_ELT);
        PipelineNodeSystemPart fam = pip.obtainPipelineNode(FAM_ELT);

        // create the internal topology
        List<Processor> procs = new ArrayList<Processor>();
        TestProcessor pSrc = new TestProcessor(src.getName());
        procs.add(pSrc);
        TestProcessor pFam = new TestProcessor(fam.getName());
        procs.add(pFam);
        Stream pSrcPFam = new Stream("f1", pSrc, pFam);
        pSrc.setOutputs(pSrcPFam);
        pFam.setInputs(pSrcPFam);
        PipelineTopology topo = new PipelineTopology(procs);
        pip.setTopology(topo);
        
        NodeImplementationSystemPart alg = pip.getAlgorithm(algorithm.getName());
        fam.setCurrent(alg);
        AlgorithmProfilePredictionManager.notifyAlgorithmChanged(new AlgorithmChangedMonitoringEvent(pip.getName(), 
            fam.getName(), algorithm.getName()));
        
        src.setValue(Scalability.ITEMS, inputRate, null);
        long timestamp = algorithm.getProfilingTime();
        for (int t = 1; t < maxTime; t++) {
            boolean updateCapacity = false;
            for (IObservable obs : MEASURED) {
                // write into algorithm via family, update items/capacity
                StateUtils.setValue(fam, obs, algorithm.getObservation(obs, t), null);
                // manipulate the time to simulated time
                fam.setLastUpdate(obs, timestamp);
                updateCapacity |= StateUtils.changesLatency(obs);
            }
            if (updateCapacity) {
                StateUtils.updateCapacity(fam, null, false, timestamp);
                fam.setLastUpdate(ResourceUsage.CAPACITY, timestamp);
            }
            // update the profile
            update(algorithm, timestamp, pip, fam);
            timestamp = timestamp + TIME_STEP; // simulated seconds
        }
        
        state.clear();
        ObservationFactory.registerCreator(Scalability.ITEMS, null, creator);
        CoordinationManager.unregisterNameMapping(mapping);
    }
    
    /**
     * Updates the predictor and records the actual values.
     * 
     * @param alg the algorithm to predict for
     * @param timestamp the actual (simulated) timestamp
     * @param pip the pipeline the algorithm is used within
     * @param fam the family of the algorithm
     */
    private static void update(AlgorithmDescriptor alg, long timestamp, PipelineSystemPart pip, 
        PipelineNodeSystemPart fam) {
        Entry entry = alg.createLogEntry(timestamp);
        AlgorithmProfilePredictionManager.update(pip.getName(), fam.getName(), fam);
        for (IObservable obs : RELEVANT) {
            entry.setObserved(obs, fam.getObservedValue(obs));
            entry.setPredicted(obs, AlgorithmProfilePredictionManager.predict(pip.getName(), fam.getName(), 
                alg.getName(), obs, null));
        }
    }
    
    /**
     * Prints the logs in Excel TSV format.
     * @param out the output stream
     */
    void printLogs(PrintStream out) {
        int max = 0;
        for (AlgorithmDescriptor alg : algorithms.values()) {
            List<Entry> log = alg.getLog();
            max = Math.max(max, log.size());
        }
        for (AlgorithmDescriptor alg : algorithms.values()) {
            out.print(logHeader(alg.getName()));
        }
        out.println();
        for (int e = 0; e < max; e++) {
            for (AlgorithmDescriptor alg : algorithms.values()) {
                List<Entry> log = alg.getLog();
                out.print(log.get(e));
            }
            out.println();
        }        
    }

    /**
     * Executed before all tests.
     * 
     * @throws IOException shall not occur
     */
    @Before
    public void startup() throws IOException {
        algorithms.clear();
        testDataFolder = new File(FileUtils.getTempDirectoryPath(), "profileSelectionTests");
        AlgorithmProfilePredictionManager.clear();
        FileUtils.deleteQuietly(testDataFolder);
        testDataFolder.mkdirs();
        AlgorithmProfilePredictionManager.useTestData(testDataFolder.getAbsolutePath());
        AlgorithmProfilePredictionManager.notifyPipelineLifecycleChange(new PipelineLifecycleEvent(PIP_NAME, 
            PipelineLifecycleEvent.Status.STARTING, null));

        AlgorithmDescriptor desc;
        desc = registerDescriptor(HW_ALG, "01.10.2016 10:00:00");
        desc.addTrace(TimeBehavior.THROUGHPUT_ITEMS, new ConvergingTrace(INPUT_RATE * 1.5, 4)); // 4: force crossover
        desc.addTrace(TimeBehavior.LATENCY, new ConstrantTrace(100));
        
        desc = registerDescriptor(SW_ALG, "01.10.2016 15:00:00");
        desc.addTrace(TimeBehavior.THROUGHPUT_ITEMS, new ConvergingTrace(INPUT_RATE));
        desc.addTrace(TimeBehavior.LATENCY, new ConstrantTrace(150));
        
        desc = registerDescriptor(SW_OSC_ALG, "02.10.2016 9:00:00");
        desc.addTrace(TimeBehavior.THROUGHPUT_ITEMS, new ConvergingOsciallatingTrace(INPUT_RATE, 0, 0.02));
        desc.addTrace(TimeBehavior.LATENCY, new ConstrantTrace(150));
        
        for (AlgorithmDescriptor alg : algorithms.values()) {
            simulateProfile(alg, LAST_TIME, INPUT_RATE);
        }
    }

    /**
     * Executed after all tests.
     */
    @After
    public void shutdown() {
        AlgorithmProfilePredictionManager.notifyPipelineLifecycleChange(new PipelineLifecycleEvent(PIP_NAME, 
            PipelineLifecycleEvent.Status.STOPPED, null));
        AlgorithmProfilePredictionManager.useTestData(null);
        FileUtils.deleteQuietly(testDataFolder);
        AlgorithmProfilePredictionManager.clear();
        algorithms.clear();
    }

    /**
     * Tests the profile value prediction.
     */
    @Test
    public void testPrediction() {
        for (AlgorithmDescriptor alg : algorithms.values()) {
            for (IObservable obs : MEASURED) { // CAPACITY / ITEMS does not work, also as validated
                int origSteps = ProfilingRegistry.getPredictionSteps(obs);
                for (int i = 0; i < 5; i++) { // prediction steps ahead
                    long time = (LAST_TIME + i) * TIME_STEP;
                    double o;
                    if (alg.hasTrace(obs)) {
                        o = alg.getObservation(obs, time);
                    } else {  // we have no trace to produce the observation, take the last logged measured one
                        o = alg.getLastLoggedMeasuredValue(obs); 
                    }
                    double p = AlgorithmProfilePredictionManager.predict(PIP_NAME, FAM_ELT, alg.getName(), obs, null);
                    Assert.assertTrue("OBS " + obs + " for " + alg.getName() + " not in 15% " + o + " " + p + " for " 
                        + i + " prediction steps", Math.abs(o - p) / o <= 0.15);
                }
                ProfilingRegistry.registerPredictionSteps(obs, origSteps);
            }
        }
        //printLogs(System.out);
    }
    
    /**
     * Tests the algorithm ranking.
     */
    @Test
    public void testRanking() {
        Set<IObservable> observables = new HashSet<IObservable>();
        Map<IObservable, Double> weighting = new HashMap<IObservable, Double>();
        for (IObservable obs : RELEVANT) {
            observables.add(obs);
            weighting.put(obs, 1.0);
        }
        Map<String, Map<IObservable, Double>> result = AlgorithmProfilePredictionManager.predict(PIP_NAME, FAM_ELT, 
            algorithms.keySet(), observables, null);
        Assert.assertNotNull(result);
        for (AlgorithmDescriptor alg : algorithms.values()) {
            Map<IObservable, Double> prediction = result.get(alg.getName());
            Assert.assertNotNull(prediction);
            long time = LAST_TIME * TIME_STEP;
            for (IObservable obs : MEASURED) { // CAPACITY / ITEMS does not work, also as validated
                double o;
                if (alg.hasTrace(obs)) {
                    o = alg.getObservation(obs, time);
                } else {  // we have no trace to produce the observation, take the last logged measured one
                    o = alg.getLastLoggedMeasuredValue(obs); 
                }
                double p = AlgorithmProfilePredictionManager.predict(PIP_NAME, FAM_ELT, alg.getName(), obs, null);
                Assert.assertTrue("OBS " + obs + " for " + alg.getName() + " not in 15% " + o + " " + p, 
                    Math.abs(o - p) / o <= 0.15);
            }            
        }
        String best = simpleWeighting(result, weighting);
        Assert.assertEquals(HW_ALG, best); // by construction
    }
    
    /**
     * Implements a simple weighting of mass predictions.
     * 
     * @param predictions the predictions
     * @param weighting the weighting
     * @return the "best" solution
     */
    private static String simpleWeighting(Map<String, Map<IObservable, Double>> predictions, 
        Map<IObservable, Double> weighting) {
        String best = null;
        double bestVal = 0;
        for (Map.Entry<String, Map<IObservable, Double>> pEnt : predictions.entrySet()) {
            String algorithm = pEnt.getKey();
            Map<IObservable, Double> algPredictions = pEnt.getValue();
            double algVal = 0;
            double sum = 0;
            double weights = 0;
            for (Map.Entry<IObservable, Double> ent : weighting.entrySet()) {
                IObservable obs = ent.getKey();
                Double weight = ent.getValue();
                if (null != obs && null != weight) {
                    Double predicted = algPredictions.get(obs);
                    if (null != predicted) {
                        sum += predicted * weight;
                    }
                    weights += weight;
                }
            }
            if (weights != 0) {
                algVal = sum / weights;
            } else {
                algVal = 0;
            }
            if (null == best || algVal > bestVal) {
                best = algorithm;
            }
        }
        return best;
    }

}
