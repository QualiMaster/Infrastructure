/*
 * Copyright 2009-2019 University of Hildesheim, Software Systems Engineering
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

import org.apache.log4j.LogManager;

import eu.qualimaster.coordination.RepositoryConnector.IPhase;
import eu.qualimaster.coordination.events.ModelUpdatedEvent;
import eu.qualimaster.events.EventHandler;
import net.ssehub.easy.reasoning.core.frontend.ReasonerAdapter;

/**
 * Implements a default model updated event handler adjusting the given 
 * reasoner adapter for the given (model) phase.
 * 
 * @author Holger Eichelberger
 */
public class ModelUpdatedEventHandler extends EventHandler<ModelUpdatedEvent> {

    private ReasonerAdapter adapter;
    private IPhase phase;
    
    /**
     * Creates an event handler instance.
     * 
     * @param phase the phase to operate on
     * @param adapter the adapter to adjust
     */
    public ModelUpdatedEventHandler(IPhase phase, ReasonerAdapter adapter) {
        super(ModelUpdatedEvent.class);
        this.phase = phase;
        this.adapter = adapter;
    }

    @Override
    protected void handle(ModelUpdatedEvent event) {
        switch (event.getType()) {
        case CHANGING:
            RepositoryConnector.unregisterFromReasoning(phase, adapter);
            break;
        case CHANGED:
            RepositoryConnector.registerForReasoning(phase, adapter);
            break;
        default:
            LogManager.getLogger(getClass()).warn("Event type " + event.getType() 
                + " is unknown and remains unhandled.");
            break;
        }
    }

}
