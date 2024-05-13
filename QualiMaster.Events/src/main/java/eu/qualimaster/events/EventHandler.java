package eu.qualimaster.events;

/**
 * Describes an event handler.
 * 
 * @param <E> the type of event handled
 * @author Holger Eichelberger
 */
public abstract class EventHandler <E extends IEvent> {
    
    private Class<E> eventClass;
    
    /**
     * Creates a new event handler.
     * 
     * @param eventClass the handled class of events
     */
    protected EventHandler(Class<E> eventClass) {
        this.eventClass = eventClass;
    }
    
    /**
     * Called internally to handle a generic event.
     * 
     * @param event the event to handle
     */
    void doHandle(IEvent event) {
        handle(handles().cast(event));
    }
    
    /**
     * Returns the class of events being handled.
     * 
     * @return the handled class of events
     */
    public Class<E> handles() {
        return eventClass;
    }
    
    /**
     * Returns the name of the handled event class. In internal cases, this may
     * differ from {@link #handles()}.
     * 
     * @return the name of the handled event class
     */
    String getEventClassName() {
        return eventClass.getName();
    }
    
    /**
     * Handles an event of the supported event type.
     * 
     * @param event the event to be handled
     */
    protected abstract void handle(E event);

    /**
     * Returns whether the given event shall be consumed rather than processed, i.e., if <code>true</code> is returned, 
     * {@link #handle(IEvent)} will not be called for <code>event</code>.
     * 
     * @param event the event to be checked (not casted to E due to performance reasons)
     * @return <code>true</code> to consume to event, <code>false</code> to process
     */
    protected boolean consume(IEvent event) {
        return false;
    }
    
    /**
     * Returns whether this handler handles the given channel.
     * 
     * @param channel the channel name (may be <b>null</b>, see {@link IEvent#getChannel()})
     * @return <code>true</code> if this handler handles this channel (default), <code>false</code> else
     */
    public boolean handlesChannel(String channel) {
        return true;
    }
    
}
