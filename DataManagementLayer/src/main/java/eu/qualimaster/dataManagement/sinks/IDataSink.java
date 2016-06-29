package eu.qualimaster.dataManagement.sinks;

import eu.qualimaster.dataManagement.common.IDataElement;
import eu.qualimaster.observables.IMeasurable;

/**
 * Represents a data sink, i.e, a measurable data management element acting as an endpoint
 * of data management, allowing e.g. to pass data processing results to QualiMaster applications.
 * Please note that the actual method for data output is defined in a domain-specific way ("generated" 
 * from the configuration). Configuration of a data source also happens through domain-specific methods.
 * Data sinks must have a public no-argument constructor.
 * 
 * @author Holger Eichelberger
 */
public interface IDataSink extends IDataElement, IMeasurable {

}
