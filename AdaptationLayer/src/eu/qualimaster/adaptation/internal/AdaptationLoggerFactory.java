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
package eu.qualimaster.adaptation.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.log4j.LogManager;

import eu.qualimaster.adaptation.AdaptationConfiguration;
import net.ssehub.easy.instantiation.core.model.buildlangModel.ITracer;

/**
 * Creates the adaptation logger. The logger is intended to support reflective adaptation.
 * 
 * @author Holger Eichelberger
 */
public class AdaptationLoggerFactory {

    private static IAdaptationLogger logger = createLogger();
    
    /**
     * Returns the adaptation logger.
     * 
     * @return the adaptation logger (may be <b>null</b> for none)
     */
    private static IAdaptationLogger createLogger() {
        IAdaptationLogger logger = null;
        File f = getAdaptationLogFile();
        if (null != f) {
            try {
                FileOutputStream fos = new FileOutputStream(f);
                logger = new AdaptationLoggerFile(new PrintStream(fos));
            } catch (IOException e) {
                LogManager.getLogger(AdaptationLoggerFactory.class).error("While creating adaptation logger " 
                    + e.getMessage());
            }
        }
        return logger;
    }
    
    /**
     * Closes the actual logger.
     */
    public static void closeLogger() {
        if (null != logger) {
            logger.close();
        }
    }
        
    /**
     * Returns the actual adaptation log file.
     * 
     * @return the log file, <b>null</b> if no logging shall happen
     */
    public static File getAdaptationLogFile() {
        File result = null;
        String location = AdaptationConfiguration.getAdaptationLogReflectiveLocation();
        if (!AdaptationConfiguration.isEmpty(location)) {
            result = new File(location, "reflective.log");
        }
        return result;
    }
    
    /**
     * Creates the log tracer as a delegating tracer.
     * 
     * @param tracer the tracer to delegate to
     * @return the delegating tracer
     */
    public static DelegatingLogTracer createTracer(ITracer tracer) {
        return new DelegatingLogTracer(tracer, logger); // null is not a problem for the logger
    }
    
    /**
     * Returns the adaptation logger.
     * 
     * @return the logger (may be <b>null</b>)
     */
    public static IAdaptationLogger getLogger() {
        return logger;
    }
    
}
