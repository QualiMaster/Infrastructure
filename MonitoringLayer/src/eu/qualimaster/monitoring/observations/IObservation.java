package eu.qualimaster.monitoring.observations;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * An observation represents monitored data.
 * An observation may be a single value or a composite of values (represented by 
 * keys) aggregated when calling {@link #getValue()}. Composites are important
 * when multiple parts of the infrastructure are combined to In case that a specific
 * {@link IObservation} does not support key-based observation compounds,
 * setting or incrementing values directly goes to represented value. Links are not copied, must be
 * re-established by caller. 
 * 
 * @author Holger Eichelberger
 */
public interface IObservation extends Serializable {

    /**
     * Returns whether this observation is a composite.
     * 
     * @return <code>true</code> if it is a composite, <code>false</code> else
     */
    public boolean isComposite();

    /**
     * Sets the value by replacing the existing one.
     * 
     * @param value the new value (may be <b>null</b> but then this method will perform an update of the data)
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     */
    public void setValue(double value, Object key);
    
    /**
     * Sets the value by replacing the existing one.
     * 
     * @param value the new value (may be <b>null</b> but then this method will perform an update of the data)
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     */
    public void setValue(Double value, Object key);

    /**
     * Increments the value by adding <code>value</code> to the existing one. 
     * 
     * @param value the new value (may be <b>null</b> but then this method will perform an update of the data)
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     */
    public void incrementValue(double value, Object key);
    
    /**
     * Increments the value by adding <code>value</code> to the existing one. 
     * 
     * @param value the new value (may be <b>null</b> but then this method will perform an update of the data, may 
     *   be <b>null</b> for actual value)
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     */
    public void incrementValue(Double value, Object key);

    /**
     * Returns the (aggregated) value of the observation.
     * 
     * @return the aggregated value
     */
    public double getValue();
    
    /**
     * Returns the local (not-aggregated) value of the observation.
     * 
     * @return the local value
     */
    public double getLocalValue();

    /**
     * Returns the specific component value of the observation.
     * 
     * @param key the key representing the compound in a composite observation,
     *   may be <b>null</b>
     * @return the component value (may be <b>null</b>)
     */
    public AtomicDouble getValue(Object key);

    /**
     * Returns whether the value has been set.
     * 
     * @return <code>true</code> if the value has been set, <code>false</code> else
     */
    public boolean isValueSet();
    
    /**
     * Clears this observation (for testing).
     */
    public void clear();

    /**
     * Copies this observation.
     * 
     * @param provider the actual observation provider to use instead of a potentially stored one
     * @return the copied observation
     */
    public IObservation copy(IObservationProvider provider);
    
    /**
     * Returns the timestamp of the last update.
     * 
     * @return the timestamp, negative if no value was set so far
     */
    public long getLastUpdate();
    
    /**
     * Returns the timestamp of the very first update.
     * 
     * @return the timestamp, negative if no value was set so far
     */
    public long getFirstUpdate();
    
    /**
     * Manipulates the timestamp of the last update [testing].
     * 
     * @param timestamp the timestamp, negative if no value was set
     */
    public void setLastUpdate(long timestamp);

    /**
     * Returns the number of components.
     * 
     * @return the number of components
     */
    public int getComponentCount();
    
    /**
     * Clears the specified components.
     * 
     * @param keys the keys for the components to be cleared
     */
    public void clearComponents(Collection<Object> keys);
    
    /**
     * Returns the actually used component keys.
     * 
     * @return the actually used component keys (always a copy, an unmodifiable set or <b>null</b>)
     */
    public Set<Object> getComponentKeys();

    /**
     * Links the (compounds of the) given observation into this observation. Changes
     * on both sides are reflected.
     * 
     * @param observation the observation to be linked
     * @see #unlink(IObservation)
     */
    public void link(IObservation observation);

    /**
     * Unlinks the (compounds of the) given observation into this observation. 
     * 
     * @param observation the observation to be unlinked
     * @see #link(IObservation)
     */
    public void unlink(IObservation observation);
    
    /**
     * Returns the number of links.
     * 
     * @return the number of links
     */
    public int getLinkCount();
    
    /**
     * Returns the specific link.
     * 
     * @param index the 0-based index of the link 
     * @return the link
     * @throws IndexOutOfBoundsException if <code>index &lt; 0 || index &gt;={@link #getLinkCount()}</code> 
     */
    public IObservation getLink(int index);
    
    /**
     * Returns whether this observation requires statistics calculation while reading or while writing.
     * 
     * @return <code>true</code> for reading, <code>false</code> for writing
     */
    public boolean statisticsWhileReading();

    /**
     * Called to indicate that state-based cleanup shall be performed (if needed) after an algorithm switch.
     * 
     * @param direct whether this is a direct call or an indirect call through other observations
     */
    public void switchedTo(boolean direct);
    
}
