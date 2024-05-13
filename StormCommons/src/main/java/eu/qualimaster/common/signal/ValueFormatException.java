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
 * Thrown if a value format is not correct.
 * 
 * @author Holger Eichelberger
 */
public class ValueFormatException extends Exception {

    private static final long serialVersionUID = 9157542434680825699L;

    /**
     * Creates a new value format exception based on a given message.
     * 
     * @param message the message text
     */
    public ValueFormatException(String message) {
        super(message);
    }
    
    /**
     * Creates a value format exception from a given throwable.
     * 
     * @param th the throwable to take the message from
     */
    public ValueFormatException(Throwable th) {
        super(th.getMessage(), th);
    }
    
}
