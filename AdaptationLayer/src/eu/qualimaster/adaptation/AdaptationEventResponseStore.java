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

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.adaptation.events.AdaptationEventResponse;
import eu.qualimaster.adaptation.events.AdaptationEventResponse.ResultType;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.ResponseStore;

/**
 * Implements a specific adaptation event/coordination command response store.
 * 
 * @author Holger Eichelberger
 */
class AdaptationEventResponseStore extends AdaptationResponseStore<Object, AdaptationEvent, 
    CoordinationCommandExecutionEvent> {

    private static final IStoreHandler<Object, AdaptationEvent, CoordinationCommandExecutionEvent> DEFAULT_HANDLER 
        = new IStoreHandler<Object, AdaptationEvent, CoordinationCommandExecutionEvent>() {
    
            @Override
            public String getRequestMessageId(AdaptationEvent request) {
                return request.getMessageId(); // unused
            }
    
            @Override
            public String getResponseMessageId(CoordinationCommandExecutionEvent response) {
                return response.getMessageId();
            }
    
            @Override
            public AdaptationEvent castRequest(Object event) {
                return ResponseStore.cast(AdaptationEvent.class, event);
            }
    
            @Override
            public CoordinationCommandExecutionEvent castResponse(Object event) {
                return ResponseStore.cast(CoordinationCommandExecutionEvent.class, event);
            }
    
        };

    /**
     * Creates the response store.
     * 
     * @param timeout the timeout (disabled if 0 or negative)
     */
    public AdaptationEventResponseStore(int timeout) {
        super(timeout, DEFAULT_HANDLER);
    }

    @Override
    public void sendResponse(String failMessage) {
        AdaptationEvent request = getCurrentRequest();
        if (null != request && null != failMessage) {
            AdaptationEventResponse response = new AdaptationEventResponse(request, ResultType.FAILED, failMessage);
            EventManager.send(response);
        }
    }

}
