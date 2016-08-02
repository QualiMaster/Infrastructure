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
package eu.qualimaster.common.signal;

import backtype.storm.task.TopologyContext;

/**
 * A specialized signal spout intended for pipeline sources. Uses {@link SourceMonitor} instead
 * of {@link Monitor}. Use this class only for real pipeline sources, not intermediary sources!
 * 
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public abstract class BaseSignalSourceSpout extends BaseSignalSpout {

    /**
     * Creates a signal source spout.
     * 
     * @param name the name of the spout
     * @param namespace the namespace
     */
    public BaseSignalSourceSpout(String name, String namespace) {
        super(name, namespace);
    }

    /**
     * Creates a signal source spout.
     * 
     * @param name the name of the spout
     * @param namespace the namespace
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public BaseSignalSourceSpout(String name, String namespace, boolean sendRegular) {
        super(name, namespace, sendRegular);
    }
    
    @Override
    protected Monitor createMonitor(String namespace, String name, boolean includeItems, TopologyContext context, 
        boolean sendRegular) {
        SourceMonitor monitor = new SourceMonitor(namespace, name, true, context, sendRegular);
        configure(monitor);
        return monitor;
    }

    /**
     * Allows configuring a new source monitor instance for this class, e.g., to change the aggregation
     * frequency or to add aggregation key providers.
     * 
     * @param monitor the monitor instance
     */
    protected void configure(SourceMonitor monitor) {
    }
    
}
