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
package eu.qualimaster.events;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements a time-based response store for events. This store does not clear itself
 * rather than being cleared explicitly from outside. However, this class is intended to
 * be generic also for other similar event mechanisms, e.g., on top-level of the 
 * infrastructure for external communication. As these event mechanisms may not have the
 * same interfaces and we want to reuse the implementation, we handle type-specific operations
 * through a replaceable plug-in {@link IStoreHandler store handler} and provide a default
 * store handler for the QualiMaster event bus.
 * 
 * @param <E> the basic event type (may be <code>Object</code> in case of unrelated event hierarchies)
 * @param <R> the request message type
 * @param <A> the answer/response message type
 * @author Holger Eichelberger
 */
public class ResponseStore<E, R extends E, A extends E> {
    
    /**
     * Defines the interface for a store handler.
     * 
     * @param <E> the basic event type
     * @param <R> the request message type
     * @param <A> the answer/response message type
     * @author Holger Eichelberger
     */
    public interface IStoreHandler<E, R, A> {
        
        /**
         * Returns the message id for a request.
         * 
         * @param request the request
         * @return the message id
         */
        public String getRequestMessageId(R request);

        /**
         * Returns the message id for a response.
         * 
         * @param response the response
         * @return the message id
         */
        public String getResponseMessageId(A response);

        /**
         * Casts a general event to a request if possible.
         * 
         * @param event the event
         * @return the instance of type R of E, <b>null</b> if not possible
         */
        public R castRequest(E event);

        /**
         * Casts a general event to a response if possible.
         * 
         * @param event the event
         * @return the instance of type R of A, <b>null</b> if not possible
         */
        public A castResponse(E event);
    }
    
    /**
     * Implements a default store handler for the event bus.
     * 
     * @author Holger Eichelberger
     */
    public static final IStoreHandler<IEvent, IReturnableEvent, IResponseEvent> DEFAULT_HANDLER 
        = new IStoreHandler<IEvent, IReturnableEvent, IResponseEvent>() {

            @Override
            public String getRequestMessageId(IReturnableEvent request) {
                return request.getMessageId();
            }

            @Override
            public String getResponseMessageId(IResponseEvent request) {
                return request.getMessageId();
            }

            @Override
            public IReturnableEvent castRequest(IEvent event) {
                return cast(IReturnableEvent.class, event);
            }

            @Override
            public IResponseEvent castResponse(IEvent event) {
                return cast(IResponseEvent.class, event);
            }
            
        };
    
    /**
     * Records when a certain event has been sent.
     * 
     * @param <R> the request type
     * @author Holger Eichelberger
     */
    protected static class EventRecord<R> {
        
        private R event;
        private long timestamp = System.currentTimeMillis();

        /**
         * Creates the event record.
         * 
         * @param event the event sent
         */
        private EventRecord(R event) {
            this.event = event;
        }
        
        /**
         * The timestamp when {@link #event} was sent.
         * 
         * @return the timestamp
         */
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * Returns the event sent.
         * 
         * @return the event
         */
        public R getEvent() {
            return event;
        }
        
    }

    private int timeout;
    private long lastCleanup;
    private IStoreHandler<E, R, A> handler;
    private Map<String, EventRecord<R>> data = new HashMap<String, EventRecord<R>>();

    /**
     * Creates a response store with a certain timeout.
     * 
     * @param timeout the timeout (disabled if 0 or negative)
     * @param handler the store handler
     */
    public ResponseStore(int timeout, IStoreHandler<E, R, A> handler) {
        this.timeout = Math.max(0, timeout);
        this.handler = handler;
    }

    /**
     * Creates a response store for the event bus.
     * 
     * @param timeout the timeout (disabled if 0 or negative)
     * @return the store instance
     */
    public static ResponseStore<IEvent, IReturnableEvent, IResponseEvent> createDefaultStore(int timeout) {
        return new ResponseStore<>(timeout, DEFAULT_HANDLER);
    }
    
