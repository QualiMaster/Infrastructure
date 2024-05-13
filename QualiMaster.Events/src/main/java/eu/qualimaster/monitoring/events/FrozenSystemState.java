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
package eu.qualimaster.monitoring.events;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import eu.qualimaster.common.QMGenerics;
import eu.qualimaster.common.QMInternal;
import eu.qualimaster.common.QMSupport;
import eu.qualimaster.file.Utils;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;

/**
 * Represents a frozen system state for monitoring.
 * 
 * @author Holger Eichelberger
 */
@QMSupport
public class FrozenSystemState implements Serializable {
    
    // set to IVML type names??
    public static final String INFRASTRUCTURE = "Infrastructure"; // no name
    public static final String INFRASTRUCTURE_NAME = "";
    public static final String MACHINE = "Machine";
    public static final String HWNODE = "HwNode";
    public static final String CLOUDENV = "Cloud";
    public static final String ALGORITHM = "Algorithm";
    public static final String DATASOURCE = "DataSource";
    public static final String DATASINK = "DataSink";
    public static final String PIPELINE = "Pipeline";
    public static final String PIPELINE_ELEMENT = "PipelineElement";
    public static final String ACTUAL = "Actual";

    @QMInternal
    public static final String SEPARATOR = ":";
    private static final long serialVersionUID = 4880902220348531183L;

    private Map<String, Double> values;

    /**
     * Creates a frozen systems state instance.
     */
    public FrozenSystemState() {
        values = new HashMap<String, Double>();
    }

    /**
     * Creates a frozen systems state instance from a map of values. This allows rt-VIL
     * to use this class as a wrapper.
     * 
     * @param values the values to be wrapped
     */
    public FrozenSystemState(
        @QMGenerics(types = {String.class, Double.class }) Map<String, Double> values) {
        this.values = null == values ? new HashMap<String, Double>() : values;
    }
    
