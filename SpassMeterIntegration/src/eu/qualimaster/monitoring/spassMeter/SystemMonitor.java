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
package eu.qualimaster.monitoring.spassMeter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.uni_hildesheim.sse.monitoring.runtime.annotations.TimerState;
import de.uni_hildesheim.sse.monitoring.runtime.boot.MonitoringGroupSettings;
import de.uni_hildesheim.sse.monitoring.runtime.boot.RecorderFrontend;
import de.uni_hildesheim.sse.monitoring.runtime.boot.StreamType;
import de.uni_hildesheim.sse.monitoring.runtime.recording.SystemMonitoring;
import de.uni_hildesheim.sse.monitoring.runtime.recordingStrategies.ProcessData;
import de.uni_hildesheim.sse.monitoring.runtime.recordingStrategies.ProcessData.Measurements;
import de.uni_hildesheim.sse.system.GathererFactory;
import de.uni_hildesheim.sse.system.IMemoryDataGatherer;
import de.uni_hildesheim.sse.system.IProcessorDataGatherer;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;

/**
 * Implements a general resource system monitor service.
 * 
 * @author Holger Eichelberger
 */
public class SystemMonitor {

    public static final int OUT_STEP = 500;
    private static final IMemoryDataGatherer MEMORY_DATA_GATHERER = GathererFactory.getMemoryDataGatherer();
    private static final IProcessorDataGatherer PROCESSOR_DATA_GATHERER = GathererFactory.getProcessorDataGatherer();
    private static final boolean REPORT_AVERAGE = false;

    private Timer timer;
    private ISystemMonitorListener listener;

    /**
     * Listens on updates of the system monitor.
     * 
     * @author Holger Eichelberger
     */
    public interface ISystemMonitorListener {

        /**
         * Called when new observations are made available.
         * 
         * @param observations
         *            the new observations
         */
        public void updateObservations(Map<IObservable, Double> observations);

    }

    /**
     * A fake recorder frontend to obtain output events from
     * {@link SystemMonitoring}. Aim: Keep SPASS-meter as it is.
     * 
     * @author Holger Eichelberger
     */
    private class NodeRecorderFrontend extends RecorderFrontend {

        private boolean firstTime = true;

        @Override
        public void assignAllTo(String arg0, boolean arg1) {
        }

        @Override
        public void changeValueContext(String arg0, boolean arg1) {
        }

        @Override
        public void clearTemporaryData() {
        }

        @Override
        public void configurationChange(String arg0) {
        }

        @Override
        public void enableVariabilityDetection(boolean arg0) {
        }

        @Override
        public void endSystem() {
        }

        @Override
        public void enter(String arg0, String arg1, boolean arg2, boolean arg3, long arg4) {
        }

        @Override
        public void exit(String arg0, String arg1, boolean arg2, boolean arg3, long arg4) {
        }

        @Override
        public void memoryAllocated(Object arg0) {
        }

        @Override
        public void memoryAllocated(Object arg0, long arg1) {
        }

        @Override
        public void memoryAllocated(long arg0, long arg1) {
        }

        @Override
        public void memoryFreed(Object arg0) {
        }

        @Override
        public void memoryFreed(Object arg0, long arg1) {
        }

        @Override
        public void memoryFreed(long arg0, long arg1) {
        }

        @Override
        public void notifyProgramEnd() {
        }

        @Override
        public void notifyProgramStart() {
        }

        @Override
        public void notifyThreadEnd() {
        }

        @Override
        public void notifyThreadEnd(long arg0) {
        }

        @Override
        public void notifyThreadStart(Thread arg0) {
        }

        @Override
        public void notifyThreadStart(long arg0) {
        }

        @Override
        public void notifyTimer(String arg0, TimerState arg1, boolean arg2) {
        }

        @Override
        public void notifyValueChange(String arg0, Object arg1) {
        }

        @Override
        public void notifyValueChange(String arg0, int arg1) {
        }

