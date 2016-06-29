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

}
