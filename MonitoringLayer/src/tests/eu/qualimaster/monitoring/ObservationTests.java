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

import eu.qualimaster.coordination.CoordinationManager;
import eu.qualimaster.coordination.INameMapping;
import eu.qualimaster.coordination.IdentityMapping;
import eu.qualimaster.monitoring.observations.DelegatingStatisticsObservation;
import eu.qualimaster.monitoring.observations.DelegatingTimeFramedObservation;
import eu.qualimaster.monitoring.observations.SingleObservation;
import eu.qualimaster.monitoring.systemState.PipelineNodeSystemPart;
import eu.qualimaster.monitoring.systemState.PipelineSystemPart;
import eu.qualimaster.monitoring.systemState.StateUtils;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

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
        INameMapping mapping = new IdentityMapping("pip");
        CoordinationManager.registerTestMapping(mapping); // avoid full startup
        SingleObservation base = new SingleObservation();
        DelegatingStatisticsObservation obs = new DelegatingStatisticsObservation(
            new DelegatingTimeFramedObservation(base, 1000));
        
        Assert.assertEquals(0, obs.getValue(), 1);
        Assert.assertEquals(0, obs.getAverageValue(), 1);
        Assert.assertEquals(0, obs.getMinimumValue(), 1);
        Assert.assertEquals(0, obs.getMaximumValue(), 1);
        
        long start = System.currentTimeMillis();
        base.setValue(500, null); // 500 over 1 s -> 500
        double v1 = obs.getValue();
        sleep(1000);
        base.setValue(500, null); // 500 over 1 s -> 500

        double v2 = obs.getValue();
        Assert.assertEquals(500, v2, 25); // time diffs
        base.setValue(1000, null); // reset to 1000 over 1 s -> 1000
        sleep(1000);
        long diff = System.currentTimeMillis() - start;
        double exp = 1000 / (diff / 1000);
        double v3 = obs.getValue();
        Assert.assertEquals(exp, v3, 5); // time diffs
        Assert.assertEquals((v1 + v2 + v3) / 4, obs.getAverageValue(), 1); // every read counts!
        Assert.assertEquals(0, obs.getMinimumValue(), 1);
        Assert.assertEquals(Math.max(Math.max(v1, v2), v3), obs.getMaximumValue(), 5);
        CoordinationManager.unregisterNameMapping(mapping);
    }
    
    /**
     * Tests the time-framed aggregation.
     */
    @Test
    public void testTimeFramedAggregation() {
        INameMapping mapping = new IdentityMapping("pip");
        CoordinationManager.registerTestMapping(mapping); // avoid full startup
        SystemState state = new SystemState();
        PipelineSystemPart pip = state.obtainPipeline("pip");
        PipelineNodeSystemPart node = pip.obtainPipelineNode("node");
        
        double[] data = {2, 10, 10, 10, 10, 10, 10, 55792, 126885, 700336, 912036, 1248618, 1498365, 1959288, 
            2074404, 2151250};
        
        long time = System.currentTimeMillis();
        for (int i = 0; i < data.length; i++) {
            StateUtils.setValue(node, TimeBehavior.THROUGHPUT_ITEMS, data [i], null);
            System.out.print(".");
            if (i + 1 < data.length) { // don't sleep for the last one!
                sleep(1000); // needed for the time frame although imprecise
            }
        }
        System.out.println(".");
        long timeDiff = System.currentTimeMillis() - time;
        double items = node.getObservedValue(Scalability.ITEMS);
        double expectedItems = (data[data.length - 1] / timeDiff) * 1000;
        double diff = Math.abs(items - expectedItems);
        Assert.assertTrue("expected " + expectedItems + " actual " + items + " diff " + diff + " not < 100 ", 
            diff < 100);
        CoordinationManager.unregisterNameMapping(mapping);
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
