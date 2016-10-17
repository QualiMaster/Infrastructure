package eu.qualimaster.adaptation.events;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;
import eu.qualimaster.infrastructure.PipelineOptions;

/**
 * Notifies the adaptation that a resource check of a pipeline to be started shall be done.
 * 
 * @author Holger Eichelberger
 */
public class CheckBeforeStartupAdaptationEvent extends AdaptationEvent {

    private static final long serialVersionUID = -6707728566855995516L;
    private PipelineLifecycleEvent event;
    private PipelineOptions options;

    /**
     * Creates a startup check adaptation event.
     * 
     * @param event the causing lifecycle event
     */
    @QMInternal
    public CheckBeforeStartupAdaptationEvent(PipelineLifecycleEvent event) {
        this.event = event;
        PipelineOptions opts = event.getOptions();
        this.options = null == opts ? null : new PipelineOptions(opts);
    }
    
    /**
     * The name of the pipeline being affected.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return event.getPipeline();
    }

    /**
     * Returns the actual pipeline options (modifiable, will be passed back to caller).
     * 
     * @return the actual pipeline options (may be <b>null</b>)
     */
    public PipelineOptions getOptions() {
        return options;
    }
    
    @QMInternal
    @Override
    public boolean adjustLifecycle(String failReason, Integer failCode) {
        boolean adjusted = false;
        if (null == failReason && null == failCode) {
            EventManager.send(new PipelineLifecycleEvent(event, PipelineLifecycleEvent.Status.CHECKED, options));
            adjusted = true;
        } 
        return adjusted;
    }

}
