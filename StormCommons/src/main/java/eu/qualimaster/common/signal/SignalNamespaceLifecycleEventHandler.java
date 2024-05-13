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
package eu.qualimaster.common.signal;

import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent.Status;
import eu.qualimaster.infrastructure.PipelineOptions;

/**
 * A lifecycle handler for enabling {@link SignalManager} pipeline namespaces on worker side. Don't use in 
 * infrastructure components.
 * 
 * @author Holger Eichelberger
 */
class SignalNamespaceLifecycleEventHandler extends EventHandler<PipelineLifecycleEvent> {

    private static SignalNamespaceLifecycleEventHandler instance;
    
    /**
     * Creates a signal namespace event handler.
     */
    private SignalNamespaceLifecycleEventHandler() {
        super(PipelineLifecycleEvent.class);
    }
    
    /**
     * Registers the handler.
     */
    static synchronized void registerHandler() {
        if (null == instance) {
            instance = new SignalNamespaceLifecycleEventHandler();
            EventManager.register(instance);
        }
    }
    
    /**
     * Unregistes the handler.
     */
    static synchronized void unregisterHandler() {
        if (null != instance) {
            EventManager.unregister(instance);
            instance = null;
        }
    }

    @Override
    protected void handle(PipelineLifecycleEvent event) {
        // see coordination layer lifecycle handler for state
        if (Status.CREATED == event.getStatus()) {
            String namespace = event.getPipeline();
            SignalMechanism.initEnabledSignalNamespaceState(namespace);
            PipelineOptions opts = event.getOptions();
            if (null != opts && opts.isSubPipeline()) {
                namespace = opts.getMainPipeline();
            }
            SignalMechanism.initEnabledSignalNamespaceState(namespace);
        }
    }

}
