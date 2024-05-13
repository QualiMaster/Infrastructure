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

import org.apache.hadoop.mapred.JobConf;
import org.apache.log4j.Logger;

import eu.qualimaster.common.signal.AlgorithmSignalHandler.IAlgorithmHolder;
import eu.qualimaster.reflection.ReflectionHelper;

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
     * Defines the interface of a configurer instance.
     * 
     * @param <T> the algorithm/source/sink type
     * @author Holger Eichelberger
     */
    public interface IConfigurer<T> {
        
        /**
         * Returns the name of the signal namespace.
         * 
         * @return the namespace
         */
        public String getNamespace();

        /**
         * Returns the element name for sending signals to.
         * 
         * @return the element name
         */
        public String getElementName();
        
        /**
         * Configures the algorithm and parameter signal handlers.
         * 
         * @param receiver the receiver to configure
         */
        public void configure(DefaultHadoopSignalReceiver<T> receiver);
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

    /**
     * Configures a Hadoop signal receiver.
     * 
     * @param <T> the algorithm/source/sink type
     * @param prefix the configuration key prefix
     * @param job the job configuration
     * @param receiver the receiver to configure
     * @return a configured hadoop signal handler, <b>null</b> if no configurer was specified or creating the 
     *     configurer failed
     */
    public static <T> HadoopSignalHandler configure(String prefix, JobConf job, 
        DefaultHadoopSignalReceiver<T> receiver) {
        HadoopSignalHandler handler = null;
        String key = HadoopSignalHandler.getConfigurerKey(prefix); 
        String clsName = job.get(key);
        if (null != clsName) {
            try {
                Class<?> cls = Class.forName(clsName);
                @SuppressWarnings("unchecked")
                IConfigurer<T> configurer = (IConfigurer<T>) ReflectionHelper.createInstance(cls);
                configurer.configure(receiver);
                handler = new HadoopSignalHandler(configurer.getNamespace(), configurer.getElementName(), 
                    receiver, job);
            } catch (ClassNotFoundException e) {
                Logger.getLogger(DefaultHadoopSignalReceiver.class).error(
                    "Cannot create configurer: " + e.getMessage());
            } catch (IllegalAccessException e) {
                Logger.getLogger(DefaultHadoopSignalReceiver.class).error(
                    "Cannot create configurer: " + e.getMessage());
            } catch (InstantiationException e) {
                Logger.getLogger(DefaultHadoopSignalReceiver.class).error(
                    "Cannot create configurer: " + e.getMessage());
            }
        } else {
            Logger.getLogger(DefaultHadoopSignalReceiver.class).error(
                "Configurer not specified: " + key);            
        }
        return handler; 
    }

}
