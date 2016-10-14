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

import java.util.Map;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.dataManagement.sources.IHistoricalDataProvider;

/**
 * Used to register historical data provides with parts of the infrastructure.
 * May be re-sent if the provider or the id-names map changes.
 * Implicitly, this event can be used to get informed about upcoming data sources.
 * 
 * @author Holger Eichelberger
 * @author Andrea Ceroni
 */
@QMInternal
public class HistoricalDataProviderRegistrationEvent extends DataManagementEvent {

    private static final long serialVersionUID = 6523232536281102253L;
    private String pipeline;
    private String source;
    private IHistoricalDataProvider provider;
    private Map<String, String> idsNamesMap;

    /**
     * Creates a registration event instance.
     * 
     * @param pipeline the logical, configured name of the pipeline
     * @param source the logical, configured name of the data source
     * @param provider the historical data provider (must be serializable)
     */
    public HistoricalDataProviderRegistrationEvent(String pipeline, String source, IHistoricalDataProvider provider) {
        this(pipeline, source, provider, null);
    }
    
    /**
     * Creates a registration event instance.
     * 
     * @param pipeline the logical, configured name of the pipeline
     * @param source the logical, configured name of the data source
     * @param provider the historical data provider (must be serializable)
     * @param idsNamesMap the mapping of ids to names (may be <b>null</b> for no mapping) 
     */
    public HistoricalDataProviderRegistrationEvent(String pipeline, String source, IHistoricalDataProvider provider, 
        Map<String, String> idsNamesMap) {
        this.pipeline = pipeline;
        this.source = source;
        this.provider = provider;
        this.idsNamesMap = idsNamesMap;
    }

    /**
     * Returns the (configured, logical) name of the pipeline.
     * 
     * @return the name of the pipeline
     */
    public String getPipeline() {
        return pipeline;
    }

    /**
     * Returns the (configured, logical) name of the data source.
     * 
     * @return the name of the data source
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the historical data provider.
     * 
     * @return the historical data provide - <b>null</b> if no provider is available
     */
    public IHistoricalDataProvider getProvider() {
        return provider;
    }
    
    /**
     * Provides access to the mapping between ids and names of source keys (e.g. stocks or hashtags).
     * 
     * @return the id-name mapping of source keys (may be <b>null</b> if such mapping does not exist)
     */
    public Map<String, String> getIdsNamesMap() {
        return idsNamesMap;
    }
    
}
