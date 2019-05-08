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

import java.io.Closeable;
import java.util.HashMap;

import org.apache.hadoop.mapred.JobConf;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.Configuration;
import eu.qualimaster.events.EventManager;

/**
 * Implements a basic signal listener for the Hadoop experiments.
 * 
 * @author Holger Eichelberger
 */
public class HadoopSignalHandler implements SignalListener, IShutdownListener, Closeable {

    public static final String PREFIX_SOURCE = "source";
    public static final String PREFIX_SINK = "sink";
    @Deprecated
    public static final String POSTFIX_NAMESPACE = "namespace";
    @Deprecated
    public static final String POSTFIX_ELEMENTNAME = "elementName";
    public static final String POSTFIX_CONFIGURER = "configurer";
    public static final String KEY_SEPARATOR = ".";
    
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
     * @param jConf the job configuration to configure from
     */
    public HadoopSignalHandler(String namespace, String elementName, IHadoopSignalReceiver receiver, JobConf jConf) {
        this(namespace, elementName, receiver, toConf(jConf));
    }

    /**
     * Creates a Hadoop signal listener.
     * 
     * @param namespace the namespace
     * @param elementName the element name
     * @param receiver the receiver to delegate to
     * @param conf the Storm-like configuration to configure from, containing at least {@link Configuration#HOST_EVENT}
     *      and {@link Configuration#HOST_EVENT}
     */
    @SuppressWarnings("rawtypes") // storm style
    public HadoopSignalHandler(String namespace, String elementName, IHadoopSignalReceiver receiver, 
        java.util.Map conf) {
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
     * Composes a Job-conf key.
     * 
     * @param prefix prefix part
     * @param postfix postfix part
     * @return the composed key
     */
    public static String composeKey(String prefix, String postfix) {
        return prefix + KEY_SEPARATOR + postfix;
    }
    
    /**
     * Returns the configurer key.
     * 
     * @param prefix the key prefix
     * @return the configurer key
     */
    public static String getConfigurerKey(String prefix) {
        return HadoopSignalHandler.composeKey(prefix, POSTFIX_CONFIGURER);
    }

    /**
     * Creates a default signal handler and configures itself from prefix.namespace and prefix.elementName from 
     * {@code jConf}.
     * 
     * @param receiver the signal receiver
     * @param prefix the prefix string
     * @param jConf the Hadoop job configuration
     * @return the created signal handler
     */
    @Deprecated
    public static HadoopSignalHandler createDefaultHadoopSignalHandler(IHadoopSignalReceiver receiver, 
        String prefix, JobConf jConf) {
        String namespace = jConf.get(composeKey(prefix, POSTFIX_NAMESPACE));
        String elementName = jConf.get(composeKey(prefix, POSTFIX_ELEMENTNAME));
        return new HadoopSignalHandler(namespace, elementName, receiver, jConf);
    }

    /**
     * Returns the Storm-like configuration from the relevant data of {@code jConf}.
     * 
     * @param jConf the Hadoop configuration
     * @return the Storm-like configuration
     */
    @SuppressWarnings("rawtypes")
    public static java.util.Map toConf(JobConf jConf) {
        int port = 1024;
        try {
            port = Integer.parseInt(jConf.get(Configuration.PORT_EVENT));
        } catch (NumberFormatException e) {
            LogManager.getLogger(HadoopSignalHandler.class).warn("No event port configured, using " + 1024);
        } catch (IllegalArgumentException e) {
            LogManager.getLogger(HadoopSignalHandler.class).warn("No event port configured, using " + 1024);
            
        }
        return toConf(jConf.get(Configuration.HOST_EVENT), port);
    }

    /**
     * Returns the Storm-like configuration from the given data.
     * 
     * @param host the event host
     * @param port the event port
     * @return the Storm-like configuration
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static java.util.Map toConf(String host, int port) {
        java.util.Map conf = new HashMap();
        conf.put(Configuration.HOST_EVENT, host);
        conf.put(Configuration.HOST_EVENT, port);
        return conf;
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
        receiver.notifyShutdown(signal);
        close();
    }
    
    @Override
    public void close() {
        signalConnection.close();
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
