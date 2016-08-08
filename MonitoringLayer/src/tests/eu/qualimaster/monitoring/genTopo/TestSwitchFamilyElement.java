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

/**
 * The specific hw family element notifying the sub-nodes about shutdown.
 * 
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public class TestSwitchFamilyElement extends SendingBolt {

    public static final String FAMILY = "process";
    public static final String INTERMEDIARY = "Intermediary";
    public static final String MAPPER = "CorrelationSWMapper";
    public static final String PROCESSOR = "CorrelationSWHayashiYoshida";
    public static final String OUT_INTERMEDIARY = "OutIntermediary";
    public static final String OUT_RECEIVER = "SwitchEnd";
    
    /**
     * Creates the test family.
     * 
     * @param name the source name
     * @param namespace the namespace
     * @param sendMonitoringEvents whether monitoring events shall be sent out
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     * @param port the communication port
     */
    public TestSwitchFamilyElement(String name, String namespace, boolean sendMonitoringEvents, 
        boolean sendRegular, int port) {
        super(name, namespace, sendMonitoringEvents, sendRegular, port);
    }
    
    @Override
    protected void prepareShutdown(ShutdownSignal signal) {
        super.prepareShutdown(signal);
        send(new ShutdownSignal(getPipeline(), INTERMEDIARY));
        send(new ShutdownSignal(getPipeline(), MAPPER));
        send(new ShutdownSignal(getPipeline(), PROCESSOR));
        send(new ShutdownSignal(getPipeline(), OUT_INTERMEDIARY));
        send(new ShutdownSignal(getPipeline(), OUT_RECEIVER));
    }

}
