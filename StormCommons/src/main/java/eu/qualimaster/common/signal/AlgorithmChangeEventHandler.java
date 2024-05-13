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
 * Implements an algorithm change event handler.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmChangeEventHandler extends AbstractTopologyExecutorSignalHandler<AlgorithmChangeSignal> {

    private IAlgorithmChangeListener listener;

    /**
     * Creates a new algorithm change event handler.
     * 
     * @param listener the listener to send notifications to
     * @param namespace the namespace to filter for
     * @param executor the executor to filter for
     * @see #createAndRegister(IAlgorithmChangeListener)
     */
    public AlgorithmChangeEventHandler(IAlgorithmChangeListener listener, String namespace, String executor) {
        super(AlgorithmChangeSignal.class, namespace, executor);
        this.listener = listener;
    }

    @Override
    protected void handle(AlgorithmChangeSignal event) {
        listener.notifyAlgorithmChange(event);
    }
    
    /**
     * Creates and registers an algorithm event change handler for the given <code>listener</code>.
     * 
     * @param listener the listener to create the handler for
     * @param namespace the namespace to filter for
     * @param executor the executor to filter for
     * @return the created handler
     */
    public static AlgorithmChangeEventHandler createAndRegister(IAlgorithmChangeListener listener, String namespace, 
        String executor) {
        AlgorithmChangeEventHandler result = new AlgorithmChangeEventHandler(listener, namespace, executor);
        EventManager.register(result);
        return result;
    }

}
