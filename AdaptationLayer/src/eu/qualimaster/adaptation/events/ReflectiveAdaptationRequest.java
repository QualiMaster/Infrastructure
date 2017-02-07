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
package eu.qualimaster.adaptation.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import eu.qualimaster.adaptation.reflective.Setup;
import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractReturnableEvent;

/**
 * An event for requesting a reflective adaptation.
 * 
 * @author Andrea Ceroni
 */
@QMInternal
public class ReflectiveAdaptationRequest extends AbstractReturnableEvent {

    private static final long serialVersionUID = 6941328179172553914L;
    private Setup setup;
    private Map<String, ArrayList<String>> headers;
    private String latestMonitoring;
    
    /**
     * Creates a reflective adaptation request for a given setup.
     * 
     * @param setup the setup (scenario) for which the reflective adaptation is required
     * @param headers the list of observables for platform, pipelines, nodes included
     * in the monitoring trace
     * @param latestMonitoring the latest recorded monitoring information
     */
    public ReflectiveAdaptationRequest(Setup setup, Map<String, ArrayList<String>> headers, 
            String latestMonitoring) {
        this.setup = setup;
        this.headers = new HashMap<String, ArrayList<String>>(headers);
        this.latestMonitoring = latestMonitoring;
    }

    /**
     * Returns the setup.
     * 
     * @return the setup
     */
    public Setup getSetup() {
        return setup;
    }

    /**
     * Returns the headers.
     * 
     * @return the headers
     */
    public Map<String, ArrayList<String>> getHeaders() {
        return headers;
    }

    /**
     * Returns the latest monitoring.
     * 
     * @return the latestMonitoring
     */
    public String getLatestMonitoring() {
        return latestMonitoring;
    }
    
}
