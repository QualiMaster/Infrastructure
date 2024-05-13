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
package eu.qualimaster.coordination.shutdown;

import eu.qualimaster.coordination.commands.ShutdownCommand;

/**
 * Implements a shutdown procedure.
 * 
 * @author Holger Eichelberger
 */
public interface IShutdownProcedure {

    /**
     * Performs the shutdown.
     * 
     * @param command the shutdown command
     * @param configuration parameters etc. (may be <b>null</b> or empty)
     */
    public void shutdown(ShutdownCommand command, String configuration);
    
    /**
     * The name of the procedure to be used in the configuration.
     * 
     * @return the name
     */
    public String name();
    
}
