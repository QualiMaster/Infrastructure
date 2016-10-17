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
package eu.qualimaster.adaptation.internal;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.monitoring.events.FrozenSystemState;

/**
 * Defines the interface for logging the adaptation activities (for reflective adaptation). Passed in instances
 * must not be modified!
 * 
 * @author Holger Eichelberger
 */
public interface IAdaptationLogger {

    /**
     * Starts the adaptation for a given <code>event</code> and <code>state</code>.
     * 
     * @param event the causing adaptation event
     * @param state the actual (frozen) system state
     * @see #endAdaptation()
     */
    public void startAdaptation(AdaptationEvent event, FrozenSystemState state);

    /**
     * Notifies the logger that a strategy has been executed.
     * 
     * @param name the name of the strategy
     * @param successful <code>true</code> for successful execution (if this is a non-nested strategy, 
     *   the changes will be enacted), <code>false</code> else
     */
    public void executedStrategy(String name, boolean successful);

    /**
     * Notifies the logger that a tactic has been executed.
     * 
     * @param name the name of the tactic
     * @param successful <code>true</code> for successful execution (if the calling strategy does not object, 
     *   the changes will be enacted), <code>false</code> else
     */
    public void executedTactic(String name, boolean successful);

    /**
     * Notifies the logger that a coordination command is being executed as part of the enactment.
     * 
     * @param command the coordination comman
     */
    public void enacting(CoordinationCommand command);
    
    /**
     * Ends the adaptation started with {@link #startAdaptation(AdaptationEvent, FrozenSystemState)}.
     * 
     * @param successful <code>true</code> for successful execution
     */
    public void endAdaptation(boolean successful);

}
