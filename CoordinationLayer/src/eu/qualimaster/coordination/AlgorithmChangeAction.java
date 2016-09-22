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
package eu.qualimaster.coordination;

import java.util.List;

import eu.qualimaster.common.signal.ParameterChange;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;

/**
 * Implements a (deferrable) algorithm change action.
 * 
 * @author Holger Eichelberger
 */
class AlgorithmChangeAction implements IAction {

    private AlgorithmChangeCommand command;
    private List<ParameterChange> parameters;
    private IExecutionTracer tracer;
    
    /**
     * Creates an algorithm change action.
     * 
     * @param command the command
     * @param parameters explicit parameters (may be <b>null</b> than only the cached ones will be used)
     * @param tracer the execution tracer
     */
    AlgorithmChangeAction(AlgorithmChangeCommand command, List<ParameterChange> parameters, IExecutionTracer tracer) {
        this.command = command;
        this.parameters = parameters;
        this.tracer = tracer;
    }
    
    @Override
    public void execute() {
        CoordinationCommandExecutionVisitor vis = new CoordinationCommandExecutionVisitor(tracer);
        vis.handleAlgorithmChangeImpl(command, parameters);
    }

}
