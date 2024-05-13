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
package eu.qualimaster.common.signal;

import eu.qualimaster.events.EventManager;

/**
 * Implements an event handler for shutdown signals.
 * 
 * @author Holger Eichelberger
 */
public class ShutdownEventHandler extends AbstractTopologyExecutorSignalHandler<ShutdownSignal> {

    private IShutdownListener listener;

    /**
     * Creates a new shutdown change event handler.
     * 
     * @param listener the listener to send notifications to
     * @param namespace the namespace to filter for
     * @param executor the executor to filter for
     * @see #createAndRegister(IShutdown)
     */
    public ShutdownEventHandler(IShutdownListener listener, String namespace, String executor) {
        super(ShutdownSignal.class, namespace, executor);
        this.listener = listener;
    }
    
    @Override
    protected void handle(ShutdownSignal signal) {
        listener.notifyShutdown(signal);
    }

    /**
     * Creates and registers an algorithm event change handler for the given <code>listener</code>.
     * 
     * @param listener the listener to create the handler for
     * @param namespace the namespace to filter for
     * @param executor the executor to filter for
     * @return the created handler
     */
    public static ShutdownEventHandler createAndRegister(IShutdownListener listener, String namespace, 
        String executor) {
        ShutdownEventHandler result = new ShutdownEventHandler(listener, namespace, executor);
        EventManager.register(result);
        return result;
    }
    
}
