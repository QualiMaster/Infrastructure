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
 * A monitor for additional parallel threads.
 * 
 * @author Holger Eichelberger
 */
public class ThreadMonitor extends AbstractMonitor {
    
    private Monitor parent;

    /**
     * Creates and attaches the sub-monitor to its parent.
     * 
     * @param parent the parent
     */
    ThreadMonitor(Monitor parent) {
        this.parent = parent;
    }

    @Override
    public void aggregateExecutionTime(long start, int itemsCount) {
        parent.aggregateExecutionTime(start, itemsCount);
    }

    @Override
    public void emitted(Object tuple) {
        parent.emitted(tuple);
    }

}
