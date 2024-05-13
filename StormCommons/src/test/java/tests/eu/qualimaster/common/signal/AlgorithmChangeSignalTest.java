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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.common.signal.AlgorithmChangeSignal;
import eu.qualimaster.common.signal.IAlgorithmChangeListener;
import eu.qualimaster.common.signal.ParameterChange;
import eu.qualimaster.pipeline.AlgorithmChangeParameter;

/**
 * Performs tests for the algorithm change signal.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmChangeSignalTest {

    /**
     * Defines a test listener which records whether a signal was received.
     * 
     * @author Holger Eichelberger
     */
    private static class TestListener implements IAlgorithmChangeListener {

        private boolean receivedSignal;
        private AlgorithmChangeSignal expected;
        
        /**
         * Defines the signal to look for.
         * 
         * @param signal the signal
         */
        public void expect(AlgorithmChangeSignal signal) {
            this.expected = signal;
            receivedSignal = false;
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
        public void notifyAlgorithmChange(AlgorithmChangeSignal signal) {
            Assert.assertNotNull(signal);
            Assert.assertEquals(expected.getNamespace(), signal.getNamespace());
            Assert.assertEquals(expected.getExecutor(), signal.getExecutor());
            Assert.assertEquals(expected.getAlgorithm(), signal.getAlgorithm());
            Assert.assertEquals(expected.getChangeCount(), signal.getChangeCount());
            Assert.assertEquals(expected.getCauseMessageId(), signal.getCauseMessageId());
            for (int c = 0; c < expected.getChangeCount(); c++) {
                ParameterChange expectedChange = expected.getChange(c);
                ParameterChange actualChange = signal.getChange(c);
                Assert.assertEquals(expectedChange.getName(), actualChange.getName());
                Assert.assertEquals(expectedChange.getValue(), actualChange.getValue());
            }
            Assert.assertEquals(expected.getParameters(), signal.getParameters());
            receivedSignal = true;
        }
        
    }
    
    /**
     * Tests the algorithm change signal.
     */
    @Test
    public void testChange() {
        final String topo = "TOPO";
        final String exec = "exec";
        final String alg = "alg";
        final String param1 = "param1";
        final String value1 = "val1";
        final String param2 = "param2";
        final String value2 = "val2";
        
        final int inPort = 1234;
        final int outPort = 4321;
        final int warmup = 50;
        final String host = "localhost";
        final String msgId = "423de707-921e-4b30-8159-5e6b80011b81";
        
        TestListener listener = new TestListener();
        AlgorithmChangeSignal signal = new AlgorithmChangeSignal(topo, exec, alg, msgId);
        listener.expect(signal);
        AlgorithmChangeSignal.notify(signal.createPayload(), topo, exec, listener);
        Assert.assertTrue(listener.receivedSignal());

        signal = new AlgorithmChangeSignal(topo, exec, alg, "");
        listener.expect(signal);
        AlgorithmChangeSignal.notify(signal.createPayload(), topo, exec, listener);
        Assert.assertTrue(listener.receivedSignal());

        List<ParameterChange> changes = new ArrayList<ParameterChange>();
        changes.add(new ParameterChange(param1, value1));
        changes.add(new ParameterChange(param2, value2));
        signal = new AlgorithmChangeSignal(topo, exec, alg, changes, msgId);
        listener.expect(signal);
        AlgorithmChangeSignal.notify(signal.createPayload(), topo, exec, listener);
        Assert.assertTrue(listener.receivedSignal());
        
        // instance reuse is not intended but ok here
        Map<AlgorithmChangeParameter, Serializable> params = new HashMap<AlgorithmChangeParameter, Serializable>();
        params.put(AlgorithmChangeParameter.INPUT_PORT, inPort);
        params.put(AlgorithmChangeParameter.OUTPUT_PORT, outPort);
        signal.setParameters(params);
        signal.setIntParameter(AlgorithmChangeParameter.WARMUP_DELAY, warmup);
        signal.setStringParameter(AlgorithmChangeParameter.COPROCESSOR_HOST, host);
        
        assertEquals(inPort, signal.getIntParameter(AlgorithmChangeParameter.INPUT_PORT, null));
        assertEquals(outPort, signal.getIntParameter(AlgorithmChangeParameter.OUTPUT_PORT, null));
        assertEquals(warmup, signal.getIntParameter(AlgorithmChangeParameter.WARMUP_DELAY, null));
        assertEquals(host, signal.getStringParameter(AlgorithmChangeParameter.COPROCESSOR_HOST, null));
        
        listener.expect(signal);
        AlgorithmChangeSignal.notify(signal.createPayload(), topo, exec, listener);
        Assert.assertTrue(listener.receivedSignal());
    }
    
    /**
     * Asserts that <code>expected</code> and actual are equal.
     * 
     * @param expected the expected value
     * @param actual the actual value
     */
    private static void assertEquals(int expected, Integer actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual.intValue());
    }

    /**
     * Asserts that <code>expected</code> and actual are equal.
     * 
     * @param expected the expected value
     * @param actual the actual value
     */
    private static void assertEquals(String expected, String actual) {
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);
    }

}
