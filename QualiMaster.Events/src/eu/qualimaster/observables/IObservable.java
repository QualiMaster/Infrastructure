package eu.qualimaster.observables;

import java.io.Serializable;

import eu.qualimaster.common.QMInternal;

/**
 * Things that can be observed.
 * 
 * @author Holger Eichelberger
 */
public interface IObservable extends Serializable {
    
    /**
     * Returns the (symbolic) name of the observable.
     * 
     * @return the symbolic name
     */
    public String name();
    
    /**
     * Whether an observable is considered to be internal to the monitoring layer and shall not be modified from
     * outside.
     * 
     * @return <code>true</code> if internal, <code>false</code> else
     */
    @QMInternal
    public boolean isInternal();

}
