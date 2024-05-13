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
package eu.qualimaster.infrastructure;

import eu.qualimaster.common.QMInternal;

/**
 * Notifies the infrastructure about a pipeline running out of data. This event shall only be used
 * in case of pipelines of limited lifetime, such as experimental, testing, debugging or profiling 
 * pipelines. The infrastructure may decide to terminate the respective pipeline, but it must not
 * terminate it.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class EndOfDataEvent extends InfrastructureEvent {

    private static final long serialVersionUID = 7833601509983424341L;
    private String pipeline;
    private String source;
    
    /**
     * Notifies that <code>source</code> in <code>pipeline</code> ran out of data. The infrastructure
     * may decide to terminate <code>pipeline</code>, e.g., if all sources report this event.
     * 
     * @param pipeline the pipeline running out of data
     * @param source the (logical, configured) name of the source
     */
    public EndOfDataEvent(String pipeline, String source) {
        this.pipeline = pipeline;
        this.source = source;
    }

    /**
     * Returns the pipeline.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * Returns the source running out of data.
     * 
     * @return the source
     */
    public String getSource() {
        return source;
    }
    
}
