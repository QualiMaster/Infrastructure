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
import eu.qualimaster.adaptation.events.IHandler;
import eu.qualimaster.adaptation.external.ExecutionResponseMessage;
import eu.qualimaster.adaptation.external.RequestMessage;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.events.EventManager;

/**
 * Handles coordination command execution events.
 * 
 * @author Holger Eichelberger
 */
class CoordinationCommandExecutionHandler implements IHandler<CoordinationCommandExecutionEvent> {

    @Override
    public void handle(CoordinationCommandExecutionEvent event) {
        RequestMessage request = AdaptationEventQueue.getRequest(event);
        if (null != request) {
            ExecutionResponseMessage.ResultType result;
            if (event.isSuccessful()) {
                result = ExecutionResponseMessage.ResultType.SUCCESSFUL;
            } else {
                result = ExecutionResponseMessage.ResultType.FAILED;
            }
            AdaptationManager.send(new ExecutionResponseMessage(request, result, event.getMessage()));
        }
        AdaptationEvent evt = AdaptationEventQueue.getEvent(event);
        if (null != evt) {
            AdaptationEventResponse.ResultType result;
            if (event.isSuccessful()) {
                result = AdaptationEventResponse.ResultType.SUCCESSFUL;
            } else {
                result = AdaptationEventResponse.ResultType.FAILED;
            }
            EventManager.send(new AdaptationEventResponse(evt, result, event.getMessage()));
        }
        AdaptationEventQueue.getCommand(event);
    }

}
