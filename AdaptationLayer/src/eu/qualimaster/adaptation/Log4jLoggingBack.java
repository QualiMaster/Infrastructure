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
package eu.qualimaster.adaptation;

import org.apache.log4j.LogManager;

import eu.qualimaster.adaptation.external.Logging.ILoggingBack;

/**
 * Implements the logging back for Log4j.
 * 
 * @author Holger Eichelberger
 */
public class Log4jLoggingBack implements ILoggingBack {

    public static final ILoggingBack INSTANCE = new Log4jLoggingBack();

    /**
     * Prevents external instantiation.
     */
    private Log4jLoggingBack() {
    }
    
    /**
     * Returns the caller class.
     * 
     * @return the caller class
     */
    private static Class<?> getCaller() {
        Class<?> result = Log4jLoggingBack.class;
        Throwable tmp = new Throwable();
        StackTraceElement[] elts = tmp.getStackTrace();
        if (null != elts && elts.length > 3) { // 0: this method, 1: logging back, 2: static in this class
            String cls = elts[3].getClassName();
            try {
                result = Class.forName(cls);
            } catch (ClassNotFoundException e) {
                // ignore, keep result
            }
        }
        return result;
    }
        
    @Override
    public void error(String text, Throwable throwable) {
        LogManager.getLogger(getCaller()).error(text, throwable);
        
    }

    @Override
    public void error(String text) {
        LogManager.getLogger(getCaller()).error(text);
    }

    @Override
    public void info(String text) {
        LogManager.getLogger(getCaller()).info(text);
    }

    @Override
    public void debug(String text) {
        LogManager.getLogger(getCaller()).debug(text);
    }

}
