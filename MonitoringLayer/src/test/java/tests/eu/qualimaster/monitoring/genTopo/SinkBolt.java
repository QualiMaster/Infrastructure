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

import backtype.storm.tuple.Tuple;

/**
 * Simulates a sink.
 * 
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public class SinkBolt extends AbstractProcessor {

    /**
     * Creates a sink.
     * 
     * @param name the name of the processor
     * @param namespace the containing namespace
     * @param sendMonitoringEvents do send monitoring events
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public SinkBolt(String name, String namespace, boolean sendMonitoringEvents, boolean sendRegular) {
        super(name, namespace, sendMonitoringEvents, sendRegular);
    }
    
    @Override
    public void execute(Tuple input) {
        startMonitoring();
        System.out.println("SINK " + getName() + " " + input.getInteger(0));
        emitted(input);
        getCollector().ack(input);
        if (doSendMonitoringEvents()) {
            endMonitoring();
        }
    }

}
