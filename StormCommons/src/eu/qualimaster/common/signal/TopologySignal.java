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

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractEvent;

/**
 * Defines the root of the topology signals, i.e., QM events that may be sent through the event
 * bus in order to replace the Curator framework (or just emulate it by providing a common interface
 * to the signal information used in QM).
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public abstract class TopologySignal extends AbstractEvent {

    private static final long serialVersionUID = -6179915335623274137L;
    
    /**
     * Sends this signal via the given connection.
     * 
     * @param connection the signal connection
     * @throws SignalException in case that the execution / signal sending fails
     */
    public abstract void sendSignal(AbstractSignalConnection connection) throws SignalException;
}
