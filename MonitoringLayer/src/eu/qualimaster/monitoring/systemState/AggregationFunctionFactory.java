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
package eu.qualimaster.monitoring.systemState;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.qualimaster.observables.IObservable;

/**
 * A factory for common aggregation functions in order to relate them to observables.
 * 
 * @author Holger Eichelberger
 */
public class AggregationFunctionFactory {

    private static final Map<IObservable, IAggregationFunctionCreator> CREATORS = new HashMap<>();
    
    /**
     * Defines the interface for creating aggregator instances.
     * 
     * @author Holger Eichelberger
     */
    public interface IAggregationFunctionCreator {

        /**
         * Creates an aggregator.
         * 
         * @return the aggregator instance (may be <b>null</b> if the creation fails)
         */
        public IAggregationFunction create();
        
    }
    
    /**
     * A creator which just returns constants.
     * 
     * @author Holger Eichelberger
     */
    public static class ConstantAggregationFunctionCreator implements IAggregationFunctionCreator {
        
        private IAggregationFunction function;

        /**
         * Creates an aggregator for a given "constant" binary function, i.e., the binary function is 
         * treated as if it is "constant" and has no state.
         * 
         * @param function the function
         */
        public ConstantAggregationFunctionCreator(IAggregationFunction function) {
            this.function = function;
        }

        @Override
        public IAggregationFunction create() {
            return function;
        }
        
    }
    
    /**
     * A creator which creates new individual instances. Due to a reflective constructor call, this
     * creator may fail and cause log entries.
     * 
     * @author Holger Eichelberger
     */
    public static class NewInstanceAggregationFunctionCreator implements IAggregationFunctionCreator {

        private Class<? extends IAggregationFunction> cls;

        /**
         * Creates a new creator instance for <code>cls</code>.
         * 
         * @param cls the class to create instances from
         */
        public NewInstanceAggregationFunctionCreator(Class<? extends IAggregationFunction> cls) {
            this.cls = cls;
        }
        
        @Override
        public IAggregationFunction create() {
            IAggregationFunction result;
            try {
                result = cls.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                result = null;
                Logger.getLogger(getClass()).error("no aggregation function created due to " + e.getMessage(), e);
            }
            return result;
        }
        
    }

    /**
     * Registers the creator for a given observable. Overwrites existing registrations
     * 
     * @param observable the observable, ignored if <b>null</b>
     * @param creator the creator, ignored if <b>null</b>
     */
    public static synchronized void register(IObservable observable, IAggregationFunctionCreator creator) {
        if (null != observable && null != creator) {
            CREATORS.put(observable, creator);
        }
    }
    
    /**
     * Unregisters the creator for a given observable.
     * 
     * @param observable the observable, ignored if <b>null</b>
     */
    public static synchronized void unregister(IObservable observable) {
        if (null != observable) {
            CREATORS.remove(observable);
        }
    }
    
    /**
     * Creates an aggregation function for <code>observable</code> if possible and registered.
     * The result may be an object constant or an individual function.
     * 
     * @param observable the observable to return the aggregation function for
     * @return the aggregation function for <code>observable</code> (may be <b>null</b>)
     */
    public static synchronized IAggregationFunction createAggregationFunction(IObservable observable) {
        IAggregationFunction result;
        IAggregationFunctionCreator creator = CREATORS.get(observable);
        if (null != creator) {
            result = creator.create();
        } else {
            result = null;
        }
        return result;
    }

}
