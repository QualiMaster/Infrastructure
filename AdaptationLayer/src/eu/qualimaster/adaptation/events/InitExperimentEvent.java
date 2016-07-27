/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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
 * Event to indicate the execution of the resource init algorithm. [experimental]
 * 
 * @author Holger Eichelberger
 */
public class InitExperimentEvent extends AdaptationEvent /* no IPipelineAdaptationEvent here */ {

    private static final long serialVersionUID = 6938547396387573157L;
    private int numWorkers;
    private int numExecutors;
    private String pipeline;

    /**
     * Creates an init experiment event.
     * 
     * @param numWorkers the available number of workers (determined if negative)
     * @param numExecutors the available (target) number of executors (determined if negative)
     * @param pipeline the pipeline to initialize (may be <b>null</b> for all)
     */
    @QMInternal
    public InitExperimentEvent(int numWorkers, int numExecutors, String pipeline) {
        this.numWorkers = numWorkers;
        this.numExecutors = numExecutors;
        this.pipeline = pipeline;
    }

    /**
     * Returns the available number of workers to use.
     * 
     * @return the number of workers, open to the algorithm if negative
     */
    public int getNumWorkers() {
        return numWorkers;
    }

    /**
     * Returns the available number of executors to use.
     * 
     * @return the number of executors, open to the algorithm if negative
     */
    public int getNumExecutors() {
        return numExecutors;
    }

    /**
     * Returns the pipeline to initialize.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return pipeline;
    }

}
