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
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;

/**
 * Implements the test family.
 * 
 * @author Holger Eichelberger
 */
public class TestFamilyFamilyElement extends AbstractProcessor {

    private static final long serialVersionUID = 7654006005629709996L;
    private String algorithm;

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
    public TestFamilyFamilyElement(String name, String namespace, boolean sendMonitoringEvents, String algorithm, 
        boolean sendRegular) {
        super(name, namespace, sendMonitoringEvents, sendRegular);
        this.algorithm = algorithm;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        if (null != algorithm) {
            EventManager.send(new AlgorithmChangedMonitoringEvent(getPipeline(), getName(), algorithm));
        }
    }

}
