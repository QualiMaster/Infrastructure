package eu.qualimaster.monitoring.storm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.thrift7.TException;

import eu.qualimaster.monitoring.AbstractClusterMonitoringTask;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.systemState.PlatformSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.ResourceUsage;
import backtype.storm.generated.ClusterSummary;
import backtype.storm.generated.SupervisorSummary;

/**
 * A regular task executed to obtain information from a storm cluster.
 * 
 * @author Holger Eichelberger
 */
public class StormClusterMonitoringTask extends AbstractClusterMonitoringTask {

    private static final Logger LOGGER = LogManager.getLogger(StormClusterMonitoringTask.class);
    private StormConnection connection;
    
    /**
     * Creates a monitoring task for a storm cluster.
     * 
     * @param connection the Storm connection
     * @param state the system state to be modified as part of the monitoring
     */
    public StormClusterMonitoringTask(StormConnection connection, SystemState state) {
        super(state);
        this.connection = connection;
    }

    /**
     * Performs the actual monitoring aggregation.
     * 
     * @param part the system part
     * @param supervisors the actual supervisors
     */
    public static void aggregate(PlatformSystemPart part, List<SupervisorSummary> supervisors) {
        // for cleaning up left overs
        Set<Object> usedKeysClear = new HashSet<Object>();
        usedKeysClear.addAll(part.getComponentKeys(ResourceUsage.USED_MACHINES));
        Set<Object> availableKeysClear = new HashSet<Object>();
        availableKeysClear.addAll(part.getComponentKeys(ResourceUsage.AVAILABLE_MACHINES));
        
        // aggregate - multiple supervisors may be on the same machine
        Set<Object> used = new HashSet<Object>();
        Set<Object> unused = new HashSet<Object>();
        Set<Object> available = new HashSet<Object>();
        
        int supervisorCount = supervisors.size();
        for (int s = 0; s < supervisorCount; s++) {
            SupervisorSummary sSummary = supervisors.get(s);
            String key = sSummary.get_host();
            if (sSummary.get_num_used_workers() > 0) {
                used.add(key);
            } else {
                unused.add(key);
            }
            available.add(key);
            usedKeysClear.remove(key);
            availableKeysClear.remove(key);
            part.obtainMachine(key);
        }
        for (Object key : available) {
            part.setValue(ResourceUsage.AVAILABLE_MACHINES, 1, key);
            part.obtainMachine(key.toString()).setValue(ResourceUsage.AVAILABLE, 1, null);
        }
        for (Object key : used) {
            part.setValue(ResourceUsage.USED_MACHINES, 1, key);
        }
        for (Object key : unused) {
            part.setValue(ResourceUsage.USED_MACHINES, 0, key);
        }
        part.clearComponents(ResourceUsage.USED_MACHINES, usedKeysClear);
        part.clearComponents(ResourceUsage.AVAILABLE_MACHINES, availableKeysClear);
        part.removeMachines(availableKeysClear);
    }
    
    @Override
    public void monitor() {
        if (connection.open()) {
            try {
                ClusterSummary summary = connection.getClusterSummary();
                List<SupervisorSummary> supervisors = summary.get_supervisors();
                PlatformSystemPart part = getState().getPlatform();
                
                aggregate(part, supervisors);
                sendSummaryEvent(part, null, MonitoringManager.DEMO_MSG_INFRASTRUCTURE);
            } catch (TException e) {
                LOGGER.error("Cannot obtain thrift data " + e.getMessage(), e);
            } catch (IllegalStateException e) {
                // monitoring runs longer than topology exists... ignore
            }
        }
    }
    
    @Override
    public int getFrequency() {
        return MonitoringConfiguration.getClusterMonitoringFrequency();
    }

    @Override
    protected void failover(Throwable th) {
        connection.close(); // hope that it silently closes and a reconnect happens in the next round
    }
    
}
