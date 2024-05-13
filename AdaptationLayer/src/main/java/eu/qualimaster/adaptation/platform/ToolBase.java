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
package eu.qualimaster.adaptation.platform;

import java.io.File;

import org.apache.commons.lang.SystemUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Some reusable tool functions.
 * 
 * @author Holger Eichelberger
 */
public abstract class ToolBase {

    public static final String LOG_CFG_FILE = "logback.xml";
    public static final String CFG_FILE = "qm.infrastructure.cfg";

    /**
     * Registers a shutdown hook calling {@link #shuttingDown()} if activated.
     */
    protected void registerShutdownHook() {
        // for Ctrl-C
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            
            @Override
            public void run() {
                shuttingDown();
            }
        }));
    }
    
    /**
     * Is called from the shutdown hook after {@link #registerShutdownHook()} is called.
     */
    protected void shuttingDown() {
    }
    
    /**
     * A waiting callback to stop waiting.
     * 
     * @author Holger Eichelberger
     */
    protected interface IWaitingCallback {

        /**
         * Whether waiting shall go on.
         * 
         * @return <code>true</code> go on waiting, <code>false</code> stop waiting
         */
        public boolean continueWaiting();
        
    }
    
    /**
     * Waits endless.
     */
    protected static void waitEndless() {
        // and wait
        wait(null);
    }

    /**
     * Waits endless.
     * 
     * @param callback a callback to stop waiting (wait endless if <b>null</b>)
     */
    protected static void wait(final IWaitingCallback callback) {
        // and wait
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                while (null == callback || callback.continueWaiting()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }
    
    /**
     * Returns whether <code>file</code> is a directory and accessible.
     * 
     * @param file the file
     * @return <code>true</code> if <code>file</code> is a directory and accessible, <code>false</code> else 
     */
    protected static boolean isAccessibleDir(File file) {
        return file.exists() && file.isDirectory() && file.canWrite();
    }
    
    /**
     * Configures logging from a given file.
     * 
     * @param file the file to be used for logging
     * @param reset whether the logging context shall be reset (may be relevant for single-step configurations)
     */
    public static void configureLogging(File file, boolean reset) {
        File logDir = new File(System.getProperty("java.io.tmpdir"));
        if (SystemUtils.IS_OS_UNIX) {
            File tmp = new File("/var/log");
            if (isAccessibleDir(tmp)) {
                logDir = tmp;
            }
        }
        System.setProperty("qm.log.dir", logDir.getAbsolutePath());

        if (file.exists() && file.canRead()) {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                // Call context.reset() to clear any previous configuration, e.g. default 
                // configuration. For multi-step configuration, omit calling context.reset().
                if (reset) {
                    context.reset();
                }
                context.reset(); 
                configurator.doConfigure(file);
            } catch (JoranException je) {
                // StatusPrinter will handle this
            }
            StatusPrinter.printInCaseOfErrorsOrWarnings(context);
        } // ignore and use default configuration
    }

    /**
     * Returns whether <code>file</code> is readable.
     * 
     * @param file the file to be checked
     * @return <code>true</code> if <code>file</code> is readable, <code>false</code> else
     */
    protected static boolean isReadable(File file) {
        return file.exists() && file.isFile() && file.canRead();
    }

    /**
     * Returns the configuration file, possibly considering OS-specific default file locations.
     * 
     * @param fileName the file name
     * @return the related file object
     */
    static File obtainConfigurationFile(String fileName) {
        File result = null;
        if (SystemUtils.IS_OS_UNIX) {
            File tmp = new File("/etc/qualiMaster", fileName);
            if (isReadable(tmp)) {
                result = tmp;
            } else {
                tmp = relocate(fileName);
                if (isReadable(tmp)) {
                    result = tmp;
                }
            }
        }
        if (null == result) {
            // use local directory instead (interactive mode)
            result = new File(fileName); 
        }
        return result;
    }
    
    /**
     * Configures the logging for the infrastructure configuration {@link #LOG_CFG_FILE}.
     */
    protected static void configureLogging() {
        configureLogging(obtainConfigurationFile(LOG_CFG_FILE), false);
    }
    
    /**
     * Relocates a file to the QM infrastructure home.
     * 
     * @param name the name of the file
     * @return the relocated file
     */
    protected static File relocate(String name) {
        return new File(qmHome(), name);
    }
    
    /**
     * Returns the QM infrastructure home (through "qm.home.dir" or ".").
     * 
     * @return the QM infrastructure home
     */
    protected static File qmHome() {
        return new File(System.getProperty("qm.home.dir", "."));
    }

}
