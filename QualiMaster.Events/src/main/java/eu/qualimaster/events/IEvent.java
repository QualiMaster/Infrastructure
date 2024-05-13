package eu.qualimaster.events;

import java.io.Serializable;

import eu.qualimaster.common.QMInternal;

/**
 * The basic interface of an event.
 * 
 * @author Holger Eichelberger
 */
public interface IEvent extends Serializable {

    /**
     * Returns the channel this event shall be distributed over. Basically, the event bus
     * distributes the events according to their class / type name to registered
     * event handlers. In order to be more efficient, a channel (selector) can be used
     * to select the right event handlers already within the event manager, i.e., before 
     * distributing the events.
     * 
     * @return the channel (<b>null</b> for all registered event handlers)
     */
    @QMInternal
    public String getChannel();
    
}
