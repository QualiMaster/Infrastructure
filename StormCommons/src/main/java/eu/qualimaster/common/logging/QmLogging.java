/*
 * Copyright 2009-2014 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.common.logging;

import java.util.Map;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

/**
 * The QM specific logging functionality. By default, QMspecific logging is disabled as it requires an additional
 * event forwarding thread. You can enable it through {@link #enable(Map)} on a STORM topology configuration setting
 * {@link #ENABLING_PROPERTY} to true.
 * 
 * To utilize this form of logging, please use either {@link eu.qualimaster.common.signal.BaseSignalBolt} or 
 * {@link eu.qualimaster.common.signal.BaseSignalSpout} or call 
 * {@link eu.qualimaster.common.signal.StormSignalConnection#configureEventBus(java.util.Map)}.
 * 
 * @author Holger Eichelberger
 */
public class QmLogging {
    
    public static final String ENABLING_PROPERTY = "qmLogging.enable";
    private static final QmAppender APPENDER = new QmAppender();

    /**
     * Changes the root logging level.
     * 
     * @param level the new logging level
     */
    public static void setLogLevel(Level level) {
        setLogLevel(Logger.ROOT_LOGGER_NAME, level);
    }

    /**
     * Returns the logger instance for a certain class.
     * 
     * @param cls the class the logger shall be returned for
     * @return the logger instance (may be <b>null</b>)
     */
    private static Logger getLogger(Class<?> cls) {
        // getLogger(cls.getName()) would also do the job, but just to be sure
        Logger logger;
        Object tmp = LoggerFactory.getLogger(cls);
        if (tmp instanceof Logger) { // shall be the case in STORM
            logger = (Logger) tmp;
        } else {
            logger = null;
        }
        return logger;
    }
    
    /**
     * Returns the logger instance for a certain logger name.
     * 
     * @param loggerName the logger name
     * @return the logger instance (may be <b>null</b>)
     */
    private static Logger getLogger(String loggerName) {
        Logger logger;
        Object tmp = LoggerFactory.getLogger(loggerName);
        if (tmp instanceof Logger) { // shall be the case in STORM
            logger = (Logger) tmp;
        } else {
            logger = null;
        }
        return logger;
    }

    /**
     * Disables unwanted infrastructure logging.
     */
    public static void disableUnwantedLogging() {
        disableLogger(getLogger(org.apache.storm.zookeeper.ClientCnxn.class));
        disableLogger(getLogger(org.apache.storm.zookeeper.server.NIOServerCnxn.class));
        disableLogger(getLogger(org.apache.storm.curator.framework.imps.CuratorFrameworkImpl.class));
    }

    /**
     * Disables a logger.
     * 
     * @param logger the logger to disable (may be <b>null</b>, ignored then)
     */
    private static void disableLogger(Logger logger) {
        if (null != logger) {
            logger.setLevel(Level.OFF);
        }
    }
    
    /**
     * Changes the log level.
     * 
     * @param loggerName the name of the logger
     * @param level the new logging level
     */
    public static void setLogLevel(String loggerName, Level level) {
        Logger logger = getLogger(loggerName);
        if (null != logger) {
            logger.setLevel(level);
        }
    }
    
    /**
     * Installs the QM logging into slf4j/logback.
     */
    public static void install() {
        Object tmp = LoggerFactory.getILoggerFactory();
        if (tmp instanceof LoggerContext) {
            LoggerContext context = (LoggerContext) tmp;
            APPENDER.setContext(context);
            APPENDER.start();
        }
        configureLogger(Logger.ROOT_LOGGER_NAME);
    }
    
    /**
     * Configures a logger for QM appending.
     * 
     * @param loggerName the name of the logger
     */
    private static void configureLogger(String loggerName) {
        Logger logger = getLogger(loggerName);
        if (null != logger && null == logger.getAppender(APPENDER.getName())) {
            logger.addAppender(APPENDER);
        }
    }
    
    /**
     * Enables logging on the given STORM topology configuration.
     * 
     * @param cfg the configuration
     * @return <code>cfg</code> (builder pattern)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Map enable(Map cfg) {
        cfg.put(QmLogging.ENABLING_PROPERTY, Boolean.TRUE);
        return cfg;
    }
    
}
