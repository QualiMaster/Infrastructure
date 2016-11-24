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
package eu.qualimaster.common.signal;

/**
 * Thrown if sending a signal fails.
 * 
 * @author Holger Eichelberger
 */
public class SignalException extends Exception {

    private static final long serialVersionUID = -6635515691636886553L;

    /**
     * Creates a new exception based on a given message.
     * 
     * @param message the message text
     */
    public SignalException(String message) {
        super(message);
    }
    
    /**
     * Creates a new exception based on a given message.
     * 
     * @param message the message text
     * @param th the throwable representing the cause
     */
    public SignalException(String message, Throwable th) {
        super(message, th);
    }
    
    /**
     * Creates a new exception from a given throwable.
     * 
     * @param th the throwable to take the message from
     */
    public SignalException(Throwable th) {
        super(th.getMessage(), th);
    }
    
}
