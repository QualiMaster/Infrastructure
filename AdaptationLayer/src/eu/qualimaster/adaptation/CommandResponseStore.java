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

import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.events.IEvent;
import eu.qualimaster.events.ResponseStore;

/**
 * Stores (CLI) commands an execution events.
 * 
 * @author Holger Eichelberger
 */
class CommandResponseStore extends AdaptationResponseStore<IEvent, CoordinationCommand, 
    CoordinationCommandExecutionEvent> {

    private static final IStoreHandler<IEvent, CoordinationCommand, CoordinationCommandExecutionEvent> DEFAULT_HANDLER 
        = new IStoreHandler<IEvent, CoordinationCommand, CoordinationCommandExecutionEvent>() {

            @Override
            public String getRequestMessageId(CoordinationCommand request) {
                return request.getMessageId(); // unused
            }
    
            @Override
            public String getResponseMessageId(CoordinationCommandExecutionEvent response) {
                return response.getMessageId();
            }
    
            @Override
            public CoordinationCommand castRequest(IEvent event) {
                return ResponseStore.cast(CoordinationCommand.class, event);
            }
    
            @Override
            public CoordinationCommandExecutionEvent castResponse(IEvent event) {
                return ResponseStore.cast(CoordinationCommandExecutionEvent.class, event);
            }
    
        };

    /**
     * Creates the response store.
     * 
     * @param timeout the timeout (disabled if 0 or negative)
     */
    public CommandResponseStore(int timeout) {
        super(timeout, DEFAULT_HANDLER);
    }

    @Override
    public void sendResponse(String failMessage) {
        // not needed
    }

}
