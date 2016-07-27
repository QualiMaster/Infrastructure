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

import java.util.concurrent.atomic.AtomicReference;

import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.CoordinationCommandNotifier;
import eu.qualimaster.coordination.commands.CoordinationCommandNotifier.ICoordinationCommandNotifier;
import eu.qualimaster.events.ResponseStore;

/**
 * Implements a response store which automatically links coordination commands with requests.
 * Instances of this class are registered as {@link ICoordinationCommandNotifier}.
 * 
 * @param <E> the event type
 * @param <R> the request type
 * @param <A> the answer/response type
 * @author Holger Eichelberger
 */
public abstract class AdaptationResponseStore<E, R extends E, A extends E> extends ResponseStore<E, R, A> 
    implements ICoordinationCommandNotifier {

    private AtomicReference<R> currentRequest = new AtomicReference<R>(null);
    
    /**
     * Creates a response store with a certain timeout.
     * 
     * @param timeout the timeout (disabled if 0 or negative)
     * @param handler the store handler
     */
    public AdaptationResponseStore(int timeout, IStoreHandler<E, R, A> handler) {
        super(timeout, handler);
        CoordinationCommandNotifier.addNotifier(this);
    }

    @Override
    public void notifySent(CoordinationCommand command) {
        R current = currentRequest.get();
        if (null != current) {
            sent(current, command.getMessageId());
        }
        AdaptationFiltering.modifyPipelineElementFilters(command, true);
    }

    /**
     * Defines the current request for linking on {@link #notifySent(CoordinationCommand)}.
     * 
     * @param request the request
     */
    public void setCurrentRequest(R request) {
        currentRequest.set(request);
    }
    
    /**
     * Returns the current request.
     * 
     * @return the current request (may be <b>null</b>)
     */
    protected R getCurrentRequest() {
        return currentRequest.get();
    }
    
    /**
     * Tries to send a failure message for the current request. If there is no current request,
     * nothing will happen. 
     * 
     * @param failMessage the message characterizing the explicit failure from adaptation 
     *   (may be <b>null</b>, but ignored then) 
     */
    public abstract void sendResponse(String failMessage);
    
    /**
     * Disposes this instance.
     */
    public void dispose() {
        CoordinationCommandNotifier.removeNotifier(this);
    }

}