    /**
     * Loads a system state from a properties file.
     * 
     * @param file the file to load from
     * @throws IOException in case of I/O exceptions
     */
    @QMInternal
    public FrozenSystemState(File file) throws IOException {
        this.values = new HashMap<String, Double>();
        Properties prop = new Properties();
        FileInputStream in = new FileInputStream(file);
        prop.load(in);
        in.close();
        for (Map.Entry<Object, Object> entry : prop.entrySet()) {
            try {
                String key = entry.getKey().toString();
                Double value = Double.valueOf(entry.getValue().toString());
                values.put(key, value);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
    }

    /**
     * Returns the access key.
     * 
     * @param prefix the prefix denoting the type
     * @param name the name of the individual element
     * @param observable the observable
     * @return the observed value or <b>null</b> if nothing was observed (so far)
     */
    public static String obtainKey(String prefix, String name, IObservable observable) {
        return prefix + SEPARATOR + name + SEPARATOR + (null == observable ? null : observable.name());
    }

    /**
     * Returns the name sub-key for a pipeline element.
     * 
     * @param pipeline the pipeline name
     * @param element the element name
     * @return the observed value or <b>null</b> if nothing was observed (so far)
     */
    @QMInternal
    public static String obtainPipelineElementSubkey(String pipeline, String element) {
        return pipeline + SEPARATOR + element;
    }
    
    /**
     * Defines the value for an observation.
     * 
     * @param prefix the prefix denoting the type
     * @param name the name of the individual element
     * @param observable the observable
     * @param value the actual value (may be <b>null</b>)
     */
    @QMInternal
    public void setObservation(String prefix, String name, IObservable observable, Double value) {
        values.put(obtainKey(prefix, name, observable), value);
    }
    
    /**
     * Defines the value of an observation for a pipeline element.
     * 
     * @param prefix the prefix denoting the type
     * @param pipeline the pipeline name
     * @param element the element name
     * @param observable the observable
     * @param value the actual value (may be <b>null</b>)
     */
    @QMInternal
    public void setObservation(String prefix, String pipeline, String element, IObservable observable, Double value) {
        values.put(obtainKey(prefix, obtainPipelineElementSubkey(pipeline, element), observable), value);
    }
    
    /**
     * Sets whether an algorithm is considered to be active in a pipeline at the moment of freezing.
     * 
     * @param pipeline the name of the pipeline
     * @param nodeName the name of the node
     * @param algorithmName the name of the algorithm
     */
    @QMInternal
    public void setActiveAlgorithm(String pipeline, String nodeName, String algorithmName) {
        // difficult: it would be nice to transfer the index of the algorithm from active, but if models diverge
        // decision: transport the name as last pseudo segment and separate during mapping
        // AVAILABLE and value are irrelevant
        values.put(composeActiveAlgorithmKey(pipeline,  nodeName, algorithmName), 1.0);
    }

    /**
     * Composes the active algorithm key for {@code pipeline}/{@code nodeName} and {@code algorithmName} as active
     * algorithm.
     * 
     * @param pipeline the pipeline name
     * @param nodeName the node name
     * @param algorithmName the algorithm name
     * @return the composed key
     */
    private static String composeActiveAlgorithmKey(String pipeline, String nodeName, String algorithmName) {
        return obtainKey(ACTUAL, 
            obtainPipelineElementSubkey(pipeline, obtainPipelineElementSubkey(nodeName, algorithmName)), 
            ResourceUsage.AVAILABLE);
    }
    
    /**
     * Returns whether this frozen state contains {@code algorithmName} as active algorithm for 
     * {@code pipeline}/{@code nodeName}.
     * 
     * @param pipeline the pipeline name
     * @param nodeName the node name
     * @param algorithmName the algorithm name
     * @return {@code true} if the given algorithm is set as active on the given pipeline/node, {@code false} else
     */
    public boolean hasActiveAlgorithm(String pipeline, String nodeName, String algorithmName) {
        return values.containsKey(composeActiveAlgorithmKey(pipeline,  nodeName, algorithmName));
    }

    /**
     * Returns the active algorithm for {@code pipeline}/{@code nodeName}.
     * 
     * @param pipeline the pipeline name
     * @param nodeName the node name
     * @return the active algorithm name, <b>null</b> if none is known
     */
    public String getActiveAlgorithm(String pipeline, String nodeName) {
        // not really efficient, intended for testing...
        String result = null;
        String prefix = ACTUAL + SEPARATOR + obtainPipelineElementSubkey(pipeline, nodeName) + SEPARATOR;
        String postfix = SEPARATOR + ResourceUsage.AVAILABLE;
        for (String k : values.keySet()) {
            if (k.startsWith(prefix) && k.endsWith(postfix)) {
                result = k.substring(prefix.length(), k.length() - postfix.length());
                break;
            }
        }
        return result;
    }
    
    /**
     * Returns an observation.
     * 
     * @param prefix the prefix denoting the type
     * @param name the name of the individual element
     * @param observable the observable
     * @param dflt the default value to return if nothing was observed (so far)
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    @QMInternal
    public Double getObservation(String prefix, String name, IObservable observable, Double dflt) {
        return getObservation(obtainKey(prefix, name, observable), dflt);
    }
    
    /**
     * Returns an observation.
     * 
     * @param key the access key (see {@link #obtainKey(String, String, IObservable)}.
     * @param dflt the default value to return if nothing was observed (so far)
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    @QMInternal
    public Double getObservation(String key, Double dflt) {
        Double result = values.get(key);
        if (null == result) {
            result = dflt;
        }
        return result;
    }
    
    /**
     * Returns an observation for the infrastructure.
     * 
     * @param observable the observable
     * @return the observed value or <b>null</b> if nothing was observed (so far)
     */
    public Double getInfrastructureObservation(IObservable observable) {
        return getInfrastructureObservation(observable, null);
    }
    
    /**
     * Returns an observation for the infrastructure.
     * 
     * @param observable the observable
     * @param dflt the default value to return if nothing was observed (so far)
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    public Double getInfrastructureObservation(IObservable observable, Double dflt) {
        return getObservation(INFRASTRUCTURE, INFRASTRUCTURE_NAME, observable, dflt);
    }
    
    /**
     * Returns an observation for a cloud environment.
     * @param name the name of the cloud environment
     * @param observable the observable
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    public Double getCloudObservation(String name, IObservable observable) {
        return getCloudObservation(name, observable, null);
    }
    
    /**
     * Returns an observation for a cloud environment.
     * @param name the name of the cloud environment
     * @param observable the observable
     * @param dflt the default value to return if nothing was observed (so far)
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    public Double getCloudObservation(String name, IObservable observable, Double dflt) {
        return getObservation(CLOUDENV, name, observable, dflt);
    }
    
    /**
     * Returns an observation for a machine.
     * 
     * @param name the name of the machine
     * @param observable the observable
     * @return the observed value or <b>null</b> if nothing was observed (so far)
     */
    public Double getMachineObservation(String name, IObservable observable) {
        return getMachineObservation(name, observable, null);
    }
    /**
     * Returns an observation for a machine.
     * 
     * @param name the name of the machine
     * @param observable the observable
     * @param dflt the default value to return if nothing was observed (so far)
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    public Double getMachineObservation(String name, IObservable observable, Double dflt) {
        return getObservation(MACHINE, name, observable, dflt);
    }

    /**
     * Returns an observation for a hardware node.
     * 
     * @param name the name of the hardware node
     * @param observable the observable
     * @return the observed value or <b>null</b> if nothing was observed (so far)
     */
    public Double getHwNodeObservation(String name, IObservable observable) {
        return getHwNodeObservation(name, observable, null);
    }
    
    /**
     * Returns an observation for a hardware node.
     * 
     * @param name the name of the hardware node
     * @param observable the observable
     * @param dflt the default value to return if nothing was observed (so far)
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    public Double getHwNodeObservation(String name, IObservable observable, Double dflt) {
        return getObservation(HWNODE, name, observable, dflt);
    }

    /**
     * Returns an observation for a data source used in a pipeline.
     * 
     * @param pipeline the name of the pipeline
     * @param name the name of the data source
     * @param observable the observable
     * @return the observed value or <b>null</b> if nothing was observed (so far)
     */
    public Double getDataSourceObservation(String pipeline, String name, IObservable observable) {
        return getDataSourceObservation(pipeline, name, observable, null);
    }
    
    /**
     * Returns an observation for a data source used in a pipeline.
     * 
     * @param pipeline the name of the pipeline
     * @param name the name of the data source
     * @param observable the observable
     * @param dflt the default value to return if nothing was observed (so far)
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    public Double getDataSourceObservation(String pipeline, String name, IObservable observable, Double dflt) {
        return getObservation(DATASOURCE, obtainPipelineElementSubkey(pipeline, name), observable, dflt);
    }

    /**
     * Returns an observation for a data sink used in a pipeline.
     * 
     * @param pipeline the name of the pipeline
     * @param name the name of the data sink
     * @param observable the observable
     * @return the observed value or <b>null</b> if nothing was observed (so far)
     */
    public Double getDataSinkObservation(String pipeline, String name, IObservable observable) {
        return getDataSinkObservation(pipeline, name, observable, null);
    }
    
    /**
     * Returns an observation for a data sink used in a pipeline.
     * 
     * @param pipeline the name of the pipeline
     * @param name the name of the data sink
     * @param observable the observable
     * @param dflt the default value to return if nothing was observed (so far)
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    public Double getDataSinkObservation(String pipeline, String name, IObservable observable, Double dflt) {
        return getObservation(DATASINK, obtainPipelineElementSubkey(pipeline, name), observable, dflt);
    }

    /**
     * Returns an observation for a pipeline.
     * 
     * @param name the name of the pipeline
     * @param observable the observable
     * @return the observed value or <b>null</b> if nothing was observed (so far)
     */
    public Double getPipelineObservation(String name, IObservable observable) {
        return getPipelineObservation(name, observable, null);
    }
    
    /**
     * Returns an observation for a pipeline.
     * 
     * @param name the name of the pipeline
     * @param observable the observable
     * @param dflt the default value to return if nothing was observed (so far)
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    public Double getPipelineObservation(String name, IObservable observable, Double dflt) {
        return getObservation(PIPELINE, name, observable, dflt);
    }

    /**
     * Returns an observation for a pipeline source.
     * 
     * @param pipeline the name of the pipeline
     * @param element the name of the element
     * @param observable the observable
     * @return the observed value or <b>null</b> if nothing was observed (so far)
     */
    public Double getPipelineElementObservation(String pipeline, String element, IObservable observable) {
        return getPipelineElementObservation(pipeline, element, observable, null);
    }
    
    /**
     * Returns an observation for a pipeline source.
     * 
     * @param pipeline the name of the pipeline
     * @param element the name of the element
     * @param observable the observable
     * @param dflt the default value to return if nothing was observed (so far)
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    public Double getPipelineElementObservation(String pipeline, String element, IObservable observable, Double dflt) {
        return getObservation(PIPELINE_ELEMENT, obtainPipelineElementSubkey(pipeline, element), observable, dflt);
    }
   
    /**
     * Returns an observation for an algorithm used in a pipeline.
     * 
     * @param pipeline the name of the pipeline
     * @param algorithm the name of the algorithm
     * @param observable the observable
     * @return the observed value or <b>null</b> if nothing was observed (so far)
     */
    public Double getAlgorithmObservation(String pipeline, String algorithm, IObservable observable) {
        return getAlgorithmObservation(pipeline, algorithm, observable, null);
    }

    /**
     * Returns an observation for an algorithm used in a pipeline.
     * 
     * @param pipeline the name of the pipeline
     * @param algorithm the name of the algorithm
     * @param observable the observable
     * @param dflt the default value to return if nothing was observed (so far)
     * @return the observed value or <code>dflt</code> if nothing was observed (so far)
     */
    public Double getAlgorithmObservation(String pipeline, String algorithm, IObservable observable, Double dflt) {
        return getObservation(ALGORITHM, obtainPipelineElementSubkey(pipeline, algorithm), observable, dflt);
    }

    /**
     * Returns the mapping.
     * 
     * @return the mapping
     */
    @QMInternal
    public Map<String, Double> getMapping() {
        return values;
    }
    
    /**
     * Converts the frozen system state into properties.
     * 
     * @return the related properties
     */
    @QMInternal
    public Properties toProperties() {
        Properties prop = new Properties();
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();
            if (null != key && null != value) {
                prop.put(key, value.toString());
            }
        }
        return prop;
    }
    
    /**
     * Stores the system state in a file.
     * 
     * @param file the file
     * @throws IOException in case of I/O problems
     */
    @QMInternal
    public void store(File file) throws IOException {
        Properties prop = toProperties();
        FileWriter out = Utils.createFileWriter(file);
        prop.store(out, "");
        out.close();
    }
    
    @QMInternal
    @Override
    public String toString() {
        return values.toString();
    }

}
