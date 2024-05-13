/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A synchronous event store enabling synchronous communication via the events. Usage: Create an appropriate instance, 
 * create an event and call {@link #waitFor(long, long, IReturnableEvent)} on that event.
 * 
 * @param <R> the request type
 * @param <A> the response type
 * @author Holger Eichelberger
 */
public class SynchronousEventStore<R extends IReturnableEvent, A extends IResponseEvent> {

    private Map<String, A> store = Collections.synchronizedMap(new HashMap<String, A>());
    private WaitForHandler handler;

    /**
     * Creates an event store.
     * 
     * @param responseClass the class for responses
     */
    public SynchronousEventStore(Class<A> responseClass) {
        this.handler = new WaitForHandler(responseClass);
        EventManager.register(this.handler);
    }

    /**
     * Implements a handler for response events.
     * 
     * @author Holger Eichelberger
     */
    private class WaitForHandler extends EventHandler<A> {

        /**
         * Creates the handler.
         * 
         * @param eventClass the class of the response event
         */
        protected WaitForHandler(Class<A> eventClass) {
            super(eventClass);
        }

        @Override
        protected void handle(A response) {
            store.put(response.getMessageId(), response);
        }
        
    }

    /**
     * Waits for a response.
     * 
     * @param timeout the timeout for waiting for the response in ms (ignored if less than 100 ms)
     * @param sleep the sleep time between two checks in ms (ignored if less than 100 ms) 
     * @param request the request (unsent)
     * @return the response if received within the timeout, <b>null</b> else
     */
    public A waitFor(long timeout, long sleep, R request) {
        long end = System.currentTimeMillis() + Math.max(100, timeout);
        long s = Math.max(100, sleep);
        EventManager.send(request);
        String msgId = request.getMessageId();
        while (!store.containsKey(msgId) && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(s);
            } catch (InterruptedException e) {
            }
        }
        return store.remove(msgId);
    }
    
}
