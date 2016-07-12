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

import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.coordination.CoordinationConfiguration;
import eu.qualimaster.coordination.commands.ShutdownCommand;
import eu.qualimaster.dataManagement.events.ShutdownEvent;
import eu.qualimaster.events.EventManager;

/**
 * Performs the configurable/pluggable shutdown of the infrastructure. Basically, the infrastructure
 * just shuts down the running pipelines as well as itself and does not care for the execution 
 * systems. This is due to the fact that shutting down the entire QualiMaster infrastructure depends on 
 * the installation, e.g., whether a process supervisor daemon is used for controlling Storm and the 
 * infrastructure, whether/how to shut down the hardware. Thus, this class provides a way to plug in
 * a certain piece of code that performs the required shutdown procedure. This is basically determined
 * by the infrastructure configuration and executed transparently within this class.
 * 
 * @author Holger Eichelberger
 */
public class Shutdown {

    private static final Map<String, IShutdownProcedure> PROCEDURES = new HashMap<String, IShutdownProcedure>();
    private static final IShutdownProcedure DEFAULT = new IShutdownProcedure() {
        
        @Override
        public void shutdown(ShutdownCommand command, String configuration) {
            EventManager.send(new ShutdownEvent());
        }
        
        @Override
        public String name() {
            return null;
        }
    };
    
    /**
     * Registers a shutdown procedure.
     * 
     * @param procedure the procedure
     */
    public static void register(IShutdownProcedure procedure) {
        if (null != procedure) {
            PROCEDURES.put(procedure.getClass().getName(), procedure);
            String name = procedure.name();
            if (null != name) {
                PROCEDURES.put(name, procedure);    
            }
        }
    }
    
    static {
        register(new ShellCommandShutdown());
    }
    
    /**
     * Performs a shutdown.
     * 
     * @param command the shutdown command
     */
    public static void shutdown(ShutdownCommand command) {
        IShutdownProcedure procedure = null;
        String name = CoordinationConfiguration.getShutdownProcedure();
        String cfg = null;
        if (null != name) {
            procedure = PROCEDURES.get(name);
        }
        if (null == procedure) {
            procedure = DEFAULT;
            cfg = "";
        } else {
            cfg = CoordinationConfiguration.getShutdownProcedureConfiguration();
        }
        procedure.shutdown(command, cfg);
    }
    
}
