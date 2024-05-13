package eu.qualimaster.monitoring.hardware;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.InfrastructurePart;
import eu.qualimaster.monitoring.AbstractMonitoringTask.IPiggybackTask;
import eu.qualimaster.monitoring.events.ResourceChangedAdaptationEvent;
import eu.qualimaster.monitoring.hardware.HardwareConfigurationAccess.HardwareMonitoringInfo;
import eu.qualimaster.monitoring.systemState.HwNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PlatformSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;

/**
 * Implements a Maxeler DFE availability monitoring plugin. 
 * 
 * @author ap0n
 * @author Holger Eichelberger
 */
public class MaxelerDfeMonitor implements IPiggybackTask {

    private static final Logger LOGGER = LogManager.getLogger(MaxelerDfeMonitor.class);
    private MonitorClient client;
    private SystemState state;
    private String name;
    private String host;
    private int port;
    private int dfes;
    private boolean firstRun = true;
    private boolean debug = false;
    
    /**
     * Creates a Maxeler DFE monitor.
     * 
     * @param info the hardware information object
     * @param state the actual monitored system state
     */
    public MaxelerDfeMonitor(HardwareMonitoringInfo info, SystemState state) {
        this.name = info.getName();
        this.host = info.getHost();
        this.state = state;
        this.port = info.getPort();
        dfes = info.getAuxInt(HardwareConfigurationAccess.AUX_DFES, -1);
        connect();
    }
    
    /**
     * Enables the debug mode.
     * 
     * @param debug the new debug mode
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    /**
     * Connects if required.
     */
    private void connect() {
        if (null == client) {
            try {
                client = new MonitorClient(host, port);
            } catch (ConnectException e) {
                stop(); //msg pollutes log
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                stop();
            }
        }
    }

    @Override
    public void stop() {
        close(true);
    }
    
    /**
     * Closes the client connection.
     * 
     * @param logException log exceptions or not
     */
    private void close(boolean logException) {
        if (null != client) {
            try {
                client.close();
            } catch (IOException e) {
                if (logException) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
            client = null;
        }
    }
    
    @Override
    public void run() {
        connect(); // connect if not already connected
        int freeDFEs = -1;
        int usedDFEs = -1;
        if (null != client && dfes >= 0) {
            try {
                if (debug) {
                    LOGGER.info("Requesting DFEs from " + host + "@" + port);
                }
                client.sendDFEMonitorRequest();
                String data = client.receiveData();
                if (debug) {
                    LOGGER.info("Received '" + data + "'");
                }
                freeDFEs = dfes; // fallback, pretend that all are used if no meaningful data is provided
                if (null != data && data.length() > 0) {
                    try {
                        freeDFEs = Integer.parseInt(data);
                    } catch (NumberFormatException e) {
                        LOGGER.error("While receiving HW monitoring data " + e.getMessage(), e);
                    }
                }
                usedDFEs = Math.max(0, dfes - freeDFEs); // misconfigured?
            } catch (SocketTimeoutException e) {
                // don't care
                close(false);
            } catch (IOException e) {
                LOGGER.debug(e.getMessage(), e);    
                close(false);
            }
        }
        
        PlatformSystemPart platform = state.getPlatform();
        if (freeDFEs < 0 || usedDFEs < 0) { // machine not reachable at all
            List<Object> keys = new ArrayList<Object>();
            keys.add(name);
            platform.removeHwNode(keys);
            platform.clearComponents(ResourceUsage.AVAILABLE_DFES, keys);
            platform.clearComponents(ResourceUsage.USED_DFES, keys);
            firstRun = true;
        } else {
            HwNodeSystemPart hwNode = platform.obtainHwNode(name);
            Double availDfesBefore = hwNode.getObservedValue(ResourceUsage.AVAILABLE_DFES);
            Double usedDfesBefore = hwNode.getObservedValue(ResourceUsage.USED_DFES);
            
            // init
            platform.setValue(ResourceUsage.AVAILABLE_DFES, dfes, host);
            hwNode.setValue(ResourceUsage.AVAILABLE_DFES, dfes, host);
            platform.setValue(ResourceUsage.USED_DFES, usedDFEs, host);
            hwNode.setValue(ResourceUsage.USED_DFES, usedDFEs, host);
            hwNode.setValue(ResourceUsage.AVAILABLE, 1, null);

            if (!firstRun) { // avoid unnecessary event
                Double availDfesAfter = hwNode.getObservedValue(ResourceUsage.AVAILABLE_DFES);
                Double usedDfesAfter = hwNode.getObservedValue(ResourceUsage.USED_DFES);
                
                // preliminary
                Map<String, Double> beforeMap = new HashMap<String, Double>();
                Map<String, Double> afterMap = new HashMap<String, Double>();
                fillIfDifferent(beforeMap, afterMap, ResourceUsage.AVAILABLE_CPUS, availDfesBefore, availDfesAfter);
                fillIfDifferent(beforeMap, afterMap, ResourceUsage.USED_CPUS, usedDfesBefore, usedDfesAfter);
                if (!beforeMap.isEmpty() || !afterMap.isEmpty()) {
                    ResourceChangedAdaptationEvent evt = new ResourceChangedAdaptationEvent(InfrastructurePart.HARDWARE,
                        name, beforeMap, afterMap);
                    EventManager.send(evt);
                }
            }
            firstRun = false;
        }
    }
    
    /**
     * Fill the maps if the values before / after differ.
     * 
     * @param beforeMap the map of the different values before monitoring (may be modified as a side effect)
     * @param afterMap the map of the different values after monitoring (may be modified as a side effect)
     * @param observable the observable in question
     * @param before the value before (may be <b>null</b>)
     * @param after the value after (may be <b>null</b>)
     */
    private void fillIfDifferent(Map<String, Double> beforeMap, Map<String, Double> afterMap, 
        IObservable observable, Double before, Double after) {
        boolean record = false;
        if (null == before && null != after) {
            record = true;
        } else if (null != before && null == after) {
            record = true;
        } else if (null != before && null != after) {
            if (before.intValue() != after.intValue()) {
                record = true;
            }
        }
        if (record) {
            beforeMap.put(observable.toString(), before);
            afterMap.put(observable.toString(), after);
        }
    }
    
    /**
     * Simple test.
     * 
     * @param args the first argument shall be the name, the second the IP and the third the port.
     */
    public static void main(String[] args) {
        if (args.length == 3) { 
            String name = args[0];
            String host = args[1];
            int port = 0;
            try {
                port = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage());
                System.exit(0);
            }
        
            SystemState state = new SystemState();
            Map<String, Object> aux = new HashMap<String, Object>();
            aux.put(HardwareConfigurationAccess.AUX_DFES, 4);
            HardwareMonitoringInfo info = new HardwareMonitoringInfo(name, host, port, aux);
            MaxelerDfeMonitor monitor = new MaxelerDfeMonitor(info, state);
            monitor.run();
            PlatformSystemPart platform = state.getPlatform();
            System.out.println("Available DFEs: " + platform.getObservedValueInt(ResourceUsage.AVAILABLE_DFES));
            System.out.println("Used DFEs: " + platform.getObservedValueInt(ResourceUsage.USED_DFES));
            monitor.stop();
        }
    }
    
}
