package eu.qualimaster.monitoring.observations;

import java.util.List;

import eu.qualimaster.monitoring.parts.IPartType;
import eu.qualimaster.observables.IObservable;

/**
 * Allows to add additional observables to the system state. Please note that {@link ObservationFactory}
 * must be configured accordingly.
 * 
 * @author Holger Eichelberger
 */
public interface ISystemStateConfigurer {

    /**
     * Returns additional observables for <code>type</code>.
     * 
     * @param type the system type
     * @return the additional observables (may be <b>null</b> if none shall be added)
     */
    public List<IObservable> additionalObservables(IPartType type);
    
}
