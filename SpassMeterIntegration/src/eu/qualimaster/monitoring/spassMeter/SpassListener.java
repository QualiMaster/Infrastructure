package eu.qualimaster.monitoring.spassMeter;

import de.uni_hildesheim.sse.monitoring.runtime.annotations.Helper;
import de.uni_hildesheim.sse.monitoring.runtime.configuration.Configuration;
import de.uni_hildesheim.sse.monitoring.runtime.plugins.IMeasurements;
import de.uni_hildesheim.sse.monitoring.runtime.plugins.IMonitoringGroup;
import de.uni_hildesheim.sse.monitoring.runtime.plugins.MonitoringGroupBurstChangeListener;
import de.uni_hildesheim.sse.monitoring.runtime.plugins.MonitoringGroupCreationListener;
import de.uni_hildesheim.sse.monitoring.runtime.recording.SystemMonitoring;
import de.uni_hildesheim.sse.monitoring.runtime.recordingStrategies.RecorderElement;
import de.uni_hildesheim.sse.monitoring.runtime.utils.HashMap;
import de.uni_hildesheim.sse.monitoring.runtime.utils.LongHashMap.MapElement;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.AlgorithmMonitoringEvent;
import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.events.ComponentKeyRegistry;
import eu.qualimaster.monitoring.events.PlatformMonitoringEvent;
import eu.qualimaster.observables.ResourceUsage;

/**
 * Implements a listener which connects SPASS-meter to the QM infrastructure.
 * 
 * @author Holger Eichelberger
 */
class SpassListener implements MonitoringGroupCreationListener, MonitoringGroupBurstChangeListener {

    private HashMap<String, IMonitoringGroup> groups = new HashMap<String, IMonitoringGroup>();
    
    @Override
    public void notifyBurstChange(IMeasurements system, IMeasurements jvm) {
        // PARALELLEIZE?
        HashMap<String, IMonitoringGroup> tmp;
        synchronized (SpassListener.class) {
            // Avoid concurrent modifications during loop
            tmp = groups;
            groups = new HashMap<String, IMonitoringGroup>();
        }
        for (HashMap.Entry<String, IMonitoringGroup> entry : tmp.entries()) {
            IMonitoringGroup group = entry.getValue();
            String algorithmId = entry.getKey();
            Iterable<MapElement<RecorderElement>> elements = group.instanceRecorderElements();
            if (null != elements) {
                for (MapElement<RecorderElement> rElt : elements) {
                    ComponentKey key = ComponentKeyRegistry.getRegisteredComponentKey(rElt.getKey());
                    if (null != key) {
                        sendAlgorithmMonitoringEvent(group, algorithmId, key);
                    }
                }
            }
        }
        synchronized (SpassListener.class) {
            // Restore groups, otherwise monitoring will stop.
            if (!groups.isEmpty()) {
                tmp.putAll(groups); 
            }
            groups = tmp;
        }
        double jvmMemUse = jvm.getAvgMemUse();
        if (jvmMemUse > 0) {
            EventManager.handle(new PlatformMonitoringEvent(ComponentKeyRegistry.getPipelineName(), 
                ResourceUsage.MEMORY_USE, jvmMemUse, null));
        }
        String freq = System.getProperty("qm.spass.frequency", null);
        if (null != freq) {
            try {
                int f = Integer.parseInt(freq);
                int outInterval = (int) (Math.max(0, f) % SystemMonitoring.LOAD_COUNTER_PERIOD);
                Configuration.INSTANCE.setOutInterval(outInterval);
            } catch (NumberFormatException e) {
                // ignore
            }
            System.clearProperty("qm.spass.frequency");
        }
    }
    
    /**
     * Sends an algorithm monitoring event.
     * 
     * @param group the monitoring group to take the data from
     * @param algorithmId the algorithm id (class name)
     * @param key the component key (may be <b>null</b>)
     */
    private static void sendAlgorithmMonitoringEvent(IMonitoringGroup group, String algorithmId, 
        ComponentKey key) {
        long memUse = group.getMemUse();
        if (memUse > 0) {
            EventManager.handle(new AlgorithmMonitoringEvent(ComponentKeyRegistry.getPipelineName(), algorithmId, key, 
                ResourceUsage.MEMORY_USE, memUse));
        }
    }

    @Override
    public void contributionCreated(String recId, String contribution, IMonitoringGroup element) {
        synchronized (SpassListener.class) {
            groups.put(recId + "." + contribution, element);
        }
    }

    @Override
    public void monitoringGroupCreated(String recId, IMonitoringGroup element) {
        synchronized (SpassListener.class) {
            if (!Helper.RECORDER_ID.equals(recId) && !Helper.PROGRAM_ID.equals(recId)) {
                groups.put(recId, element);
            }
        }
    }

    @Override
    public void configurationCreated(String recId, IMonitoringGroup element) {
    }

}
