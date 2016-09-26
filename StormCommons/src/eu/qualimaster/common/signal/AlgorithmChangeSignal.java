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
package eu.qualimaster.common.signal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.pipeline.AlgorithmChangeParameter;

/**
 * Causes an algorithm change on a pipeline.
 * 
 * @author Holger Eichelberger
 * @author Cui Qin
 */
@QMInternal
public class AlgorithmChangeSignal extends AbstractTopologyExecutorSignal {

    private static final String IDENTIFIER = "alg";
    private static final long serialVersionUID = 3573667327144368263L;
    private String algorithm;
    private Map<String, Serializable> parameters = new HashMap<String, Serializable>();
    private List<ParameterChange> changes;

    /**
     * Creates an algorithm change signal.
     * 
     * @param topology the name of the topology
     * @param executor the name of the executor
     * @param algorithm the name of the algorithm to change
     * @deprecated use {@link #AlgorithmChangeSignal(String, String, String, String)} instead
     */
    @Deprecated
    public AlgorithmChangeSignal(String topology, String executor, String algorithm) {
        this(topology, executor, algorithm, (String) null);
    }

    /**
     * Creates an algorithm change signal.
     * 
     * @param topology the name of the topology
     * @param executor the name of the executor
     * @param algorithm the name of the algorithm to change
     * @param causeMsgId the message id of the causing message (may be <b>null</b> or empty if there is none)
     */
    public AlgorithmChangeSignal(String topology, String executor, String algorithm, String causeMsgId) {
        this(topology, executor, algorithm, new ArrayList<ParameterChange>(), causeMsgId);
    }

    /**
     * Creates an algorithm change signal.
     * 
     * @param topology the name of the topology
     * @param executor the name of the executor
     * @param algorithm the name of the algorithm to change
     * @param changes optional parameter changes
     * @deprecated use {@link #AlgorithmChangeSignal(String, String, String, List, String)} instead
     */
    @Deprecated
    public AlgorithmChangeSignal(String topology, String executor, String algorithm, List<ParameterChange> changes) {
        this(topology, executor, algorithm, changes, null);
    }
    
    /**
     * Creates an algorithm change signal.
     * 
     * @param topology the name of the topology
     * @param executor the name of the executor
     * @param algorithm the name of the algorithm to change
     * @param changes optional parameter changes
     * @param causeMsgId the message id of the causing message (may be <b>null</b> or empty if there is none)
     */
    public AlgorithmChangeSignal(String topology, String executor, String algorithm, List<ParameterChange> changes, 
        String causeMsgId) {
        super(topology, executor, causeMsgId);
        this.algorithm = algorithm;

        // remove after cleanup of ParameterChangeSignal
        this.changes = changes;
        if (null == changes) {
            throw new IllegalArgumentException("changes must be given");
        }
    }
    
    /**
     * Sets a set of parameters at once.
     * 
     * @param params the parameters to set
     */
    public void setParameters(Map<AlgorithmChangeParameter, Serializable> params) {
        for (Map.Entry<AlgorithmChangeParameter, Serializable> entry : params.entrySet()) {
            parameters.put(entry.getKey().name(), entry.getValue());
        }
    }
    
    /**
     * Sets an integer parameter.
     * 
     * @param param the parameter identifier
     * @param value the value
     */
    public void setIntParameter(AlgorithmChangeParameter param, int value) {
        AlgorithmChangeParameter.setIntParameter(parameters, param, value);
    }
    
    /**
     * Returns an integer parameter.
     * 
     * @param param the parameter identifier
     * @param dflt the default value in case that the parameter is not specified (may be <b>null</b>)
     * @return the value of <code>param</code>, <code>dflt</code> if not specified
     */
    public Integer getIntParameter(AlgorithmChangeParameter param, Integer dflt) {
        return AlgorithmChangeParameter.getIntParameter(parameters, param, dflt);
    }

    /**
     * Sets a String parameter.
     * 
     * @param param the parameter identifier
     * @param value the value
     * @throws IllegalArgumentException in case that parameter does not accet a String value
     */
    public void setStringParameter(AlgorithmChangeParameter param, String value) {
        AlgorithmChangeParameter.setStringParameter(parameters, param, value);
    }
    
    /**
     * Returns a String parameter.
     * 
     * @param param the parameter identifier
     * @param dflt the default value in case that the parameter is not specified (may be <b>null</b>)
     * @return the value of <code>param</code>, <code>dflt</code> if not specified
     */
    public String getStringParameter(AlgorithmChangeParameter param, String dflt) {
        return AlgorithmChangeParameter.getStringParameter(parameters, param, dflt);
    }
    
    /**
     * Returns all defined parameters. (for testing)
     * 
     * @return all parameters
     */
    public Map<AlgorithmChangeParameter, Serializable> getParameters() {
        return AlgorithmChangeParameter.convert(parameters);
    }
    
    // >>remove after cleanup of ParameterChangeSignal
    
    /**
     * Returns the number of parameter changes.
     * 
     * @return the number of parameter changes
     */
    public int getChangeCount() {
        return changes.size();
    }
    
    /**
     * Returns the specified parameter change.
     * 
     * @param index the 0-based index of the change
     * @return the specified change
     * @throws IndexOutOfBoundsException if <code>index &lt; 0 || index &gt;={@link #getChangeCount()}</code>
     */
    public ParameterChange getChange(int index) {
        return changes.get(index);
    }
    
    // <<remove after cleanup of ParameterChangeSignal
    
    /**
     * The algorithm name to be enacted.
     * 
     * @return the name of the algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    @Override
    public byte[] createPayload() {
        return defaultSerialize(IDENTIFIER);
    }
    
    /**
     * Interprets the payload and sends it to the given listener if appropriate. [public for testing]
     * 
     * @param payload the signal payload
     * @param topology the name of the target topology (irrelevant)
     * @param executor the name of the target executor (irrelevant)
     * @param listener the listener
     * @return <code>true</code> if done, <code>false</code> else
     */
    public static boolean notify(byte[] payload, String topology, String executor, IAlgorithmChangeListener listener) {
        boolean done = false;
        AlgorithmChangeSignal sig = defaultDeserialize(payload, IDENTIFIER, AlgorithmChangeSignal.class);
        if (null != sig) {
            listener.notifyAlgorithmChange(sig);
            done = true;
        }
        return done;
    }
    
    @Override
    public String toString() {
        return "AlgChangeSignal " + super.toString() + " " + getAlgorithm();
    }

}
