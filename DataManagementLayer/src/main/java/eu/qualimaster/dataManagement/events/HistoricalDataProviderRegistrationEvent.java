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
package eu.qualimaster.dataManagement.events;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.dataManagement.sources.IHistoricalDataProvider;

/**
 * Used to register historical data provides with parts of the infrastructure.
 * Implicitly, this event can be used to get informed about upcoming data sources.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public class HistoricalDataProviderRegistrationEvent extends DataManagementEvent {

    private static final long serialVersionUID = 6523232536281102253L;
    private String pipeline;
    private String source;
    private IHistoricalDataProvider provider;

    /**
     * Creates a registration event instance.
     * 
     * @param pipeline the logical, configured name of the pipeline
     * @param source the logical, configured name of the data source
     * @param provider the historical data provider (must be serializable)
     */
    public HistoricalDataProviderRegistrationEvent(String pipeline, String source, IHistoricalDataProvider provider) {
        this.pipeline = pipeline;
        this.source = source;
        this.provider = provider;
    }

    /**
     * Returns the (configured, logical) name of the pipeline.
     * 
     * @return the name of the pipeline
     */
    protected String getPipeline() {
        return pipeline;
    }

    /**
     * Returns the (configured, logical) name of the data source.
     * 
     * @return the name of the data source
     */
    protected String getSource() {
        return source;
    }

    /**
     * Returns the historical data provider.
     * 
     * @return the historical data provide - <b>null</b> if no provider is available
     */
    protected IHistoricalDataProvider getProvider() {
        return provider;
    }
    
}
