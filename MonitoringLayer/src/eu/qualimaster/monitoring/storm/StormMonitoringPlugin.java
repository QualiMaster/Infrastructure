package eu.qualimaster.monitoring.storm;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.monitoring.AbstractClusterMonitoringTask;
import eu.qualimaster.monitoring.AbstractContainerMonitoringTask;
import eu.qualimaster.monitoring.IMonitoringPlugin;
import eu.qualimaster.monitoring.hardware.HardwareConfigurationAccess;
import eu.qualimaster.monitoring.hardware.HardwareConfigurationAccess.HardwareMonitoringInfo;
import eu.qualimaster.monitoring.hardware.MaxelerDfeMonitor;
import eu.qualimaster.monitoring.systemState.SystemState;

/**
 * A monitoring plugin for Apache Storm based on Nimbus client, Thrift and Maxeler hardware monitoring.
 * 
 * @author Holger Eichelberger
 */
public class StormMonitoringPlugin implements IMonitoringPlugin {

    private StormConnection connection;
    
    @Override
    public void start() {
        connection = new StormConnection();
        connection.open(); // just try, no problem if this fails
    }

    @Override
    public void stop() {
        connection.close();
    }

    @Override
    public AbstractContainerMonitoringTask createPipelineTask(String pipeline, SystemState state, 
        Class<? extends AdaptationEvent> adaptationFilter) {
        return new ThriftMonitoringTask(pipeline, connection, state, adaptationFilter);
    }
    
    @Override
    public AbstractClusterMonitoringTask createClusterTask(SystemState state) {
        StormClusterMonitoringTask result = new StormClusterMonitoringTask(connection, state);
        HardwareMonitoringInfo[] info = HardwareConfigurationAccess.getHardwareClusterInfo();
        if (null != info) {
            for (int i = 0; i < info.length; i++) {
                result.add(new MaxelerDfeMonitor(info[i], state));
            }
        }
        return result;
    }
    
}
