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
package tests.eu.qualimaster.monitoring.genTopo;

import eu.qualimaster.common.signal.ShutdownSignal;
import eu.qualimaster.common.signal.SignalException;

/**
 * The specific hw family element notifying the sub-nodes about shutdown.
 * 
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public class TestFamilyHwFamilyElement extends TestFamilyFamilyElement {

    public static final String HW_BOLT = "GenTopoHardwareCorrelationFinancialHardwareConnectionBolt";
    public static final String HW_SPOUT = "GenTopoHardwareCorrelationFinancialHardwareConnectionSpout";
    
    /**
     * Creates the test family.
     * 
     * @param name the source name
     * @param namespace the namespace
     * @param sendMonitoringEvents whether monitoring events shall be sent out
     * @param algorithm the actual algorithm (may be <b>null</b> for no changed event)
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public TestFamilyHwFamilyElement(String name, String namespace, boolean sendMonitoringEvents, 
        String algorithm, boolean sendRegular) {
        super(name, namespace, sendMonitoringEvents, algorithm, sendRegular);
    }
    
    @Override
    protected void prepareShutdown(ShutdownSignal signal) {
        send(new ShutdownSignal(getName(), HW_BOLT));
        send(new ShutdownSignal(getName(), HW_SPOUT));
    }
    
    /**
     * Sends a shutdown signal.
     * 
     * @param signal the signal
     */
    private static void send(ShutdownSignal signal) {
        try {
            signal.sendSignal();
        } catch (SignalException e) {
            System.out.println(e.getMessage());
        }
    }

}
