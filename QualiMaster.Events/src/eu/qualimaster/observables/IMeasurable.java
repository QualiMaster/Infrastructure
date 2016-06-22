package eu.qualimaster.observables;

/**
 * The basic interface for something that can be measured in terms of the observables
 * of the platform. Returning a Map of {@link IObservable} and Double might be more efficient,
 * but this interface follows more the style we briefly discussed during the technical 
 * integration telco (it's just the generic form complying to the generic monitoring layer).
 * 
 * @author Holger Eichelberger
 */
public interface IMeasurable {
    
    /**
     * Returns the measurement of an observable.
     * 
     * @param observable the observable to be measured
     * @return the observed measurement, can be <b>null</b> if not yet implemented
     */
    public Double getMeasurement(IObservable observable);
    
}
