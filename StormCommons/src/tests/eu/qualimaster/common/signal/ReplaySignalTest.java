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

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.common.signal.IReplayListener;
import eu.qualimaster.common.signal.ReplaySignal;

/**
 * Tests the replay signal.
 * 
 * @author Holger Eichelberger
 */
public class ReplaySignalTest {

    /**
     * Defines a test listener which records whether a signal was received.
     * 
     * @author Holger Eichelberger
     */
    private static class TestListener implements IReplayListener {
        
        private boolean receivedSignal;
        private ReplaySignal expected;

        /**
         * Defines the signal to look for.
         * 
         * @param signal the signal
         */
        public void expect(ReplaySignal signal) {
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
        public void notifyReplay(ReplaySignal signal) {
            Assert.assertNotNull(signal);
            Assert.assertEquals(expected.getNamespace(), signal.getNamespace());
            Assert.assertEquals(expected.getExecutor(), signal.getExecutor());
            Assert.assertEquals(expected.getTicket(), signal.getTicket());
            Assert.assertEquals(expected.getStartReplay(), signal.getStartReplay());
            if (expected.getStartReplay()) {
                Assert.assertEquals(expected.getStart(), signal.getStart());
                Assert.assertEquals(expected.getEnd(), signal.getEnd());
                Assert.assertEquals(expected.getSpeed(), signal.getSpeed(), 0.05);
                Assert.assertEquals(expected.getQuery(), signal.getQuery());
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
        final String msgId = "423de707-921e-4b30-8159-5e6b80011b81";
        final int ticket = 1;
        final Date start = new Date();
        final Date end = null;
        final int speed = 10;
        final String query = "aaa";
        
        ReplaySignal signal = new ReplaySignal(topology, executor, true, ticket, msgId);
        signal.setReplayStartInfo(start, end, speed, query);
        TestListener listener = new TestListener();
        listener.expect(signal);
        ReplaySignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());

        signal = new ReplaySignal(topology, executor, false, ticket, msgId);
        listener.expect(signal);
        ReplaySignal.notify(signal.createPayload(), topology, executor, listener);
        Assert.assertTrue(listener.receivedSignal());
    }

}