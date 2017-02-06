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

import eu.qualimaster.common.QMInternal;

/**
 * Used to initialize the models for reflective adaptation.
 * May be sent different times, with the possibility of keeping the previously
 * initialized models.
 * 
 * @author Andrea Ceroni
 */
@QMInternal
public class ReflectiveAdaptationRegistrationEvent extends AdaptationEvent {

    private static final long serialVersionUID = 6523232536281102253L;
    private String setupsPath;
    private boolean keepAvailableModels;

    /**
     * Creates a registration event instance. Any previously initialized model will be deleted.
     * 
     * @param setupsPath the path to the xml file describing the available setups for which models exist.
     */
    public ReflectiveAdaptationRegistrationEvent(String setupsPath) {
        this(setupsPath, false);
    }
    
    /**
     * Creates a registration event instance. Includes the possibility to keep previously initialized
     * models.
     * 
     * @param setupsPath the path to the xml file describing the available setups for which models exist
     * @param keepAvailableModels flag for keeping the models that are already available (in case 
     * of multiple initializations)
     */
    public ReflectiveAdaptationRegistrationEvent(String setupsPath, boolean keepAvailableModels) {
        this.setupsPath = setupsPath;
        this.keepAvailableModels = keepAvailableModels;
    }

    /**
     * @return the setupsPath
     */
    public String getSetupsPath() {
        return setupsPath;
    }

    /**
     * @return the keepAvailableModels
     */
    public boolean isKeepAvailableModels() {
        return keepAvailableModels;
    }
    
}
