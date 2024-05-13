/*
 * Copyright 2009-2019 University of Hildesheim, Software Systems Engineering
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Implements a generic plug-in parameter signal handler in case that code generation shall be simplified or
 * cannot be applied due to pre-defined class hierarchies.
 * 
 * @author Holger Eichelberger
 */
public class ParameterSignalHandler implements Serializable, IParameterChangeListener {

    /**
     * Defines the interface for a plug-in parameter change handler.
     * 
     * @author Holger Eichelberger
     */
    public interface IParameterChangeHandler extends Serializable {

        /**
         * The name of the parameter this handler reacts on.
         * 
         * @return the name of the parameter
         */
        public String getName();
        
        /**
         * Handles the parameter change.
         * 
         * @param change the change information
         */
        public void handle(ParameterChange change);
        
    }
    
    /**
     * Provides an abstract default implementation of {@link IParameterChangeHandler}.
     * 
     * @author Holger Eichelberger
     */
    public abstract static class AbstractParameterChangeHandler implements IParameterChangeHandler {

        private static final long serialVersionUID = 7123152990533146617L;
        private String name;

        /**
         * Creates the handler for a specific parameter name.
         * 
         * @param name the parameter name
         */
        protected AbstractParameterChangeHandler(String name) {
            this.name = name;
        }
        
        
        @Override
        public String getName() {
            return name;
        }
        
    }
    
    private static final long serialVersionUID = 8879442982814250422L;
    private Map<String, IParameterChangeHandler> handlers = new HashMap<String, IParameterChangeHandler>();
    
    /**
     * Adds a parameter handler. Existing handlers will be overwritten.
     * 
     * @param handler the handler
     */
    public void addHandler(IParameterChangeHandler handler) {
        handlers.put(handler.getName(), handler);
    }

    @Override
    public void notifyParameterChange(ParameterChangeSignal signal) {
        for (int s = 0, n = signal.getChangeCount(); s < n; s++) {
            ParameterChange change = signal.getChange(s);
            String name = change.getName();
            IParameterChangeHandler handler = handlers.get(name);
            if (null != handler) {
                handler.handle(change);
            } else {
                getLogger().warn("Unknown parameter change handler for parameter: " + name);
            }
        }
    }

    /**
     * Returns the logger.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return LogManager.getLogger(ParameterSignalHandler.class);
    }

}
