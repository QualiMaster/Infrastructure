package eu.qualimaster.adaptation.events;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractReturnableEvent;

/**
 * The QualiMaster adaptation event base class.
 * 
 * @author Holger Eichelberger
 */
public abstract class AdaptationEvent extends AbstractReturnableEvent {

    private static final long serialVersionUID = -500444841339730486L;
    
    /**
     * If this is a wrapping event, return the wrapped event.
     * 
     * @return the wrapped event (<b>this</b>)
     */
    @QMInternal
    public AdaptationEvent unpack() {
        return this;
    }
    
    /**
     * May adjust the lifecylce of a pipeline.
     * 
     * @param failReason the reason for a failing adaptation (may be <b>null</b>)
     * @param failCode the code for a failing adaptation (may be <b>null</b>)
     * @return <code>true</code> for handled / adjusted, <code>false</code> else
     */
    @QMInternal
    public boolean adjustLifecycle(String failReason, Integer failCode) {
        return false;
    }

}