    /**
     * Implements a safe cast from <code>obj</code> to an instance of T if possible.
     * 
     * @param <T> the target type
     * @param cls the class defining the target type
     * @param obj the object to cast
     * @return the casted object or <b>null</b> if a cast is not possible
     */
    public static <T> T cast(Class<T> cls, Object obj) {
        T result = null;
        if (cls.isInstance(obj)) {
            result = cls.cast(obj);
        }
        return result;
    }

    /**
     * To be called when a certain event is emitted. Is recorded if it is a recordable event.
     * 
     * @param event the event
     */
    public void sentEvent(E event) {
        R request = handler.castRequest(event);
        if (null != request) {
            sent(request);
        }
    }

    /**
     * To be called when a certain event is emitted.
     * 
     * @param event the event
     */
    public void sent(R event) {
        sent(event, handler.getRequestMessageId(event));
    }

    /**
     * To be called when a certain event is emitted. This method explicitly allows 
     * specifying the message id to be used as a key for storing. Please note that
     * using this method requires that the message id of the A events used in 
     * {@link #received(Object)} or {@link #receivedEvent(Object)} must fit to the
     * explicitly given message ids.
     * 
     * @param event the event
     * @param messageId the message id to store events
     */
    public void sent(R event, String messageId) {
        synchronized (data) {
            data.put(messageId, new EventRecord<R>(event));
        }
    }
    
    /**
     * To be called when an event is received.
     * 
     * @param event the event
     * @return the corresponding request event (<b>null</b> if there is none recorded
     *     or already cleared).
     */
    public R receivedEvent(E event) {
        R result = null;
        A response = handler.castResponse(event);
        if (null != response) {
            result = received(response);
        }
        return result;
    }

    /**
     * Handles a received event and returns the corresponding request element.
     * 
     * @param event the received event
     * @return the corresponding request event (<b>null</b> if there is none recorded
     *     or already cleared).
     * @see #remove(EventRecord)
     */
    public R received(A event) {
        R result = null;
        String respId = handler.getResponseMessageId(event);
        synchronized (data) {
            EventRecord<R> record = data.get(respId);
            if (null != record && checkRemove(event, record)) {
                data.remove(respId);
                if (null != record) {
                    result = record.getEvent();
                }
            }
        }
        return result;
    }
    
    /**
     * Called to determine whether <code>record</code> shall be removed.
     * 
     * @param event the received event causing the removal check
     * @param record the record to be tested
     * @return <code>true</code> for remove, <code>false</code> else
     */
    protected boolean checkRemove(A event, EventRecord<R> record) {
        return true;
    }
    
    /**
     * Returns whether the given <code>messageId</code> is known.
     * 
     * @param messageId the message id
     * @return <code>true</code> if known, <code>false</code> else
     */
    public boolean registered(String messageId) {
        boolean result = false;
        synchronized (data) {
            result = data.containsKey(messageId);
        }
        return result;
    }
    
    /**
     * Clears all events that are older than given by the timeout and the current time.
     * 
     * @see #removingBytimeout(EventRecord)
     */
    public void clear() {
        long border = System.currentTimeMillis() - timeout;
        if (timeout > 0 && (0 == lastCleanup || lastCleanup < border)) {
            // currently we expect that not so many entries will remain, if any
            synchronized (data) {
                Iterator<EventRecord<R>> iter = data.values().iterator();
                while (iter.hasNext()) {
                    EventRecord<R> rec = iter.next();
                    if (rec.getTimestamp() < border) {
                        iter.remove();
                        removingBytimeout(rec);
                    }
                }
            }
        }
    }
    
    /**
     * Is called when <code>rec</code> is removed from the store by timeout.
     * 
     * @param rec the record
     */
    protected void removingBytimeout(EventRecord<R> rec) {
    }
    
    /**
     * Clears all entries.
     */
    public void clearAll() {
        synchronized (data) {
            data.clear();
        }
    }
    
    /**
     * Returns whether this store is empty.
     * 
     * @return <code>true</code> if this store is empty, <code>false</code> else
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

}
