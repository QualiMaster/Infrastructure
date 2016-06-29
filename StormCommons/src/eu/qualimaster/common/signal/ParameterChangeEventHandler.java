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
 * Implements an parameter change event handler.
 * 
 * @author Holger Eichelberger
 */
public class ParameterChangeEventHandler extends AbstractTopologyExecutorSignalHandler<ParameterChangeSignal> {

    private IParameterChangeListener listener;

    /**
     * Creates a new parameter change event handler.
     * 
     * @param listener the listener to send notifications to
     * @param namespace the namespace to react on
     * @param executor the executor to filter for
     */
    protected ParameterChangeEventHandler(IParameterChangeListener listener, String namespace, String executor) {
        super(ParameterChangeSignal.class, namespace, executor);
        this.listener = listener;
    }

    @Override
    protected void handle(ParameterChangeSignal event) {
        listener.notifyParameterChange(event);
    }
    
    /**
     * Creates and registers an parameter event change handler for the given <code>listener</code>.
     * 
     * @param listener the listener to create the handler for
     * @param namespace the namespace to react on
     * @param executor the executor to filter for
     * @return the created handler
     */
    public static ParameterChangeEventHandler createAndRegister(IParameterChangeListener listener, String namespace, 
        String executor) {
        ParameterChangeEventHandler result = new ParameterChangeEventHandler(listener, namespace, executor);
        EventManager.register(result);
        return result;
    }

}
