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
package eu.qualimaster.monitoring.events;

import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.observables.IObservable;

/**
 * Implements a multi-observation host monitoring event.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class PlatformMultiObservationHostMonitoringEvent extends MonitoringEvent {

    private static final long serialVersionUID = -7322761397161311728L;
    private String host;
    private Map<IObservable, Double> observations;
    
    /**
     * Creates an observation event.
     * 
     * @param host the host name
     * @param observations the observations
     */
    public PlatformMultiObservationHostMonitoringEvent(String host, Map<IObservable, Double> observations) {
        this.host = host;
        this.observations = observations;
    }
    
    /**
     * Returns all observations.
     * 
     * @return the observations
     */
    public Map<IObservable, Double> getObservations() {
        return observations;
    }
    
    /**
     * Returns the host this event applies to.
     * 
     * @return the host
     */
    public String getHost() {
        return host;
    }

}
