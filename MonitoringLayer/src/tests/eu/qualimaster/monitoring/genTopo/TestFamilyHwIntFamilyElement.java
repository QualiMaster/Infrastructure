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

import java.util.Map;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import eu.qualimaster.common.signal.ShutdownSignal;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;

/**
 * The specific hw family element notifying the sub-nodes about shutdown.
 * 
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public class TestFamilyHwIntFamilyElement extends SendingBolt {

    public static final String FAMILY = "process";
    public static final String INTERMEDIARY = "Intermediary";
    public static final String HW_BOLT = "GenTopoHardwareCorrelationFinancialHardwareConnectionBolt";
    public static final String HW_SPOUT = "GenTopoHardwareCorrelationFinancialHardwareConnectionSpout";
    public static final String OUT_INTERMEDIARY = "OutIntermediary";
    public static final String OUT_RECEIVER = "SwitchEnd";
    
    private String algorithm;
    
    // checkstyle: stop parameter number check
    
    /**
     * Creates the test family.
     * 
     * @param name the source name
     * @param namespace the namespace
     * @param sendMonitoringEvents whether monitoring events shall be sent out
     * @param algorithm the actual algorithm (may be <b>null</b> for no changed event)
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     * @param port the communicating port
     */
    public TestFamilyHwIntFamilyElement(String name, String namespace, boolean sendMonitoringEvents, 
        String algorithm, boolean sendRegular, int port) {
        super(name, namespace, sendMonitoringEvents, sendRegular, port);
        this.algorithm = algorithm;
    }
    
    // checkstyle: resume parameter number check
    
    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        if (null != algorithm) {
            EventManager.send(new AlgorithmChangedMonitoringEvent(getPipeline(), getName(), algorithm));
        }
    }
    
    @Override
    protected void prepareShutdown(ShutdownSignal signal) {
        super.prepareShutdown(signal);
        send(new ShutdownSignal(getPipeline(), INTERMEDIARY));
        send(new ShutdownSignal(getPipeline(), HW_BOLT));
        send(new ShutdownSignal(getPipeline(), HW_SPOUT));
        send(new ShutdownSignal(getPipeline(), OUT_INTERMEDIARY));
        send(new ShutdownSignal(getPipeline(), OUT_RECEIVER));
    }
    
}
