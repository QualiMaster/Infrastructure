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
package eu.qualimaster.adaptation.external;

/**
 * A very simple logging frontend so that this part of the adaptation layer can be used in different contexts.
 * However, we do not rely on an existing logging frontend interface to avoid library concepts (we already had).
 * 
 * @author Holger Eichelberger
 */
public class Logging {

    /**
     * An interface for binding the logging calls against a backside logging instance.
     * 
     * @author Holger Eichelberger
     *
     */
    public interface ILoggingBack {

        /**
         * Logs an error.
         * 
         * @param text the text describing the error
         * @param throwable the throwable causing the error
         */
        public void error(String text, Throwable throwable);

        /**
         * Logs an error.
         * 
         * @param text the text describing the error
         */
        public void error(String text);

        /**
         * Logs an information message.
         * 
         * @param text the text describing the error
         */
        public void info(String text);

        /**
         * Logs a debug message.
         * 
         * @param text the text describing the debug information
         */
        public void debug(String text);

    }
    
    public static final ILoggingBack DEFAULT_BACK = new ILoggingBack() {
        
        @Override
        public void info(String text) {
            System.out.println(text);
        }

        @Override
        public void error(String text, Throwable throwable) {
            System.out.println(text);
            throwable.printStackTrace(System.out);
        }

        @Override
        public void error(String text) {
            System.out.println(text);
        }

        @Override
        public void debug(String text) {
            System.out.println(text);
        }
    };
   
    private static ILoggingBack back = DEFAULT_BACK;
    
    /**
     * Sets the logging back instance.
     * 
     * @param backInstance the logging back instance
     */
    public static final void setBack(ILoggingBack backInstance) {
        if (null != backInstance) {
            back = backInstance;
        }
    }

    /**
     * Logs an error.
     * 
     * @param text the text describing the error
     * @param throwable the throwable causing the error
     */
    public static void error(String text, Throwable throwable) {
        back.error(text, throwable);
    }

    /**
     * Logs an error.
     * 
     * @param text the text describing the error
     */
    public static void error(String text) {
        back.error(text);
    }

    /**
     * Logs an information message.
     * 
     * @param text the text describing the error
     */
    public static void info(String text) {
        back.info(text);
    }

    /**
     * Logs a debug message.
     * 
     * @param text the text describing the debug information
     */
    public static void debug(String text) {
        back.debug(text);
    }

}
