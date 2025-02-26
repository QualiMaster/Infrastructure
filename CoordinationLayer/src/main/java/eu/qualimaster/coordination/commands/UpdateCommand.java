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
package eu.qualimaster.coordination.commands;

import eu.qualimaster.common.QMInternal;

/**
 * Updates the infrastructure model. Shall only be applied as long as the configuration
 * is safely evolved.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class UpdateCommand extends CoordinationCommand {

    private static final long serialVersionUID = 8693670232247980331L;

    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitUpdateCommand(this);
    }

}
