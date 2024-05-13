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

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.common.signal.IShutdownListener;
import eu.qualimaster.common.signal.ShutdownSignal;

/**
 * Performs tests for the shutdown signal.
 * 
 * @author Holger Eichelberger
 */
public class ShutdownSignalTest {

    /**
     * Defines a test listener which records whether a signal was received.
     * 
     * @author Holger Eichelberger
     */
    private static class TestListener implements IShutdownListener {

        private boolean receivedSignal;
        private ShutdownSignal expected;
        
        /**
         * Defines the signal to look for.
         * 
         * @param signal the signal
         */
        public void expect(ShutdownSignal signal) {
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
        public void notifyShutdown(ShutdownSignal signal) {
            Assert.assertNotNull(signal);
            Assert.assertEquals(expected.getNamespace(), signal.getNamespace());
            Assert.assertEquals(expected.getExecutor(), signal.getExecutor());
            receivedSignal = true;
        }
        
    }
    
    /**
     * Tests the algorithm change signal.
     */
    @Test
    public void testShutdown() {
        final String topo = "TOPO";
        final String exec = "exec";
        
        TestListener listener = new TestListener();
        ShutdownSignal signal = new ShutdownSignal(topo, exec);
        listener.expect(signal);
        ShutdownSignal.notify(signal.createPayload(), topo, exec, listener);
        Assert.assertTrue(listener.receivedSignal());
    }
    
}
