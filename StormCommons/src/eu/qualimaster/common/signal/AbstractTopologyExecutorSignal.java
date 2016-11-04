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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.storm.curator.framework.CuratorFramework;

import eu.qualimaster.common.QMInternal;

/**
 * Defines an abstract topology executor signal, i.e., a signal that is sent to an executor in a topology. 
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public abstract class AbstractTopologyExecutorSignal extends TopologySignal {
    
    public static final String SEPARATOR = "/";
    private static final long serialVersionUID = 7262999433585164281L;
    private String topology;
    private String executor;
    private String causeMsgId;

    /**
     * Creates a topology executor signal.
     * 
     * @param topology the topology namespace
     * @param executor the executor name
     * @param causeMsgId the message id of the causing message (may be <b>null</b> or empty if there is none)
     */
    protected AbstractTopologyExecutorSignal(String topology, String executor, String causeMsgId) {
        this.topology = topology;
        this.executor = executor;
        this.causeMsgId = null == causeMsgId ? "" : causeMsgId;
    }

    /**
     * Returns the namespace of the topology.
     * 
     * @return the namespace of the topology receiving the signal
     */
    public String getNamespace() {
        return topology; // keep as "virtual" namespace for namespace state
    }
    
    /**
     * Returns the name of the topology.
     * 
     * @return the name of the topology
     */
    public String getTopology() {
        return topology;
    }
    
    /**
     * Returns the name of the executor.
     * 
     * @return the name of the executor receiving the signal
     */
    public String getExecutor() {
        return executor;
    }
    
    /**
     * Executes / sends the signal on the pipeline / infrastructure. Please note that this method
     * uses the default settings for sending signals, i.e., based on a valid {@link eu.qualimaster.Configuration}.
     * If you already have a {@link AbstractSignalConnection signal connection instance}, please
     * send the signal vial {@link AbstractSignalConnection#sendSignal(AbstractTopologyExecutorSignal)}
     * or {@link #sendSignal(AbstractSignalConnection)} as this uses the settings of the signal connection
     * as you do not have to care about releasing the underlying signal mechanism (resource consumption).
     * 
     * @throws SignalException in case that the execution / signal sending fails
     */
    public void sendSignal() throws SignalException {
        SignalMechanism.sendSignal(null, this);
    }

    @Override
    public void sendSignal(AbstractSignalConnection connection) throws SignalException {
        if (connection.isConnected()) {
            sendSignal(connection.getClient());
        }
    }
    
    /**
     * Sends this signal via the given curator framework. Keep package visibility to encapsulate
     * curator.
     * @param mechanism the mechanism to send by
     * @throws SignalException in case that the execution / signal sending fails
     */
    void sendSignal(CuratorFramework mechanism) throws SignalException {
        SignalMechanism.sendSignal(mechanism, this);
    }
    
    /**
     * Creates the payload for curator-based sending. [public for testing]
     * 
     * @return the payload
     */
    public abstract byte[] createPayload();

    @Override
    public String toString() {
        return topology + SEPARATOR + executor;
    }

    @Override
    public String getChannel() {
        return topology + SEPARATOR + executor;
    }
    
    /**
     * The causing message id.
     * 
     * @return the causing message id (may be empty if there is none)
     */
    public String getCauseMessageId() {
        return causeMsgId;
    }

    /**
     * Serializes this instance to a byte array prefixed by <code>identifier</code>.
     * 
     * @param identifier the identifier
     * @return the byte array
     */
    protected byte[] defaultSerialize(String identifier) {
        byte[] result;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(identifier.getBytes());
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            oos.close();
            result = baos.toByteArray();
        } catch (IOException e) {
            result = new byte[0];
        }
        return result;
    }
    
    /**
     * De-serializes <code>payload</code> if it starts with <code>identifier</code> to an instance of <code>cls</code>.
     * 
     * @param <T> the instance type
     * @param payload the payload (serialized and prefixed by {@link #defaultSerialize(String)}.
     * @param identifier the identifier <code>payload</code> must be prefixed with, else result will be <b>null</b>
     * @param cls the class <code>payload</code> shall contain an instance of
     * @return <b>null</b> if <code>identifier</code> or type does not match, the instance else
     */
    protected static <T extends AbstractTopologyExecutorSignal> T defaultDeserialize(byte[] payload, String identifier, 
        Class<T> cls) {
        T result = null;
        byte[] id = identifier.getBytes();
        if (payload.length > id.length + 1) {
            boolean match = true;
            int i = 0;
            while (match && i < id.length) {
                match = id[i] == payload[i];
                i++;
            }
            if (match) {
                ByteArrayInputStream bais = new ByteArrayInputStream(payload, i, payload.length - i);
                try {
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    Object o = ois.readObject();
                    if (cls.isInstance(o)) {
                        result = cls.cast(o);
                    }
                    ois.close();
                } catch (IOException e) {
                } catch (ClassNotFoundException e) {
                }
            }
        }
        return result;
    }
    
    /**
     * Returns the logger for this class.
     * 
     * @return the logger
     */
    private static Logger getLogger() {
        return LogManager.getLogger(AbstractTopologyExecutorSignal.class);
    }
    
}
