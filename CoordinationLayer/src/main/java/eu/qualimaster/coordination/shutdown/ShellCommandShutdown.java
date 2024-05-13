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

import java.io.IOException;

import org.apache.log4j.LogManager;

import eu.qualimaster.coordination.commands.ShutdownCommand;

/**
 * Performs a shutdown via command shell. The configuration string is interpreted as (qualified) shell script 
 * to be executed.
 * 
 * @author Holger Eichelberger
 */
public class ShellCommandShutdown implements IShutdownProcedure {
    
    public static final String NAME = "shell";

    @Override
    public void shutdown(ShutdownCommand command, String configuration) {
        if (null != configuration && configuration.length() > 0) {
            try {
                Runtime.getRuntime().exec(configuration);
            } catch (IOException e) {
                LogManager.getLogger(getClass()).error(
                    "While shutting down the infrastructure: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String name() {
        return NAME;
    }

}
