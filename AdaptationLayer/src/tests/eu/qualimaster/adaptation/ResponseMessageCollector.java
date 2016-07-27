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
package tests.eu.qualimaster.adaptation;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.adaptation.events.AdaptationEventResponse;
import eu.qualimaster.adaptation.external.ExecutionResponseMessage;
import eu.qualimaster.adaptation.external.RequestMessage;

/**
 * A generic response message collector asserting expected combinations of requests and responses.
 * 
 * @param <R> the request type
 * @param <A> the response / answer type
 * @author Holger Eichelberger
 */
public class ResponseMessageCollector<R, A> {

    /**
     * Defines message states.
     * 
     * @author Holger Eichelberger
     */
    public enum MessageState {
        REGISTERED,
        RESPONDED,
        RESPONDED_TOO_OFTEN,
        UNEXPECTED
    }

    /**
     * Handles messages, in particular provides access to their ids.
     *
     * @param <M> the message type
     * @author Holger Eichelberger
     */
    public interface IMessageHandler<M> {
        
        /**
         * Returns the request message id.
         * 
         * @param message the message
         * @return the id
         */
        public String getMessageId(M message);
    }
    
    public static final IMessageHandler<RequestMessage> REQUESTMESSAGE_HANDLER 
        = new IMessageHandler<RequestMessage>() {

            @Override
            public String getMessageId(RequestMessage message) {
                return message.getMessageId();
            }
            
        };

    public static final IMessageHandler<ExecutionResponseMessage> EXECUTIONRESPONSE_HANDLER 
        = new IMessageHandler<ExecutionResponseMessage>() {

            @Override
            public String getMessageId(ExecutionResponseMessage message) {
                return message.getMessageId();
            }
            
        };
        
        
    public static final IMessageHandler<AdaptationEvent> ADAPTATIONEVENT_HANDLER 
        = new IMessageHandler<AdaptationEvent>() {

            @Override
            public String getMessageId(AdaptationEvent message) {
                return message.getMessageId();
            }
    
            
        };

    public static final IMessageHandler<AdaptationEventResponse> ADAPTATIONEVENTRESPONSE_HANDLER 
        = new IMessageHandler<AdaptationEventResponse>() {

            @Override
            public String getMessageId(AdaptationEventResponse message) {
                return message.getMessageId();
            }
            
        };

    private IMessageHandler<R> requestHandler;
    private IMessageHandler<A> responseHandler;
    private HashMap<String, MessageState> expectedResponse = new HashMap<String, MessageState>();

    /**
     * Creates a new response message collector.
     * 
     * @param requestHandler the request message handle
     * @param responseHandler the response message handle
     */
    public ResponseMessageCollector(IMessageHandler<R> requestHandler, IMessageHandler<A> responseHandler) {
        this.requestHandler = requestHandler;
        this.responseHandler = responseHandler;
    }

    /**
     * Creates an internal (message-message) collector.
     * 
     * @return the internal message collector
     */
    public static ResponseMessageCollector<RequestMessage, ExecutionResponseMessage> createExternalCollector() {
        return new ResponseMessageCollector<>(REQUESTMESSAGE_HANDLER, EXECUTIONRESPONSE_HANDLER);
    }

    /**
     * Creates an internal (adaptation-adaptation) message collector.
     * 
     * @return the internal message collector
     */
    public static ResponseMessageCollector<AdaptationEvent, AdaptationEventResponse> createInternalCollector() {
        return new ResponseMessageCollector<>(ADAPTATIONEVENT_HANDLER, ADAPTATIONEVENTRESPONSE_HANDLER);
    }

    /**
     * Called to notify the reception of a response / answer.
     * 
     * @param msg the response / answer message
     */
    public void received(A msg) {
        String id = responseHandler.getMessageId(msg);
        MessageState state = expectedResponse.get(id);
        if (MessageState.REGISTERED == state) {
            expectedResponse.put(id, MessageState.RESPONDED);
        } else if (MessageState.RESPONDED == state) {
            expectedResponse.put(id, MessageState.RESPONDED_TOO_OFTEN);
        } else if (null == state) {
            expectedResponse.put(id, MessageState.UNEXPECTED);
        }
    }

    /**
     * Registers a message to indicate that a response is expected.
     * 
     * @param msg the message to register
     */
    public void registerForResponse(R msg) {
        expectedResponse.put(requestHandler.getMessageId(msg), MessageState.REGISTERED);
    }
    
    /**
     * Appends <code>text</code> to base, adding <code>separator</code> if <code>text</code> is not empty.
     * 
     * @param base the base string to append to
     * @param separator the separator
     * @param text the text to append
     * @return the appended string
     */
    private String append(String base, String separator, String text) {
        String tmp = base;
        if (tmp.length() > 0) {
            tmp += separator;
        }
        tmp += text;
        return tmp;
    }

    /**
     * Asserts the responses vs. the registered messages.
     */
    public void assertResponses() {
        String tooOften = "";
        String unexpected = "";
        String notReceived = "";
        for (Map.Entry<String, MessageState> entry : expectedResponse.entrySet()) {
            String key = entry.getKey();
            MessageState state = entry.getValue();
            if (MessageState.RESPONDED_TOO_OFTEN == state) {
                tooOften = append(tooOften, ", ", key);
            } else if (MessageState.UNEXPECTED == state) {
                unexpected = append(unexpected, ", ", key);
            } else if (MessageState.REGISTERED == state) {
                notReceived = append(notReceived, ", ", key);
            }
        }
        String msg = "";
        if (tooOften.length() > 0) {
            msg += "too often: " + tooOften;
        }
        if (unexpected.length() > 0) {
            msg = append(msg, " and ", "unexpected " + unexpected);
        }
        if (notReceived.length() > 0) {
            msg = append(msg, " and ", "not received " + notReceived);
        }
        if (msg.length() > 0) {
            Assert.fail("Adaptation Message Responses " + msg);
        }
    }
    
}
