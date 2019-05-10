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

import backtype.storm.stateTransfer.StateTransfer;
import eu.qualimaster.reflection.ReflectionHelper;

/**
 * Implements a generic algorithm signal handler. Allows to defer received signals until {@code {@link #execute()}} 
 * or to execute them immediately. Supports {@link StateTransfer}.
 * 
 * @param <T> the algorithm type
 * @author Holger Eichelberger
 */
public class AlgorithmSignalHandler<T> implements Serializable, IAlgorithmChangeListener {

    /**
     * Provides access to the current algorithm / holder state.
     * 
     * @param <T> the algorithm type
     * @author Holger Eichelberger
     */
    public interface IAlgorithmHolder<T> extends Serializable {
        
        /**
         * Returns the current algorithm.
         * 
         * @return the current
         */
        public T getCurrentAlgorithm();

        /**
         * Sets the new algorithm.
         * 
         * @param algorithm the new algorithm
         */
        public void setCurrentAlgorithm(T algorithm);
        
    }
    
    /**
     * Defines the interface of a plug-in algorithm change handler.
     * 
     * @param <T> the algorithm type
     * @author Holger Eichelberger
     */
    public interface IAlgorithmChangeHandler<T> extends Serializable {

        /**
         * Returns the name of the algorithm to change to.
         * 
         * @return the name
         */
        public String getName();
        
        /**
         * Given that {@code origin} is the actual algorithm, shall we perform the change, i.e., 
         * call {@code #handle()} next?
         * 
         * @param origin the origin algorithm (may be <b>null</b> for no current algorithm)
         * @return {@code true} for perform the algorithm change, {@code false}
         */
        public boolean changeFrom(T origin);
        
        /**
         * Performs the algorithm change.
         * 
         * @return the new algorithm instance
         */
        public T handle();
        
    }
    
    /**
     * Implements a default abstract algorithm change handler. Default behavior: Switches to the new algorithm if the 
     * origin algorithm is either <b>null</b> or of a different immediate type.
     * 
     * @param <T> the algorithm type
     * @author Holger Eichelberger
     */
    public abstract static class AbstractAlgorithmChangeHandler<T> implements IAlgorithmChangeHandler<T> {

        private static final long serialVersionUID = 4977191744044132210L;
        private String name;
        private Class<? extends T> cls;

        /**
         * Creates a handler instance.
         * 
         * @param name the name of the algorithm to react on
         * @param cls the immediate class this algorithm change handler will switch to
         */
        protected AbstractAlgorithmChangeHandler(String name, Class<? extends T> cls) {
            this.name = name;
        }
        
        /**
         * Returns the handled class.
         * 
         * @return the handled class
         */
        protected Class<? extends T> getHandledClass() {
            return cls;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public boolean changeFrom(T origin) {
            return null == origin || !origin.getClass().equals(cls);
        }

    }

    /**
     * Implements a default algorithm change handler. Default behavior: Switches to the new algorithm if the 
     * origin algorithm is either <b>null</b> or of a different immediate type. Creates an instance of the handled
     * class using its no-arg constructor. 
     * 
     * @param <T> the algorithm type
     * @author Holger Eichelberger
     */
    public static class DefaultAlgorithmChangeHandler<T> extends AbstractAlgorithmChangeHandler<T> {
        
        private static final long serialVersionUID = 8031010380634897333L;

        /**
         * Creates a handler instance.
         * 
         * @param name the name of the algorithm to react on
         * @param cls the immediate class this algorithm change handler will switch to
         */
        public DefaultAlgorithmChangeHandler(String name, Class<? extends T> cls) {
            super(name, cls);
        }
        
        @Override
        public T handle() {
            T result = null;
            try {
                result = (T) ReflectionHelper.createInstance(getHandledClass());
            } catch (InstantiationException e) {
                getLogger().warn("Cannot create algorithm instance: " + e.getMessage());
            } catch (IllegalAccessException e) {
                getLogger().warn("Cannot create algorithm instance: " + e.getMessage());
            }
            return result;
        }
        
    }

    private static final long serialVersionUID = 7156238110043291252L;
    private Map<String, IAlgorithmChangeHandler<T>> handlers = new HashMap<String, IAlgorithmChangeHandler<T>>();
    private IAlgorithmHolder<T> holder;
    private boolean immediate;
    private AlgorithmChangeSignal deferred;
    
    /**
     * Creates a generic deferred algorithm signal handler instance.
     * 
     * @param holder the algorithm (state) holder
     */
    public AlgorithmSignalHandler(IAlgorithmHolder<T> holder) {
        this(holder, true);
    }

    /**
     * Creates a generic algorithm signal handler instance.
     * 
     * @param holder the algorithm (state) holder
     * @param immediate shall algorithm change events be executed immediately or stored and deferred until polled 
     *     through {@link #execute()}
     */
    public AlgorithmSignalHandler(IAlgorithmHolder<T> holder, boolean immediate) {
        this.holder = holder;
        this.immediate = immediate;
    }

    /**
     * Adds a plugin algorithm change handler. Overwrites existing handlers.
     * 
     * @param handler the handler
     */
    public void addHandler(IAlgorithmChangeHandler<T> handler) {
        handlers.put(handler.getName(), handler);
    }
    
    @Override
    public void notifyAlgorithmChange(AlgorithmChangeSignal signal) {
        if (immediate) {
            setAlgorithm(signal.getAlgorithm(), signal);
        } else {
            deferred = signal;
        }
    }

    /**
     * Executes a deferred algorithm change. Does nothing if there is no such change.
     */
    protected void execute() {
        if (!immediate && null != deferred) {
            setAlgorithm(deferred.getAlgorithm(), deferred);
        }
    }

    /**
     * Immediately changes the algorithm.
     * 
     * @param name the name of the algorithm
     */
    protected void setAlgorithm(String name) {
        setAlgorithm(name, null);
    }
    
    /**
     * Immediately changes the algorithm.
     * 
     * @param name the name of the algorithm
     * @param signal the actual signal (may be <b>null</b> for immediate changes without parameters)
     */
    private void setAlgorithm(String name, AlgorithmChangeSignal signal) {
        IAlgorithmChangeHandler<T> handler = handlers.get(name);
        if (null != handler) {
            T origin = holder.getCurrentAlgorithm();
            if (handler.changeFrom(origin)) {
                T target = handler.handle();
                //if (null != signal) {
                    //TODO AlgorithmChangeParameter
                //}
                holder.setCurrentAlgorithm(target);
                try {
                    StateTransfer.transferState(target, origin);
                } catch (SecurityException e) {
                    getLogger().warn("Cannot perform state transfer: " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    getLogger().warn("Cannot perform state transfer: " + e.getMessage());
                } catch (IllegalAccessException e) {
                    getLogger().warn("Cannot perform state transfer: " + e.getMessage());
                } catch (InstantiationException e) {
                    getLogger().warn("Cannot perform state transfer: " + e.getMessage());
                }
            }
        } else {
            getLogger().warn("Unknown algorithm change handler for algorithm: " + name);
        }
    }

    /**
     * Returns the logger.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return LogManager.getLogger(AlgorithmSignalHandler.class);
    }

}
