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
package eu.qualimaster.dataManagement.sources;

import java.io.File;
import java.io.IOException;

/**
 * Interface of a forward object providing access to historical data of a certain {@link ISource data source}.
 * Implementations shall be self-contained, not related to infrastructure properties and fully serializable.
 * 
 * @author Holger Eichelberger
 */
public interface IHistoricalDataProvider {

    /**
     * Obtains historical data.
     * 
     * @param timeHorizon the time horizon in milliseconds into the past
     * @param target the target file where to store the data (may be temporary)
     * @throws IOException in case that obtaining the historical data fails
     */
    public void obtainHistoricalData(long timeHorizon, File target) throws IOException;
    
}
