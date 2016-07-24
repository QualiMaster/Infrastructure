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

/**
 * An abstract single-thread monitor.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractMonitor {

    private transient long startTime;
    private transient int count;

    /**
     * Starts monitoring for an execution method and informs the monitoring plugins.
     */
    public void startMonitoring() {
        startTime = System.currentTimeMillis();
        count = 0;
    }

    /**
     * Ends monitoring for an execution method and informs the monitoring plugins.
     */
    public void endMonitoring() {
        aggregateExecutionTime(startTime, count);
    }

    /**
     * Explicitly sets the number of emitted tuples.
     * 
     * @param count the number of emitted tuples
     */
    protected void setEmitCount(int count) {
        this.count = count;
    }

    /**
     * Counts emitting for a sink execution method.
     * 
     * @param tuple the tuple emitted
     */
    public abstract void emitted(Object tuple);

    /**
     * Aggregate the execution time and sends the recorded value to the monitoring layer if the send interval
     * is outdated. Shall be used only in combination with a corresponding start time measurement.
     *
     * @param start the start execution time
     * @param itemsCount the number of items emitted since <code>start</code>, (negative is turned to <code>0</code>)
     */
    public abstract void aggregateExecutionTime(long start, int itemsCount);
    
    /**
     * Ends monitoring with emitting <code>tuple</code>.
     * 
     * @param tuple the emitted tuple
     */
    public void endMonitoring(Object tuple) {
        emitted(tuple);
        endMonitoring();
    }

}
