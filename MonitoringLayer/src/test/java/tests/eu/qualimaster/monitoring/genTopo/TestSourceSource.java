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

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;
import eu.qualimaster.common.signal.AggregationKeyProvider;
import eu.qualimaster.common.signal.BaseSignalSourceSpout;
import eu.qualimaster.common.signal.SourceMonitor;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.EndOfDataEvent;

/**
 * Implements the test source.
 * 
 * @author Holger Eichelberger
 */
public class TestSourceSource extends BaseSignalSourceSpout {

    public static final long LATENCY = 300;
    private static final long serialVersionUID = -4142872611182850871L;
    private transient SpoutOutputCollector collector;
    private boolean sendMonitoringEvents;
    private int number;
    private int maxNumber;

    /**
     * Creates the source.
     * 
     * @param name the source name
     * @param namespace the namespace
     * @param sendMonitoringEvents whether monitoring events shall be sent out
     */
    public TestSourceSource(String name, String namespace, boolean sendMonitoringEvents) {
        super(name, namespace, true);
        this.sendMonitoringEvents = sendMonitoringEvents;
    }
    
    /**
     * Sets the maximum number of events to cause an {@link EndOfDataEvent}.
     * 
     * @param maxNumber the maximum number
     */
    public void maxNumEvents(int maxNumber) {
        this.maxNumber = maxNumber;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void open(Map stormConf, TopologyContext context, SpoutOutputCollector collector) {
        super.open(stormConf, context, collector);
        this.collector = collector;
    }
    
    @Override
    public void nextTuple() {
        startMonitoring();
        Utils.sleep(LATENCY);
        Integer value = number++;
        if (null != value) {
            this.collector.emit(new Values(value));
            if (sendMonitoringEvents) {
                endMonitoring();
            }
        }
        if (maxNumber > 0 && number > maxNumber) {
            EventManager.send(new EndOfDataEvent(getPipeline(), getName()));
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("number"));
    }
    
    @Override
    public void configure(SourceMonitor monitor) {
        monitor.setAggregationInterval(1000);
        monitor.registerAggregationKeyProvider(new AggregationKeyProvider<Integer>(Integer.class) {

            @Override
            public String getAggregationKey(Integer tuple) {
                return String.valueOf(tuple);
            }
        });
    }

}
