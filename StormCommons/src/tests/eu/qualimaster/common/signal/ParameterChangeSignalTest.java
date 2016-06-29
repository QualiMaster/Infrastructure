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
package tests.eu.qualimaster.common.signal;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.common.signal.IParameterChangeListener;
import eu.qualimaster.common.signal.ParameterChange;
import eu.qualimaster.common.signal.ParameterChangeSignal;

/**
 * Performs tests for the parameter change signal.
 * 
 * @author Holger Eichelberger
 */
public class ParameterChangeSignalTest {

    /**
     * Defines a test listener which records whether a signal was received.
     * 
     * @author Holger Eichelberger
     */
    private static class TestListener implements IParameterChangeListener {
        
        private boolean receivedSignal;
        private ParameterChangeSignal expected;

        /**
         * Defines the signal to look for.
         * 
         * @param signal the signal
         */
        public void expect(ParameterChangeSignal signal) {
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
        public void notifyParameterChange(ParameterChangeSignal signal) {
            Assert.assertNotNull(signal);
            Assert.assertEquals(expected.getNamespace(), signal.getNamespace());
            Assert.assertEquals(expected.getExecutor(), signal.getExecutor());
            Assert.assertEquals(expected.getChangeCount(), signal.getChangeCount());
            for (int c = 0; c < expected.getChangeCount(); c++) {
                ParameterChange expectedChange = expected.getChange(c);
                ParameterChange actualChange = signal.getChange(c);
                Assert.assertEquals(expectedChange.getName(), actualChange.getName());
                Assert.assertEquals(expectedChange.getValue(), actualChange.getValue());
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
        final String param1 = "param1";
        final String value1 = "val1";
        final String param2 = "param2";
        final String value2 = "val2";
        final String msgId = "423de707-921e-4b30-8159-5e6b80011b81";
        
        try {
            new ParameterChangeSignal(topology, executor, (List<ParameterChange>) null, "");
            Assert.fail("expected exception missing");
        } catch (IllegalArgumentException e) {
            // fine
        }
        
        try {
            new ParameterChangeSignal(topology, executor, new ArrayList<ParameterChange>(), "");
            Assert.fail("expected exception missing");
        } catch (IllegalArgumentException e) {
            // fine
        }
        
        ParameterChangeSignal signal = new ParameterChangeSignal(topology, executor, param1, value1, msgId);
        TestListener listener = new TestListener();
        listener.expect(signal);
        ParameterChangeSignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());

        signal = new ParameterChangeSignal(topology, executor, param1, value1, "");
        listener.expect(signal);
        ParameterChangeSignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());

        List<ParameterChange> changes = new ArrayList<ParameterChange>();
        changes.add(new ParameterChange(param1, value1));
        changes.add(new ParameterChange(param2, value2));
        signal = new ParameterChangeSignal(topology, executor, changes, msgId);
        listener.expect(signal);
        ParameterChangeSignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());
    }

    /**
     * Tests handling a signal in the QM priority pipeline (based on A. Nydriotis).
     */
    @Test
    public void testPrioPipelineSignal() {
        final String namespace = "PriorityPip";
        final String executor = "PriorityPip_Source0";
        final String parameter = "playerList";
        final String value = "addMarketplayer/1,2,3";
        IParameterChangeListener listener = new IParameterChangeListener() {
            
            @Override
            public void notifyParameterChange(ParameterChangeSignal signal) {
                Assert.assertEquals(namespace, signal.getNamespace());
                Assert.assertEquals(executor, signal.getExecutor());
                Assert.assertEquals(1, signal.getChangeCount());
                Assert.assertEquals(parameter, signal.getChange(0).getName());
                Assert.assertEquals(value, signal.getChange(0).getStringValue());
            }
        };
        ParameterChangeSignal.notify("param:1|playerList:addMarketplayer/1,2,3".getBytes(), 
            namespace, executor, listener);
    }
    
}
