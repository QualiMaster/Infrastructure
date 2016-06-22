package tests.eu.qualimaster.events;

import java.util.LinkedList;
import java.util.List;

import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.events.IEvent;
import eu.qualimaster.events.TimerEvent;

/**
 * Implements a simple recording event handler.
 * 
 * @param <E> the specific type of events
 * @author Holger Eichelberger
 */
class RecordingEventHandler <E extends IEvent> extends EventHandler<E> {

    private List<E> events = new LinkedList<E>();
    private String channel;
    private long interArrivalSum;
    private long lastArrival;

    /**
     * Creates the event handler.
     * 
     * @param eventClass the class of events to be handled
     */
    protected RecordingEventHandler(Class<E> eventClass) {
        this(eventClass, null);
    }

    /**
     * Creates the event handler.
     * 
     * @param eventClass the class of events to be handled
     * @param channel the channel to listen for / react on
     */
    protected RecordingEventHandler(Class<E> eventClass, String channel) {
        super(eventClass);
        this.channel = channel;
    }
    
    /**
     * Creates a recording event handler (no channel).
     * 
     * @param <E> the event type
     * @param cls the event class
     * @return the recording event handler
     */
    static <E extends IEvent> RecordingEventHandler<E> create(Class<E> cls) {
        return create(cls, null);
    }
    
    /**
     * Creates a recording event handler for timer events.
     * 
     * @return the recording event handler
     */
    static RecordingEventHandler<TimerEvent> createTimerHandler() {
        return create(TimerEvent.class, TimerEvent.CHANNEL);
    }
    
    /**
     * Creates a recording event handler.
     * 
     * @param <E> the event type
     * @param cls the event class
     * @param channel the channel to listen for / react on
     * @return the recording event handler
     */
    static <E extends IEvent> RecordingEventHandler<E> create(Class<E> cls, String channel) {
        RecordingEventHandler<E> result = new RecordingEventHandler<E>(cls, channel);
        EventManager.register(result);
        return result;
    }

    @Override
    protected void handle(E event) {
        events.add(event);
        long now = System.currentTimeMillis();
        if (lastArrival > 0) {
            interArrivalSum += (now - lastArrival);
        }
        lastArrival = now;
    }
    
    /**
     * Returns the average inter-arrival time.
     * 
     * @return the average inter-arrival time (zero if no arrivals happened)
     */
    public double getAverageInterArrivalTime() {
        double result;
        int count = getReceivedCount();
        if (count > 1) {
            result = interArrivalSum / ((double) count - 1);
        } else {
            result = 0;
        }
        return result;
    }
    
    /**
     * Returns whether this handler received <code>event</code>.
     * 
     * @param event the event to check for
     * @return <code>true</code> if event was received, <code>false</code> else
     */
    public boolean received(IEvent event) {
        return events.contains(event);
    }
    
    /**
     * Returns the number of received events.
     * 
     * @return the number of received events
     */
    public int getReceivedCount() {
        return events.size();
    }
    
    /**
     * Returns the specified received event.
     * 
     * @param index the 0-based index
     * @return the specified event
     * @throws IndexOutOfBoundsException in case that 
     *     <code>index &lt; 0 || index &gt;={@link #getReceivedCount()}</code>
     */
    public E getReceived(int index) {
        return events.get(index);
    }
    
    @Override
    public String toString() {
        return events.toString();
    }
    
    @Override
    public boolean handlesChannel(String channel) {
        boolean handles = true;
        if (null != this.channel) {
            handles = this.channel.equals(channel);
        }
        return handles;
    }
    
}