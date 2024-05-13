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

import java.util.Map;

import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.MonitoringFrequency;
import eu.qualimaster.common.QMInternal;

/**
 * A signal that causes load shedding.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class MonitoringChangeSignal extends AbstractTopologyExecutorSignal {

    private static final long serialVersionUID = -7250274235018545554L;
    private static final String IDENTIFIER = "mon";
    private Map<MonitoringFrequency, Integer> frequencies;
    private Map<IObservable, Boolean> observables;

    /**
     * Creates the signal.
     * 
     * @param topology the topology
     * @param executor the executor name
     * @param frequencies the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for 
     *     completely disabled
     * @param observables the enabled/disabled observables, <b>null</b> for unspecified
     * @param causeMsgId the message id of the causing message (may be <b>null</b> or empty if there is none)
     */
    public MonitoringChangeSignal(String topology, String executor, Map<MonitoringFrequency, Integer> frequencies, 
        Map<IObservable, Boolean> observables, String causeMsgId) {
        super(topology, executor, causeMsgId);
        this.frequencies = frequencies;
        this.observables = observables;
    }
    
    /**
     * Returns the desired monitoring frequencies.
     * 
     * @return the desired monitoring frequencies, <b>null</b> for unspecified, 0 or negative for completely disabled
     */
    public Map<MonitoringFrequency, Integer> getFrequencies() {
        return frequencies;
    }
    
    /**
     * Returns the desired frequency for a certain frequency kind.
     * 
     * @param frequency the frequency kind
     * @return the desired monitoring frequency, <b>null</b> for unspecified, 0 or negative for completely disabled
     */
    public Integer getFrequency(MonitoringFrequency frequency) {
        return null == frequencies || null == frequency ? null : frequencies.get(frequency);
    }
    
    /**
     * Returns the enabled/disabled observables.
     * 
     * @return the enabled/disabled observables, <b>null</b> for unspecified
     */
    public Map<IObservable, Boolean> getObservables() {
        return observables;
    }
    
    /**
     * Returns whether <code>observable</code> shall be enabled/disabled.
     * 
     * @param observable the observable
     * @return <b>null</b> if not specified or <code>observables == <b>null</b></code>, the Boolean flag else
     */
    public Boolean getEnabled(IObservable observable) {
        return null == observables || null == observable ? null : observables.get(observable);
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
    public static boolean notify(byte[] payload, String topology, String executor, IMonitoringChangeListener listener) {
        boolean done = false;
        MonitoringChangeSignal sig = defaultDeserialize(payload, IDENTIFIER, MonitoringChangeSignal.class);
        if (null != sig) {
            listener.notifyMonitoringChange(sig);
            done = true;
        }
        return done;
    }

    @Override
    public String toString() {
        return "MonitoringChangeSignal " + super.toString() + " freq " + getFrequencies() + " obs " + getObservables();
    }

}
