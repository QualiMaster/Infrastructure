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
package eu.qualimaster.monitoring.systemState;

import eu.qualimaster.observables.IObservable;
import eu.qualimaster.observables.ResourceUsage;
import eu.qualimaster.observables.Scalability;
import eu.qualimaster.observables.TimeBehavior;

/**
 * Some utility methods for setting/calculating values.
 * 
 * @author Holger Eichelberger
 */
public class StateUtils {

    /**
     * Updates the capacity for now. Actually, derived observations would be better, but this is sufficient for now.
     * 
     * @param part the system part
     * @param key the component key
     * @param ignoreNoTuples ignore phases where no tuples are observed and enlarge observation window (-> thrift)
     * @see #updateCapacity(SystemPart, Object, boolean, long)
     */
    public static void updateCapacity(SystemPart part, Object key, boolean ignoreNoTuples) {
        updateCapacity(part, key, ignoreNoTuples, System.currentTimeMillis());
    }

    /**
     * Updates the capacity for a given point in time [testing, simulation]. Actually, derived observations would be 
     * better, but this is sufficient for now.
     * 
     * @param part the system part
     * @param key the component key
     * @param ignoreNoTuples ignore phases where no tuples are observed and enlarge observation window (-> thrift)
     * @param timestamp the actual point in time for updating
     */
    public static void updateCapacity(SystemPart part, Object key, boolean ignoreNoTuples, long timestamp) {
        Double last = part.getStoreValue(ResourceUsage.CAPACITY, key);
        Double lastExecuted = part.getStoreValue(TimeBehavior.THROUGHPUT_ITEMS, key);
        double now = timestamp;
        if (null != last && null != lastExecuted) {
            double window = now - last;
            if (window > 5000) { // > 0! 
                double executed = part.getObservedValue(TimeBehavior.THROUGHPUT_ITEMS);
                double executedInWindow = executed - lastExecuted;
                // thrift does not always return something - enlarge window
                if (!ignoreNoTuples || (ignoreNoTuples && executedInWindow > 0)) {
                    double executeLatency = part.getObservedValue(TimeBehavior.LATENCY); // avg
                    double capacity = Math.min(1, Math.max(0, (executedInWindow * executeLatency) / window));
                    part.setValue(ResourceUsage.CAPACITY, capacity, key);
                    part.setStoreValue(TimeBehavior.THROUGHPUT_ITEMS, executed, key);
                    part.setStoreValue(ResourceUsage.CAPACITY, now, key);
                }
            }
        } else {
            part.setStoreValue(TimeBehavior.THROUGHPUT_ITEMS, 0.0, key);
            part.setStoreValue(ResourceUsage.CAPACITY, now, key);
        }
    }

    /**
     * Returns whether the given observable changes the latency.
     * 
     * @param observable the observable to check
     * @return <code>true</code> if it changes the latency, <code>false</code> else
     */
    public static boolean changesLatency(IObservable observable) {
        return TimeBehavior.LATENCY == observable || TimeBehavior.THROUGHPUT_ITEMS == observable;
    }

    /**
     * Changes a value and updates dependent values.
     * 
     * @param part the system part
     * @param observable the observable
     * @param observation the observation
     * @param key the component key
     */
    public static void setValue(SystemPart part, IObservable observable, double observation, Object key) {
        part.setValue(observable, observation, key);
        if (TimeBehavior.THROUGHPUT_ITEMS == observable) {
            part.setValue(Scalability.ITEMS, observation, key);
//double it =
            //part.getObservedValue(Scalability.ITEMS); // TODO fix me: force calculation
//System.err.println(part.getName()+ " " + it+" "+observation+" "+key);
        }
    }

}
