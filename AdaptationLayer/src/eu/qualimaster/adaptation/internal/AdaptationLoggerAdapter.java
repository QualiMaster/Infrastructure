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
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.monitoring.events.FrozenSystemState;

/**
 * An empty implementation of {@link IAdaptationLogger}.
 * 
 * @author Holger Eichelberger
 */
public class AdaptationLoggerAdapter implements IAdaptationLogger {

    @Override
    public void startAdaptation(AdaptationEvent event, FrozenSystemState state) {
    }

    @Override
    public void executedStrategy(String name, boolean successful) {
    }

    @Override
    public void executedTactic(String name, boolean successful) {
    }

    @Override
    public void enacting(CoordinationCommand command) {
    }

    @Override
    public void endAdaptation(boolean successful) {
    }

    @Override
    public void enacted(CoordinationCommand command, CoordinationCommandExecutionEvent event) {
    }

}
