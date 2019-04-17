package eu.qualimaster.adaptation.events;

import eu.qualimaster.common.QMInternal;

/**
 * A regular adaptation event is issued regularly by the Adaptation Layer in order
 * to trigger regular adaptation actions. However, regular events may be delayed if other events must be executed 
 * before in the sequence of the event queue. In that time, regular adaptation events are not scheduled again to avoid
 * bulks of delayed adaptation events.
 *  
 * @author Holger Eichelberger
 */
public class RegularAdaptationEvent extends AdaptationEvent {

    private static final long serialVersionUID = 7890533871975194113L;
    private long timestamp;
    
    /**
     * Creates a regular adaptation event.
     */
    @QMInternal
    public RegularAdaptationEvent() {
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Returns the timestamp of this event.
     * 
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

}
