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
package tests.eu.qualimaster;

import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Tests {@link FrozenSystemState}.
 * 
 * @author Holger Eichelberger
 */
public class FrozenSystemStateTest {

    /**
     * Tests {@link FrozenSystemState}.
     */
    @Test
    public void testFrozenSystemState() {
        FrozenSystemState state = new FrozenSystemState();
        Assert.assertNull(state.getAlgorithmObservation(null, null, (IObservable) null));
        Assert.assertNull(state.getAlgorithmObservation("pip", "elt", TimeBehavior.LATENCY));
        Assert.assertNull(state.getDataSinkObservation("pip", "elt", TimeBehavior.LATENCY));
        Assert.assertNull(state.getDataSourceObservation("pip", "elt", TimeBehavior.LATENCY));
        Assert.assertNull(state.getHwNodeObservation("elt", TimeBehavior.LATENCY));
        Assert.assertNull(state.getInfrastructureObservation(TimeBehavior.LATENCY));
        Assert.assertNull(state.getPipelineObservation("pip", TimeBehavior.LATENCY));
        Assert.assertNull(state.getPipelineElementObservation("pip", "elt", TimeBehavior.LATENCY));
        // undefined + defaults
        assertEquals(1.1, state.getAlgorithmObservation("pip", "elt", TimeBehavior.LATENCY, 1.1));
        assertEquals(1.1, state.getDataSinkObservation("pip", "elt", TimeBehavior.LATENCY, 1.1));
        assertEquals(1.1, state.getDataSourceObservation("pip", "elt", TimeBehavior.LATENCY, 1.1));
        assertEquals(1.1, state.getHwNodeObservation("elt", TimeBehavior.LATENCY, 1.1));
        assertEquals(1.1, state.getInfrastructureObservation(TimeBehavior.LATENCY, 1.1));
        assertEquals(1.1, state.getPipelineObservation("pip", TimeBehavior.LATENCY, 1.1));
        assertEquals(1.1, state.getPipelineElementObservation("pip", "elt", TimeBehavior.LATENCY, 1.1));
        
        state.setObservation(FrozenSystemState.ALGORITHM, "pip", "elt", TimeBehavior.LATENCY, 4.0);
        assertEquals(4.0, state.getAlgorithmObservation("pip", "elt", TimeBehavior.LATENCY));
        assertEquals(4.0, state.getAlgorithmObservation("pip", "elt", TimeBehavior.LATENCY, 1.1));
        state.setObservation(FrozenSystemState.DATASINK, "pip", "elt", TimeBehavior.ENACTMENT_DELAY, 5.0);
        assertEquals(5.0, state.getDataSinkObservation("pip", "elt", TimeBehavior.ENACTMENT_DELAY));
        assertEquals(5.0, state.getDataSinkObservation("pip", "elt", TimeBehavior.ENACTMENT_DELAY, 1.1));
        state.setObservation(FrozenSystemState.DATASOURCE, "pip", "elt", Scalability.VOLATILITY, 6.0);
        assertEquals(6.0, state.getDataSourceObservation("pip", "elt", Scalability.VOLATILITY));
        assertEquals(6.0, state.getDataSourceObservation("pip", "elt", Scalability.VOLATILITY, 1.1));
        state.setObservation(FrozenSystemState.HWNODE, "elt", TimeBehavior.THROUGHPUT_ITEMS, 7.0);
        assertEquals(7.0, state.getHwNodeObservation("elt", TimeBehavior.THROUGHPUT_ITEMS));
        assertEquals(7.0, state.getHwNodeObservation("elt", TimeBehavior.THROUGHPUT_ITEMS, 1.1));
        state.setObservation(FrozenSystemState.INFRASTRUCTURE, "", TimeBehavior.THROUGHPUT_VOLUME, 8.0);
        assertEquals(8.0, state.getInfrastructureObservation(TimeBehavior.THROUGHPUT_VOLUME));
        assertEquals(8.0, state.getInfrastructureObservation(TimeBehavior.THROUGHPUT_VOLUME, 1.1));
        state.setObservation(FrozenSystemState.PIPELINE, "pip", Scalability.VARIETY, 10.0);
        assertEquals(10.0, state.getPipelineObservation("pip", Scalability.VARIETY));
        assertEquals(10.0, state.getPipelineObservation("pip", Scalability.VARIETY, 1.1));
        state.setObservation(FrozenSystemState.PIPELINE_ELEMENT, "pip", "elt", Scalability.VELOCITY, 9.0);
        assertEquals(9.0, state.getPipelineElementObservation("pip", "elt", Scalability.VELOCITY));
        assertEquals(9.0, state.getPipelineElementObservation("pip", "elt", Scalability.VELOCITY, 1.1));
        
        Map<String, Double> mapping = state.getMapping();
        Assert.assertEquals(7, mapping.size());
        assertEquals(4.0, mapping.get(
            FrozenSystemState.obtainKey(FrozenSystemState.ALGORITHM, 
                FrozenSystemState.obtainPipelineElementSubkey("pip", "elt"), TimeBehavior.LATENCY)));
        assertEquals(5.0, mapping.get(
            FrozenSystemState.obtainKey(FrozenSystemState.DATASINK, 
                FrozenSystemState.obtainPipelineElementSubkey("pip", "elt"), TimeBehavior.ENACTMENT_DELAY)));        
        assertEquals(6.0, mapping.get(
            FrozenSystemState.obtainKey(FrozenSystemState.DATASOURCE, 
                FrozenSystemState.obtainPipelineElementSubkey("pip", "elt"), Scalability.VOLATILITY)));
        assertEquals(7.0, mapping.get(
            FrozenSystemState.obtainKey(FrozenSystemState.HWNODE, "elt", TimeBehavior.THROUGHPUT_ITEMS)));
        assertEquals(8.0, mapping.get(
            FrozenSystemState.obtainKey(FrozenSystemState.INFRASTRUCTURE, "", TimeBehavior.THROUGHPUT_VOLUME)));
        assertEquals(10.0, mapping.get(
            FrozenSystemState.obtainKey(FrozenSystemState.PIPELINE, "pip", Scalability.VARIETY)));
        assertEquals(9.0, mapping.get(
            FrozenSystemState.obtainKey(FrozenSystemState.PIPELINE_ELEMENT, 
                FrozenSystemState.obtainPipelineElementSubkey("pip", "elt"), Scalability.VELOCITY)));
        Properties prop = state.toProperties();
        Assert.assertEquals(mapping.size(), prop.size());
        for (Map.Entry<String, Double> ent : mapping.entrySet()) {
            Object val = prop.getProperty(ent.getKey());
            Assert.assertNotNull(val);
            Assert.assertEquals(val, ent.getValue().toString());
        }
        state.toString(); // nothing to test
    }
    
    /**
     * An assert for a default delta of 0.5.
     * 
     * @param expected the epected value
     * @param actual teh actual value
     */
    private static void assertEquals(double expected, double actual) {
        Assert.assertEquals(expected, actual, 0.5);
    }
    
}
