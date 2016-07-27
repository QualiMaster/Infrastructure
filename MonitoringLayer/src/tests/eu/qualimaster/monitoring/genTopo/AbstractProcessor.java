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
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import eu.qualimaster.common.signal.BaseSignalBolt;

/**
 * Implements an abstract throughput processor.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractProcessor extends BaseSignalBolt  {

    public static final long LATENCY = 350; 
    public static final String STREAM_NAME = "number";
    private static final long serialVersionUID = -6043292538379260988L;
    private transient OutputCollector collector;
    private boolean sendMonitoringEvents;

    /**
     * Creates an abstract processor.
     * 
     * @param name the name of the processor
     * @param namespace the containing namespace
     * @param sendMonitoringEvents do send monitoring events
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public AbstractProcessor(String name, String namespace, boolean sendMonitoringEvents, boolean sendRegular) {
        super(name, namespace, sendRegular);
        this.sendMonitoringEvents = sendMonitoringEvents;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        this.collector = collector;
    }

    /**
     * Returns the collector instance.
     * 
     * @return the collector instance
     */
    protected OutputCollector getCollector() {
        return collector;
    }
    
    /**
     * Returns whether monitoring events shall be sent.
     * 
     * @return <code>true</code> for sending, <code>false</code> else
     */
    protected boolean doSendMonitoringEvents() {
        return sendMonitoringEvents;
    }
    
    @Override
    public void execute(Tuple input) {
        startMonitoring();
        Utils.sleep(LATENCY);
        collector.emit(new Values(input.getInteger(0)));
        collector.ack(input);
        if (sendMonitoringEvents) {
            endMonitoring();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields(STREAM_NAME));
    }

}
