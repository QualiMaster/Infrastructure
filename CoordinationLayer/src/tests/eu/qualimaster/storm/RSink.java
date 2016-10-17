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
package tests.eu.qualimaster.storm;

import java.util.Map;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import eu.qualimaster.common.signal.AbstractReplaySink;

/**
 * Implements a testing replay sink.
 * 
 * @author Holger Eichelberger
 */
public class RSink extends AbstractReplaySink {

    private static final long serialVersionUID = -8201443447929302832L;

    /**
     * Creates the sink.
     * 
     * @param name
     *            the name of the bolt
     * @param namespace
     *            the namespace of the bolt
     * @param sendRegular
     *            whether this monitor shall care for sending regular events (
     *            <code>true</code>) or not (<code>false</code>, for
     *            thrift-based monitoring)
     */
    protected RSink(String name, String namespace, boolean sendRegular) {
        super(name, namespace, sendRegular);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void registerHandlers(Map conf, TopologyContext context) {
        // no handlers, just check for events
    }

}
