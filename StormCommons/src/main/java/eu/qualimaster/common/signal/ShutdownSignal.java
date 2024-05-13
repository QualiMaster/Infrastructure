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

import eu.qualimaster.common.QMInternal;

/**
 * Causes pipeline parts to prepare for shutdown.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class ShutdownSignal extends AbstractTopologyExecutorSignal {

    private static final long serialVersionUID = 2386717187649780240L;

    /**
     * Creates a shutdown signal.
     * 
     * @param topology the name of the topology
     * @param executor the name of the executor
     */
    public ShutdownSignal(String topology, String executor) {
        super(topology, executor, null);
    }
    
    @Override
    public byte[] createPayload() {
        return "shutdown".getBytes();
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
    public static boolean notify(byte[] payload, String topology, String executor, IShutdownListener listener) {
        boolean done = false;
        String tmp = new String(payload);
        if ("shutdown".equals(tmp)) {
            listener.notifyShutdown(new ShutdownSignal(topology, executor));
            done = true;
        }
        return done;
    }
    
    @Override
    public String toString() {
        return "ShutdownSignal " + super.toString();
    }

}
