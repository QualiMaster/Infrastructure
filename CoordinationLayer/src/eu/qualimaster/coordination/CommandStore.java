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
package eu.qualimaster.coordination;

import eu.qualimaster.events.ResponseStore;
import eu.qualimaster.monitoring.events.IEnactmentCompletedMonitoringEvent;

/**
 * Relates a (flat) set of commands to potential incoming enactment completed messages.
 * 
 * @author Holger Eichelberger
 */
class CommandStore extends ResponseStore<Object, ActiveCommands, IEnactmentCompletedMonitoringEvent> {

    /**
     * Implements the store handler.
     * 
     * @author Holger Eichelberger
     */
    private static class Handler implements IStoreHandler<Object, ActiveCommands, IEnactmentCompletedMonitoringEvent> {

        @Override
        public String getResponseMessageId(IEnactmentCompletedMonitoringEvent request) {
            return request.getCauseMessageId();
        }

        @Override
        public String getRequestMessageId(ActiveCommands response) {
            return response.getCauseMessageId();
        }

        @Override
        public IEnactmentCompletedMonitoringEvent castResponse(Object event) {
            return IEnactmentCompletedMonitoringEvent.class.cast(event);
        }

        @Override
        public ActiveCommands castRequest(Object event) {
            return ActiveCommands.class.cast(event);
        }
        
    }
    
    /**
     * Creates a command store.
     * 
     * @param timeout the timeout for removing mappings automatically
     */
    CommandStore(int timeout) {
        super(timeout, new Handler());
    }
    
    @Override
    protected boolean checkRemove(IEnactmentCompletedMonitoringEvent event, EventRecord<ActiveCommands> record) {
        ActiveCommands cmds = record.getEvent();
        cmds.checkRemove(event);
        return cmds.isEmpty();
    }

    @Override
    protected void removingBytimeout(EventRecord<ActiveCommands> rec) {
        rec.getEvent().notifyTimeout();
    }

}
