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
package eu.qualimaster.adaptation.events;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractResponseEvent;

/**
 * Response event for an adaptation event.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class AdaptationEventResponse extends AbstractResponseEvent<AdaptationEvent> {

    /**
     * Defines the execution result type.
     * 
     * @author Holger Eichelberger
     */
    public enum ResultType {
        SUCCESSFUL, 
        FAILED
    }

    private static final long serialVersionUID = 8882191691968082956L;
    
    private ResultType result;
    private String message;

    /**
     * Creates a new adaptation event response.
     * 
     * @param returnable the original request
     * @param result the execution result
     * @param message a message detailing the execution (may be <b>null</b> or empty if 
     *   <code>result=={@link ResultType#SUCCESSFUL}</code>
     */
    public AdaptationEventResponse(AdaptationEvent returnable, ResultType result, String message) {
        super(returnable);
        this.result = result;
        this.message = message;
    }
    
    /**
     * Returns the execution result.
     * 
     * @deprecated to be removed, call {@link #getResultType()} instead
     * @return the execution result
     */
    @Deprecated
    public ResultType getState() {
        return result;
    }

    /**
     * Returns the execution result type.
     * 
     * @return the execution result type
     */
    public ResultType getResultType() {
        return result;
    }

    /**
     * Returns the message detailing the execution event.
     * 
     * @return a message detailing the execution (may be <b>null</b> or empty if 
     *     <code>{@link #getResultType()}=={@link State#SUCCESSFUL}</code>
     */
    public String getMessage() {
        return message;
    }

}
