package eu.qualimaster.dataManagement.sources;

import eu.qualimaster.dataManagement.common.IDataElement;
import eu.qualimaster.observables.IMeasurable;

/**
 * Represents a data source, i.e, a measurable data management element to be 
 * used for data ingestion into data processing. Please note that the actual method
 * for data ingestion is defined in a domain-specific way ("generated" from the configuration). 
 * Configuration of a data source also happens through domain-specific methods. 
 * Data sinks must have a public no-argument constructor.
 * 
 * @author Holger Eichelberger
 */
public interface IDataSource extends IDataElement, IMeasurable {

    // methods to influence a potential buffer size need to be available through the configuration
    // for adaptation and, thus, will be added to the configuration

    /**
     * Provides access to historical data for this source. This method is intended for source
     * volume prediction. If no result is provided, the source volume prediction shall fall back
     * to a warmup aggregation.
     * 
     * @return the data provider (may be <b>null</b> if not implemented)
     */
    //public IHistoricalDataProvider getHistoricalDataProvider();
    
}
