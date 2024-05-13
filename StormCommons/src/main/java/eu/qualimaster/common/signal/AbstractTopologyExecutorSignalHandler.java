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

import eu.qualimaster.events.EventHandler;

/**
 * Implements an abstract topology executor signal handler.
 * 
 * @param <E> the specific event type
 * @author Holger Eichelberger
 */
public abstract class AbstractTopologyExecutorSignalHandler <E extends AbstractTopologyExecutorSignal> 
    extends EventHandler<E> {

    private String channel;

    /**
     * Creates a signal handler.
     * 
     * @param eventClass the specific event class
     * @param namespace the namespace to react on
     * @param executor the executor
     */
    protected AbstractTopologyExecutorSignalHandler(Class<E> eventClass, String namespace, String executor) {
        super(eventClass);
        this.channel = namespace + AbstractTopologyExecutorSignal.SEPARATOR + executor;
    }

    @Override
    public boolean handlesChannel(String channel) {
        return this.channel.equals(channel); // channel may be null
    }

}
