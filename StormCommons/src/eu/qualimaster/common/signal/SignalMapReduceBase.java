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
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.log4j.Logger;

import eu.qualimaster.common.signal.AlgorithmSignalHandler.IAlgorithmChangeHandler;
import eu.qualimaster.common.signal.AlgorithmSignalHandler.IAlgorithmHolder;
import eu.qualimaster.common.signal.ParameterSignalHandler.IParameterChangeHandler;

/**
 * Implements a signal-enabled base class for Hadoop.
 * 
 * @param <T> the algorithm type
 * @author Holger Eichelberger
 */
public abstract class SignalMapReduceBase<T> extends MapReduceBase implements IAlgorithmHolder<T> {

    private static final long serialVersionUID = 4150525332007541340L;
    private HadoopSignalHandler signalHandler;
    private DefaultHadoopSignalReceiver<T> receiver = new DefaultHadoopSignalReceiver<T>(this);
    private transient T algorithm;
    private String namespace;
    private String elementName;

    /**
     * Creates an instance.
     * 
     * @param namespace the job namespace
     * @param elementName the mapper/reducer/source/sink name
     * @see #createReceiver()
     */
    protected SignalMapReduceBase(String namespace, String elementName) {
        this.namespace = namespace;
        this.elementName = elementName;
        receiver = createReceiver();
    }

    @Override
    public T getCurrentAlgorithm() {
        return algorithm;
    }

    @Override
    public void setCurrentAlgorithm(T algorithm) {
        this.algorithm = algorithm;
    }
    
    @Override
    public void configure(JobConf job) {
        super.configure(job);
        signalHandler = new HadoopSignalHandler(namespace, elementName, receiver, job);
    }
    
    /**
     * Creates the signal receiver. [extensibility]
     * 
     * @return the signal receiver
     */
    protected DefaultHadoopSignalReceiver<T> createReceiver() {
        return new DefaultHadoopSignalReceiver<T>(this);
    }
    
    /**
     * Adds a plugin algorithm change handler. Overwrites existing handlers.
     * 
     * @param handler the handler
     */
    protected void addHandler(IAlgorithmChangeHandler<T> handler) {
        receiver.getAlgorithmHandler().addHandler(handler);
    }
    
    /**
     * Adds a parameter handler. Existing handlers will be overwritten.
     * 
     * @param handler the handler
     */
    protected void addHandler(IParameterChangeHandler handler) {
        receiver.getParameterHandler().addHandler(handler);
    }
    
    /**
     * Immediately changes the algorithm.
     * 
     * @param name the name of the algorithm
     */
    protected void setAlgorithm(String name) {
        receiver.getAlgorithmHandler().setAlgorithm(name);
    }
    
    /**
     * Executes a deferred algorithm change. Does nothing if there is no such change.
     */
    protected void executeDeferredAlgorithmChange() {
        receiver.getAlgorithmHandler().execute();
    }

    /**
     * Returns the signal listener/handler instance.
     * 
     * @return the signal listener
     */
    protected SignalListener getSignalHandler() {
        return signalHandler;
    }

    /**
     * Returns the logger.
     * 
     * @return the responsible logger
     */
    protected Logger getLogger() {
        return Logger.getLogger(getClass());
    }

}
