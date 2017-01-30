package eu.qualimaster.adaptation.platform;

import java.io.File;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import eu.qualimaster.adaptation.AdaptationConfiguration;
import eu.qualimaster.adaptation.AdaptationManager;
import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.ZkUtils;
import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.dataManagement.events.IShutdownListener;
import eu.qualimaster.dataManagement.events.ShutdownEvent;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.MonitoringInformationEvent;

/**
 * A simple main class for the entire platform. Sorry for just putting it on the top layer
 * of the platform. This program reads its settings from qm.infrastructure.cfg, which may
 * be given as command line parameter, in the same directory or in Unix in <code>/etc/qualiMaster</code>. 
 * Similarly, a logback configuration file (<code>logback.xml</code>) can be placed in one of these two
 * directories or specified through the JVM parameter (<code>logback.configurationFile</code>).
 * By default, this application logs to the local directory (set as <code>qm.log.dir</code>) if not under 
 * Unix <code>/var/log/qualiMaster</code> does exist and is writable. 
 * 
 * @author Holger Eichelberger
 */
public class Main extends ToolBase implements IShutdownListener {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private static final WaitingCallback WAITING_CALLBACK = new WaitingCallback();
    
    /**
     * Implements the waiting callback to end waiting when the infrastructure shuts down due to an event.
     * 
     * @author Holger Eichelberger
     */
    private static class WaitingCallback implements IWaitingCallback {

        private boolean continueWaiting = true;

        @Override
        public boolean continueWaiting() {
            return continueWaiting;
        }
        
        /**
         * Stops waiting.
         */
        void stopWaiting() {
            continueWaiting = false;
        }
        
    }
    
    /**
     * Starts up the platform.
     * 
     * @param configFile the configuration file (may be <b>null</b>)
     */
    private void startupPlatform(File configFile) {
        if (null != configFile && configFile.exists()) {
            LOGGER.info("Reading platform configuration " + configFile + " ...");
            AdaptationConfiguration.configure(configFile);
        }
        LOGGER.info("Starting event bus ...");
        EventManager.startServer();  
        EventManager.disableLoggingFor(MonitoringInformationEvent.class);
        LOGGER.info("Starting data manager ...");
        DataManager.start();
        LOGGER.info("Starting coordination manager ... (task reallocation" + ZkUtils.isQmStormVersion() + ")");
        CoordinationManager.start();
        LOGGER.info("Starting monitoring manager ...");
        MonitoringManager.start();
        LOGGER.info("Starting adaptation manager ...");
        AdaptationManager.setShutdownListener(this);
        AdaptationManager.start();
        LOGGER.info("QualiMaster infrastructure is up and running ... terminate by Ctrl-C");
    }
    
    /**
     * Shuts down the platform.
     */
    private void shutdownPlatform() {
        LOGGER.info("Stopping adaptation manager ...");
        AdaptationManager.stop();
        LOGGER.info("Stopping monitoring manager ...");
        MonitoringManager.stop();
        LOGGER.info("Stopping coordination manager ...");
        CoordinationManager.stop();
        LOGGER.info("Stopping data manager ...");
        DataManager.stop();
        LOGGER.info("Stopping event manager ...");
        EventManager.stop();        
    }
    
    /**
     * The main program for starting and shutting down the platform (Ctrl-C). Runs forever.
     * 
     * @param args the command line arguments (if given, the first parameter denotes the 
     *   platform configuration file)
     */
    public static void main(String[] args) {
        configureLogging();
        // for Ctrl-C
        Main main = new Main();
        main.registerShutdownHook();
        
        File configFile;
        if (args.length > 0) {
            configFile = new File(args[0]);
        } else {
            configFile = obtainConfigurationFile(CFG_FILE);
        }
        
        // startup
        main.startupPlatform(configFile);
        
        // and wait
        wait(WAITING_CALLBACK);
    }

    @Override
    protected void shuttingDown() {
        shutdownPlatform();
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        WAITING_CALLBACK.stopWaiting();
    }
    
}
