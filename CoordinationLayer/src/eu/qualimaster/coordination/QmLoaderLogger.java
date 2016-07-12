/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import de.uni_hildesheim.sse.easy.loader.framework.Log;

/**
 * Implements a delegating loader logger to link the EASy loader with the logging framework used in QualiMaster.
 * 
 * @author Holger Eichelberger
 */
class QmLoaderLogger implements Log.LoaderLogger {
    
    private Logger logger;

    /**
     * Creates a loader logger.
     * 
     * @param caller the caller class to tag the logger
     */
    QmLoaderLogger(Class<?> caller) {
        logger = LogManager.getLogger(caller);
    }
    
    @Override
    public void error(String error) {
        log(Level.ERROR, error, null);
    }

    @Override
    public void error(String error, Exception exception) {
        log(Level.ERROR, error, exception);
    }

    @Override
    public void warn(String warning) {
        log(Level.WARN, warning, null);    
    }

    @Override
    public void warn(String warning, Exception exception) {
        log(Level.WARN, warning, exception);
    }

    @Override
    public void info(String msg) {
        log(Level.INFO, msg, null);
    }

    /**
     * Logs a message and determines the qualified name of the real caller, assuming
     * that this method is called by one of the public log methods in this class, thus
     * omitting two stack trace elements is correct.
     * 
     * @param priority the logging priority
     * @param text the text to log
     * @param exception an optional exception trace (may be <b>null</b>)
     */
    private void log(Priority priority, String text, Exception exception) {
        Throwable t = new Throwable();
        StackTraceElement[] trace = t.getStackTrace();
        String fqcn;
        if (trace.length >= 3) {
            fqcn = trace[2].getClassName();
        } else {
            fqcn = getClass().getName();
        }
        logger.log(fqcn, priority, text, exception);
    }

}