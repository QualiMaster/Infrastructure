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
package tests.eu.qualimaster.monitoring;

import org.junit.Test;
import org.junit.Assert;

import eu.qualimaster.monitoring.observations.DelegatingStatisticsObservation;
import eu.qualimaster.monitoring.observations.DelegatingTimeFramedObservation;
import eu.qualimaster.monitoring.observations.SingleObservation;

/**
 * Tests individual observations.
 * 
 * @author Holger Eichelberger
 */
public class ObservationTests {
    
    /**
     * Test the combination of time-framed absolute observation and statistics observation.
     */
    @Test
    public void timeFramedAbsoluteStatistics() {
        DelegatingStatisticsObservation obs = new DelegatingStatisticsObservation(
            new DelegatingTimeFramedObservation(new SingleObservation(), 1000));
        
        Assert.assertEquals(0, obs.getValue(), 1);
        Assert.assertEquals(0, obs.getAverageValue(), 1);
        Assert.assertEquals(0, obs.getMinimumValue(), 1);
        Assert.assertEquals(0, obs.getMaximumValue(), 1);
        
        obs.setValue(500, null); // 500 over 1 s -> 500
        sleep(1000);
        obs.setValue(500, null); // 500 over 1 s -> 500

        Assert.assertEquals(500, obs.getValue(), 5); // time diffs
        obs.setValue(1000, null); // reset to 1000 over 1 s -> 1000
        sleep(1000);
        Assert.assertEquals(1000, obs.getValue(), 5); // time diffs
        
        Assert.assertEquals((500 + 1000) / 3, obs.getAverageValue(), 1);
        Assert.assertEquals(0, obs.getMinimumValue(), 1);
        Assert.assertEquals(1000, obs.getMaximumValue(), 5);
    }

    /**
     * Sleeps for <code>ms</code>.
     * 
     * @param ms the time to sleep
     */
    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException t) {
        }
    }
    
}
