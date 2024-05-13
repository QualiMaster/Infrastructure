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

import eu.qualimaster.adaptation.external.ExecutionResponseMessage;

/**
 * An optional handler for testing. Please use only for testing
 * 
 * @author Holger Eichelberger
 */
public class TestHandler {

    /**
     * Defines a message handler for testing.
     * 
     * @author Holger Eichelberger
     */
    public interface ITestHandler {

        /**
         * Called in case of an (otherwise) undispatched message.
         * 
         * @param message the message
         */
        public void handle(ExecutionResponseMessage message);
        
    }

    private static ITestHandler testHandler;

    /**
     * Returns the test handler.
     * 
     * @return the test handler (may be <b>null</b>)
     */
    public static ITestHandler getHandler() {
        return testHandler;
    }
    
    /**
     * Defines the test handler.
     * 
     * @param handler the handler instance (<b>null</b> for disable)
     */
    public static void setTestHandler(ITestHandler handler) {
        testHandler = handler;
    }
}
