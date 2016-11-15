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
import eu.qualimaster.common.signal.AlgorithmChangeSignal;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import tests.eu.qualimaster.storm.SendingBolt;

/**
 * A sending family.
 * 
 * @author Holger Eichelberger
 */
public class SendingFamily extends SendingBolt {

    private static final long serialVersionUID = -77605200450456341L;
    private String algorithm;
    
    /**
     * Creates a HW bolt.
     * 
     * @param name the name of the processor
     * @param namespace the containing namespace
     * @param sendMonitoringEvents do send monitoring events
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     * @param port the connection port
     */
    public SendingFamily(String name, String namespace, boolean sendMonitoringEvents, boolean sendRegular, int port) {
        super(name, namespace, sendMonitoringEvents, sendRegular, port);
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        if (null != algorithm) {
            EventManager.send(new AlgorithmChangedMonitoringEvent(getPipeline(), getName(), algorithm));
        }
    }

    @Override
    public void notifyAlgorithmChange(AlgorithmChangeSignal signal) {
        String alg = signal.getAlgorithm();
        if (null == this.algorithm || !this.algorithm.equals(alg)) {
            this.algorithm = alg;
            EventManager.send(new AlgorithmChangedMonitoringEvent(getPipeline(), getName(), algorithm));
        }
    }

}
