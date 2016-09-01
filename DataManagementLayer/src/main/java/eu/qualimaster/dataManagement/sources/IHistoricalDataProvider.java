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
import java.io.Serializable;
import java.util.HashSet;

/**
 * Interface of a forward object providing access to historical data of a certain {@link ISource data source}.
 * Implementations shall be self-contained, not related to infrastructure properties and fully serializable.
 * 
 * @author Holger Eichelberger
 */
public interface IHistoricalDataProvider extends Serializable {

    /**
     * Obtains historical data.
     * 
     * @param timeHorizon the time horizon in milliseconds into the past
     * @param term the term (either stock or hashtag) for which the historical data is demanded
     * @param target the target file where to store the data (may be temporary)
     * @throws IOException in case that obtaining the historical data fails
     */
    public void obtainHistoricalData(long timeHorizon, String term, File target) throws IOException;
    
    /**
     * Obtains historical data.
     * 
     * @param timeHorizon the time horizon in milliseconds into the past
     * @param term the term (either stock or hashtag) for which the historical data is demanded
     * @param target the target file where to store the data (may be temporary)
     * @param dataUrl the url from where the data has to be downloaded
     * @throws IOException in case that obtaining the historical data fails
     */
    public void obtainHistoricalData(long timeHorizon, String term, File target, String dataUrl) throws IOException;
    
    /**
     * Gets the default set of terms to be monitored, used during the initialization of the volume prediction.
     * @return the set of terms to be monitored by default
     */
    public HashSet<String> getDefaultMonitoredTerms();
    
    /**
     * Gets the default set of terms to be available for blind prediction, used during the initialization of the volume prediction.
     * @return the set of terms to be available for blind prediction by default
     */
    public HashSet<String> getDefaultBlindTerms();
    
    /** Checks whether the instance is running in test mode or not.
	 * @return the test
	 */
	public boolean isTest();

	/** Sets the test mode.
	 * @param test the test to set
	 */
	public void setTest(boolean test);
}
