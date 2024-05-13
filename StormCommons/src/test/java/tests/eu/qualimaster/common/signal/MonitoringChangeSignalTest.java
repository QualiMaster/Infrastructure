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
package tests.eu.qualimaster.common.signal;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.common.signal.IMonitoringChangeListener;
import eu.qualimaster.common.signal.MonitoringChangeSignal;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.MonitoringFrequency;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Tests the monitoring change signal.
 * 
 * @author Holger Eichelberger
 */
public class MonitoringChangeSignalTest {

    /**
     * Defines a test listener which records whether a signal was received.
     * 
     * @author Holger Eichelberger
     */
    private static class TestListener implements IMonitoringChangeListener {
        
        private boolean receivedSignal;
        private MonitoringChangeSignal expected;

        /**
         * Defines the signal to look for.
         * 
         * @param signal the signal
         */
        public void expect(MonitoringChangeSignal signal) {
            receivedSignal = false;
            expected = signal;
        }
        
        /**
         * Returns whether the signal was received.
         * 
         * @return <code>true</code> if the signal was received
         */
        public boolean receivedSignal() {
            return receivedSignal;
        }

        @Override
        public void notifyMonitoringChange(MonitoringChangeSignal signal) {
            Assert.assertNotNull(signal);
            Assert.assertEquals(expected.getNamespace(), signal.getNamespace());
            Assert.assertEquals(expected.getExecutor(), signal.getExecutor());
            if (null == expected.getFrequencies()) {
                Assert.assertNull(signal.getFrequencies());
            } else {
                Assert.assertEquals(expected.getFrequencies(), signal.getFrequencies());
            }
            if (null == expected.getObservables()) {
                Assert.assertNull(signal.getObservables());
            } else {
                Assert.assertEquals(expected.getObservables(), signal.getObservables());
            }
            receivedSignal = true;
        }
        
    }
    
    /**
     * Tests the signal as well as turning it into payload and back.
     */
    @Test
    public void testSignal() {
        final String topology = "TOPO";
        final String executor = "exec";
        final String msgId = "423de707-921e-4b30-8159-5e6b80011b92";
        
        MonitoringChangeSignal signal = new MonitoringChangeSignal(topology, executor, null, null, msgId);
        TestListener listener = new TestListener();
        listener.expect(signal);
        MonitoringChangeSignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());

        signal = new MonitoringChangeSignal(topology, executor, MonitoringFrequency.createAllMap(123), null, msgId);
        listener = new TestListener();
        listener.expect(signal);
        MonitoringChangeSignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());

        Map<IObservable, Boolean> observables = new HashMap<IObservable, Boolean>();
        signal = new MonitoringChangeSignal(topology, executor, null, observables, msgId);
        listener = new TestListener();
        listener.expect(signal);
        MonitoringChangeSignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());

        observables.put(TimeBehavior.ENACTMENT_DELAY, Boolean.FALSE);
        signal = new MonitoringChangeSignal(topology, executor, null, observables, msgId);
        listener = new TestListener();
        listener.expect(signal);
        MonitoringChangeSignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());
    }

}
