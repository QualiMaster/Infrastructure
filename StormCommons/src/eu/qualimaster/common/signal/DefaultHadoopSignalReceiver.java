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

import eu.qualimaster.common.signal.AlgorithmSignalHandler.IAlgorithmHolder;

/**
 * Implements a default hadoop signal receiver implementing the algorithm change and parameter change
 * signal handling via {@link AlgorithmSignalHandler} and {@link ParameterSignalHandler}.
 * 
 * @param <T> the algorithm type
 * @author Holger Eichelberger
 */
public class DefaultHadoopSignalReceiver<T> implements IHadoopSignalReceiver {

    private AlgorithmSignalHandler<T> algHandler;
    private ParameterSignalHandler paramHandler;

    /**
     * A self-contained signal receiver being its own algorithm holder.
     * 
     * @param <T> the algorithm type
     * @author Holger Eichelberger
     */
    public static class SelfContainedHadoopSignalReceiver<T> extends DefaultHadoopSignalReceiver<T> 
        implements IAlgorithmHolder<T> {

        private static final long serialVersionUID = 8866490508667250705L;
        private T algorithm;

        /**
         * Creates an instance.
         */
        public SelfContainedHadoopSignalReceiver() {
            initialize(this);
        }
        
        @Override
        public T getCurrentAlgorithm() {
            return algorithm;
        }

        @Override
        public void setCurrentAlgorithm(T algorithm) {
            this.algorithm = algorithm;
        }
        
    }
    
    /**
     * Creates a signal receiver for a given algorithm holder. Call {@link #initialize(IAlgorithmHolder)} explicitly.
     * 
     * @see #initialize(IAlgorithmHolder)
     */
    protected DefaultHadoopSignalReceiver() {
    }

    /**
     * Creates a signal receiver for a given algorithm holder.
     * 
     * @param holder the algorithm holder
     * @see #initialize(IAlgorithmHolder)
     */
    public DefaultHadoopSignalReceiver(IAlgorithmHolder<T> holder) {
        initialize(holder);
    }
    
    /**
     * Creates a self-contained default signal receiver.
     * 
     * @param <T> the algorithm type
     * @return the self-contained signal receiver
     */
    public static <T> DefaultHadoopSignalReceiver<T> createSelfContainedReceiver() {
        return new DefaultHadoopSignalReceiver<>(new SelfContainedHadoopSignalReceiver<T>());
    }
    
    /**
     * Initializes the inner structures.
     * 
     * @param holder the algorithm holder
     */
    protected void initialize(IAlgorithmHolder<T> holder) {
        algHandler = new AlgorithmSignalHandler<T>(holder);
        paramHandler = new ParameterSignalHandler();
    }

    /**
     * Returns the algorithm handler.
     * 
     * @return the algorithm handler
     */
    public AlgorithmSignalHandler<T> getAlgorithmHandler() {
        return algHandler;
    }

    /**
     * Returns the parameter handler.
     * 
     * @return the parameter handler
     */
    public ParameterSignalHandler getParameterHandler() {
        return paramHandler;
    }

    @Override
    public void notifyAlgorithmChange(AlgorithmChangeSignal signal) {
        algHandler.notifyAlgorithmChange(signal);
    }

    @Override
    public void notifyParameterChange(ParameterChangeSignal signal) {
        paramHandler.notifyParameterChange(signal);
    }

    @Override
    public void notifyShutdown(ShutdownSignal signal) {
    }

}
