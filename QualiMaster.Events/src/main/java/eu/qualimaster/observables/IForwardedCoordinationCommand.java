package eu.qualimaster.observables;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.IEvent;

/**
 * An event issued by the coordination layer for processing enactment in another layer.
 * Such commands contain the enactment timestamp in order to inform the adaptation layer 
 * properly about failing so that the adaptation layer can retrieve the related command
 * information from the coordination / enactment log.
 * 
 * @author Holger Eichelberger
 */
@QMInternal
public interface IForwardedCoordinationCommand extends IEvent {
    
    /**
     * Returns the enactment timestamp issued by the coordination layer (for tracking back
     * erroneous execution).
     * 
     * @return the timestamp (shall be ignored if <b>0</b> or negative)
     */
    public long getTimestamp();

}
