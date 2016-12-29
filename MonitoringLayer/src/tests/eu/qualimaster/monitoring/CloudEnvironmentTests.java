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
package tests.eu.qualimaster.monitoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;

import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.CloudResourceMonitoringEvent;
import eu.qualimaster.monitoring.systemState.CloudEnvironmentSystemPart;
import eu.qualimaster.monitoring.systemState.PlatformSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.CloudResourceUsage;
import eu.qualimaster.observables.IObservable;

/**
 * Tests the cloud environment.
 * 
 * @author Holger Eichelberger
 */
public class CloudEnvironmentTests {

    private SystemState state;
    
    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        state = new SystemState();
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        state.clear();
        state = null;
    }
    
    /**
     * Basic tests for the cloud environment.
     */
    @Test
    public void testCloudEnvironment() {
        PlatformSystemPart psp = state.getPlatform();
        CloudEnvironmentSystemPart aws = psp.obtainCloudEnvironment("AWS");
        CloudEnvironmentSystemPart on = psp.obtainCloudEnvironment("ON");
        
        assertValue(aws, CloudResourceUsage.BANDWIDTH, 100, on);
        assertValue(on, CloudResourceUsage.PING, 250, aws);
        assertValue(on, CloudResourceUsage.USED_HARDDISC_MEM, 100000, aws);
        assertValue(aws, CloudResourceUsage.USED_PROCESSORS, 3, on);
        assertValue(aws, CloudResourceUsage.USED_WORKING_STORAGE, 123000, on);
    }

    /**
     * Tests the cloud resource monitoring event.
     */
    @Test
    public void testCloudResourceMonitoringEvent() {
        final String env = "AWS-EU";
        
        // cleanup
        PlatformSystemPart psp = MonitoringManager.getSystemState().getPlatform();
        List<Object> envs = new ArrayList<Object>();
        envs.add(env);
        psp.removeCloudEnvironment(envs);
        Assert.assertNull(psp.getCloudEnvironment(env));
        
        Map<IObservable, Double> observations = new HashMap<IObservable, Double>();
        observations.put(CloudResourceUsage.BANDWIDTH, 112.0);
        observations.put(CloudResourceUsage.PING, 252.0);
        observations.put(CloudResourceUsage.USED_HARDDISC_MEM, 10023.0);
        observations.put(CloudResourceUsage.USED_PROCESSORS, 4.0);
        observations.put(CloudResourceUsage.USED_WORKING_STORAGE, 124000.0);
        CloudResourceMonitoringEvent evt = new CloudResourceMonitoringEvent(env, observations);
        
        MonitoringManager.handleEvent(evt);
        CloudEnvironmentSystemPart cloudEnv = psp.getCloudEnvironment(env);
        Assert.assertNotNull(cloudEnv);
        for (Map.Entry<IObservable, Double> obs : observations.entrySet()) {
            Assert.assertTrue(cloudEnv.hasValue(obs.getKey()));
            Assert.assertEquals(obs.getValue(), cloudEnv.getObservedValue(obs.getKey()), 0.05);
        }
        
        // cleanup
        psp.removeCloudEnvironment(envs);
        Assert.assertNull(psp.getCloudEnvironment(env));
    }

    /**
     * Sets and asserts <code>value</code> for <code>observable</code> on <code>part</code> and checks that
     * <code>unset</code> does not have a value for <code>observable</code>.
     * 
     * @param part the part to be changed
     * @param observable the observable
     * @param value the value to set and expect
     * @param unset the part to be checked for unset (ignored if <b>null</b>)
     * @see #assertValue(CloudEnvironmentSystemPart, CloudResourceUsage, double)
     */
    private void assertValue(CloudEnvironmentSystemPart part, IObservable observable, double value, 
        CloudEnvironmentSystemPart unset) {
        assertValue(part, observable, value);
        if (null != unset) {
            Assert.assertFalse(unset.hasValue(observable));
        }
    }

    /**
     * Sets and asserts <code>value</code> for <code>observable</code> on <code>part</code>.
     * 
     * @param part the part to be changed
     * @param observable the observable
     * @param value the value to set and expect
     */
    private void assertValue(CloudEnvironmentSystemPart part, IObservable observable, double value) {
        part.setValue(observable, value, null);
        Assert.assertTrue(part.hasValue(observable));
        Assert.assertEquals(value, part.getObservedValue(observable), 0.05);
    }
    
}
