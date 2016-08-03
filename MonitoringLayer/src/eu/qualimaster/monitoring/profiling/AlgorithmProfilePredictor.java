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
package eu.qualimaster.monitoring.profiling;

import eu.qualimaster.infrastructure.PipelineLifecycleEvent;

/**
 * Interface to the prediction of algorithm quality properties.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmProfilePredictor {

    /**
     * Called upon startup of the infrastructure.
     */
    public static void start() {
    }

     /**
     * Notifies the predictor about changes in the lifecycle of pipelines.
     *  
     * @param event the lifecycle event
     */
    public static void notifyPipelineLifecycleChange(PipelineLifecycleEvent event) {
    }

     /**
     * Called upon shutdown of the infrastructure. Clean up global resources here.
     */
    public static void stop() {
    }

}
