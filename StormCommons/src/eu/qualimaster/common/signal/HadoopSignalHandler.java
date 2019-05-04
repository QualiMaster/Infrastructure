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

import java.util.HashMap;

import org.apache.hadoop.mapred.JobConf;
import org.apache.log4j.Logger;

import eu.qualimaster.Configuration;
import eu.qualimaster.events.EventManager;

/**
 * Implements a basic signal listener for the Hadoop experiments.
 * 
 * @author Holger Eichelberger
 */
public class HadoopSignalHandler implements SignalListener, IShutdownListener {
    
    private String namespace;
    private String elementName;
    private IHadoopSignalReceiver receiver;
    private StormSignalConnection signalConnection;
    private transient AlgorithmChangeEventHandler algorithmEventHandler;
    private transient ParameterChangeEventHandler parameterEventHandler;
    private transient ShutdownEventHandler shutdownEventHandler;

    /**
     * Creates a Hadoop signal listener.
     * 
     * @param namespace the namespace
     * @param elementName the element name
     * @param receiver the receiver to delegate to
     */
    public HadoopSignalHandler(String namespace, String elementName, IHadoopSignalReceiver receiver) {
        this(namespace, elementName, receiver, null);
    }
    
    /**
     * Creates a Hadoop signal listener.
     * 
     * @param namespace the namespace
     * @param elementName the element name
     * @param receiver the receiver to delegate to
     * @param jConf the job configuration to configure from
     */
    public HadoopSignalHandler(String namespace, String elementName, IHadoopSignalReceiver receiver, JobConf jConf) {
        @SuppressWarnings("rawtypes")
        java.util.Map conf = new HashMap(); // contents/source unclear
        putInt(conf, Configuration.HOST_EVENT, jConf.get(Configuration.HOST_EVENT));
        putInt(conf, Configuration.PORT_EVENT, jConf.get(Configuration.PORT_EVENT));
        StormSignalConnection.configureEventBus(conf);
        this.namespace = namespace;
        this.elementName = elementName;
        this.receiver = receiver;
        signalConnection = new StormSignalConnection(elementName, this, namespace, conf);
        if (Configuration.getPipelineSignalsQmEvents()) {
            algorithmEventHandler = AlgorithmChangeEventHandler.createAndRegister(receiver, namespace, elementName);
            parameterEventHandler = ParameterChangeEventHandler.createAndRegister(receiver, namespace, elementName);
            shutdownEventHandler = ShutdownEventHandler.createAndRegister(this, namespace, elementName);
        }
    }
    
    /**
     * Puts an int value to {@code conf}. If the conversion fails, nothing happens.
     * 
     * @param conf the configuration to modify
     * @param key the key
     * @param value the value to be turned into an int
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void putInt(java.util.Map conf, String key, Object value) {
        if (value instanceof Integer) {
            conf.put(key, value);
        } else if (value != null) {
            try {
                conf.put(key, Integer.parseInt(value.toString()));
            } catch (NumberFormatException e) {
            }
        }
    }
    
    @Override
    public void onSignal(byte[] data) {
        getLogger().info("onSignal: Listening on the signal! " + namespace + "/" + elementName);
        boolean done = AlgorithmChangeSignal.notify(data, namespace, elementName, receiver);
        if (!done) {
            done = ParameterChangeSignal.notify(data, namespace, elementName, receiver);
        }
        if (!done) {
            done = ShutdownSignal.notify(data, namespace, elementName, this);
        }
    }
    
    @Override
    public final void notifyShutdown(ShutdownSignal signal) {
        signalConnection.close();
        receiver.notifyShutdown(signal);
        if (Configuration.getPipelineSignalsQmEvents()) {
            EventManager.unregister(algorithmEventHandler);
            EventManager.unregister(parameterEventHandler);
            EventManager.unregister(shutdownEventHandler);
        }
    }
    
    /**
     * Sends an algorithm changed event.
     * 
     * @param algorithm the new algorithm enacted
     */
    public void sendAlgorithmChangedEvent(String algorithm) {
        signalConnection.sendAlgorithmChangedEvent(algorithm); // goes anyway over QMEvents
    }
    
    /**
     * Returns the logger.
     * 
     * @return the logger
     */
    protected Logger getLogger() {
        return Logger.getLogger(getClass());
    }

}
