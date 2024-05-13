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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.common.shedding.DefaultLoadSheddingParameter;
import eu.qualimaster.common.shedding.ILoadSheddingParameter;
import eu.qualimaster.common.signal.ILoadSheddingListener;
import eu.qualimaster.common.signal.LoadSheddingSignal;

/**
 * Tests the load shedding signal.
 * 
 * @author Holger Eichelberger
 */
public class LoadSheddingSignalTest {

    /**
     * Defines a test listener which records whether a signal was received.
     * 
     * @author Holger Eichelberger
     */
    private static class TestListener implements ILoadSheddingListener {
        
        private boolean receivedSignal;
        private LoadSheddingSignal expected;

        /**
         * Defines the signal to look for.
         * 
         * @param signal the signal
         */
        public void expect(LoadSheddingSignal signal) {
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
        public void notifyLoadShedding(LoadSheddingSignal signal) {
            Assert.assertNotNull(signal);
            Assert.assertEquals(expected.getNamespace(), signal.getNamespace());
            Assert.assertEquals(expected.getExecutor(), signal.getExecutor());
            Assert.assertEquals(expected.getShedder(), signal.getShedder());
            Assert.assertEquals(expected.getParameterNames(), signal.getParameterNames());
            for (String name : expected.getParameterNames()) {
                Assert.assertEquals(expected.getParameter(name), signal.getParameter(name));
                Assert.assertEquals(expected.getIntParameter(name, -1), signal.getIntParameter(name, -1));
                try {
                    ILoadSheddingParameter param = DefaultLoadSheddingParameter.valueOf(name);
                    Assert.assertEquals(expected.getParameter(param), signal.getParameter(param));
                    Assert.assertEquals(expected.getIntParameter(param, -1), signal.getIntParameter(param, -1));
                } catch (IllegalArgumentException e) {
                    // ignore, no default parameter
                }
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
        final String msgId = "423de707-921e-4b30-8159-5e6b80011b91";
        final String shedder = "aaa";
        
        LoadSheddingSignal signal = new LoadSheddingSignal(topology, executor, shedder, null, msgId);
        TestListener listener = new TestListener();
        listener.expect(signal);
        LoadSheddingSignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());

        signal = new LoadSheddingSignal(topology, executor, shedder, new HashMap<String, Serializable>(), msgId);
        listener = new TestListener();
        listener.expect(signal);
        LoadSheddingSignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());

        Map<String, Serializable> parameter = new HashMap<String, Serializable>();
        parameter.put(DefaultLoadSheddingParameter.NTH_TUPLE.name(), 25);
        signal = new LoadSheddingSignal(topology, executor, shedder, parameter, msgId);
        listener = new TestListener();
        listener.expect(signal);
        LoadSheddingSignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());
    }

}
