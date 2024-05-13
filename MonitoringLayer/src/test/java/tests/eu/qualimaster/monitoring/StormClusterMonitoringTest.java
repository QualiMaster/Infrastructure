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

import java.util.ArrayList;

import org.junit.Test;
import org.junit.Assert;

import backtype.storm.generated.SupervisorSummary;
import eu.qualimaster.monitoring.storm.StormClusterMonitoringTask;
import eu.qualimaster.monitoring.systemState.PlatformSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;

/**
 * Tests the core functionality of {@link StormClusterMonitoringTest}.
 * 
 * @author Holger Eichelberger
 */
public class StormClusterMonitoringTest {
    
    /**
     * Tests the cluster aggregation.
     */
    @Test
    public void testAggregation() {
        SystemState state = new SystemState();
        ArrayList<SupervisorSummary> supervisors = new ArrayList<SupervisorSummary>();
        PlatformSystemPart part = state.getPlatform();

        // empty is unrealistic except for killing supervisors or having an empty cluster
        StormClusterMonitoringTask.aggregate(part, supervisors);
        assertEquals(0, part, ResourceUsage.USED_MACHINES);
        
        supervisors.clear();
        supervisors.add(new SupervisorSummary("host1", 10, 1, 1, "host1"));
        supervisors.add(new SupervisorSummary("host2", 10, 1, 1, "host2"));

        StormClusterMonitoringTask.aggregate(part, supervisors);
        assertEquals(2, part, ResourceUsage.USED_MACHINES);

        supervisors.clear();
        supervisors.add(new SupervisorSummary("host1", 10, 1, 1, "host1"));
        supervisors.add(new SupervisorSummary("host2", 10, 1, 0, "host2"));
        
        StormClusterMonitoringTask.aggregate(part, supervisors);
        assertEquals(1, part, ResourceUsage.USED_MACHINES);

        supervisors.clear();
        supervisors.add(new SupervisorSummary("host1", 10, 1, 0, "host1"));
        supervisors.add(new SupervisorSummary("host2", 10, 1, 0, "host2"));
        
        StormClusterMonitoringTask.aggregate(part, supervisors);
        assertEquals(0, part, ResourceUsage.USED_MACHINES);

        
        // empty is unrealistic except for killing supervisors or having an empty cluster
        supervisors.clear();
        StormClusterMonitoringTask.aggregate(part, supervisors);
        assertEquals(0, part, ResourceUsage.USED_MACHINES);

    }
    
    /**
     * Asserts <code>expected</code> on the <code>observable</code> of <code>part</code>.
     * 
     * @param expected the expected value
     * @param part the part to take the observation from
     * @param observable the observed value
     */
    private static void assertEquals(double expected, SystemPart part, IObservable observable) {
        Assert.assertEquals(expected, part.getObservedValue(observable), 0.005);
    }

}
