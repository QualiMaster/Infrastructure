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
import java.util.List;

import org.apache.log4j.Logger;

import eu.qualimaster.common.QMInternal;

/**
 * Causes a parameter change on a topology.
 * 
 * @author Holger Eichelberger
 * @author Cui Qin
 */
@QMInternal
public class ParameterChangeSignal extends AbstractTopologyExecutorSignal {
    private static final Logger LOGGER = Logger.getLogger(ParameterChangeSignal.class);
    private static final String IDENTIFIER = "param";
    private static final long serialVersionUID = 3036809569625332550L;
    private List<ParameterChange> changes = new ArrayList<ParameterChange>();

    /**
     * Creates a parameter change signal.
     * 
     * @param topology the topology name
     * @param executor the executor name
     * @param parameter the parameter name
     * @param value the new value
     * @deprecated use {@link #ParameterChangeSignal(String, String, String, Serializable, String)} instead
     */
    @Deprecated
    public ParameterChangeSignal(String topology, String executor, String parameter, Serializable value) {
        this(topology, executor, parameter, value, null);
    }
    
    /**
     * Creates a parameter change signal.
     * 
     * @param topology the topology name
     * @param executor the executor name
     * @param parameter the parameter name
     * @param value the new value
     * @param causeMsgId the message id of the causing message (may be <b>null</b> or empty if there is none)
     */
    public ParameterChangeSignal(String topology, String executor, String parameter, Serializable value, 
        String causeMsgId) {
        super(topology, executor, causeMsgId);
        this.changes.add(new ParameterChange(parameter, value));
    }

    /**
     * Creates a parameter change signal for multiple parameter changes.
     * 
     * @param topology the topology name
     * @param executor the executor name
     * @param changes the parameter changes on the same executor
     * @throws IllegalArgumentException if no changes are given
     * @deprecated use {@link #ParameterChangeSignal(String, String, List, String)} instead
     */
    @Deprecated
    public ParameterChangeSignal(String topology, String executor, List<ParameterChange> changes) {
        this(topology, executor, changes, null);
    }
    
    /**
     * Creates a parameter change signal for multiple parameter changes.
     * 
     * @param topology the topology name
     * @param executor the executor name
     * @param changes the parameter changes on the same executor
     * @param causeMsgId the message id of the causing message (may be <b>null</b> or empty if there is none)
     * @throws IllegalArgumentException if no changes are given
     */
    public ParameterChangeSignal(String topology, String executor, List<ParameterChange> changes, String causeMsgId) {
        super(topology, executor, causeMsgId);
        this.changes = changes;
        if (null == changes || changes.isEmpty()) {
            throw new IllegalArgumentException("changes must be given");
        }
    }
    
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
    
    /**
     * The name of the of the first parameter to change.
     * 
     * @return the name of the parameter to change.
     * @deprecated use the corresponding method of {@link ParameterChange} via 
     *   {@link #getChange(int)} instead
     */
    @Deprecated
    public String getParameter() {
        return getChange(0).getName();
    }
    
    /**
     * Returns the value of the first change to be enacted.
     * 
     * @return the value
     * @deprecated use the corresponding method of {@link ParameterChange} via 
     *   {@link #getChange(int)} instead
     */
    @Deprecated
    public Serializable getValue() {
        return getChange(0).getValue();
    }

    /**
     * Returns the value of the first change as a string.
     * 
     * @return the value as a string
     * @deprecated use the corresponding method of {@link ParameterChange} via 
     *   {@link #getChange(int)} instead
     */
    @Deprecated
    public String getStringValue() {
        return getChange(0).getStringValue();
    }

    /**
     * Returns the value of the first change as integer.
     * 
     * @return the value
     * @throws ValueFormatException in case that the value cannot be converted
     * @deprecated use the corresponding method of {@link ParameterChange} via 
     *   {@link #getChange(int)} instead
     */
    @Deprecated
    public int getIntValue() throws ValueFormatException {
        return getChange(0).getIntValue();
    }

    
    /**
     * Returns the value of the first change as double.
     * 
     * @return the value
     * @throws ValueFormatException in case that the value cannot be converted
     * @deprecated use the corresponding method of {@link ParameterChange} via 
     *   {@link #getChange(int)} instead
     */
    @Deprecated
    public double getDoubleValue() throws ValueFormatException {
        return getChange(0).getDoubleValue();
    }

    /**
     * Returns the value of the first change as boolean.
     * 
     * @return the value
     * @throws ValueFormatException in case that the value cannot be converted
     * @deprecated use the corresponding method of {@link ParameterChange} via 
     *   {@link #getChange(int)} instead
     */
    @Deprecated
    public boolean getBooleanValue() throws ValueFormatException {
        return getChange(0).getBooleanValue();
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
     * @return <code>true</code> if handled, <code>false</code> else
     */
    public static boolean notify(byte[] payload, String topology, String executor, IParameterChangeListener listener) {
        boolean done = false;
        LOGGER.info("Notifying the signal!");
        ParameterChangeSignal sig = defaultDeserialize(payload, IDENTIFIER, ParameterChangeSignal.class);
        if (null != sig) {
            listener.notifyParameterChange(sig);
            done = true;
        }
        return done;
    }
 
    @Override
    public String toString() {
        return "ParamChangeSignal " + super.toString() + " " + changes;
    }
   
}