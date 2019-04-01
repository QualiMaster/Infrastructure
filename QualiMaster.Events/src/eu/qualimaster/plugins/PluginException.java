/*
 * Copyright 2009-2019 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.plugins;

/**
 * Plugin execution exception.
 * 
 * @author Holger Eichelberger
 */
public class PluginException extends Exception {

    private static final long serialVersionUID = -1634920696080505217L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param  message the detail message
     */
    public PluginException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and
     * cause.
     *
     * @param  message the detail message
     * @param  cause the cause 
     */
    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
