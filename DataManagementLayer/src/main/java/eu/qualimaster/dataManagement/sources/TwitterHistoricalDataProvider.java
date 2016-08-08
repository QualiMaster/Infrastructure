package eu.qualimaster.dataManagement.sources;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Provides access to the historical twitter data archived by the Data Management Layer.
 * Implementations shall be self-contained, not related to infrastructure properties and fully serializable.
 * 
 * @author Andrea Ceroni
 */
public class TwitterHistoricalDataProvider implements IHistoricalDataProvider,Serializable
{
    /**
     * Obtains twitter data via the Data Management Layer
     * 
     * @param timeHorizon the time horizon in milliseconds into the past
     * @param term the name of the stock for which the historical data is demanded (format: INDEX_NAME·STOCK_NAME)
     * @param target the target file where to store the data
     * @throws IOException in case that obtaining the historical data fails
     */
    public void obtainHistoricalData(long timeHorizon, String term, File target) throws IOException
    {
    	// TODO First clarify and implement how aggregated volume data for Twitter is stored.
    }
}
