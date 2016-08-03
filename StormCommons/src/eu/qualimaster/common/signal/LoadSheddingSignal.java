/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
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
import java.util.Set;

import eu.qualimaster.common.shedding.ILoadSheddingParameter;
import eu.qualimaster.common.QMInternal;
import eu.qualimaster.common.shedding.ILoadShedderConfigurer;

/**
 * A signal that causes load shedding.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class LoadSheddingSignal extends AbstractTopologyExecutorSignal implements ILoadShedderConfigurer {

    private static final String IDENTIFIER = "shed";
    private static final long serialVersionUID = -49190263656392482L;
    private String shedder;
    private Map<String, Serializable> parameter = new HashMap<String, Serializable>();

    /**
     * Creates the signal.
     * 
     * @param topology the topology
     * @param executor the executor name
     * @param shedder the (identification or class) name of the shedder (disable shedding if <b>null</b> or empty)
     * @param parameter the shedder parameters
     * @param causeMsgId the message id of the causing message (may be <b>null</b> or empty if there is none)
     */
    public LoadSheddingSignal(String topology, String executor, String shedder, 
        Map<String, Serializable> parameter, String causeMsgId) {
        super(topology, executor, causeMsgId);
        this.shedder = shedder;
        if (null != parameter) {
            this.parameter.putAll(parameter);
        }
    }
    
    /**
     * Returns the identification or class name of the shedder.
     * 
     * @return the name of the shedder, disable shedding if <b>null</b> or empty
     */
    public String getShedder() {
        return shedder;
    }

    @Override
    public int getIntParameter(String name, int dflt) {
        int result = dflt;
        Serializable tmp = parameter.get(name);
        if (tmp instanceof Integer) {
            result = (Integer) tmp;
        } else if (null != tmp) {
            try {
                result = Integer.parseInt(tmp.toString().trim());
            } catch (NumberFormatException e) {
            }
        }
        return result;
    }

    @Override
    public int getIntParameter(ILoadSheddingParameter param, int dflt) {
        return getIntParameter(param.name(), dflt);
    }

    @Override
    public Serializable getParameter(ILoadSheddingParameter param) {
        return getParameter(param.name());
    }

    @Override
    public Serializable getParameter(String name) {
        return parameter.get(name);
    }
    
    /**
     * Returns the parameter names defined for this signal.
     * 
     * @return the parameter names
     */
    public Set<String> getParameterNames() {
        return parameter.keySet();
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
    public static boolean notify(byte[] payload, String topology, String executor, ILoadSheddingListener listener) {
        boolean done = false;
        LoadSheddingSignal sig = defaultDeserialize(payload, IDENTIFIER, LoadSheddingSignal.class);
        if (null != sig) {
            listener.notifyLoadShedding(sig);
            done = true;
        }
        return done;
    }

    @Override
    public String toString() {
        return "LoadShedderSignal " + super.toString() + " " + getShedder() + " " + parameter;
    }

}
