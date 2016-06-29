package eu.qualimaster.dataManagement.common;

import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;

/**
 * The common interface of all data management units. Please note
 * that not all data management elements also have measurement / monitoring
 * capabilities.<br/>
 * Data management elements are created, but shall not be connected by default. 
 * In particular data sources and sinks, but also intermediary data management 
 * elements are connected at a distinct point in time during the pipeline 
 * startup process by the Coordination Layer.<br/>
 * Please note that a data management element may also be virtual, e.g., a wrapper 
 * or a proxy for an actual implementation in order to introduce transparent raw
 * data storage, in particular for sources and sinks.<br/>
 * In particular, real time data sources shall only have few data access methods, 
 * preferrably exactly one in order to avoid accidental reordering of the stream 
 * sequence.
 * 
 * @author Holger Eichelberger
 */
public interface IDataElement {

    /**
     * Connects the data management element, i.e., after executing this method,
     * data can be obtained or stored. Calling this method again after the element
     * is connected, nothing shall happen.
     */
    public void connect();
    
    /**
     * Disconnects the data management element, i.e., after calling this method
     * no data shall be available anymore. Calling this method again after the element
     * is disconnected, nothing shall happen.
     */
    public void disconnect();

    /**
     * Defines the preferred storage strategy. An implementation may ignore this call
     * if already a strategy is defined and cannot be changed.
     * 
     * @param strategy the storage strategy
     */
    public void setStrategy(IStorageStrategyDescriptor strategy);
    
    /**
     * Returns the actual storage strategy.
     * 
     * @return the actual storage strategy
     */
    public IStorageStrategyDescriptor getStrategy();

}
