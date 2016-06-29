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
package eu.qualimaster.common.hardware;

/**
 * Some utility functions.
 * 
 * @author Holger Eichelberger
 */
public class Utils {

    /**
     * Prevents external creation.
     */
    private Utils() {
    }
    
    /**
     * Returns whether an algorithm id is valid.
     * 
     * @param id the algorithm id
     * @return <code>true</code> in case of valid, <code>false</code> else
     */
    public static boolean isValid(String id) {
        return null != id && id.length() > 0; // TODO ask Gregory
    }
    
    /**
     * Returns whether a text sent along with a message indicates an error.
     * 
     * @param message the message
     * @return <code>true</code> in case of an error, <code>false</code> else
     */
    public static boolean isError(String message) {
        return null != message && message.length() > 0 && message.startsWith("-");
    }
    
    /**
     * Returns whether a text sent along with a message indicates success.
     * 
     * @param message the message
     * @return <code>true</code> in case of success, <code>false</code> else
     */
    public static boolean isSuccess(String message) {
        return null != message && message.length() > 0 && !message.startsWith("-");
    }

}
