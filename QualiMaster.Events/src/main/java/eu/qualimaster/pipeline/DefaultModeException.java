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
package eu.qualimaster.pipeline;

/**
 * Shall be called within a pipeline to indicate that data processing failed
 * and the pipeline shall go on with (Java) default values, e.g., to enable debugging. In 
 * the Chania meeting in May 2015 we decided to go for a runtime exception rather than a
 * checked exception in order to keep existing interfaces. Anyway, it is advisable that you
 * declare that your methods may throw this exception in order to enable switching to a 
 * checked exception later.
 *  
 * @author Holger Eichelberger
 */
public class DefaultModeException extends RuntimeException {

    private static final long serialVersionUID = -4133997997138956458L;

    /**
     * Creates a default mode exception.
     * 
     * @param message a message describing the cause of requesting the default mode
     */
    public DefaultModeException(String message) {
        super(message);
    }

    /**
     * Creates a default mode exception.
     * 
     * @param message a message describing the cause of requesting the default mode
     * @param cause a throwable pointing to the actual cause for default mode
     */
    public DefaultModeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a default mode exception.
     * 
     * @param cause a throwable pointing to the actual cause for default mode (its message
     *   is taken as the message of this exception)
     */
    public DefaultModeException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

}