        @Override
        public void notifyValueChange(String arg0, byte arg1) {
        }

        @Override
        public void notifyValueChange(String arg0, char arg1) {
        }

        @Override
        public void notifyValueChange(String arg0, short arg1) {
        }

        @Override
        public void notifyValueChange(String arg0, long arg1) {
        }

        @Override
        public void notifyValueChange(String arg0, double arg1) {
        }

        @Override
        public void notifyValueChange(String arg0, float arg1) {
        }

        @Override
        public void notifyValueChange(String arg0, String arg1) {
        }

        @Override
        public void notifyValueChange(String arg0, boolean arg1) {
        }

        @Override
        public void printCurrentState() {
            Map<IObservable, Double> observations = new HashMap<IObservable, Double>();
            if (firstTime) {
                observations.put(ResourceUsage.AVAILABLE_MEMORY,
                        Double.valueOf(MEMORY_DATA_GATHERER.getMemoryCapacity()));
                observations.put(ResourceUsage.AVAILABLE_FREQUENCY,
                        Double.valueOf(PROCESSOR_DATA_GATHERER.getMaxProcessorSpeed()));
                observations.put(ResourceUsage.AVAILABLE_CPUS,
                        Double.valueOf(PROCESSOR_DATA_GATHERER.getNumberOfProcessors()));
                firstTime = false;
            }
            if (REPORT_AVERAGE) {
                ProcessData pData = SystemMonitoring.getProcessData();
                Measurements measurements = pData.getSystem();
                observations.put(ResourceUsage.MEMORY_USE, measurements.getAvgMemUse());
                observations.put(ResourceUsage.LOAD, measurements.getAvgLoad());
                ProcessData.release(pData);
            } else {
                observations.put(ResourceUsage.MEMORY_USE, Double.valueOf(MEMORY_DATA_GATHERER.getCurrentMemoryUse()));
                observations.put(ResourceUsage.LOAD, PROCESSOR_DATA_GATHERER.getCurrentSystemLoad());
            }

            if (null != listener) {
                listener.updateObservations(observations);
            }
        }

        @Override
        public void printStatistics() {
        }

        @Override
        public int readIo(String arg0, String arg1, int arg2, StreamType arg3) {
            return 0;
        }

        @Override
        public void registerAsOverheadStream(InputStream arg0) {
        }

        @Override
        public void registerAsOverheadStream(OutputStream arg0) {
        }

        @Override
        public void registerForRecording(String arg0, MonitoringGroupSettings arg1) {
        }

        @Override
        public void registerThisThread(boolean arg0) {
        }

        @Override
        public int writeIo(String arg0, String arg1, int arg2, StreamType arg3) {
            return 0;
        }

    }

    /**
     * Creates a system monitor.
     * 
     * @param listener
     *            the listener to report the caller to
     */
    public SystemMonitor(ISystemMonitorListener listener) {
        this.listener = listener;
    }

    /**
     * Starts this system monitor.
     * 
     * @param frequency
     *            the monitoring frequency - will be discretized in units of
     *            {@value #OUT_STEP} ms due to SPASS-meter.
     */
    public void start(int frequency) {
        frequency = Math.max(100, frequency);
        int outInterval = frequency / OUT_STEP;
        if (frequency % OUT_STEP != 0) {
            outInterval++;
        }

        RecorderFrontend.instance = new NodeRecorderFrontend();

        if (REPORT_AVERAGE) {
            de.uni_hildesheim.sse.monitoring.runtime.configuration.Configuration.INSTANCE.setOutInterval(outInterval);
            SystemMonitoring.startTimer();
        } else {
            timer = new Timer();
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    RecorderFrontend.instance.printCurrentState();

                }
            }, 0, outInterval * OUT_STEP);
        }

    }

    /**
     * Stops this system monitor.
     */
    public void stop() {
        SystemMonitoring.stopTimer();
        if (null != timer) {
            timer.cancel();
        }
    }

}
