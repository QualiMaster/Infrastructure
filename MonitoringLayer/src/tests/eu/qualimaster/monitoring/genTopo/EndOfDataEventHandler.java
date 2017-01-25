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
package tests.eu.qualimaster.monitoring.genTopo;

import eu.qualimaster.coordination.commands.PipelineCommand;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.infrastructure.EndOfDataEvent;

/**
 * An end-of-data event handler which directly stops the respective pipeline.
 * 
 * @author Holger Eichelberger
 */
public class EndOfDataEventHandler extends EventHandler<EndOfDataEvent> {

    private boolean received;
    
    /**
     * Creates the event handler.
     */
    public EndOfDataEventHandler() {
        super(EndOfDataEvent.class);
    }
    
    /**
     * Returns whether an end-of-data event was received / responded.
     * 
     * @return <code>true</code> if received, <code>false</code> else
     */
    public boolean wasReceived() {
        return received;
    }

    @Override
    protected void handle(EndOfDataEvent event) {
        new PipelineCommand(event.getPipeline(), PipelineCommand.Status.STOP).execute();
        received = true;
    }

}
